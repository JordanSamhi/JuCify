package lu.uni.trux.jucify.callgraph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javatuples.Pair;

import com.github.dakusui.combinatoradix.Permutator;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.parse.Parser;
import lu.uni.trux.jucify.ResultsAccumulator;
import lu.uni.trux.jucify.instrumentation.DummyBinaryClass;
import lu.uni.trux.jucify.utils.Constants;
import lu.uni.trux.jucify.utils.CustomPrints;
import lu.uni.trux.jucify.utils.Utils;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.LongType;
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
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
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

	public void importBinaryCallGraph(List<Pair<String, String>> files) {
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

	    Pattern list_pattern = Pattern.compile(".*\\[(.*)\\].*");	

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
				Map<String, List<Pair<SootMethod,List<String>>>> nativeToJava = new HashMap<String, List<Pair<SootMethod,List<String>>>>();
				Stmt stmt = null;
				InvokeExpr ie = null;
				List<Pair<SootMethod,List<String>>> javaTargets = null;
				for(String line = is.readLine(); line != null; line = is.readLine()) {
					if(line.startsWith(Constants.HEADER_ENTRYPOINTS_FILE)) {
						continue;
					}
					String[] split = line.split(",");
					String clazz = split[0].trim();
					String method = split[1].trim();
					String sig = split[2].trim();
					String target = split[3].trim();
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
					if(split.length > 9) {
						String invokeeClass = split[5].trim();
						String invokeeMethod = split[6].trim();
						String invokeeSig = split[7].trim();
						Matcher matcher = list_pattern.matcher(line);
						List<String> argument_expressions = new ArrayList<>();;
						if(matcher.find()) {
							String match = matcher.group(1);
							if(!match.equals(""))
								argument_expressions = Arrays.asList(match.split(","));
						}
						pairNewSig = Utils.compactSigtoJimpleSig(invokeeSig);
						newSig = Utils.toJimpleSignature(invokeeClass, pairNewSig.getValue1(), invokeeMethod, pairNewSig.getValue0());
						if(!Scene.v().containsMethod(newSig)) {
							Utils.addPhantomMethod(newSig);
						}
						sm = Scene.v().getMethod(newSig);
						javaTargets = nativeToJava.get(target);
						if(javaTargets == null) {
							javaTargets = new ArrayList<>();
							nativeToJava.put(target, javaTargets);
						}
						javaTargets.add(new Pair<>(sm, argument_expressions));
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
								m.getParameterTypes());
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
						javaTargets = nativeToJava.get(name);
						Unit lastAdded = null,
								insertPoint = null;
						if(javaTargets != null && !javaTargets.isEmpty()) {
							List<Value> return_values = new ArrayList<>(); // List of values returned by the added invocations
							for(Pair<SootMethod, List<String>> invoke_info: javaTargets) {
								SootMethod met = invoke_info.getValue0();
								List<String> argument_expressions = invoke_info.getValue1();
								Type ret = met.getReturnType();
								Body b = sm.retrieveActiveBody();
								Stmt newStmt = null;
								Local local = null;
								LocalGenerator lg = new LocalGenerator(b);
								local = DummyBinaryClass.v().getOrGenerateLocal(b, this.getfirstAfterIdenditiesUnits(b), met.getDeclaringClass().getType());

								// First try to use symbols to generate invokee arguments
								boolean symbolic_generation_done = false;
								if(argument_expressions.size() > 0) {
									boolean parsing_ok = true;
									// Construct parameters using symbols
									List<Value> parameters = new ArrayList<Value>();
									int arg_number = 0;
									Pattern pattern = Pattern.compile("<BV32 ([a-zA-Z0-9_#]+)>");
									for(String arg_angr_str : argument_expressions) {	
										Type arg_type = met.getParameterType(arg_number);
										
										// TODO: Only simple symbols (BV32) are currently parsed
										// Full angr expression should be parsed
								        Matcher matcher = pattern.matcher(arg_angr_str);
										if(!matcher.find()) {
											parsing_ok = false;
											break;
										}
										String symbol_name = matcher.group(1);			
										Value corresponding_jimple_value = null;
										if(symbol_name.startsWith("param")) {
											int parameter_number = Integer.parseInt(symbol_name.split("_")[1].substring(1));
											corresponding_jimple_value = sm.getActiveBody().getParameterLocal(parameter_number);
										}
										else if(symbol_name.startsWith("return")) {
											int return_number = Integer.parseInt(symbol_name.split("_")[1].substring(1)) - 1;
											System.out.println(symbol_name);
											System.out.println(return_number);
											System.out.println(return_values);
											if(return_values.size() > return_number) {
												corresponding_jimple_value = return_values.get(return_number);												
											} else {
												parsing_ok = false;
												break;												
											}
										}
										else if(symbol_name.startsWith("0x")) {
											int immediate_number = Integer.parseInt(symbol_name.substring(2), 16);
											if(arg_type instanceof IntType) {
												corresponding_jimple_value = IntConstant.v(immediate_number);
											} else if (arg_type instanceof LongType) {
												corresponding_jimple_value = LongConstant.v(immediate_number);
											} else {
												parsing_ok = false;
												break;	
											}
										} else {
											parsing_ok = false;
											break;											
										}										
										
										if(corresponding_jimple_value != null) {
											parameters.add(corresponding_jimple_value);
										} else {
											parsing_ok = false;
											break;											
										}
										arg_number++;
									}
									
									if(parsing_ok) {
										// Create corresponding invoke statement
										if(met.isConstructor()) {
											ie = Jimple.v().newSpecialInvokeExpr(local, met.makeRef(), parameters);
										}else if(met.isStatic()) {
											ie = Jimple.v().newStaticInvokeExpr(met.makeRef(), parameters);
										}else {
											ie = Jimple.v().newVirtualInvokeExpr(local, met.makeRef(), parameters);
										}
									
										// Stores return value if any
										if(ret.equals(VoidType.v())) {
											newStmt = Jimple.v().newInvokeStmt(ie);
										}else {
											local = lg.generateLocal(met.getReturnType());
											newStmt = Jimple.v().newAssignStmt(local, ie);
											return_values.add(local);
										}
									
										// Add statements to the method body
										if(newStmt != null) {
											if(lastAdded == null) {
												insertPoint = this.getfirstAfterIdenditiesUnitsAfterInit(b);
												b.getUnits().insertBefore(newStmt, insertPoint);
											}else {
												insertPoint = lastAdded;
												b.getUnits().insertAfter(newStmt, insertPoint);
											}
											lastAdded = newStmt;
										}
									
										symbolic_generation_done = true;
									}
								}
								
								// If symbol usage failed, go to fallback mode (permutations and opaque predicate)
								if(!symbolic_generation_done){
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
								
								b.validate();
							}
						}
					}						
				}

				// GENERATE BINARY NODES INTO SOOT CALL GRAPH
				for(MutableNode node: g.nodes()) {
					name = Utils.removeNodePrefix(node.name().toString());
					if(!nodesToMethods.containsKey(name)) {
						sm = DummyBinaryClass.v().addBinaryMethod(name, VoidType.v(), Modifier.PUBLIC, new ArrayList<>());
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
