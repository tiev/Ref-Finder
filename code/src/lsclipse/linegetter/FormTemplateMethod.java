package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class FormTemplateMethod implements LineGetter {

	@Override
	public String getName() {
		return "Form template method";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches("^after_subtype.*"))
				continue;
			CodeSegment segment = retriever.findCode(statement);
			if (segment != null)
				segments.add(segment);
		}

		return segments;
	}

}
