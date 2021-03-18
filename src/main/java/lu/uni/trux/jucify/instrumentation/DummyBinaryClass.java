package lu.uni.trux.jucify.instrumentation;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import lu.uni.trux.jucify.utils.Constants;
import lu.uni.trux.jucify.utils.Utils;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;

/*-
 * #%L
 * JuCify
 * 
 * %%
 * Copyright (C) 2021 Jordan Samhi
 * University of Luxembourg - Interdisciplinary Centre for
 * Security Reliability and Trust (SnT) - TruX - All rights reserved
 *
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

public class DummyBinaryClass {

	private SootClass clazz;
	private static DummyBinaryClass instance;
	private int opaquePredicateCount;

	private DummyBinaryClass() {
		this.generateClass();
		this.opaquePredicateCount = 0;
	}

	public static DummyBinaryClass v() {
		if(instance == null) {
			instance = new DummyBinaryClass();
		}
		return instance;
	}
	
	private void generateClass() {
		this.clazz = new SootClass(Constants.DUMMY_BINARY_CLASS, Modifier.PUBLIC);
		this.clazz.setSuperclass(Scene.v().getSootClass(Constants.JAVA_LANG_OBJECT));
		Scene.v().addClass(this.clazz);
		this.clazz.setApplicationClass();
		this.generateInitMethod();
	}
	
	private void generateInitMethod() {
		SootMethod sm = new SootMethod(Constants.INIT,
				new ArrayList<Type>(), VoidType.v(), Modifier.PUBLIC);
		JimpleBody body = Jimple.v().newBody(sm);
		LocalGenerator lg = new LocalGenerator(body);
		sm.setActiveBody(body);
		UnitPatchingChain units = body.getUnits();
		Local thisLocal = lg.generateLocal(RefType.v(Constants.DUMMY_BINARY_CLASS));
		units.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(Constants.DUMMY_BINARY_CLASS))));
		units.add(Jimple.v().newInvokeStmt(
				Jimple.v().newSpecialInvokeExpr(thisLocal,
						Utils.getMethodRef(Constants.JAVA_LANG_OBJECT, Constants.INIT_METHOD_SUBSIG))));
		units.add(Jimple.v().newReturnVoidStmt());
		body.validate();
		this.clazz.addMethod(sm);
	}
	
	public SootMethod addBinaryMethod(String name, Type t, int modifiers, List<Type> params) {
		SootMethod sm = new SootMethod(name,
				params, t, Modifier.PUBLIC);
		JimpleBody body = Jimple.v().newBody(sm);
		LocalGenerator lg = new LocalGenerator(body);
		sm.setActiveBody(body);
		UnitPatchingChain units = body.getUnits();
		Local thisLocal = lg.generateLocal(RefType.v(Constants.DUMMY_BINARY_CLASS));
		units.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(Constants.DUMMY_BINARY_CLASS))));
		Local l = null;
		for(Type type: params) {
			l = lg.generateLocal(type);
			units.add(Jimple.v().newIdentityStmt(l, Jimple.v().newParameterRef(type, params.indexOf(type))));
		}
		Stmt ret = null;
		Local retLoc = null;
		if(t.equals(VoidType.v())) {
			ret = Jimple.v().newReturnVoidStmt();
		}else {
			retLoc = lg.generateLocal(t);
			ret = Jimple.v().newReturnStmt(retLoc);
		}
		units.add(ret);
		if(retLoc != null) {
			this.checkOpaquePredicateLocalExistence(body);
			for(Local local: body.getLocals()) {
				if(local.getType().equals(t) && !local.equals(retLoc)) {
					this.addOpaquePredicateToLastReturnStmt(body, local);
				}
			}
		}
		body.validate();
		this.clazz.addMethod(sm);
		return sm;
	}
	
	private Local checkOpaquePredicateLocalExistence(Body b) {
		for(Local l: b.getLocals()) {
			if(l.getType().equals(IntType.v()) && l.getName().equals(Constants.OPAQUE_PREDICATE_LOCAL)) {
				return l;
			}
		}
		Local loc = Jimple.v().newLocal(Constants.OPAQUE_PREDICATE_LOCAL, IntType.v());
		b.getLocals().add(loc);
		return loc;
	}
	
	public void addOpaquePredicateToLastReturnStmt(Body b, Local retLoc) {
		final PatchingChain<Unit> units = b.getUnits();
		Unit last = units.getLast();
		Local opaquePredicateLocal = checkOpaquePredicateLocalExistence(b);
		IfStmt ifStmt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(opaquePredicateLocal, IntConstant.v(opaquePredicateCount++)), last);
		units.insertBefore(ifStmt, last);
		ifStmt.setTarget(last);
		units.insertAfter(Jimple.v().newReturnStmt(retLoc), ifStmt);
		b.validate();
	}
}
