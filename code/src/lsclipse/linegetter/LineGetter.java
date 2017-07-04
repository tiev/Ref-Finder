package lsclipse.linegetter;

import java.util.List;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public interface LineGetter {
	public abstract String getName();
	
	public abstract List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents);
}
