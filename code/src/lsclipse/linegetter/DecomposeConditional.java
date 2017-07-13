package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class DecomposeConditional implements LineGetter {

	@Override
	public String getName() {
		return "Decompose conditional";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches("^(deleted_conditional|added_method|after_calls|added_methodbody|deleted_methodbody|extract_method).*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				if (segment != null)
					segments.addAll(segment);
			}
		}

		return segments;
	}

}
