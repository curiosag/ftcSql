package interfacing;

import java.util.HashMap;
import java.util.List;

import cg.common.check.Check;
import util.CollectionUtil;

public class Completions {
	private HashMap<String, AbstractCompletion> nameToCompletion = new HashMap<String, AbstractCompletion>(); 
	
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
	
}
