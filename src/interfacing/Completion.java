package interfacing;

public class Completion {

	public final String name;
	public final String completion;
	
	public Completion(String name, String completion) {
		this.name = name;
		this.completion = completion;
	}

	@Override
	public String toString()
	{
		return name;
	}
	
}
