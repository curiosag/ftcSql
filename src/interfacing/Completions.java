package interfacing;

import java.util.HashMap;
import java.util.List;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;
import util.CollectionUtil;

public class Completions {
	private HashMap<String, AbstractCompletion> nameToCompletion = new HashMap<String, AbstractCompletion>(); 
	
	public final Optional<OrderedIntTuple> replacementBoundaries;
	
	public Completions(Optional<OrderedIntTuple> replacementBoundaries)
	{
		this.replacementBoundaries = replacementBoundaries;
	}
	
	public CodeSnippetCompletion addSnippet(SqlCompletionType completionType, String name, String snippet){
		CodeSnippetCompletion  result = new CodeSnippetCompletion(completionType, name, snippet);
		nameToCompletion.put(name, result);
		return result;
	};
	
	public ModelElementCompletion addModelElement(SqlCompletionType type, String displayName, AbstractCompletion parent){
		ModelElementCompletion result = new ModelElementCompletion(type, displayName,  parent);
		nameToCompletion.put(displayName, result);
		return result;
	};
	
	public void addAll(Completions completions)
	{
		for (AbstractCompletion c : completions.getAll()) 
			nameToCompletion.put(c.displayName, c);
	}
	
	public AbstractCompletion getCompletion(String name){
		AbstractCompletion c = nameToCompletion.get(name);
		Check.notNull(c);
		return c;
	}
	
	public List<AbstractCompletion> getAll()
	{
		return CollectionUtil.toList(nameToCompletion.values());
	}
	
	public int size()
	{
		return nameToCompletion.size();
	}

	public void add(AbstractCompletion c) {
		nameToCompletion.put(c.displayName, c);
	}

	public static String patchFromCompletion(AbstractCompletion completion) {
		String result = null;
		if (completion instanceof ModelElementCompletion)
			result = ((ModelElementCompletion) completion).displayName;
		else if (completion instanceof CodeSnippetCompletion)
			result = ((CodeSnippetCompletion) completion).snippet;
		else
			Check.fail("unexpected type : " + completion.getClass().getName());
		return result;
	}
	
}
