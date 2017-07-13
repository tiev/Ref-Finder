package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class InlineTemp implements LineGetter {

	@Override
	public String getName() {
		return "Inline temp";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		String varStr, addedMethodName, deletedMethodName;
		varStr = addedMethodName = deletedMethodName = "";
		
		for (String statement : dependents) {
			if (statement.matches("^deleted_localvar.*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				if (segment != null)
					segments.addAll(segment);
				varStr = statement;
			}
			if (statement.matches("^added_methodbody.*")) {
				addedMethodName = retriever.getParamIn(statement, 0);
			}
			if (statement.matches("^deleted_methodbody.*")) {
				deletedMethodName = retriever.getParamIn(statement, 0);
			}
		}
		
		if (!addedMethodName.isEmpty()) {
			String expression = retriever.getParamIn(varStr, 3);
			List<CodeSegment> segment = retriever.findCodeInNewMethod(expression, addedMethodName);
			if (segment != null)
				segments.addAll(segment);
		}
		if (!deletedMethodName.isEmpty()) {
			String identifier = retriever.getParamIn(varStr, 2);
			List<CodeSegment> segment = retriever.findCodeInOldMethod(identifier, deletedMethodName);
			if (segment != null)
				segments.addAll(segment);
		}
		
		return segments;
	}

}
