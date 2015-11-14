package interfacing;

import java.util.HashMap;
import java.util.List;

import cg.common.check.Check;
import util.CollectionUtil;

public class Completions {
	private HashMap<String, Completion> nameToCompletion = new HashMap<String, Completion>(); 
	
	public void add(String name, String completion){
		nameToCompletion.put(name, new Completion(name, completion));
	};
	
	public String getCompletion(String name){
		Completion c = nameToCompletion.get(name);
		Check.notNull(c);
		return c.completion;
	}
	
	public List<Completion> getAll()
	{
		return CollectionUtil.toList(nameToCompletion.values());
	}
	
	public int size()
	{
		return nameToCompletion.size();
	}
	
}
