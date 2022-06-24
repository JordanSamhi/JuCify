package lu.uni.trux.jucify.callgraph.symbevent;

import lu.uni.trux.jucify.callgraph.Ast.Base;

import org.javatuples.Pair;

import lu.uni.trux.jucify.callgraph.AstUtils;
import lu.uni.trux.jucify.callgraph.SymbolStorage;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;

public class GetObjectFieldEvent extends GetFieldEvent {
	
	public GetObjectFieldEvent(Base obj_ptr, String classname, String field_name) {
		super(obj_ptr, classname, field_name);
	}

	public Unit generateCode(Body b, Unit u) {		
		Pair<Value, Unit> p = AstUtils.generateValueFromAst(obj_ptr, b, u);
		Value obj = p.getValue0();
		u = p.getValue1();
		
		SootClass cls = Scene.v().getSootClass(classname);
		SootField sf = cls.getFieldByName(field_name);
		Type field_type = sf.getType();
		SootFieldRef sfref = sf.makeRef();

		Value field_value = Jimple.v().newInstanceFieldRef(obj, sfref);

		LocalGenerator lg = new LocalGenerator(b);
		Local field_local= lg.generateLocal(field_type);
		
		AssignStmt stmt = Jimple.v().newAssignStmt(field_local, field_value);
		b.getUnits().insertAfter(stmt, u);	
		u = stmt;
		
		SymbolStorage.storage.addSymbol(b, "##field##"+field_name, field_local);
		
		return u;
	}

}
