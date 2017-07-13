package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class ReplaceExceptionWithTest implements LineGetter {

	@Override
	public String getName() {
		return "Replace exception with test";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches("^(deleted_trycatch|added_conditional).*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				if (segment != null)
					segments.addAll(segment);
			}
		}

		return segments;
	}

}
