package changetypes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import changetypes.CodeSegment.LineSegment;
import lsclipse.utils.StringCleaner;

public class CodeLineRetriever {
	private Map<String, CodeSegment> oldMap_, newMap_;
	public static String PARAM_RGX = "\\((.*)\\)";
	private static Pattern PATTERN_PARAM = Pattern.compile("^[^\\(]*" + PARAM_RGX + "$");
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

	public String getParamStr(String statement) {
		Matcher m = PATTERN_PARAM.matcher(statement);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}
	
	public String getParamIn(String statement, int index) {
		return getParamAt(getParamStr(statement), index);
	}
	
	public List<CodeSegment> findCodeOldMethodName(String methodName) {
		MethodDeclaration md = findOldMethod(methodName);
		if (md != null) {
			List<CodeSegment> ret = new ArrayList<CodeSegment>();
			ret.add(CodeSegment.extract(md.getName()));
			return ret;
		}
		return null;
	}

	public List<CodeSegment> findCodeNewMethodName(String methodName) {
		MethodDeclaration md = findNewMethod(methodName);
		if (md != null) {
			List<CodeSegment> ret = new ArrayList<CodeSegment>();
			ret.add(CodeSegment.extract(md.getName()));
			return ret;
		}
		return null;
	} 
	
	public MethodDeclaration findMethod(Map<String, CodeSegment> map, String methodName) {
		try {
			CodeSegment methodSegment = map.get(methodName);
			ICompilationUnit file = (ICompilationUnit) methodSegment.getFile();
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(file);
			parser.setResolveBindings(true);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			MethodSearchVisitor visitor = new MethodSearchVisitor(methodSegment);
			cu.accept(visitor);
			if (visitor.method != null)
				return visitor.method;
		} catch (Exception e) {
			System.err.println("Cannot find method declaration " + methodName);
		}
		
		return null;
	}

	public MethodDeclaration findOldMethod(String methodName) {
		return findMethod(oldMap_, methodName);
	}
	
	public MethodDeclaration findNewMethod(String methodName) {
		return findMethod(newMap_, methodName);
	}
	
	private class MethodSearchVisitor extends ASTVisitor {
		public MethodDeclaration method;
		private CodeSegment methodSegment;
		
		public MethodSearchVisitor(CodeSegment segment) {
			methodSegment = segment;
		}
		
		public boolean visit(MethodDeclaration node) {
			if (CodeSegment.extract(node).equals(methodSegment))
				method = node;
			return false;
		}
	}

	public List<CodeSegment> findCodeInOldMethod(String expression, String methodName) {
		MethodDeclaration md = findOldMethod(methodName);
		if (md != null) {
			return findCodeInMethod(expression, md);
		}
		return null;
	}

	public List<CodeSegment> findCodeInNewMethod(String expression, String methodName) {
		MethodDeclaration md = findNewMethod(methodName);
		if (md != null) {
			return findCodeInMethod(expression, md);
		}
		return null;
	}
	
