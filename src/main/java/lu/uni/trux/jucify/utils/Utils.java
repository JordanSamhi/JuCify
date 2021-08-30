package lu.uni.trux.jucify.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;

import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

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

public class Utils {

	private static Map<String, String> compactTypesToJimpleTypes = null;

	public static String removeNodePrefix(String s) {
		if(s.startsWith(Constants.NODE_PREFIX)) {
			return s.substring(5, s.length());
		}
		return s;
	}

	public static SootMethodRef getMethodRef(String className, String methodName) {
		return Scene.v().getSootClass(className).getMethod(methodName).makeRef();
	}

	public static Unit addMethodCall(SootMethod caller, SootMethod callee) {
		Body b = caller.retrieveActiveBody();
		final PatchingChain<Unit> units = b.getUnits();
		Local thisLocal = b.getThisLocal();
		Unit newUnit = Jimple.v().newInvokeStmt(
				Jimple.v().newSpecialInvokeExpr(thisLocal,
						Utils.getMethodRef(Constants.DUMMY_BINARY_CLASS, callee.getSubSignature())));
		units.insertBefore(newUnit, units.getLast());
		return newUnit;
	}

	public static Pair<String, String> compactSigtoJimpleSig(String sig) {
		sig = sig.trim();
		String[] split = sig.split("\\)");
		System.out.println(sig);
		String ret = split[1];
		String[] splitSplit = split[0].split("\\(");
		String params = null;
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if(splitSplit.length != 0) {
			params = splitSplit[1];
			List<String> paramsList = new ArrayList<String>();
			int i = 0;
			StringBuilder tmpStr = null;
			while(i < params.length()) {
				char c = params.charAt(i);
				if(c == ' ') {
					i++;
					continue;
				}
				if(c == 'L') {
					tmpStr = new StringBuilder();
					while(c != ';') {
						c = params.charAt(i);
						tmpStr.append(c);
						i++;
					}
					paramsList.add(getCompactTypesToJimpleTypes(tmpStr.toString()));
				}else if (c == '[') {
					tmpStr = new StringBuilder();
					tmpStr.append(params.charAt(++i));
					paramsList.add(String.format("%s[]", tmpStr.toString()));
				}
				else {
					paramsList.add(getCompactTypesToJimpleTypes(String.valueOf(c)));
					i++;
				}
			}
			sb.append(String.join(",", paramsList));
		}
		sb.append(")");
		ret = getCompactTypesToJimpleTypes(ret);
		return new Pair<String, String>(sb.toString(), ret);
	}

	private static String getCompactTypesToJimpleTypes(String key) {
		if(compactTypesToJimpleTypes == null) {
			compactTypesToJimpleTypes = new HashMap<String, String>();
			compactTypesToJimpleTypes.put("V", "void");
			compactTypesToJimpleTypes.put("Z", "boolean");
			compactTypesToJimpleTypes.put("B", "byte");
			compactTypesToJimpleTypes.put("C", "char");
			compactTypesToJimpleTypes.put("S", "short");
			compactTypesToJimpleTypes.put("I", "int");
			compactTypesToJimpleTypes.put("J", "long");
			compactTypesToJimpleTypes.put("F", "float");
			compactTypesToJimpleTypes.put("D", "double");
		}
		if(key.startsWith("L")) {
			return key.substring(1, key.length() - 1).replace("/", ".");
		}else if(key.startsWith("[")) {
			if(key.contains(";")) {
				return String.format("%s[]", key.substring(2, key.length() - 1).replace("/", "."));
			}else {
				return String.format("%s[]", key.substring(1, key.length() - 1).replace("/", "."));
			}
		}
		return compactTypesToJimpleTypes.get(key);
	}

	public static Type getTypeFromString(String type) {
		if(type.equals("boolean")) {
			return BooleanType.v();
		}else if(type.equals("byte")) {
			return ByteType.v();
		}else if(type.equals("char")) {
			return CharType.v();
		}else if(type.equals("short")) {
			return ShortType.v();
		}else if(type.equals("int")) {
			return IntType.v();
		}else if(type.equals("long")) {
			return LongType.v();
		}else if(type.equals("float")) {
			return FloatType.v();
		}else if(type.equals("double")) {
			return DoubleType.v();
		}else if(type.equals("void")) {
			return VoidType.v();
		}else {
			return RefType.v(type);
		}
	}

	public static String toJimpleSignature(String clazz, String ret, String method, String params) {
		return String.format("<%s: %s %s%s>", clazz, ret, method, params);
	}

	public static String getClassNameFromSignature(String sig) {
		String tmp = sig.split(" ")[0];
		return tmp.substring(1, tmp.length() - 1);
	}

