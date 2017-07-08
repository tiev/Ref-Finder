package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class ActionLineGetter implements LineGetter {

	@Override
	public String getName() {
		return "Action";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches("^(added|deleted|move|push_down|pull_up|extract|replace)_.*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				segments.addAll(segment);
			}
		}

		return segments;
	}

}
