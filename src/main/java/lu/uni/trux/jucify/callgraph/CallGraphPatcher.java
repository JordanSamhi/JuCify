package lu.uni.trux.jucify.callgraph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;

import com.github.dakusui.combinatoradix.Permutator;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.parse.Parser;
import lu.uni.trux.jucify.ResultsAccumulator;
import lu.uni.trux.jucify.callgraph.symbevent.InvokeEvent;
import lu.uni.trux.jucify.callgraph.symbevent.ReturnEvent;
import lu.uni.trux.jucify.instrumentation.DummyBinaryClass;
import lu.uni.trux.jucify.utils.Constants;
import lu.uni.trux.jucify.utils.CustomPrints;
import lu.uni.trux.jucify.utils.Utils;
import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;

public class CallGraphPatcher {

	private CallGraph cg;
	private boolean raw;
	private List <SootMethod> newReachableNodes;

	public CallGraphPatcher(CallGraph cg, boolean raw) {
		this.cg = cg;
		this.raw = raw;
		this.newReachableNodes = new ArrayList<SootMethod>();
	}

	public void importBinaryCallGraph(List<Pair<String, String>> files, boolean useSymbolicGeneration) {
		
		MutableGraph g = null;
		InputStream dot = null;
		String name = null,
				nameTo = null,
				nameFrom = null,
				dotFile = null,
				entrypoints = null;

		SootMethod sm = null,
				m = null,
				from = null,
				to = null;

		Map<String, SootMethod> nodesToMethods = new HashMap<String, SootMethod>();

		try {
			for(Pair<String, String> pair: files) {
				dotFile = pair.getValue0();
				entrypoints = pair.getValue1();
				dot = new FileInputStream(dotFile);
				g = new Parser().read(dot);
				if(dot == null || g == null) {
					if(!raw) {
						CustomPrints.perror("Something wrong with dot file or mutable graph");
					}
					System.exit(1);
				}

				BufferedReader is = new BufferedReader(new FileReader(entrypoints));
				List<Pair<String, SootMethod>> javaToNative = new ArrayList<Pair<String, SootMethod>>();
				Map<String, Pair<ConditionalSymbExprTree, List<SootMethod>>> nativeToJava = new HashMap<String, Pair<ConditionalSymbExprTree, List<SootMethod>>>();
				Stmt stmt = null;
				InvokeExpr ie = null;
				List<SootMethod> javaTargets = null;
				Map<SootMethod, ConditionalSymbExprTree> nativeContent = new HashMap<SootMethod, ConditionalSymbExprTree>();
				for(String line = is.readLine(); line != null; line = is.readLine()) {
					if(line.startsWith(Constants.COMMENT_ENTRYPOINTS_FILE)) {
						continue;
					}
					String[] split = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // https://stackoverflow.com/questions/15738918/splitting-a-csv-file-with-quotes-as-text-delimiter-using-string-split
					int type = Integer.parseInt(split[0].trim());
					String clazz = split[1].trim();
					String method = split[2].trim();
					String sig = split[3].trim();
					String target = split[4].trim();
					Pair<String, String> pairNewSig = Utils.compactSigtoJimpleSig(sig);
					String newSig = Utils.toJimpleSignature(clazz, pairNewSig.getValue1(), method, pairNewSig.getValue0());
					if(!Scene.v().containsMethod(newSig)) {
						Utils.addPhantomMethod(newSig);
					}
					SootMethod nativeMethod = Scene.v().getMethod(newSig);
					for(SootClass sc: Scene.v().getApplicationClasses()) {
						for(SootMethod met: sc.getMethods()) {
							if(met.isConcrete()) {
								for(Unit u: met.retrieveActiveBody().getUnits()) {
									stmt = (Stmt) u;
									if(stmt.containsInvokeExpr()) {
										ie = stmt.getInvokeExpr();
										if(ie.getMethod().equals(nativeMethod)) {
											javaToNative.add(new Pair<>(target, nativeMethod));
										}
									}
								}
							}
						}
					}

					// HANDLE NATIVE TO JAVA CALLS
					if(split.length > 12 && type == Constants.JAVA_INVOKEE) {
						String invokeeClass = split[6].trim();
						String invokeeMethod = split[7].trim();
						String invokeeSig = split[8].trim();
						
						pairNewSig = Utils.compactSigtoJimpleSig(invokeeSig);
						newSig = Utils.toJimpleSignature(invokeeClass, pairNewSig.getValue1(), invokeeMethod, pairNewSig.getValue0());
						if(!Scene.v().containsMethod(newSig)) {
							Utils.addPhantomMethod(newSig);
						}
						sm = Scene.v().getMethod(newSig);

						String argument_expressions_csv = split[11].trim();
						argument_expressions_csv = argument_expressions_csv.substring(1, argument_expressions_csv.length() - 1);
						List<String> argument_expressions = Arrays.asList(argument_expressions_csv.split("\\s*,\\s*")); //https://stackoverflow.com/questions/7488643/how-to-convert-comma-separated-string-to-list
						List<Ast.Base> parsed_arg_expressions = new ArrayList<Ast.Base>();
						for(String arg : argument_expressions) {
							if(!arg.isEmpty()) {
								Decoder decoder = Base64.getDecoder();
								Ast.Base parsed_arg = Ast.Base.parseFrom(decoder.decode(arg));
								parsed_arg_expressions.add(parsed_arg);
							}
						}

						String return_value_name = split[12].trim();

						int cond_bits = Integer.parseInt(split[13].trim());
						int cond_n_bits = Integer.parseInt(split[14].trim());
						String cond_csv = split[15].trim();
						cond_csv = cond_csv.substring(1, cond_csv.length() - 1);
						List<String> cond_expressions = Arrays.asList(cond_csv.split("\\s*,\\s*")); //https://stackoverflow.com/questions/7488643/how-to-convert-comma-separated-string-to-list
						List<Ast.Base> parsed_cond_expressions = new ArrayList<Ast.Base>();
						for(String cond : cond_expressions) {
							if(!cond.isEmpty()) {
								Decoder decoder = Base64.getDecoder();
								Ast.Base parsed_cond = Ast.Base.parseFrom(decoder.decode(cond));
								parsed_cond_expressions.add(parsed_cond);
							}
						}
						
						ConditionalSymbExprTree condTree = nativeContent.get(nativeMethod);
						if(condTree == null) {
							condTree = new ConditionalSymbExprTree();
							nativeContent.put(nativeMethod, condTree);
						}
						condTree.addLeaf(cond_bits, cond_n_bits, parsed_cond_expressions, new InvokeEvent(sm, parsed_arg_expressions, return_value_name, this));						
						
						Pair<ConditionalSymbExprTree, List<SootMethod>> p = nativeToJava.get(target);
						if(p == null) {
							javaTargets = new ArrayList<>();
							p = new Pair<ConditionalSymbExprTree, List<SootMethod>>(new ConditionalSymbExprTree(), javaTargets);
							nativeToJava.put(target, p);
						}
						javaTargets = p.getValue1();
						javaTargets.add(sm);
					}
					
					// HANDLE NATIVER RETURN VALUES
					else if (split.length > 9 && type == Constants.RETURN_VALUE) {
						Decoder decoder = Base64.getDecoder();

						String ret_value_csv = split[6].trim();
						Ast.Base parsed_ret_value = Ast.Base.parseFrom(decoder.decode(ret_value_csv));

						int cond_bits = Integer.parseInt(split[7].trim());
						int cond_n_bits = Integer.parseInt(split[8].trim());
						String cond_csv = split[9].trim();
						cond_csv = cond_csv.substring(1, cond_csv.length() - 1);
						List<String> cond_expressions = Arrays.asList(cond_csv.split("\\s*,\\s*")); // https://stackoverflow.com/questions/7488643/how-to-convert-comma-separated-string-to-list
						List<Ast.Base> parsed_cond_expressions = new ArrayList<Ast.Base>();
						for (String cond : cond_expressions) {
							if(!cond.isEmpty()) {
								Ast.Base parsed_cond = Ast.Base.parseFrom(decoder.decode(cond));
								parsed_cond_expressions.add(parsed_cond);
							}
						}

						ConditionalSymbExprTree condTree = nativeContent.get(nativeMethod);
						if (condTree == null) {
							condTree = new ConditionalSymbExprTree();
							nativeContent.put(nativeMethod, condTree);
						}
						condTree.addLeaf(cond_bits, cond_n_bits, parsed_cond_expressions,
								new ReturnEvent(parsed_ret_value));
					}
				}
				is.close();

				// GENERATE BINARY NODES THAT ARE JAVA NATIVE CALLS AND INSTRUMENT THE BODY
				for(Pair<String, SootMethod> p: javaToNative) {
					name = p.getValue0();
					m = p.getValue1();
					if(!nodesToMethods.containsKey(name)) {
						sm = DummyBinaryClass.v().addBinaryMethod(name,
								m.getReturnType(), m.getModifiers(),
								m.getParameterTypes(), !useSymbolicGeneration);
						nodesToMethods.put(name, sm);
						for(SootClass sc: Scene.v().getApplicationClasses()) {
							for(SootMethod met: sc.getMethods()) {
								if(met.hasActiveBody()) {
									Body b = met.retrieveActiveBody();
									UnitPatchingChain units = b.getUnits();
									List<Unit> newUnits = null;
									Stmt point = null;
									for(Unit u: units) {
										stmt = (Stmt) u;
										if(stmt.containsInvokeExpr()) {
											ie = stmt.getInvokeExpr();
											if(ie.getMethod().equals(m)) {
												Pair<Local, Pair<List<Unit>, Stmt>> locNewUnits = DummyBinaryClass.v().checkDummyBinaryClassLocalExistence(b, stmt);
												Local dummyBinaryClassLocal = locNewUnits.getValue0();
												Pair<List<Unit>, Stmt> newUnitsPoint = locNewUnits.getValue1();
												if(newUnitsPoint != null) {
													newUnits = newUnitsPoint.getValue0();
													point = newUnitsPoint.getValue1();
												}
												if(stmt instanceof AssignStmt) {
													AssignStmt as = (AssignStmt) stmt;
													if(sm.isStatic()) {
														as.setRightOp(Jimple.v().newStaticInvokeExpr(sm.makeRef(), ie.getArgs()));
													}else if(sm.isConstructor()){
														as.setRightOp(Jimple.v().newSpecialInvokeExpr(dummyBinaryClassLocal, sm.makeRef(), ie.getArgs()));
													}else {
														as.setRightOp(Jimple.v().newVirtualInvokeExpr(dummyBinaryClassLocal, sm.makeRef(), ie.getArgs()));
													}
												}else if(stmt instanceof InvokeStmt) {
													InvokeStmt ivs = (InvokeStmt) stmt;
													if(sm.isStatic()) {
														ivs.setInvokeExpr(Jimple.v().newStaticInvokeExpr(sm.makeRef(), ie.getArgs()));
													}else if(sm.isConstructor()){
														ivs.setInvokeExpr(Jimple.v().newSpecialInvokeExpr(dummyBinaryClassLocal, sm.makeRef(), ie.getArgs()));
													}else {
														ivs.setInvokeExpr(Jimple.v().newVirtualInvokeExpr(dummyBinaryClassLocal, sm.makeRef(), ie.getArgs()));
													}
												}
											}
											// Modify native call to newly generated call + add edge to call-graph
											if(ie.getMethod().equals(m)) {
												ie.setMethodRef(sm.makeRef());
												this.cg.addEdge(new Edge(met, stmt, sm));
												this.newReachableNodes.add(sm);
												ResultsAccumulator.v().incrementNumberNewJavaToNativeCallGraphEdges();
												if(!raw) {
													CustomPrints.pinfo(String.format("Adding java-to-native Edge from %s to %s", met, sm));
												}
											}
										}
									}
									if(newUnits != null && point != null) {
										units.insertBefore(newUnits, point);
										b.validate();
									}
								}
							}
						}
						

						// Handle Native to Java	
						if(useSymbolicGeneration) {
							ConditionalSymbExprTree tree = nativeContent.get(m);
							if(tree != null) {
								Body b = sm.retrieveActiveBody();					
								tree.generateCode(b, b.getUnits().getLast());
							}
						}						
						else {						
							Pair<ConditionalSymbExprTree, List<SootMethod>> pp = nativeToJava.get(name);
							javaTargets = pp.getValue1();
							Unit lastAdded = null,
									insertPoint = null;
							if(javaTargets != null && !javaTargets.isEmpty()) {
								List<Value> return_values = new ArrayList<>(); // List of values returned by the added invocations
								for(SootMethod met: javaTargets) {
									Type ret = met.getReturnType();
									Body b = sm.retrieveActiveBody();
									Stmt newStmt = null;
									Local local = null;
									LocalGenerator lg = new LocalGenerator(b);
									local = DummyBinaryClass.v().getOrGenerateLocal(b, this.getfirstAfterIdenditiesUnits(b), met.getDeclaringClass().getType());

									int paramLength = met.getParameterCount();
									List<Value> potentialParameters = new ArrayList<Value>();

									boolean found;
									for(Type t: met.getParameterTypes()) {
										found = false;
										for(Local l: b.getLocals()) {
											if(l.getType().equals(t)) {
												if(!potentialParameters.contains(l)) {
													potentialParameters.add(l);
													found = true;
												}
											}
										}
										if(!found) {
											potentialParameters.add(DummyBinaryClass.v().generateLocalAndNewStmt(b, this.getfirstAfterIdenditiesUnits(b), t));
										}
									}

									boolean isGoodCombi = true;
									Permutator<Value> permutator = new Permutator<Value>(potentialParameters, paramLength);
									for (List<Value> parameters : permutator) {
										isGoodCombi = true;
										for(int i = 0 ; i < paramLength ; i++) {
											if(!parameters.get(i).getType().equals(met.getParameterTypes().get(i))) {
												isGoodCombi = false;
												break;
											}
										}
										// OK NOW ADD OPAQUE PREDICATE
										if(isGoodCombi) {
											if(met.isConstructor()) {
												ie = Jimple.v().newSpecialInvokeExpr(local, met.makeRef(), parameters);
											}else if(met.isStatic()) {
												ie = Jimple.v().newStaticInvokeExpr(met.makeRef(), parameters);
											}else {
												ie = Jimple.v().newVirtualInvokeExpr(local, met.makeRef(), parameters);
											}
											if(ret.equals(VoidType.v())) {
												newStmt = Jimple.v().newInvokeStmt(ie);
											}else {
												local = lg.generateLocal(met.getReturnType());
												newStmt = Jimple.v().newAssignStmt(local, ie);
												return_values.add(local);
											}
											if(newStmt != null) {
												if(lastAdded == null) {
													insertPoint = this.getfirstAfterIdenditiesUnitsAfterInit(b);
													b.getUnits().insertBefore(newStmt, insertPoint);
												}else {
													insertPoint = lastAdded;
													b.getUnits().insertAfter(newStmt, insertPoint);
												}
												lastAdded = newStmt;
												if(permutator.size() > 1) {
													DummyBinaryClass.v().addOpaquePredicate(b, b.getUnits().getSuccOf(newStmt), newStmt);
												}
											}
										}
									}

									Edge e = new Edge(sm, newStmt, met);
									this.cg.addEdge(e);
									this.newReachableNodes.add(met);
									ResultsAccumulator.v().incrementNumberNewNativeToJavaCallGraphEdges();
									if(!raw) {
										CustomPrints.pinfo(String.format("Adding native-to-java Edge from %s to %s", sm, met));
									}
									
									if(!sm.getReturnType().equals(VoidType.v())) {
										// FIX MULTIPLE RETURN OF SAME TYPE (OPAQUE PREDICATE)
										final Local retLoc = local;
										DummyBinaryClass.v().addOpaquePredicateForReturn(b, b.getUnits().getLast(), Jimple.v().newReturnStmt(retLoc));
									}
								}
							}
						}
						
						sm.retrieveActiveBody().validate();
						System.out.println(sm.retrieveActiveBody().toString());
					}						
				}

				// GENERATE BINARY NODES INTO SOOT CALL GRAPH
				for(MutableNode node: g.nodes()) {
					name = Utils.removeNodePrefix(node.name().toString());
					if(!nodesToMethods.containsKey(name)) {
						sm = DummyBinaryClass.v().addBinaryMethod(name, VoidType.v(), Modifier.PUBLIC, new ArrayList<>(), !useSymbolicGeneration);
						nodesToMethods.put(name, sm);
					}
				}

				// ADD EDGE FROM INITIAL BINARY CALL-GRAPH
				for(Link l: g.edges()) {
					nameFrom = Utils.removeNodePrefix(l.from().name().toString());
					nameTo = Utils.removeNodePrefix(l.to().name().toString());
					from = nodesToMethods.get(nameFrom);
					to = nodesToMethods.get(nameTo);
					stmt = (Stmt) Utils.addMethodCall(from, to);
					if(stmt != null) {
						Edge e = new Edge(from, stmt, to);
						this.cg.addEdge(e);
						this.newReachableNodes.add(to);
					}
				}
			}

		} catch (IOException e) {
			if(!raw) {
				CustomPrints.perror(e.getMessage());
			}
			System.exit(1);
		}
	}

