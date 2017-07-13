package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class RenameMethod implements LineGetter {

	@Override
	public String getName() {
		return "Rename method";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		for (String statement : dependents) {
			if (statement.matches("^deleted_method\\(.*")) {
				List<CodeSegment> segment = retriever.findCodeOldMethodName(
						retriever.getParamIn(statement, 0));
				if (segment != null)
					segments.addAll(segment);
			}
			if (statement.matches("^added_method\\(.*")) {
				List<CodeSegment> segment = retriever.findCodeNewMethodName(
						retriever.getParamIn(statement, 0));
				if (segment != null)
					segments.addAll(segment);
			}
		}

		return segments;
	}

}
