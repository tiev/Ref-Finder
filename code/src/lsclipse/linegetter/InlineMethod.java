package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class InlineMethod implements LineGetter {

	@Override
	public String getName() {
		return "Inline method";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches("^(deleted_method|before_calls|added_methodbody|deleted_methodbody).*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				if (segment != null)
					segments.addAll(segment);
			}
		}

		return segments;
	}

}
