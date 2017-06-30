package changetypes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.PrimitiveType.Code;

public class CodeLineRetriever {
	private Map<String, CodeSegment> oldMap_, newMap_;
	
	public CodeLineRetriever(Map<String, CodeSegment> oldMap, Map<String, CodeSegment> newMap) {
		oldMap_ = oldMap;
		newMap_ = newMap;
	}

	public String retrieve(String statement) {
		StringBuilder sb = new StringBuilder();
		Matcher m = Pattern.compile("^(added|deleted|before|after)").matcher(statement);
		if (m.find()) {
			sb.append("(");
			switch (m.group(1)) {
			case "deleted":
			case "before":
				sb.append(findCode(statement));
				sb.append(", _");
				break;
			case "added":
			case "after":
				sb.append("_, ");
				sb.append(findCode(statement));
				break;
			}
			sb.append(")");
		}
		return sb.toString();
	}
		
	public CodeSegment findCode(String statement) {
		Pattern p = Pattern.compile("^(added|deleted|before|after)_([^\\(]+)\\((.*)\\)$");
		Matcher m = p.matcher(statement);
		Map<String, CodeSegment> map = new HashMap<String, CodeSegment>();
		
		// Other LSDFact do not match 
		if (!m.find())
			return null;
		
		String operation = m.group(1); 
		switch(operation) {
		case "added":
		case "after":
			map = newMap_; break;
		case "deleted":
		case "before":
			map = oldMap_; break;
		}
		
		String paramStr = m.group(3);
		
		switch (m.group(2)) {
		case "package":
		case "type":
		case "method":
		case "field":
			return map.get(getParamAt(paramStr, 0));
		case "return":
		case "fieldoftype":
		case "accesses":
		case "calls":
		case "subtype":
		case "extends":
		case "implements":
		case "typeintype":
		case "cast":
		case "throws":
		case "getter":
		case "setter":
		case "methodmodifier":
		case "fieldmodifier":
			return map.get(joinParamString(paramStr));
		case "methodbody":
			return map.get(String.join(ASTVisitorAtomicChange.PARAM_SEPARATOR, getParamAt(paramStr, 0), "<body>"));
		case "localvar":
		case "trycatch":
		case "conditional":
			return mapByRegex(map, regexFromParam(paramStr));
		case "parameter":
			return map.get(String.join(ASTVisitorAtomicChange.PARAM_SEPARATOR, getParamAt(paramStr, 0), getParamAt(paramStr, 1)));
		case "inheritedfield":
		case "inheritedmethod":
			return null;
		}

		return null;
	}

	private String regexFromParam(String paramStr) {
		Matcher mm = Pattern.compile("(\"[^\"]*\"|\\?)").matcher(paramStr);
		StringBuilder sb = new StringBuilder();
		sb.append("^");
		Boolean first = true;
		while (mm.find()) {
			if (first)
				first = false;
			else
				sb.append(ASTVisitorAtomicChange.PARAM_SEPARATOR);
			if (mm.group(1).equals("?"))
				sb.append(".*");
			else
				sb.append(Pattern.quote(mm.group(1).replaceAll("^\"|\"$", "")));
		}
		sb.append("$");
		
		return sb.toString();
	}

	private CodeSegment mapByRegex(Map<String, CodeSegment> map, String pattern) {
		//TODO(Viet) optimize mapByRegex
		Iterator<Map.Entry<String, CodeSegment>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, CodeSegment> e = iterator.next();
			if (e.getKey().matches(pattern))
				return e.getValue();
		}
		
		return null;
	}

	public String getParamAt(String params, int index) {
		Matcher mm = Pattern.compile("(\"[^\"]+\"|\\?)").matcher(params);
		int i = 0;
		while (i <= index) {
			mm.find();
			i++;
		}

		return mm.group(1).replaceAll("^\"|\"$", "");
	}
	
	private String joinParamString(String params) {
		Matcher mm = Pattern.compile("\"([^\"]*)\"").matcher(params);
		StringBuilder sb = new StringBuilder();
		Boolean first = true;
		while (mm.find()) {
			if (first)
				first = false;
			else
				sb.append(ASTVisitorAtomicChange.PARAM_SEPARATOR);
			sb.append(mm.group(1));
		}
		return sb.toString();
	}
}
