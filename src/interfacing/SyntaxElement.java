package interfacing;

import java.util.List;

public class SyntaxElement {

	public final int from;
	public final int to;
	public final SyntaxElementType type;
	public final String value;
	
	private SyntaxElement(String value, int from, int to, SyntaxElementType type)
	{
		this.from = from;
		this.to= to;
		this.type = type;
		this.value = value;
	}
	
	public static SyntaxElement create(String value, int from, int to, SyntaxElementType type)
	{
		return new SyntaxElement(value, from, to, type);
	}
	
	public static boolean findCaseInsensitive(List<SyntaxElement> l, String value)
	{
		String lcValue = value.toLowerCase();
		for (SyntaxElement e : l) 
			if (e.value.toLowerCase().equals(lcValue))
				return true;
		return false;
	}
	
}
