package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class ReplaceMethodWithMethodObject implements LineGetter {

	@Override
	public String getName() {
		return "Replace method with method object";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();
		
		for (String statement : dependents) {
			if (statement.matches("^(added_method|added_calls).*")) {
				CodeSegment segment = retriever.findCode(statement);
				if (segment != null)
					segments.add(segment);
			}
		}
		
		return segments;
	}

}
