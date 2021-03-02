package lu.uni.trux.jucify.callgraph;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.parse.Parser;
import lu.uni.trux.jucify.instrumentation.DummyBinaryClass;
import lu.uni.trux.jucify.utils.Constants;
import lu.uni.trux.jucify.utils.Utils;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;

public class CallGraphPatcher {

	private CallGraph cg;
	
	public CallGraphPatcher(CallGraph cg) {
		this.cg = cg;
	}
	
	public void importBinaryCallGraph(String dotFile) {
		InputStream dot = null;
		MutableGraph g = null;
		try {
			dot = new FileInputStream(dotFile);
			g = new Parser().read(dot);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		if(dot == null || g == null) {
			System.err.println("Something wrong with dot file or mutable graph");
			System.exit(1);
		}
		
		String name = null,
				nameTo = null,
				nameFrom = null;
		SootMethod sm = null,
				from = null,
				to = null;
		
		Map<String, SootMethod> nodesToMethods = new HashMap<String, SootMethod>();
		for(MutableNode node: g.nodes()) {
			name = Utils.removeNodePrefix(node.name().toString());
			sm = DummyBinaryClass.v().addCMethod(name);
			nodesToMethods.put(name, sm);
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
