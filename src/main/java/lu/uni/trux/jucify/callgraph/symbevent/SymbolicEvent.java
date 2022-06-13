package lu.uni.trux.jucify.callgraph.symbevent;

import soot.Body;
import soot.Unit;

public interface SymbolicEvent {
	public Unit generateCode(Body b, Unit u);
}
