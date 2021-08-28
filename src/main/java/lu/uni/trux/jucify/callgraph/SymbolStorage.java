package lu.uni.trux.jucify.callgraph;

import java.util.HashMap;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.Type;
import soot.UnknownType;
import soot.javaToJimple.LocalGenerator;

public class SymbolStorage {

	public Map<Body, Map<String, Local>> stores = new HashMap<Body, Map<String, Local>>();

	public static SymbolStorage storage = new SymbolStorage();

	private SymbolStorage() {
	}
	
	public Local getSymbol(Body b, String symbolName) {
		return getSymbol(b, symbolName, UnknownType.v());
	}
	public Local getSymbol(Body b, String symbolName, Type defaultType) {
		Map<String, Local> s = stores.get(b);
		if (s != null) {
			Local l = s.get(symbolName);
			if(l != null)
				return l;

			if(symbolName.startsWith("param_#")) {
				l = s.get("param_" + symbolName.split("_")[1]);
				if(l != null)
					return l;
			}
		}

		LocalGenerator lg = new LocalGenerator(b);
		Local local = lg.generateLocal(defaultType);
		local.setName(symbolName);
		s.put(symbolName, local);
		return local;
	}

	public void addSymbol(Body b, String symbolName, Local symbol) {
		Map<String, Local> s = stores.get(b);
		if(s == null) {
			s = new HashMap<String, Local>();
		}
		s.put(symbolName, symbol);
		stores.put(b, s);
	}

}
