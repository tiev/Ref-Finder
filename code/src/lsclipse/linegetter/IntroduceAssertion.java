package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class IntroduceAssertion implements LineGetter {

	@Override
	public String getName() {
		return "Introduce assertion";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, String params, List<String> dependents) {
		List<CodeSegment> ret = new ArrayList<CodeSegment>();
		
		String methodName = retriever.getParamIn(params, 0);
		List<CodeSegment> segment = retriever.findCodeInNewMethod("assert", methodName);
		if (segment != null)
			ret.addAll(segment);
		
		return ret;
	}

}
