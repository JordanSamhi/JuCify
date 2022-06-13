package lu.uni.trux.jucify.callgraph;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.javatuples.Pair;

import lu.uni.trux.jucify.callgraph.symbevent.SymbolicEvent;
import soot.Body;
import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.internal.JNopStmt;

public class ConditionalSymbExprTree {

	public ConditionalSymbExprTree bit_0 = null;

	public ConditionalSymbExprTree bit_1 = null;
	
	public Set<Ast.Base> condition_exprs = new LinkedHashSet<Ast.Base>(); // We want iteration to preserve insertion order
	
	public List<SymbolicEvent> events = new ArrayList<SymbolicEvent>();
	
	public void addLeaf(int bits, int n_bits, List<Ast.Base> condition_exprs, SymbolicEvent event) {
		if(n_bits == 0) {
			this.condition_exprs.addAll(condition_exprs);
			this.events.add(event);
		} else {
			if((bits & 1) == 0) {
				if(this.bit_0 == null) {
					this.bit_0 = new ConditionalSymbExprTree();
				}
				this.bit_0.addLeaf(bits>>1, n_bits-1, condition_exprs, event);
			} else {
				if(this.bit_1 == null) {
					this.bit_1 = new ConditionalSymbExprTree();
				}
				this.bit_1.addLeaf(bits>>1, n_bits-1, condition_exprs, event);
			}
		}
	}
	
	public List<IfStmt> generateCondition(Body b, Unit u) {
		IfStmt ifStmt = null;
		IfStmt prevStmt = null;
		List<IfStmt> ifList = new ArrayList<IfStmt>();
		
		for(Ast.Base cond : this.condition_exprs) {
			Pair<Value, Unit> p = AstUtils.generateNegValueFromAst(cond, b, u);
			ifStmt = Jimple.v().newIfStmt(p.getValue0(), (Unit) null);
			
			b.getUnits().insertAfter(ifStmt, p.getValue1());
			u = ifStmt;
			
			if(prevStmt != null) {
				prevStmt.setTarget(ifStmt);
			}
			prevStmt = ifStmt;
			ifList.add(ifStmt);
		}
		
		return ifList;
	}
	
	public Unit generateCode(Body b, Unit u) {
		List<IfStmt> ifList = generateCondition(b, u);
		if(ifList.size() > 0)
			u = ifList.get(ifList.size()-1);
		
		for(SymbolicEvent ev : this.events) {
			u = ev.generateCode(b, u);
		}

		if(this.bit_0 != null) {
			u = this.bit_0.generateCode(b, u);
		}
		if(this.bit_1 != null) {
			u = this.bit_1.generateCode(b, u);
		}
		
		Unit nop = new JNopStmt();
		b.getUnits().insertAfter(nop, u);
		u = nop;

		for(IfStmt ifStmt : ifList)
			ifStmt.setTarget(u);

		return u;
	}
}
