package lu.uni.trux.jucify.callgraph;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map.Entry;

import org.javatuples.Pair;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;

import soot.Body;
import soot.BooleanType;
import soot.DoubleType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.RefType;
import soot.Unit;
import soot.Value;
import soot.dava.internal.javaRep.DIntConstant;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.StringConstant;

public class AstUtils {
	
	private static Object getField(GeneratedMessageV3 msg, String fieldName) {
		FieldDescriptor fieldDescriptor = msg.getDescriptorForType().findFieldByName(fieldName);
		Object value = msg.getField(fieldDescriptor);
		return value;
	}
	
	public static Pair<Local, Unit> generateLocalFromAst(Object a, Body b, Unit u) {
		Pair<Value, Unit> p = AstUtils.generateValueFromAst(a, b, u);
		Value subValue = p.getValue0();
		Unit next = p.getValue1();
		
		LocalGenerator lg = new LocalGenerator(b);
		Local local = lg.generateLocal(subValue.getType());
		AssignStmt assignStmt = Jimple.v().newAssignStmt(local, subValue);
		
		b.getUnits().insertAfter(assignStmt, next);
				
		return new Pair<Local, Unit>(local, assignStmt);
	}

	public static Pair<Value, Unit> generateNegValueFromAst(Object a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(a,b,u);
		return new Pair<Value, Unit>(Jimple.v().newEqExpr(p.getValue0(), DIntConstant.v(0, BooleanType.v())), p.getValue1());
	}
	
	@SuppressWarnings("unchecked")
	public static Pair<Value, Unit> generateValueFromAst(Object a, Body b, Unit u) {
		// Dynamic overloarding, reflection-style
		Method m;
		try {
			m = AstUtils.class.getDeclaredMethod("generateValueFromAst", a.getClass(), Body.class, Unit.class);
			
		} catch (IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			System.out.println("Code generation undefined for " + a.getClass().getName());
			System.out.println(a.toString());
			return null;
		}
		try {
			return (Pair<Value, Unit>) m.invoke(null, a, b, u);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {				
			e.printStackTrace();
			return null;
		}
	}
	
	private static Pair<Value, Unit> handleOneOf(GeneratedMessageV3 a, Body b, Unit u) {
		for(Entry<FieldDescriptor, Object> f : a.getAllFields().entrySet()) {
			return generateValueFromAst(f.getValue(), b ,u);
		}
		System.out.println("Unexpected: no field in Ast: " + a.toString());
		return null;
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Base a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Bool a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Bits a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.FP a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.String a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.VS a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Int a, Body b, Unit u) {
		return handleOneOf(a, b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BoolV a, Body b, Unit u) {
		return new Pair<Value, Unit>(DIntConstant.v((int) getField(a, "value"), BooleanType.v()), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BVV a, Body b, Unit u) {
		return new Pair<Value, Unit>(LongConstant.v((long) getField(a, "value")), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.FPV a, Body b, Unit u) {
		return new Pair<Value, Unit>(DoubleConstant.v((double) getField(a, "value")), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.StringV a, Body b, Unit u) {
		return new Pair<Value, Unit>(StringConstant.v((String) getField(a, "value")), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.VSV a, Body b, Unit u) {
		return new Pair<Value, Unit>(null, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.IntV a, Body b, Unit u) {
		return new Pair<Value, Unit>(LongConstant.v((long) getField(a, "value")), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BoolS a, Body b, Unit u) {
		Local symb = SymbolStorage.storage.getSymbol(b, (String) getField(a, "symbol"), BooleanType.v());
		return new Pair<Value, Unit>(symb, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BVS a, Body b, Unit u) {
		Local symb = SymbolStorage.storage.getSymbol(b, (String) getField(a, "symbol"), LongType.v());
		return new Pair<Value, Unit>(symb, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.FPS a, Body b, Unit u) {
		Local symb = SymbolStorage.storage.getSymbol(b, (String) getField(a, "symbol"), DoubleType.v());
		return new Pair<Value, Unit>(symb, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.StringS a, Body b, Unit u) {
		Local symb = SymbolStorage.storage.getSymbol(b, (String) getField(a, "symbol"), RefType.v());
		return new Pair<Value, Unit>(symb, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.VSS a, Body b, Unit u) {
		Local symb = SymbolStorage.storage.getSymbol(b, (String) getField(a, "symbol"), LongType.v());
		return new Pair<Value, Unit>(symb, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.IntS a, Body b, Unit u) {
		Local symb = SymbolStorage.storage.getSymbol(b, (String) getField(a, "symbol"), IntType.v());
		return new Pair<Value, Unit>(symb, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Bool___and__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newAndExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Bool___eq__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newEqExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Bool___invert__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newNegExpr(vArg1), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Bool___ne__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newNeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.Bool___or__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newOrExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___ne__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newNeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___eq__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newNeExpr(vArg1, vArg2), u);
	}

}
