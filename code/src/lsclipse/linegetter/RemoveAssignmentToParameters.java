package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class RemoveAssignmentToParameters implements LineGetter {

	private static String[] ASSIGNMENTS = {
			"=", "+=", "-=", "*=", "/=", "%=",
			"&=", "|=", "^=", "<<=", ">>=", ">>>="
	};
	@Override
	public String getName() {
		return "Remove assignment to parameters";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();

		String paramName = retriever.getParamIn(params, 1);
		paramName = paramName.substring(paramName.indexOf(':') + 1);
		String methodName = retriever.getParamIn(params, 0);
		
		for (String assign : ASSIGNMENTS) {
			List<CodeSegment> segment = retriever.findCodeInOldMethod(paramName + assign, methodName);
			if (segment != null)
				segments.addAll(segment);
		}
		
		// Remove segment of "==" 
		List<CodeSegment> segment = retriever.findCodeInOldMethod(paramName + "==", methodName);
		if (segment != null) {
			for (CodeSegment seg : segment)
				segments.remove(seg);
		}
		
		return segments;
	}

}
