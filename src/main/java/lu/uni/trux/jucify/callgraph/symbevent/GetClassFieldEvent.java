package lu.uni.trux.jucify.callgraph.symbevent;

import lu.uni.trux.jucify.callgraph.SymbolStorage;
import lu.uni.trux.jucify.callgraph.Ast.Base;
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

public class GetClassFieldEvent extends GetFieldEvent {
	
	public GetClassFieldEvent(Base obj_ptr, String classname, String field_name) {
		super(obj_ptr, classname, field_name);
	}

	public Unit generateCode(Body b, Unit u) {		
		SootClass cls = Scene.v().getSootClass(classname);
		SootField sf = cls.getFieldByName(field_name);
		Type field_type = sf.getType();
		SootFieldRef sfref = sf.makeRef();

		Value field_value = Jimple.v().newStaticFieldRef(sfref);

		LocalGenerator lg = new LocalGenerator(b);
		Local field_local= lg.generateLocal(field_type);
		
		AssignStmt stmt = Jimple.v().newAssignStmt(field_local, field_value);
		b.getUnits().insertAfter(stmt, u);	
		u = stmt;
		
		SymbolStorage.storage.addSymbol(b, "##field##"+field_name, field_local);
		
		return u;
	}

}
