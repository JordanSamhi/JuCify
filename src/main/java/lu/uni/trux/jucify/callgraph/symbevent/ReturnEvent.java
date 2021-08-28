package lu.uni.trux.jucify.callgraph.symbevent;

import org.javatuples.Pair;

import lu.uni.trux.jucify.callgraph.Ast;
import lu.uni.trux.jucify.callgraph.AstUtils;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;

public class ReturnEvent implements SymbolicEvent {
	
	public Ast.Base return_ast;
	
	public ReturnEvent(Ast.Base return_value) {
		this.return_ast = return_value;
	}
	
	public Unit generateCode(Body b, Unit u) {
		Pair<Local, Unit> p = AstUtils.generateLocalFromAst(this.return_ast, b, u);
		Local return_value = p.getValue0();
		ReturnStmt retStmt = Jimple.v().newReturnStmt(return_value);
		b.getUnits().insertAfter(retStmt, p.getValue1());
		return retStmt;
	}
}
