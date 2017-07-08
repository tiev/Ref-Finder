package lsclipse.linegetter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;

public class ExtractSuperclass implements LineGetter {

	@Override
	public String getName() {
		return "Extract superclass";
	}

	@Override
	public List<CodeSegment> retrieveCode(CodeLineRetriever retriever, List<String> dependents) {
		List<CodeSegment> segments = new ArrayList<CodeSegment>();
		List<CodeSegment> segment;
		Pattern patt = Pattern.compile("^added_subtype\\((.*)\\)$");
		Matcher m;

		for (String statement : dependents) {
			m = patt.matcher(statement);
			if (m.find()) {
				segment = retriever.findCode("added_type(" + m.group(1) + ")");
				segments.addAll(segment);
			}
			
			if (statement.matches("^(move_field|move_method|added_subtype).*")) {
				segment = retriever.findCode(statement);
				segments.addAll(segment);
			}
		}

		return segments;
	}

}
