package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class IntroduceExplainingVariable implements LineGetter {

	@Override
	public String getName() {
		return "Introduce explaining variable";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		String addVarStr, deletedMethodName, addedMethodName;
		addVarStr = deletedMethodName = addedMethodName = "";
		
		for (String statement : dependents) {
			if (statement.matches("^added_localvar.*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				if (segment != null)
					segments.addAll(segment);
				addVarStr = statement;
			}
			if (statement.matches("^deleted_methodbody.*")) {
				deletedMethodName = retriever.getParamIn(statement, 0);
			}
			if (statement.matches("^added_methodbody.*")) {
				addedMethodName = retriever.getParamIn(statement, 0);
			}
		}
		
		if (!deletedMethodName.isEmpty()) {
			String expression = retriever.getParamIn(addVarStr, 3);
			List<CodeSegment> segment = retriever.findCodeInOldMethod(expression, deletedMethodName);
			if (segment != null)
				segments.addAll(segment);
		}
		if (!addedMethodName.isEmpty()) {
			String varStr = retriever.getParamIn(addVarStr, 2);
			List<CodeSegment> segment = retriever.findCodeInNewMethod(varStr, addedMethodName);
			if (segment != null)
				segments.addAll(segment);
		}
		
		return segments;
	}

}
