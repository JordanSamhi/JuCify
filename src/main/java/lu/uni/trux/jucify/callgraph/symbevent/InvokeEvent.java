package lu.uni.trux.jucify.callgraph.symbevent;

import java.util.ArrayList;
import java.util.List;

import org.javatuples.Pair;

import lu.uni.trux.jucify.ResultsAccumulator;
import lu.uni.trux.jucify.callgraph.Ast;
import lu.uni.trux.jucify.callgraph.AstUtils;
import lu.uni.trux.jucify.callgraph.CallGraphPatcher;
import lu.uni.trux.jucify.callgraph.SymbolStorage;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;

public class InvokeEvent implements SymbolicEvent {
	
	public SootMethod callee;
	public List<Ast.Base> arguments;
	public String return_value_name;
	public CallGraphPatcher associatedCallGraphPatcher;
	
	public InvokeEvent(SootMethod callee, List<Ast.Base> arguments, String return_value_name, CallGraphPatcher associatedCallGraphPatcher) {
		this.callee = callee;
		this.arguments = arguments;
		this.return_value_name = return_value_name;
		this.associatedCallGraphPatcher = associatedCallGraphPatcher;
	}

	public Unit generateCode(Body b, Unit u) {
		List<Local> parameters = new ArrayList<Local>();
		for(Ast.Base arg : this.arguments) {
			Pair<Local, Unit> p = AstUtils.generateLocalFromAst(arg, b, u);
			parameters.add(p.getValue0());
			u = p.getValue1();
		}
		
		InvokeExpr ie;
		if(this.callee.isConstructor()) {
			ie = Jimple.v().newSpecialInvokeExpr(parameters.get(0), this.callee.makeRef(), parameters.subList(1, parameters.size()));
		} else if(this.callee.isStatic()) {
			ie = Jimple.v().newStaticInvokeExpr(this.callee.makeRef(), parameters.subList(1, parameters.size()));
		} else {
			ie = Jimple.v().newVirtualInvokeExpr(parameters.get(0), this.callee.makeRef(), parameters.subList(1, parameters.size()));
		}

		Stmt stmt = null;
		if(this.callee.getReturnType().equals(VoidType.v())) {
			stmt = Jimple.v().newInvokeStmt(ie);
		} else {
			LocalGenerator lg = new LocalGenerator(b);
			Local local = lg.generateLocal(this.callee.getReturnType());
			local.setName(return_value_name);
			stmt = Jimple.v().newAssignStmt(local, ie);
			if(!this.return_value_name.isEmpty())
				SymbolStorage.storage.addSymbol(b, this.return_value_name, local);
		}
		b.getUnits().insertAfter(stmt, u);
		
		System.out.println("Adding " + b.getMethod().getName() + " to " + this.callee.getName());
		Edge e = new Edge(b.getMethod(), stmt, this.callee);
		this.associatedCallGraphPatcher.getCg().addEdge(e);
		this.associatedCallGraphPatcher.getNewReachableNodes().add(this.callee);
		ResultsAccumulator.v().incrementNumberNewNativeToJavaCallGraphEdges();

		return stmt;
	}
}
