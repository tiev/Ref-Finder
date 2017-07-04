package lsclipse.linegetter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LineGetterFactory {
	
	public static Set<LineGetter> GETTERS = new HashSet<LineGetter>(Arrays.asList(
			// Refactoring types using SimpleLineGetter
			// + Change bi to uni
			// + Change uni to bi
			// + Consolidate cond expression
			// + Consolidate duplicate cond fragments
			// + Decompose conditional
			// + Encapsulate collection
			new ExtractMethod(),
			new FormTemplateMethod(),
			// + Inline method
			// + Inline temp <!> affect whole method body
			// + Introduce assertion
			// + Introduce explaining variable <!> affect whole method body
			// + Introduce null object
			// + Introduce parameter object
			new MoveMethod(),
			// + Parameterize method
			// + Preserve whole object
			// + Remove assignment to parameters
			// + Remove control flag
			// + Rename method <!> affect whole method body
			// + Replace array with object
			new ReplaceConditionalWithPolymorphism(),
			// + Replace data with object
			// + Replace exception with test
			new ReplaceMethodWithMethodObject()
			// + Replace nested cond guard clauses
			// + Replace param explicit methods
			// + Replace subclass with field
			// + Replace type code with state
			// + Replace type code with subclasses
			// + Separate query from modifier
			));
	public static LineGetter DEFAULT_GETTER = new SimpleLineGetter();
		
	public static LineGetter returnLineGetterByName(String name) {
		for (LineGetter lg : GETTERS) {
			if (lg.getName().equals(name))
				return lg;
		}

		return DEFAULT_GETTER;
	}
}
