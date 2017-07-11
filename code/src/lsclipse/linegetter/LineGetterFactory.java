package lsclipse.linegetter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LineGetterFactory {
	
	public static Set<LineGetter> GETTERS = new HashSet<LineGetter>(Arrays.asList(
			// Refactoring types using ActionLineGetter
			// + Change bi to uni
			// + Change uni to bi
			// + Consolidate cond expression
			// + Consolidate duplicate cond fragments
			// + Decompose conditional
			new DecomposeConditional(),
			// + Encapsulate collection
			new ExtractMethod(),
			// + Form template method
			new InlineMethod(),
			// + Inline temp <!> affect whole method body
			// + Introduce assertion <!> affect whole method body
			// + Introduce explaining variable <!> affect whole method body
			// + Introduce null object
			// + Introduce parameter object
			new MoveMethod(),
			// + Parameterize method
			// + Preserve whole object
			// + Remove assignment to parameters <!> affect whole method body
			// + Remove control flag
			// + Rename method <!> affect whole method body
			// + Replace array with object
			new ReplaceConditionalWithPolymorphism(),
			// + Replace data with object
			// + Replace exception with test <!> affect whole method body
			new ReplaceMethodWithMethodObject(),
			// + Replace nested cond guard clauses
			// + Replace param explicit methods
			// + Replace subclass with field
			// + Replace type code with state
			// + Replace type code with subclasses
			// + Separate query from modifier
			//------------------
			// Refactoring types defined in TopologycalSort
			// + Move field
			// + Push down field
			// + Push down method
			// + Pull up field
			// + Pull up method
			// + Collapse hierarchy
			// + Remove parameter
			// + Add parameter
			// + Extract class
			// + Inline class
			// + Hide delegate
			// + Remove middle man
			// + Introduce local extension
			// + Replace constructor with factory method
			// + Replace magic number with constant
			// + Replace param with method
			// + Hide method
			// + Pull up constructor body
			// + Extract subclass
			new ExtractSuperclass()
			// + Extract interface
			// + Change value to reference
			// + Change reference to value
			// + Encapsulate downcast
			// + Tease apart inheritance <!> move_field, move_method with name is `?`
			// + Replace inheritance with delegation
			// + Replace delegation with inheritance
			// + Replace type code with class
			// + Encapsulate field
			// + Remove setting method
			// + Replace error code with exception
			// + Self encapsulate field
			// + Extract hierarchy <!> ignore some compount refactoring that cannot trace
			// + Replace temp with query
			//-------------------
			));
	public static final LineGetter SIMPLE_GETTER = new SimpleLineGetter();
	public static final LineGetter ACTION_GETTER = new ActionLineGetter();
		
	public static LineGetter returnLineGetterByName(String name) {
		for (LineGetter lg : GETTERS) {
			if (lg.getName().equals(name))
				return lg;
		}

		return ACTION_GETTER;
	}
}
