/**
 * 
 */
package changetypes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.*;

/**
 * @author viett
 *
 */
public class CodeSegment {
	private IJavaElement file_ = null;
	public IJavaElement getFile() { return file_; }
	
	public class LineSegment {
		private int first_, last_;
		public int getFirstLineNumber() { return first_; }
		public int getLastLineNumber() { return last_ ; }
		
		public LineSegment(int line) {
			first_ = last_ = line;
		}
		public LineSegment(int first, int last) {
			first_ = first;
			last_ = last;
		}
		public String toString() {
			return first_ + (last_ != first_ ? "-" + last_ : "");
		}
		public int getBeginning() {
			return first_;
		}
		public int getEnd() {
			return last_;
		}
	}
	
	private List<LineSegment> lines = new ArrayList<LineSegment>();
	public List<LineSegment> getLines() { return lines; }
	
	public CodeSegment(IJavaElement fi) {
		file_ = fi;
	}
	
	public void addLineSegment(int first, int last) {
		lines.add(new LineSegment(first, last));
	}
	
	public void addLineSegment(int line) {
		lines.add(new LineSegment(line));
	}
	
	public String toString() {
		return lines.toString();
	}
	
	private static CodeSegment extractNode(ASTNode node) {
		try {
		CompilationUnit cu = (CompilationUnit) node.getRoot(); 
		IJavaElement f = cu.getJavaElement();
		int first = node.getStartPosition();
		int last = first + node.getLength();
		CodeSegment ret = new CodeSegment(f);
		ret.addLineSegment(cu.getLineNumber(first), cu.getLineNumber(last));
		return ret;
		} catch (Exception e) {
			System.err.println("Map code line failed for node " + node.toString());
			System.err.println(e.getMessage());
		}
		return null;
	}
	
	public static CodeSegment extract(PackageDeclaration node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(TypeDeclaration node) {
		return extractNode(node);
	}
	
	public static CodeSegment extractAtName(TypeDeclaration node) {
		return extractNode(node.getName());
	}
	
	public static CodeSegment extract(TypeDeclaration node, IVariableBinding ivb) {
		CompilationUnit cu = (CompilationUnit) node.getRoot();
		ASTNode vdf = cu.findDeclaringNode(ivb);
		return extractNode(vdf);
	}
	
	public static CodeSegment extract(TypeDeclaration node, ITypeBinding itb) {
		CompilationUnit cu = (CompilationUnit) node.getRoot();
		ASTNode vdf = cu.findDeclaringNode(itb);
		return extractNode(vdf);
	}

	public static CodeSegment extract(AnonymousClassDeclaration node, IVariableBinding ivb) {
		CompilationUnit cu = (CompilationUnit) node.getRoot();
		ASTNode vdf = cu.findDeclaringNode(ivb);
		return extractNode(vdf);
	}
	
	public static CodeSegment extract(IfStatement node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(CastExpression node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(AnonymousClassDeclaration node) {
		return extractNode(node);
	}
		
	public static CodeSegment extract(MethodDeclaration node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(MethodDeclaration node, Fact.FactTypes type) {
		switch (type) {
		case METHODMODIFIER:
		case RETURN:
		case PARAMETER:
		case THROWN:
			return extractNode(node.getName());
		case METHODBODY:
			Block body = node.getBody();
			if (body == null)
				return extractNode(node);
			return extractNode(body);
		default:
			return extractNode(node.getName());
		}
	}
	
	public static CodeSegment extract(FieldAccess node) {
		return extractNode(node);
	}
	
	public static CodeSegment extractMethod(IMethodBinding mtb, ASTNode node) {
		try {
			CompilationUnit cu = (CompilationUnit) node.getRoot();
			ASTNode mNode = cu.findDeclaringNode(mtb);
			return extractNode(mNode);
		} catch (Exception e) {
			return extractNode(node);
		}
	}
	
	public static CodeSegment extract(TryStatement node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(Name node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(MethodInvocation node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(SuperMethodInvocation node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(ClassInstanceCreation node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(ConstructorInvocation node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(SuperConstructorInvocation node) {
		return extractNode(node);
	}
	
	public static CodeSegment extract(VariableDeclarationFragment node) {
		return extractNode(node);
	}
}