	private Unit getfirstAfterIdenditiesUnits(Body b) {
		UnitPatchingChain units = b.getUnits();
		Unit u = null;
		Iterator<Unit> it = units.iterator();
		u = it.next();
		while(u instanceof IdentityStmt) {
			u = it.next();
		}
		return u;
	}

	private Unit getfirstAfterIdenditiesUnitsAfterInit(Body b) {
		UnitPatchingChain units = b.getUnits();
		Unit u = null;
		Iterator<Unit> it = units.iterator();
		u = it.next();
		while(u instanceof IdentityStmt) {
			u = it.next();
		}
		boolean found = false;
		while(!found) {
			if(u instanceof AssignStmt) {
				AssignStmt as = (AssignStmt) u;
				Value rop = as.getRightOp();
				if(rop instanceof NewExpr) {
					u = it.next();
					if(u instanceof InvokeStmt) {
						InvokeStmt is = (InvokeStmt) u;
						if(is.getInvokeExpr().getMethod().getSubSignature().equals(Constants.INIT_METHOD_SUBSIG)) {
							u = it.next();
							continue;
						}
					}
				}
			}
			found = true;
		}
		return u;
	}

	public void dotifyCallGraph(String destination) {
		DotGraph dg = new DotGraph(Constants.GRAPH_NAME);
		Iterator<Edge> it = this.cg.iterator();
		Edge next = null;
		while(it.hasNext()) {
			next = it.next();
			dg.drawEdge(next.src().getName(), next.tgt().getName());
		}
		dg.plot(destination);
	}
	
	public CallGraph getCg() {
		return this.cg;
	}

	public List<SootMethod> getNewReachableNodes() {
		return newReachableNodes;
	}

	public void setNewReachableNodes(List<SootMethod> newReachableNodes) {
		this.newReachableNodes = newReachableNodes;
	}
	
	public List<SootMethod> getNewReachableNodesNative() {
		return getNewReachableNodes(true);
	}
	
	public List<SootMethod> getNewReachableNodesJava() {
		return getNewReachableNodes(false);
	}
	
	private List<SootMethod> getNewReachableNodes(boolean b) {
		List<SootMethod> s = new ArrayList<SootMethod>();
		for(SootMethod sm: this.newReachableNodes) {
			if(sm.getDeclaringClass().getType().equals(RefType.v(Constants.DUMMY_BINARY_CLASS)) && b) {
				if(!s.contains(sm)) {
					s.add(sm);
				}
			}else if (!sm.getDeclaringClass().getType().equals(RefType.v(Constants.DUMMY_BINARY_CLASS)) && !b){
				if(!s.contains(sm) && !Utils.wasMethodPreviouslyReachableInCallGraph(cg, sm)) {
					s.add(sm);
				}
			}
		}
		return s;
	}
}
