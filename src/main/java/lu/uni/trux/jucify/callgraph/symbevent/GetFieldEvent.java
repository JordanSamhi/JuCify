package lu.uni.trux.jucify.callgraph.symbevent;

import lu.uni.trux.jucify.callgraph.Ast;

public abstract class GetFieldEvent implements SymbolicEvent {	
	public Ast.Base obj_ptr;
	public String classname;
	public String field_name;

	public GetFieldEvent(Ast.Base obj_ptr, String classname, String field_name) {
		this.obj_ptr = obj_ptr;
		this.classname = classname;
		this.field_name = field_name;
	}
}
