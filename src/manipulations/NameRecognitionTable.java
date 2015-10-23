package manipulations;

import com.google.common.base.Optional;

public class NameRecognitionTable extends NameRecognition {

	public Optional<String> TableName()
	{
		return getName1();
	}
	
	public Optional<String> TableAlias()
	{
		return getName2();
	}

}