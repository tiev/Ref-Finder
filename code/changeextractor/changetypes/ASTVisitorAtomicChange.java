/* 
*    Ref-Finder
*    Copyright (C) <2015>  <PLSE_UCLA>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * This visitor traverses an AST tree to collect structural facts about the AST
 * Facts collected include:
 * - Class name
 * - Class parent
 * - Methods in class (incl formal params)
 * - Method body (as string)
 */

package changetypes;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import lsclipse.utils.StringCleaner;

public class ASTVisitorAtomicChange extends ASTVisitor {

	public static String PARAM_SEPARATOR = "_";
	
	// Parse results
	public FactBase facts;
	private Map<String, IJavaElement> typeToFileMap_ = new HashMap<String, IJavaElement>();
	private Map<String, CodeSegment> entityLineMap_ = new HashMap<String, CodeSegment>();

	public Map<String, IJavaElement> getTypeToFileMap() {
		return Collections.unmodifiableMap(typeToFileMap_);
	}
	
	public Map<String, CodeSegment> getEntityLineMap() {
		return Collections.unmodifiableMap(entityLineMap_);
	}

	private List<String> allowedFieldMods_ = Arrays.asList(new String[] {
			"public", "private", "protected", "static", "final" });

	// temp vars
	private Stack<IMethodBinding> mtbStack = new Stack<IMethodBinding>();
	private Stack<ITypeBinding> itbStack = new Stack<ITypeBinding>();

	public ASTVisitorAtomicChange() {
		facts = new FactBase();
	}

	public void printFacts(PrintStream out) {
		facts.print(out);
	}

	public boolean visit(PackageDeclaration node) {
		try {
			String name = node.getName().toString();
			facts.add(Fact.makePackageFact(name));
			entityLineMap_.put(getEntityMapKey(name), CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Cannot resolve bindings for package "
					+ node.getName().toString());
		}
		return false;
	}

	private static String removeParameters(String name) {
		int index = name.indexOf('<');
		if (index <= 0)
			return name;
		else {
			return name.substring(0, index);
		}
	}

	private static String getModifier(IBinding ib) {
		if ((ib.getModifiers() & Modifier.PUBLIC) > 0)
			return Fact.PUBLIC;
		else if ((ib.getModifiers() & Modifier.PROTECTED) > 0)
			return Fact.PROTECTED;
		else if ((ib.getModifiers() & Modifier.PRIVATE) > 0)
			return Fact.PRIVATE;
		else
			return Fact.PACKAGE;
	}

	// Niki's added code
	private String edit_str(String str) {
		String newmBody_str = str.replace("{", "");
		newmBody_str = newmBody_str.replace("}", "");
		newmBody_str = newmBody_str.replace(" ", "");
		newmBody_str = newmBody_str.replace(";", "");
		return newmBody_str;
	}

