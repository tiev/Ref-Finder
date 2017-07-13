package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class ReplaceConditionalWithPolymorphism implements LineGetter {

	@Override
	public String getName() {
		return "Replace conditional with polymorphism";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches(
					"^(deleted_conditional|after_subtype|after_field|after_fieldoftype|added_method|added_calls|added_methodbody).*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				segments.addAll(segment);
			}
		}

		return segments;
	}

}
