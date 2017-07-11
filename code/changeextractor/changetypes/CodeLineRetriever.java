package changetypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeLineRetriever {
	private Map<String, CodeSegment> oldMap_, newMap_;
	public static String PARAM_RGX = "\\((.*)\\)";
	private static Pattern PATTERN_ATOMIC = Pattern.compile("^(added|deleted|before|after)_([^\\(]+)" + PARAM_RGX + "$");
	private static Pattern PATTERN_EXTRACT_METHOD = Pattern.compile("^extract_method" + PARAM_RGX + "$");
	private static Pattern PATTERN_MOVE_FIELD = Pattern.compile("^move_field" + PARAM_RGX + "$");
	private static Pattern PATTERN_MOVE_METHOD = Pattern.compile("^move_method" + PARAM_RGX + "$");
	private static Pattern PATTERN_PULL_PUSH = Pattern.compile("^(push_down|pull_up)_(method|field)" + PARAM_RGX + "$");
	private static Pattern PATTERN_REPLACE_CON_FAC_METHOD = Pattern.compile("^replace_constructor_with_factory_method" + PARAM_RGX + "$");
	private static Pattern PATTERN_REPLACE_TYPE_CODE_SUBCLASSES = Pattern.compile("^replace_type_code_with_subclasses" + PARAM_RGX + "$");	

	public CodeLineRetriever(Map<String, CodeSegment> oldMap, Map<String, CodeSegment> newMap) {
		oldMap_ = oldMap;
		newMap_ = newMap;
	}

	public String retrieve(String statement) {
		StringBuilder sb = new StringBuilder();
		Matcher m = PATTERN_ATOMIC.matcher(statement);
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
		else {
			if (statement.matches(PATTERN_EXTRACT_METHOD.pattern())
					|| statement.matches(PATTERN_REPLACE_CON_FAC_METHOD.pattern())
					|| statement.matches(PATTERN_REPLACE_TYPE_CODE_SUBCLASSES.pattern())
					) {
				sb.append("(_, ");
				sb.append(findCode(statement));
				sb.append(")");
			}
			
			if (statement.matches(PATTERN_MOVE_FIELD.pattern())
					|| statement.matches(PATTERN_MOVE_METHOD.pattern())
					|| statement.matches(PATTERN_PULL_PUSH.pattern())
					) {
				sb.append("(");
				sb.append(findCode(statement));
				sb.append(")");
			}
		}
		//TODO(Viet) add retrieving code lines for other facts
		return sb.toString();
	}

	public List<CodeSegment> findCode(String statement) {
		Map<String, CodeSegment> map = new HashMap<String, CodeSegment>();
		List<CodeSegment> ret = new ArrayList<CodeSegment>();

		// added, deleted, before, after
		Matcher m = PATTERN_ATOMIC.matcher(statement);
		if (m.find()) {
			String operation = m.group(1);
			switch (operation) {
			case "added":
			case "after":
				map = newMap_;
				break;
			case "deleted":
			case "before":
				map = oldMap_;
				break;
			}

			String paramStr = m.group(3);

			switch (m.group(2)) {
			case "package":
			case "type":
			case "method":
			case "field":
				ret.add(map.get(getParamAt(paramStr, 0)));
				break;
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
				ret.add(map.get(joinParamString(paramStr)));
				break;
			case "methodbody":
				ret.add(map.get(String.join(ASTVisitorAtomicChange.PARAM_SEPARATOR, getParamAt(paramStr, 0), "<body>")));
				break;
			case "localvar":
			case "trycatch":
			case "conditional":
				ret.add(mapByRegex(map, regexFromParam(paramStr)));
				break;
			case "parameter":
				ret.add(map.get(String.join(ASTVisitorAtomicChange.PARAM_SEPARATOR, getParamAt(paramStr, 0),
						getParamAt(paramStr, 1))));
				break;
			case "inheritedfield":
			case "inheritedmethod":
				// do nothing
				break;
			}
		}
		else {
			m = PATTERN_EXTRACT_METHOD.matcher(statement);
			if (m.find()) {
				ret.add(newMap_.get(getParamAt(m.group(1), 1)));
			}
			
			m = PATTERN_MOVE_FIELD.matcher(statement);
			if (m.find()) {
				String paramStr = m.group(1);
				String shortName = getParamAt(paramStr, 0);
				String source = getParamAt(paramStr, 1);
				String target = getParamAt(paramStr, 2);
				String fullSourceName = "\"" + source + "#" + shortName + "\"";
				String fullTargetName = "\"" + target + "#" + shortName + "\"";
				String deletedStatement = "deleted_field(" + fullSourceName + "," +
						"\"" + shortName + "\"" + "," +
						"\"" + source + "\")";
				ret.addAll(findCode(deletedStatement));
				
				String addedStatement = "added_field(" + fullTargetName + "," +
						"\"" + shortName + "\"" + "," +
						"\"" + target + "\")";
				ret.addAll(findCode(addedStatement));
			}

			m = PATTERN_MOVE_METHOD.matcher(statement);
			if (m.find()) {
				String paramStr = m.group(1);
				String shortName = getParamAt(paramStr, 0);
				String source = getParamAt(paramStr, 1);
				String target = getParamAt(paramStr, 2);
				String fullSourceName = "\"" + source + "#" + shortName + "\"";
				String fullTargetName = "\"" + target + "#" + shortName + "\"";
				String deletedStatement = "deleted_method(" + fullSourceName + "," +
						"\"" + shortName + "\"" + "," +
						"\"" + source + "\")";
				ret.addAll(findCode(deletedStatement));
				
				String addedStatement = "added_method(" + fullTargetName + "," +
						"\"" + shortName + "\"" + "," +
						"\"" + target + "\")";
				ret.addAll(findCode(addedStatement));
			}
			
			// pull_up_field, pull_up_method, push_down_field, push_down_method
			m = PATTERN_PULL_PUSH.matcher(statement);
			if (m.find()) {
				String obj = m.group(2);
				String paramStr = m.group(3);
				ret.addAll(findCode("move_" + obj + "(" + paramStr + ")"));
			}
			
			m = PATTERN_REPLACE_CON_FAC_METHOD.matcher(statement);
			if (m.find()) {
				String paramStr = m.group(1);
				String cons = getParamAt(paramStr, 0);
				String fact = getParamAt(paramStr, 1);
				ret.addAll(findCode("added_method(\"" + fact + "\",?,?)"));
				ret.addAll(findCode("added_calls(\"" + fact + "\",\"" + cons + "\")"));
				ret.addAll(findCode("added_methodmodifier(\"" + cons + "\",\"private\")"));
			}
			
			m = PATTERN_REPLACE_TYPE_CODE_SUBCLASSES.matcher(statement);
			if (m.find()) {
				String paramStr = m.group(1);
				ret.addAll(findCode("added_type(" + paramStr + ")"));
			}
		}

		ret.removeAll(new ArrayList<CodeSegment>() {{ add(null); }});
		return ret;
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
		Iterator<Map.Entry<String, CodeSegment>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, CodeSegment> e = iterator.next();
			if (e.getKey().matches(pattern))
				return e.getValue();
		}

		return null;
	}

	public String getParamAt(String params, int index) {
		Matcher mm = Pattern.compile("(\"[^\"]*\"|\\?)").matcher(params);
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
