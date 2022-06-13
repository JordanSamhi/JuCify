package lu.uni.trux.jucify.callgraph;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.javatuples.Pair;

import com.google.protobuf.Descriptors.FieldDescriptor;

import lu.uni.trux.jucify.utils.CustomPrints;

import com.google.protobuf.GeneratedMessageV3;

import soot.Body;
import soot.BooleanType;
import soot.DoubleType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethodRef;
import soot.SootMethodRefImpl;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.dava.internal.javaRep.DIntConstant;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.Expr;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.StringConstant;
import soot.jimple.internal.JNopStmt;

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
			CustomPrints.perror("Code generation undefined for " + a.getClass().getName());
			CustomPrints.perror(a.toString());
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
		CustomPrints.perror("Unexpected: no field in Ast: " + a.toString());
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
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___and__ a, Body b,  Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newAndExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___or__ a, Body b,  Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newOrExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___xor__ a, Body b,  Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newXorExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___add__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newAddExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___sub__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newSubExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___mul__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();
		
		return new Pair<Value, Unit>(Jimple.v().newMulExpr(vArg1, vArg2), u);
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
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_LShR a, Body b, Unit u) {				
		Pair<Local, Unit>p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local arg1 = p.getValue0();
		u = p.getValue1();
		
		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local arg2 = p.getValue0();
		u = p.getValue1();
		
		Expr shift = Jimple.v().newShrExpr(arg1, arg2);
		
		return new Pair<Value, Unit>(shift, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_Extract a, Body b, Unit u) {		
		long bot = (long) getField(a, "arg1");
		long top = (long) getField(a, "arg2");
		
		Pair<Local, Unit>p = generateLocalFromAst(getField(a, "arg3"), b, u);
		Local vArg3 = p.getValue0();
		u = p.getValue1();
		
		LocalGenerator lg = new LocalGenerator(b);
		Expr right_shift = Jimple.v().newShrExpr(vArg3, LongConstant.v(bot));
		Local tmpLocal = lg.generateLocal(right_shift.getType());
		AssignStmt assignStmt = Jimple.v().newAssignStmt(tmpLocal, right_shift);
		b.getUnits().insertAfter(assignStmt, u);
		
		Expr and = Jimple.v().newAndExpr(tmpLocal, LongConstant.v(top-bot));
		
		return new Pair<Value, Unit>(and, assignStmt);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_Concat a, Body b, Unit u) {		
		Pair<Local, Unit>p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local msb_bits = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local lsb_bits = p.getValue0();
		u = p.getValue1();

		Expr left_shift = Jimple.v().newShlExpr(msb_bits, LongConstant.v(32)); /* TODO retrieve the number of msb bits */

		LocalGenerator lg = new LocalGenerator(b);
		Local tmpLocal = lg.generateLocal(left_shift.getType());
		AssignStmt assignStmt = Jimple.v().newAssignStmt(tmpLocal, left_shift);
		b.getUnits().insertAfter(assignStmt, u);
		
		Expr add = Jimple.v().newAddExpr(lsb_bits, tmpLocal);
		
		return new Pair<Value, Unit>(add, assignStmt);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___invert__ a, Body b, Unit u) {	
		// Retrieve the value of arg1
		Pair<Local, Unit>p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local n = p.getValue0();
		u = p.getValue1();
		
		// Retrieve the Integer.reverse method
		SootClass integerCls = Scene.v().getSootClass("java.lang.Integer");
		List<Type> types = new LinkedList<Type>();
		Type intType = IntType.v();
		types.add(intType);
		SootMethodRef inverseRef = new SootMethodRefImpl(integerCls, "reverse", types, intType, true);

		// Create the Integer.reverse(arg1) statement
		Expr call = Jimple.v().newStaticInvokeExpr(inverseRef, n);
		
		return new Pair<Value, Unit>(call, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_Reverse a, Body b, Unit u) {	
		// Retrieve the value of arg1
		Pair<Local, Unit>p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local n = p.getValue0();
		u = p.getValue1();
		
		// Retrieve the Integer.reverse method
		SootClass integerCls = Scene.v().getSootClass("java.lang.Integer");
		List<Type> types = new LinkedList<Type>();
		Type intType = IntType.v();
		types.add(intType);
		SootMethodRef inverseRef = new SootMethodRefImpl(integerCls, "reverse", types, intType, true);

		// Create the Integer.reverse(arg1) statement
		Expr call = Jimple.v().newStaticInvokeExpr(inverseRef, n);
		
		return new Pair<Value, Unit>(call, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_ZeroExt a, Body b, Unit u) {	
		// In Java the number of bit is meaningless, so ignore arg1
		return generateValueFromAst(getField(a, "arg2"), b, u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___lt__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_ULT a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_SLT a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.FP___lt__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___le__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_ULE a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_SLE a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.FP___le__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newLeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___gt__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_UGT a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_SGT a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.FP___gt__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGtExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV___ge__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_UGE a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.BV_SGE a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGeExpr(vArg1, vArg2), u);
	}
	
	public static Pair<Value, Unit> generateValueFromAst(Ast.FP___ge__ a, Body b, Unit u) {
		Pair<Local, Unit> p = generateLocalFromAst(getField(a, "arg1"), b, u);
		Local vArg1 = p.getValue0();
		u = p.getValue1();

		p = generateLocalFromAst(getField(a, "arg2"), b, u);
		Local vArg2 = p.getValue0();
		u = p.getValue1();

		return new Pair<Value, Unit>(Jimple.v().newGeExpr(vArg1, vArg2), u);
	}

	public static Pair<Value, Unit> generateValueFromAst(Ast.IfBlock a, Body b, Unit u) {
		Ast.Bool condition = (Ast.Bool) getField(a, "condition");
		Ast.Base then_block = (Ast.Base) getField(a, "then_block");
		Ast.Base else_block = (Ast.Base) getField(a, "else_block");
		
		// Generate the condition value computation
		Pair<Value, Unit> condition_pair = AstUtils.generateNegValueFromAst(condition, b, u);
		
		// Generate the if statement
		// The target (the else block) will be set when the corresponding is generated
		IfStmt ifStmt = Jimple.v().newIfStmt(condition_pair.getValue0(), (Unit) null);
		b.getUnits().insertAfter(ifStmt, condition_pair.getValue1());
		
		// Generate the then block
		Pair<Value, Unit> then_stmt = AstUtils.generateValueFromAst(then_block, b, ifStmt);
		
		// Generate local to hold the final value
		LocalGenerator lg = new LocalGenerator(b);
		Local local = lg.generateLocal(then_stmt.getValue0().getType());

		// Assign the local to end the then block
		AssignStmt assignThenStmt = Jimple.v().newAssignStmt(local, then_stmt.getValue0());
		b.getUnits().insertAfter(assignThenStmt, then_stmt.getValue1());
		
		// Add a jump to skip the else
		GotoStmt jump = Jimple.v().newGotoStmt((Unit) null);
		b.getUnits().insertAfter(jump, assignThenStmt);
		
		// Generate the else block
		Pair<Value, Unit> else_stmt = AstUtils.generateValueFromAst(else_block, b, jump);

		// Set the else block as target of the if
		ifStmt.setTarget(else_stmt.getValue1());

		// Assign the local to end the then block
		AssignStmt assignElseStmt = Jimple.v().newAssignStmt(local, else_stmt.getValue0());
		b.getUnits().insertAfter(assignElseStmt, else_stmt.getValue1());		
		
		// Generate nop to be used as target for skipping the else block
		Unit nop = new JNopStmt();
		b.getUnits().insertAfter(nop, assignElseStmt);
		jump.setTarget(nop);
		
		return new Pair<Value, Unit>(local, nop);
	}
}
