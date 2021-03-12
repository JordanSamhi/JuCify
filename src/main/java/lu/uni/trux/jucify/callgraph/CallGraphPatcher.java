package lu.uni.trux.jucify.callgraph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.parse.Parser;
import lu.uni.trux.jucify.instrumentation.DummyBinaryClass;
import lu.uni.trux.jucify.utils.Constants;
import lu.uni.trux.jucify.utils.Utils;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.VoidType;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;

public class CallGraphPatcher {

	private CallGraph cg;

	public CallGraphPatcher(CallGraph cg) {
		this.cg = cg;
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
					System.err.println("Something wrong with dot file or mutable graph");
					System.exit(1);
				}


				BufferedReader is = new BufferedReader(new FileReader(entrypoints));
				List<Edge> edges = new ArrayList<Edge>();
				List<SootMethod> toAdd = new ArrayList<SootMethod>();
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
					String newSig = String.format("<%s: %s %s%s>", clazz, pairNewSig.getValue1(), method, pairNewSig.getValue0());
					SootMethod nativeMethod = Scene.v().getMethod(newSig);
					for(SootClass sc: Scene.v().getApplicationClasses()) {
						for(SootMethod met: sc.getMethods()) {
							if(met.hasActiveBody()) {
								for(Unit u: met.retrieveActiveBody().getUnits()) {
									//TODO change this test to be more precise
									if(u.toString().contains(nativeMethod.getName())) {
										toAdd.add(nativeMethod);
									}
								}
							}
						}
					}
				}
				is.close();

				for(SootMethod m: toAdd) {
					if(!nodesToMethods.containsKey(name)) {
						sm = DummyBinaryClass.v().addBinaryMethod(m.getName(),
								m.getReturnType(), m.getModifiers(),
								m.getParameterTypes());
						nodesToMethods.put(m.getName(), sm);
					}
					for(SootClass sc: Scene.v().getApplicationClasses()) {
						for(SootMethod met: sc.getMethods()) {
							if(met.hasActiveBody()) {
								for(Unit u: met.retrieveActiveBody().getUnits()) {
									//TODO change this test to be more precise
									if(u.toString().contains(m.getName())) {
										edges.add(new Edge(met, (Stmt) u, m));
									}
								}
							}
						}
					}
				}

				for(MutableNode node: g.nodes()) {
					name = Utils.removeNodePrefix(node.name().toString());
					if(!nodesToMethods.containsKey(name)) {
						sm = DummyBinaryClass.v().addBinaryMethod(name, VoidType.v(), Modifier.PUBLIC, new ArrayList<>());
						nodesToMethods.put(name, sm);
					}
				}

				for(Link l: g.edges()) {
					nameFrom = Utils.removeNodePrefix(l.from().name().toString());
					nameTo = Utils.removeNodePrefix(l.to().name().toString());
					from = nodesToMethods.get(nameFrom);
					to = nodesToMethods.get(nameTo);
					Stmt stmt = (Stmt) Utils.addMethodCall(from, to);
					if(stmt != null) {
						Edge e = new Edge(from, stmt, to);
						this.cg.addEdge(e);
					}
				}

				for(Edge e: edges) {
					System.out.println(e);
					this.cg.addEdge(e);
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
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
}
