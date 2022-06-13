package lu.uni.trux.jucify.callgraph.symbevent;

import org.javatuples.Pair;

import lu.uni.trux.jucify.callgraph.Ast;
import lu.uni.trux.jucify.callgraph.AstUtils;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.Stmt;

public class ReturnEvent implements SymbolicEvent {
	
	public Ast.Base return_ast;
	
	public ReturnEvent(Ast.Base return_value) {
		this.return_ast = return_value;
	}
	
	public Unit generateCode(Body b, Unit u) {
		Stmt retStmt;
		if(b.getMethod().getReturnType() != VoidType.v()) {		
			Pair<Local, Unit> p = AstUtils.generateLocalFromAst(this.return_ast, b, u);
			u = p.getValue1();
			Local return_value = p.getValue0();
			retStmt = Jimple.v().newReturnStmt(return_value);
		} else {
			retStmt = Jimple.v().newReturnVoidStmt();
		}
		b.getUnits().insertAfter(retStmt, u);
		return retStmt;
	}
}
