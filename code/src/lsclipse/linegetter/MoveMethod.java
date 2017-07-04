package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class MoveMethod implements LineGetter {

	@Override
	public String getName() {
		return "Move method";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches("^(deleted_method|added_method).*")) {
				CodeSegment segment = retriever.findCode(statement);
				if (segment != null)
					segments.add(segment);
			}
		}

		return segments;
	}

}