	// Niki's added code
	public boolean visit(IfStatement node) {
		Statement thenStmt = node.getThenStatement();
		Statement elseStmt = node.getElseStatement();
		Expression condExpr = node.getExpression();

		String thenStr = (thenStmt.toString()).replace('\n', ' ');
		String elseStr = "";

		if (elseStmt != null) {
			elseStr = (elseStmt.toString()).replace('\n', ' ');
		}

		if (mtbStack.isEmpty()) // not part of a method
			return true;

		IMethodBinding mtb = mtbStack.peek();
		String methodStr = getQualifiedName(mtb);
		String condStr = condExpr.toString();

		condStr = edit_str(condStr);
		thenStr = edit_str(thenStr);
		elseStr = edit_str(elseStr);

		// make conditional fact
		try {
			facts.add(Fact.makeConditionalFact(condStr, thenStr, elseStr,
					methodStr));
		} catch (Exception e) {
			System.err.println("Cannot resolve conditional \""
					+ condExpr.toString() + "\"");
			System.out.println("ifStmt: " + thenStr);
			System.out.println("elseStmt: " + elseStr);
			System.err.println(e.getMessage());
		}
		try {
			entityLineMap_.put(getEntityMapKey(condStr, thenStr, elseStr, methodStr), CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Map code line fail for conditional " + node.toString());
			System.err.println(e.getMessage());
		}
		return true;
	}

	public void endVisit(IfStatement node) {
		// System.out.println("There was a call to end visit for the if statement**********");
		// itbStack.pop();
	}

	// end of added code

	// Kyle's methods
	public boolean visit(CastExpression node) {
		if (mtbStack.isEmpty()) // not part of a method
			return true;

		Expression expression = node.getExpression();
		ITypeBinding type = node.getType().resolveBinding();
		IMethodBinding mtb = mtbStack.peek();
		String exprStr = expression.toString();
		String typeStr = getQualifiedName(type);
		String methodStr = getQualifiedName(mtb);

		exprStr = edit_str(exprStr);

		facts.add(Fact.makeCastFact(exprStr, typeStr, methodStr));
		try {
			entityLineMap_.put(getEntityMapKey(exprStr, typeStr, methodStr), CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Map code line failed for cast \"" + exprStr + "\"");
			System.err.println(e.getMessage());
		}

		return true;
	}

	public boolean visit(TryStatement node) {
		if (mtbStack.isEmpty()) // not part of a method
			return true;

		String bodyStr = node.getBody() != null ? node.getBody().toString()
				: "";
		bodyStr = edit_str(bodyStr);
		StringBuilder catchClauses = new StringBuilder();
		for (Object o : node.catchClauses()) {
			if (catchClauses.length() > 0)
				catchClauses.append(",");
			CatchClause c = (CatchClause) o;
			catchClauses.append(getQualifiedName(c.getException().getType()
					.resolveBinding()));
			catchClauses.append(":");
			if (c.getBody() != null)
				catchClauses.append(edit_str(c.getBody().toString()));
		}
		String finallyStr = node.getFinally() != null ? node.getFinally()
				.toString() : "";
		finallyStr = edit_str(finallyStr);

		IMethodBinding mtb = mtbStack.peek();
		String methodStr = getQualifiedName(mtb);

		facts.add(Fact.makeTryCatchFact(bodyStr, catchClauses.toString(),
				finallyStr, methodStr));

		entityLineMap_.put(getEntityMapKey(bodyStr, catchClauses.toString(), finallyStr, methodStr), CodeSegment.extract(node));
		return true;
	}
	
	private static String getEntityMapKey(CharSequence... elements) {
		List<CharSequence> es = new ArrayList<CharSequence>();
		for (CharSequence e : elements)
			es.add(StringCleaner.cleanupString(e.toString()));
		return String.join(PARAM_SEPARATOR, es);
	}

	private static String getQualifiedName(ITypeBinding itb) {
		if (itb.isPrimitive()) {
			return itb.getName();
		} else if (itb.isArray()) {
			try {
				StringBuilder suffix = new StringBuilder();
				for (int i = 0; i < itb.getDimensions(); ++i) {
					suffix.append("[]");
				}
				return getQualifiedName(itb.getElementType())
						+ suffix.toString();
			} catch (NullPointerException e) {
				return null;
			}
		} else if (itb.isNullType()) {
			return "null";
		} else if (itb.isClass() || itb.isInterface()) {
			if (itb.isNested()) {
				String name = itb.getName();
				if (itb.isAnonymous()) {
					// first check if already inside an anon class
					String binname = itb.getBinaryName();
					int index = binname.indexOf('$');
					name = binname.substring(index + 1, binname.length());
				}
				return getQualifiedName(itb.getDeclaringClass()) + "#" + name;
			} else {
				try {
					String pkg = itb.getPackage().getName();
					String name = itb.getName();
					// if (!name.startsWith("Class<"))
					name = removeParameters(itb.getName());
					return pkg + "%." + name;
				} catch (NullPointerException e) {
					return null;
				}
			}
		} else {
			return "java.lang%.Object"; // default name when all else fails
		}
	}

	private static String getQualifiedName(IVariableBinding ivb) {
		try {
			String name = ivb.getName();
			return getQualifiedName(ivb.getDeclaringClass()) + "#" + name;
		} catch (NullPointerException e) {
			return "";
		}
	}

	private static String getQualifiedName(IMethodBinding imb) {
		// TODO(kprete): do not delegate to getSimpleName so the qualified
		// name can contain the param types and the simple name will stay
		// as is.
		return getQualifiedName(imb.getDeclaringClass()) + "#"
				+ getSimpleName(imb);
	}

	private static String getSimpleName(ITypeBinding itb) {
		if (itb.isNested()) {
			if (itb.isAnonymous()) {
				String binname = itb.getBinaryName();
				int index = binname.indexOf('$');
				String name = binname.substring(index + 1, binname.length());
				return getSimpleName(itb.getDeclaringClass()) + "#" + name;
			} else {
				return getSimpleName(itb.getDeclaringClass()) + "#"
						+ itb.getName();
			}
		} else {
			return itb.getName();
		}
	}

	private static String getSimpleName(IMethodBinding imb) {
		try {
			// imb = imb.getMethodDeclaration();
			String name = imb.getName();
			if (imb.isConstructor())
				name = "<init>";
			String args = "";
			/*
			 * for (ITypeBinding itb : imb.getParameterTypes()) { if
			 * (args.length()>0) args+=","; args += getQualifiedName(itb); }
			 */
			args = "(" + args + ")";
			return name + args;
		} catch (NullPointerException e) {
			return "";
		}
	}

	public boolean visit(CompilationUnit node) {
		IJavaElement thisFile = node.getJavaElement();

		for (Object abstractTypeDeclaration : node.types()) {
			if (abstractTypeDeclaration instanceof TypeDeclaration) {
				TypeDeclaration td = (TypeDeclaration) abstractTypeDeclaration;
				typeToFileMap_.put(getQualifiedName(td.resolveBinding()),
						thisFile);
			}
		}

		return true;
	}

	public boolean visit(TypeDeclaration node) {
		ITypeBinding itb = node.resolveBinding();
		itbStack.push(itb);
		// make class facts
		try {
			facts.add(Fact.makeTypeFact(getQualifiedName(itb),
					getSimpleName(itb), itb.getPackage().getName(),
					itb.isInterface() ? Fact.INTERFACE : Fact.CLASS));
		} catch (Exception e) {
			System.err.println("Cannot resolve bindings for class "
					+ node.getName().toString());
		}
		
		entityLineMap_.put(getEntityMapKey(getQualifiedName(itb)), CodeSegment.extract(node));

		// make super type facts

		try {
			ITypeBinding itb2 = itb.getSuperclass();
			if (itb.getSuperclass() != null) {
				facts.add(Fact.makeSubtypeFact(getQualifiedName(itb2),
						getQualifiedName(itb)));
				facts.add(Fact.makeExtendsFact(getQualifiedName(itb2),
						getQualifiedName(itb)));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(itb2), getQualifiedName(itb)),
						CodeSegment.extract(node));
			}
			if (node.isInterface()) {
				for (ITypeBinding i2 : itb.getInterfaces()) {
					facts.add(Fact.makeSubtypeFact(getQualifiedName(i2),
							getQualifiedName(itb)));
					facts.add(Fact.makeExtendsFact(getQualifiedName(i2),
							getQualifiedName(itb)));
					entityLineMap_.put(getEntityMapKey(getQualifiedName(i2), getQualifiedName(itb)),
							CodeSegment.extract(node));
				}
			} else {
				for (ITypeBinding i2 : itb.getInterfaces()) {
					facts.add(Fact.makeSubtypeFact(getQualifiedName(i2),
							getQualifiedName(itb)));
					facts.add(Fact.makeImplementsFact(getQualifiedName(i2),
							getQualifiedName(itb)));
					entityLineMap_.put(getEntityMapKey(getQualifiedName(i2), getQualifiedName(itb)),
							CodeSegment.extract(node));
				}
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve super class bindings for class "
					+ node.getName().toString());
		}

