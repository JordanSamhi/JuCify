package lu.uni.trux.jucify.callgraph.symbevent;

import org.javatuples.Pair;

import lu.uni.trux.jucify.callgraph.AstUtils;
import lu.uni.trux.jucify.callgraph.Ast.Base;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;

public class SetObjectFieldEvent extends SetFieldEvent {

	public SetObjectFieldEvent(Base obj_ptr, String classname, String field_name, Base new_value) {
		super(obj_ptr, classname, field_name, new_value);
	}

	public Unit generateCode(Body b, Unit u) {
		Pair<Value, Unit> p = AstUtils.generateValueFromAst(obj_ptr, b, u);
		Value obj = p.getValue0();
		u = p.getValue1();		
		
		p = AstUtils.generateValueFromAst(new_value, b, u);
		Value new_value = p.getValue0();
		u = p.getValue1();
		
		SootClass cls = Scene.v().getSootClass(classname);
		SootField sf = cls.getFieldByName(field_name);
		SootFieldRef sfref = sf.makeRef();

		Value field_value = Jimple.v().newInstanceFieldRef(obj, sfref);

		AssignStmt stmt = Jimple.v().newAssignStmt(field_value, new_value);
		b.getUnits().insertAfter(stmt, u);	
		u = stmt;
		
		return u;
	}

}