	public static String getMethodNameFromSignature(String sig) {
		String tmp = sig.split(" ")[2];
		return tmp.substring(0, tmp.indexOf("("));
	}

	public static String getReturnNameFromSignature(String sig) {
		return sig.split(" ")[1];
	}

	public static List<String> getParametersNamesFromSignature(String sig) {
		String tmp = sig.split(" ")[2];
		String params = tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")"));
		String[] paramsArray = params.split(",");
		List<String> parameters = new ArrayList<String>();
		String p = null;
		for(int i = 0 ; i < paramsArray.length ; i++) {
			p = paramsArray[i];
			if(!p.isEmpty()) {
				parameters.add(p);
			}
		}
		return parameters;
	}

	public static boolean isFromNativeCode(SootMethod sm) {
		if(sm.getDeclaringClass().equals(Scene.v().getSootClass(Constants.DUMMY_BINARY_CLASS))) {
			return true;
		}
		return false;
	}

	public static void addPhantomMethod(String newSig) {
		String className = Utils.getClassNameFromSignature(newSig),
				methodName = Utils.getMethodNameFromSignature(newSig),
				returnType = Utils.getReturnNameFromSignature(newSig);
		List<String> paramsList = Utils.getParametersNamesFromSignature(newSig);
		addPhantomMethod(className, methodName, returnType, paramsList);
	}

	public static void addPhantomMethod(String className, String methodName, String returnType, List<String> paramsList) {
		List<Type> params = new ArrayList<Type>();
		for(String s: paramsList) {
			params.add(Utils.getTypeFromString(s));
		}
		SootClass sc = Scene.v().getSootClass(className);
		SootMethod sm = new SootMethod(methodName, params, Utils.getTypeFromString(returnType));
		sm.setPhantom(true);
		sc.addMethod(sm);
	}

	public static boolean wasMethodPreviouslyReachableInCallGraph(CallGraph cg, SootMethod sm) {
		Iterator<Edge> it = cg.edgesInto(sm);
		Edge next = null;
		boolean found = false;
		while(it.hasNext()) {
			next = it.next();
			if(!next.src().getDeclaringClass().getType().equals(RefType.v(Constants.DUMMY_BINARY_CLASS))) {
				found = true;
				break;
			}
		}
		return found;
	}
	
	public static List<SootMethod> wereSuccPreviouslyReachable(Iterator<Edge> edgesOutOf, CallGraph cg) {
		Edge next = null;
		List<SootMethod> methods = new ArrayList<SootMethod>();
		SootMethod tgt = null;
		while(edgesOutOf.hasNext()) {
			next = edgesOutOf.next();
			tgt = next.tgt();
			if(!methods.contains(tgt)) {
				if(!Utils.wasMethodPreviouslyReachableInCallGraph(cg, tgt)) {
					methods.add(tgt);
				}
			}
			methods.addAll(Utils.wereSuccPreviouslyReachable(cg.edgesOutOf(tgt), cg));
		}
		return methods;
	}

	public static int getNumberOfNodesInCG(CallGraph cg) {
		Iterator<Edge> it = cg.iterator();
		Edge next = null;
		MethodOrMethodContext src = null,
				tgt = null;
		List<MethodOrMethodContext> methods = new ArrayList<MethodOrMethodContext>();
		while(it.hasNext()) {
			next = it.next();
			src = next.getSrc();
			tgt = next.getTgt();
			if(!methods.contains(src)) {
				methods.add(src);
			}
			if(!methods.contains(tgt)) {
				methods.add(tgt);
			}
		}
		return methods.size();
	}

	public static int getNumberOfEdgesInCG(CallGraph cg) {
		return cg.size();
	}
	
	public static void exportCallGraphTxT(CallGraph cg, String destination) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(destination);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} 
		Iterator<Edge> it = cg.iterator();
		Iterator<Edge> itMet = null;
		List<String> tgts = new ArrayList<String>();
		Edge next = null;
		SootMethod tgt = null;
		List<SootMethod> visitedMethods = new ArrayList<SootMethod>();
		while(it.hasNext()) {
			next = it.next();
			SootMethod src = next.src();
			if(!visitedMethods.contains(src)) {
				visitedMethods.add(src);
				itMet = cg.edgesOutOf(src);
				tgts.clear();
				while(itMet.hasNext()) {
					tgt = itMet.next().tgt();
					if(tgt.isDeclared()) {
						if(!tgts.contains(tgt.getSignature())) {
							tgts.add(tgt.getSignature());
						}
					}
				}
				if(src.isDeclared()) {
					try {
						writer.write(String.format("%s ==> ['%s\\n']%s", src, String.join("\\n','", tgts), System.lineSeparator()));
					} catch (IOException e) {
						System.err.println(e.getMessage());
					}
				}
			}
		}
		try {
			writer.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
}