package lu.uni.trux.jucify.callgraph.symbevent;

import lu.uni.trux.jucify.callgraph.Ast;

public abstract class SetFieldEvent implements SymbolicEvent {
	public Ast.Base obj_ptr;
	public String classname;
	public String field_name;
	public Ast.Base new_value;
	
	public SetFieldEvent(Ast.Base obj_ptr, String classname, String field_name, Ast.Base new_value) {
		this.obj_ptr = obj_ptr;
		this.classname = classname;
		this.field_name = field_name;
		this.new_value = new_value;
	}
}
