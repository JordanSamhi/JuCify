package lu.uni.trux.jucify.callgraph.symbevent;

import java.util.Comparator;

public class EventSorterForCodeGeneration implements Comparator<SymbolicEvent> {

	public static final EventSorterForCodeGeneration comparator = new EventSorterForCodeGeneration();
	private EventSorterForCodeGeneration() {}

	private int attributeValueToInstanceType(SymbolicEvent o) {
		if (o instanceof GetFieldEvent)
			return 1;
		else if (o instanceof InvokeEvent)
			return 2;
		else if (o instanceof SetFieldEvent)
			return 3;
		else if (o instanceof ReturnEvent)
			return 4;
		throw new RuntimeException("Event type \"" + o.getClass().getName()
				+ "\" not handled by the comparator \"EventSorterForCodeGeneration\".");
	}

	@Override
	public int compare(SymbolicEvent o1, SymbolicEvent o2) {
		return Integer.compare(attributeValueToInstanceType(o1), attributeValueToInstanceType(o2));
	}

}