	public List<CodeSegment> findCodeInMethod(String expression, MethodDeclaration node) {
		List<CodeSegment> ret = new ArrayList<CodeSegment>();
		Queue<Statement> queue = new ArrayDeque<Statement>();
		Block body = node.getBody();
		
		// Add first level statements
		if (body != null) {
			for (Object statement : body.statements()) {
				if (StringCleaner.cleanupString(statement.toString()).contains(expression)) {
					queue.add((Statement) statement);
				}
			}
		}
		
		while (!queue.isEmpty()) {
			Statement statement = queue.poll();

			// Bitwise operator "|" in condition is to force evaluate all parts in condition
			boolean isLast = true;
			switch (statement.getNodeType()) {
			case ASTNode.BLOCK:
				for (Object sta : ((Block) statement).statements())
					if (tryQueue(queue, (Statement)sta, expression))
						isLast = false;
				break;
			case ASTNode.DO_STATEMENT:
				DoStatement doSta = (DoStatement) statement;
				if (tryQueue(queue, doSta.getBody(), expression)
						| tryExtract(ret, doSta.getExpression(), expression))
					isLast = false;
				break;
			case ASTNode.ENHANCED_FOR_STATEMENT:
				EnhancedForStatement eforSta = (EnhancedForStatement) statement;
				if (tryQueue(queue, eforSta.getBody(), expression)
						| tryExtract(ret, eforSta.getExpression(), expression)
						| tryExtract(ret, eforSta.getParameter(), expression)
						)
					isLast = false;
				break;
			case ASTNode.FOR_STATEMENT:
				ForStatement forSta = (ForStatement) statement;
				if (tryQueue(queue, forSta.getBody(), expression)
						| tryExtract(ret, forSta.getExpression(), expression)
						)
					isLast = false;
				for (Object ini : forSta.initializers())
					if (tryExtract(ret, (Expression)ini, expression))
						isLast = false;
				for (Object u : forSta.updaters())
					if (tryExtract(ret, (Expression)u, expression))
						isLast = false;
				break;
			case ASTNode.IF_STATEMENT:
				IfStatement ifSta = (IfStatement) statement;
				if (tryExtract(ret, ifSta.getExpression(), expression)
						| tryQueue(queue, ifSta.getThenStatement(), expression)
						| tryQueue(queue, ifSta.getElseStatement(), expression))
					isLast = false;
				break;
			case ASTNode.LABELED_STATEMENT:
				LabeledStatement labelSta = (LabeledStatement) statement;
				if (tryExtract(ret, labelSta.getLabel(), expression)
						| tryQueue(queue, labelSta.getBody(), expression)
						)
					isLast = false;
				break;
			case ASTNode.SWITCH_STATEMENT:
				SwitchStatement switchSta = (SwitchStatement) statement;
				if (tryExtract(ret, switchSta.getExpression(), expression))
					isLast = false;
				for (Object ca : switchSta.statements())
					if (tryQueue(queue, (Statement)ca, expression))
						isLast = false;
				break;
			case ASTNode.SYNCHRONIZED_STATEMENT:
				SynchronizedStatement syncSta = (SynchronizedStatement) statement;
				if (tryExtract(ret, syncSta.getExpression(), expression)
						| tryQueue(queue, syncSta.getBody(), expression))
					isLast = false;
				break;
			case ASTNode.TRY_STATEMENT:
				TryStatement trySta = (TryStatement) statement;
				if (tryQueue(queue, trySta.getBody(), expression)
						| tryQueue(queue, trySta.getFinally(), expression))
					isLast = false;
				for (Object ica : trySta.catchClauses()) {
					CatchClause ca = (CatchClause) ica;
					if (tryQueue(queue, ca.getBody(), expression)
							| tryExtract(ret, ca.getException(), expression))
						isLast = false;
				}
				for (Object re : trySta.resources())
					if (tryExtract(ret, (ASTNode)re, expression))
						isLast = false;
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				VariableDeclarationStatement varSta = (VariableDeclarationStatement) statement;
				for (Object frag : varSta.fragments())
					if (tryExtract(ret, (ASTNode) frag, expression))
						isLast = false;
				break;
			case ASTNode.WHILE_STATEMENT:
				WhileStatement whileSta = (WhileStatement) statement;
				if (tryExtract(ret, whileSta.getExpression(), expression)
						| tryQueue(queue, whileSta.getBody(), expression))
					isLast = false;
				break;
			}
			if (isLast)
				ret.add(CodeSegment.extract(statement));
		}
		return ret;
	}
	
	private boolean expectedCode(Object statement, String expression) {
		return StringCleaner.cleanupString(statement.toString())
				.contains(expression);
	}
	
	private boolean tryQueue(Queue<Statement> queue, Statement statement, String expression) {
		if (statement != null && expectedCode(statement, expression)) {
			queue.add(statement);
			return true;
		}
		return false;
	}
	
	private boolean tryExtract(List<CodeSegment> list, ASTNode node, String expression) {
		if (node != null && expectedCode(node, expression)) {
			list.add(CodeSegment.extract(node));
			return true;
		}
		return false;
	}
}