		// make fields facts
		try {
			for (IVariableBinding ivb : itb.getDeclaredFields()) {
				String visibility = getModifier(ivb);
				String fieldStr = ivb.toString();
				CodeSegment vcs = CodeSegment.extract(node, ivb);
				// System.out.println("fieldStr: " + fieldStr);
				String[] tokens = fieldStr.split(" ");
				for (String token : tokens) {
					if (allowedFieldMods_.contains(token)) {
						facts.add(Fact.makeFieldModifierFact(
								getQualifiedName(ivb), token));
						entityLineMap_.put(getEntityMapKey(getQualifiedName(ivb), token), vcs);
					}
				}
				facts.add(Fact.makeFieldFact(getQualifiedName(ivb),
						ivb.getName(), getQualifiedName(itb), visibility));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(ivb)), vcs);
				if (!ivb.getType().isParameterizedType()) {
					facts.add(Fact.makeFieldTypeFact(getQualifiedName(ivb),
							getQualifiedName(ivb.getType())));
					entityLineMap_.put(
							getEntityMapKey(getQualifiedName(ivb), getQualifiedName(ivb.getType())),
							vcs);
				} else {
					facts.add(Fact.makeFieldTypeFact(getQualifiedName(ivb),
							makeParameterizedName(ivb)));
					entityLineMap_.put(
							getEntityMapKey(getQualifiedName(ivb), makeParameterizedName(ivb)),
							vcs);
				}
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve field bindings for class "
					+ node.getName().toString());
			System.err.println(e.getMessage());
		}

		// make inner type facts
		try {
			for (TypeDeclaration t : node.getTypes()) {
				ITypeBinding intb = t.resolveBinding();
				facts.add(Fact.makeTypeInTypeFact(getQualifiedName(intb), getQualifiedName(itb)));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(intb), getQualifiedName(itb)),
						CodeSegment.extract(node, intb));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve inner type bindings for class "
					+ node.getName().toString());
		}
		/*
		 * //make abstract method facts (if this is an interface) if
		 * (node.isInterface()) { try { List abstractbodies =
		 * node.bodyDeclarations(); for (Object o : abstractbodies) {
		 * BodyDeclaration bd = (BodyDeclaration)o;
		 * System.out.println("aaa: "+bd
		 * .getNodeType()+" bbb: "+ASTNode.METHOD_DECLARATION); }
		 * 
		 * } catch (Exception e) {
		 * System.err.println("Cannot resolve abstract methods for interface " +
		 * node.getName().toString()); } }
		 */
		return true;
	}

	private String makeParameterizedName(IVariableBinding ivb) {
		StringBuilder sb = new StringBuilder();
		sb.append(getQualifiedName(ivb.getType()) + "<");
		boolean comma = false;
		for (ITypeBinding itb : ivb.getType().getTypeArguments()) {
			if (comma)
				sb.append(",");
			sb.append(getQualifiedName(itb));
			comma = true;
		}
		sb.append(">");
		return sb.toString();
	}

	public void endVisit(TypeDeclaration node) {
		itbStack.pop();
	}

	public boolean visit(AnonymousClassDeclaration node) {
		ITypeBinding itb = node.resolveBinding();
		itbStack.push(itb);

		// make class facts
		try {
			facts.add(Fact.makeTypeFact(getQualifiedName(itb),
					getSimpleName(itb), itb.getPackage().getName(),
					itb.isInterface() ? Fact.INTERFACE : Fact.CLASS));
		} catch (Exception e) {
			System.err.println("Cannot resolve bindings for anonymous class "
					+ itb.getName());
		}
		try {
			entityLineMap_.put(getEntityMapKey(getQualifiedName(itb)), CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Map code line fail for anonymous class " + itb.getName());
		}

		// make super type facts
		try {
			try {
				facts.add(Fact.makeSubtypeFact(
						getQualifiedName(itb.getSuperclass()),
						getQualifiedName(itb)));
				facts.add(Fact.makeExtendsFact(
						getQualifiedName(itb.getSuperclass()),
						getQualifiedName(itb)));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(itb.getSuperclass()), getQualifiedName(itb)),
						CodeSegment.extract(node));
			} catch (NullPointerException e) {
				return false;
			}
			for (ITypeBinding i2 : itb.getInterfaces()) {
				try {
					facts.add(Fact.makeSubtypeFact(getQualifiedName(i2),
							getQualifiedName(itb)));
					facts.add(Fact.makeImplementsFact(getQualifiedName(i2),
							getQualifiedName(itb)));
					entityLineMap_.put(getEntityMapKey(getQualifiedName(i2), getQualifiedName(itb)),
							CodeSegment.extract(node));
				} catch (NullPointerException e) {
					return false;
				}
			}
		} catch (Exception e) {
			System.err
					.println("Cannot resolve super class bindings for anonymous class "
							+ itb.getName());
		}

		// make fields facts
		try {
			for (IVariableBinding ivb : itb.getDeclaredFields()) {
				String visibility = getModifier(ivb);
				facts.add(Fact.makeFieldFact(getQualifiedName(ivb),
						ivb.getName(), getQualifiedName(itb), visibility));
				facts.add(Fact.makeFieldTypeFact(getQualifiedName(ivb),
						getQualifiedName(ivb.getType())));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(ivb)), CodeSegment.extract(node, ivb));
			}
		} catch (Exception e) {
			System.err
					.println("Cannot resolve field bindings for anonymous class "
							+ itb.getName());
		}

		// make inner type facts for self
		try {
			if (itb.isNested()) {
				facts.add(Fact.makeTypeInTypeFact(getQualifiedName(itb), getQualifiedName(itb.getDeclaringClass())));
				entityLineMap_.put(
						getEntityMapKey(getQualifiedName(itb), getQualifiedName(itb.getDeclaringClass())),
						CodeSegment.extract(node));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve inner type for anonymous class "
					+ itb.getName());
		}

		return true;
	}

	public void endVisit(AnonymousClassDeclaration node) {
		itbStack.pop();
	}

	@SuppressWarnings("unchecked")
	public boolean visit(MethodDeclaration node) {

		IMethodBinding mtb = node.resolveBinding();
		mtbStack.push(mtb);
		String nodeStr = node.toString();

		// TODO(kprete): default is actually package private
		String modifier = "protected";
		int dex = nodeStr.indexOf(' ');
		if (dex >= 0) {
			String temp = nodeStr.substring(0, dex);
			if (temp.equals("public"))
				modifier = "public";
			else if (temp.equals("private"))
				modifier = "private";
		}
		// make method fact
		try {
			String visibility = getModifier(mtb);
			facts.add(Fact.makeMethodFact(getQualifiedName(mtb),
					getSimpleName(mtb),
					getQualifiedName(mtb.getDeclaringClass()), visibility));
		} catch (Exception e) {
			System.err
					.println("Cannot resolve return method bindings for method "
							+ node.getName().toString());
		}
		
		entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb)), CodeSegment.extract(node));
		
		// make return type fact
		try {
			String returntype = getQualifiedName(mtb.getReturnType());
			facts.add(Fact.makeReturnsFact(getQualifiedName(mtb), returntype));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb), returntype), CodeSegment.extract(node, Fact.FactTypes.RETURN));
		} catch (Exception e) {
			System.err
					.println("Cannot resolve return type bindings for method "
							+ node.getName().toString());
		}
		// make modifier fact.
		try {
			facts.add(Fact.makeModifierMethodFact(getQualifiedName(mtb),
					modifier));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb), modifier), CodeSegment.extract(node, Fact.FactTypes.METHODMODIFIER));
		} catch (Exception e) {
			System.err
					.println("Cannot resolve return type bindings for method modifier "
							+ node.getName().toString());
		}
		try {// Niki's added code
			String bodystring = node.getBody() != null ? node.getBody()
					.toString() : "";
			bodystring = bodystring.replace('\n', ' ');
			// Niki's added line
			bodystring = bodystring.replace('\"', ' ');
			bodystring = bodystring.replace('"', ' ');
			bodystring = bodystring.replace('\\', ' ');
			// TODO(kprete): remove comments?
			// facts.add(Fact.makeMethodBodyFact(getQualifiedName(mtb.getDeclaringClass())+"."+getQualifiedName(mtb),
			// bodystring));
			facts.add(Fact
					.makeMethodBodyFact(getQualifiedName(mtb), bodystring));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb), "<body>"), CodeSegment.extract(node, Fact.FactTypes.METHODBODY));
		} catch (Exception e) {
			System.err.println("Cannot resolve bindings for body");
		}
		try {
			List<SingleVariableDeclaration> parameters = node.parameters();

			StringBuilder sb = new StringBuilder();
			for (SingleVariableDeclaration param : parameters) {
				if (sb.length() != 0)
					sb.append(", ");
				sb.append(param.getType().toString());
				sb.append(":");
				sb.append(param.getName().toString());
			}

			// We don't know if any change occurred in the paramList
			facts.add(Fact.makeParameterFact(getQualifiedName(mtb),
					sb.toString(), ""));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb), sb.toString()), // Parameter Fact doesn't use third param
					CodeSegment.extract(node, Fact.FactTypes.PARAMETER));
		} catch (Exception e) {
			System.err.println("Cannot resolve bindings for parameters");
		}

		try {
			List<Name> thrownTypes = node.thrownExceptions();
			for (Name n : thrownTypes) {
				facts.add(Fact.makeThrownExceptionFact(getQualifiedName(mtb),
						getQualifiedName(n.resolveTypeBinding())));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb), getQualifiedName(n.resolveTypeBinding())),
						CodeSegment.extract(node, Fact.FactTypes.THROWN));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve bindings for exceptions");
		}

		return true;
	}

	public void endVisit(MethodDeclaration node) {
		mtbStack.pop();
	}

	public boolean visit(FieldAccess node) {
		IVariableBinding ivb = node.resolveFieldBinding();

		if (mtbStack.isEmpty()) // not part of a method
			return true;

		IMethodBinding mtb = mtbStack.peek();

		// make field access fact
		try {
			// special case: if field access is on length field of an array,
			// ignore
			if (node.getName().toString().equals("length")
					&& ivb.getDeclaringClass() == null) {
				// continue
			} else { // otherwise proceed as normal
				facts.add(Fact.makeAccessesFact(
						getQualifiedName(node.resolveFieldBinding()),
						getQualifiedName(mtb)));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve field access \""
					+ node.getName().toString() + "\"");
		}
		try {
			entityLineMap_.put(
					getEntityMapKey(getQualifiedName(node.resolveFieldBinding()), getQualifiedName(mtb)),
					CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Map code line failed for field access " + node.getName().toString());
			System.err.println(e.getMessage());
		}
		// Make getter/setter facts
		try {
			String simpleMethodName = getSimpleName(mtb);
			if (simpleMethodName.toLowerCase().startsWith("get")) {
				facts.add(Fact.makeGetterFact(getQualifiedName(mtb),
						getQualifiedName(node.resolveFieldBinding())));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb), getQualifiedName(node.resolveFieldBinding())),
						CodeSegment.extract(node));
			} else if (simpleMethodName.toLowerCase().startsWith("set")) {
				facts.add(Fact.makeSetterFact(getQualifiedName(mtb),
						getQualifiedName(node.resolveFieldBinding())));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(mtb), getQualifiedName(node.resolveFieldBinding())),
						CodeSegment.extract(node));
			}
		} catch (Exception e) {
			System.err.println("Cannot resolve bindings for exceptions");
		}
		return true;
	}

	public boolean visit(SimpleName node) {
		if (mtbStack.isEmpty() && !itbStack.isEmpty()) { // not part of a method
			return false;
			/*
			 * try { return visitName(node.resolveBinding(),
			 * anonClassName.equals("")?getQualifiedName(itb):anonClassName); }
			 * catch (Exception e) {
			 * System.err.println("Cannot resolve simple name \""
			 * +node.getFullyQualifiedName().toString()+"\""); return false; }
			 */
		} else if (!mtbStack.isEmpty()) {
			if (node.getIdentifier().equals("length"))
				return false;
			try {
				return visitName(node.resolveBinding(), mtbStack.peek(), node);
			} catch (Exception e) {
				System.err.println("Cannot resolve simple name \""
						+ node.getFullyQualifiedName().toString() + "\"");
				return false;
			}
		}
		return false;
	}

	public boolean visit(QualifiedName node) {
		if (mtbStack.isEmpty() && !itbStack.isEmpty()) { // not part of a method
			return false;
		} else if (!mtbStack.isEmpty()) {
			if (node.getName().getIdentifier().equals("length")) {
				return true;
			}
			try {
				return visitName(node.resolveBinding(), mtbStack.peek(), node);
			} catch (Exception e) {
				System.err.println("Cannot resolve qualified name \""
						+ node.getFullyQualifiedName().toString() + "\"");
				return false;
			}
		}
		return false;
	}

	private boolean visitName(IBinding ib, IMethodBinding iMethodBinding, Name node)
			throws Exception {
		switch (ib.getKind()) {
		case IBinding.VARIABLE:
			IVariableBinding ivb = (IVariableBinding) ib;
			if (ivb.isField()) {
				facts.add(Fact.makeAccessesFact(getQualifiedName(ivb),
						getQualifiedName(iMethodBinding)));

				entityLineMap_.put(getEntityMapKey(getQualifiedName(ivb), getQualifiedName(iMethodBinding)), CodeSegment.extract(node));
				try {
					String simpleMethodName = getSimpleName(iMethodBinding);
					if (simpleMethodName.toLowerCase().startsWith("get")) {
						facts.add(Fact.makeGetterFact(
								getQualifiedName(iMethodBinding),
								getQualifiedName(ivb)));
						entityLineMap_.put(getEntityMapKey(getQualifiedName(iMethodBinding), getQualifiedName(ivb)), CodeSegment.extract(node));
					} else if (simpleMethodName.toLowerCase().startsWith("set")) {
						facts.add(Fact.makeSetterFact(
								getQualifiedName(iMethodBinding),
								getQualifiedName(ivb)));
						entityLineMap_.put(getEntityMapKey(getQualifiedName(iMethodBinding), getQualifiedName(ivb)), CodeSegment.extract(node));
					}
				} catch (Exception e) {
					System.err
							.println("Cannot resolve bindings for exceptions");
				}
			}
			break;
		default:
			break;
		}
		return true;

		/*
		 * IJavaElement ije = null; ije = ib.getJavaElement(); if (ije==null)
		 * return false; try { //if reference to a field if (ije instanceof
		 * ResolvedBinaryField || ije instanceof ResolvedSourceField) {
		 * NamedMember nm = (NamedMember) ije; String fieldname =
		 * nm.getDeclaringType().getPackageFragment().getElementName()+
		 * "%."+nm.getDeclaringType().getTypeQualifiedName('#')+
		 * "#"+nm.getElementName(); facts.add(Fact.makeAccessesFact(fieldname,
		 * ownerFullName)); } else if (ije instanceof ResolvedBinaryMethod ||
		 * ije instanceof ResolvedSourceMethod) { //if reference is to a method
		 * //do nothing for now } } catch (Exception e) {
		 * System.out.println("No good 2!"+ib.getName()); return false; } return
		 * false;
		 */
	}

	public boolean visit(MethodInvocation node) {
		IMethodBinding mmtb = node.resolveMethodBinding();

		if (mtbStack.isEmpty()) // not part of a method
			return true;

		// make field access fact
		try {
			// * I do not know if this fix is necessary. Check JQuery behavior.
			// For now use a quick hack.
			if (node.getExpression() != null
					&& mmtb.getDeclaringClass().getQualifiedName()
							.startsWith("java.awt.geom.Path2D")
			/*
			 * && (mmtb.getName().equals("getPathIterator") ||
			 * mmtb.getName().equals("lineTo") ||
			 * mmtb.getName().equals("moveTo") ||
			 * mmtb.getName().equals("closePath"))
			 */) {
				Expression e = node.getExpression();
				ITypeBinding itb = e.resolveTypeBinding();
				facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()),
						getQualifiedName(itb) + "#" + getSimpleName(mmtb)));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(mtbStack.peek()), getQualifiedName(itb) + "#" + getSimpleName(mmtb)),
						CodeSegment.extract(node));
			} else {
				facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()),
						getQualifiedName(mmtb)));
				entityLineMap_.put(getEntityMapKey(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb)),
						CodeSegment.extract(node));
			}
			// */
			// facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()),
			// getQualifiedName(mmtb)));
		} catch (Exception e) {
			System.err.println("Cannot resolve method invocation \""
					+ node.getName().toString() + "\"");
		}
		return true;
	}

	public boolean visit(SuperMethodInvocation node) {
		IMethodBinding mmtb = node.resolveMethodBinding();

		if (mtbStack.isEmpty()) // not part of a method
			return true;

		// make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()),
					getQualifiedName(mmtb)));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb)),
					CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Cannot resolve method invocation \""
					+ node.getName().toString() + "\"");
		}
		return true;
	}

	public boolean visit(ClassInstanceCreation node) {
		IMethodBinding mmtb = node.resolveConstructorBinding();

		if (mtbStack.isEmpty()) // not part of a method
			return true;

		// make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()),
					getQualifiedName(mmtb)));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb)),
					CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Cannot resolve class instance creation \""
					+ node.getType().toString() + "\"");
		}
		return true;
	}

	public boolean visit(ConstructorInvocation node) {
		IMethodBinding mmtb = node.resolveConstructorBinding();

		if (mtbStack.isEmpty()) // not part of a method
			return true;

		// make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()),
					getQualifiedName(mmtb)));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb)),
					CodeSegment.extract(node));
		} catch (Exception e) {
			System.err.println("Cannot resolve constructor invocation in \""
					+ "\"");
		}
		return true;
	}

	public boolean visit(SuperConstructorInvocation node) {
		IMethodBinding mmtb = node.resolveConstructorBinding();

		if (mtbStack.isEmpty()) // not part of a method
			return true;

		// make field access fact
		try {
			facts.add(Fact.makeCallsFact(getQualifiedName(mtbStack.peek()),
					getQualifiedName(mmtb)));
			entityLineMap_.put(getEntityMapKey(getQualifiedName(mtbStack.peek()), getQualifiedName(mmtb)),
					CodeSegment.extract(node));
		} catch (Exception e) {
			System.err
					.println("Cannot resolve super constructor invocation in \""
							+ "\"");
		}
		return true;
	}

	public boolean visit(VariableDeclarationStatement vds) {
		if (mtbStack.isEmpty()) // not part of a method
			return true;

		// make local variable declaration
		for (Object ovdf : vds.fragments()) {
			VariableDeclarationFragment vdf = (VariableDeclarationFragment) ovdf;
			try {
				String methodName = getQualifiedName(mtbStack.peek()),
						typeName = getQualifiedName(vds.getType().resolveBinding()),
						identifier = vdf.getName().getIdentifier(),
						expression = vdf.getInitializer().toString();
				facts.add(Fact.makeLocalVarFact(methodName, typeName, identifier, expression));
				entityLineMap_.put(getEntityMapKey(methodName, typeName, identifier, expression),
						CodeSegment.extract(vdf));
			} catch (Exception e) {
				System.err.println("Cannot resolve variable declaration \""
						+ vdf.getName().toString() + "\"");
			}
		}
		return true;
	}

}
