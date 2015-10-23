package manipulations;

import com.google.common.base.Optional;

import cg.common.check.Check;

public class NameRecognition {

	public NameRecognitionState state = NameRecognitionState.INITIAL;

	protected String[] names = new String[2];
	private int count = 0;
	
	private void add(String name)
	{
		Check.isTrue(count < 2);
		names[count] = name;
		count++;
	}

	/**
	 * reads token sequences <name1> [<separator> <name2>] where 
	 * separator is either "." or "AS" 
	 * like for "field_name", "table_name.field_name", "table_name" or "table_name AS alias"
	 */
	
	public NameRecognition() {
	}

	public void digest(String t)
	{
		Check.notEmpty(t);
		
		if (state == NameRecognitionState.ERROR)
			return;
		
		state = state.next(t);
		if (state.in(NameRecognitionState.NAME1, NameRecognitionState.NAME2))
			add(t);
	};
	
	public Optional<String> getName1()
	{
		checkStateAndValues();
		
		if (state.in(NameRecognitionState.NAME1, NameRecognitionState.NAME2, NameRecognitionState.QUALIFIER))
			return Optional.of(names[0]);
		else
			return Optional.absent();
	}
	
	public Optional<String> getName2()
	{
		checkStateAndValues();
		
		if (state == NameRecognitionState.NAME2)
			return Optional.of(names[1]);
		else
			return Optional.absent();	
	}
	
	protected void checkStateAndValues()
	{
		if (state == NameRecognitionState.ERROR)
			return;
		
		Check.isTrue(count == 0 && state == NameRecognitionState.INITIAL || count == 1 && state.in(NameRecognitionState.NAME1, NameRecognitionState.QUALIFIER) || count == 2 && state == NameRecognitionState.NAME2);
	}
}
