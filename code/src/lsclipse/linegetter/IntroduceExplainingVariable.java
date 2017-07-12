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
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		String addVarStr = "";
		for (String statement : dependents) {
			if (statement.matches("^added_localvar.*")) {
				List<CodeSegment> segment = retriever.findCode(statement);
				if (segment != null)
					segments.addAll(segment);
				addVarStr = statement;
				break;
			}
		}
		
		for (String statement : dependents) {
			if (statement.matches("^deleted_methodbody.*")) {
				String methodName = retriever.getParamAt(retriever.getParamStr(statement), 0);
				String expression = retriever.getParamAt(retriever.getParamStr(addVarStr), 3);
				List<CodeSegment> segment = retriever.findCodeInOldMethod(expression, methodName);
				if (segment != null)
					segments.addAll(segment);
			}
			
			if (statement.matches("^added_methodbody.*")) {
				String methodName = retriever.getParamAt(retriever.getParamStr(statement), 0);
				String varStr = retriever.getParamAt(retriever.getParamStr(addVarStr), 2);
				List<CodeSegment> segment = retriever.findCodeInNewMethod(varStr, methodName);
				if (segment != null)
					segments.addAll(segment);
			}
		}
		
		return segments;
	}

}
