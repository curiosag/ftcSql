package manipulations;

import com.google.common.base.Optional;

public class NameRecognitionTable extends NameRecognition {

	public Optional<String> TableName()
	{
		checkStateAndValues();
		return getName1();
	}
	
	public Optional<String> TableAlias()
	{
		checkStateAndValues();
		return getName2();
	}

}