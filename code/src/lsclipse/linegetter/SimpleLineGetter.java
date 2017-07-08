package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class SimpleLineGetter implements LineGetter {
	public String getName() {
		return "Default";
	}
	
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();
		for (String statement : dependents) {
			List<CodeSegment> segment = retriever.findCode(statement);
			if (segment == null)
				continue;
			segments.addAll(segment);
		}
		
		return segments;
	}

}
