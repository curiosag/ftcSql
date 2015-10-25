package manipulations;

import com.google.common.base.Optional;

import gc.common.structures.OrderedIntTuple;

public class NameRecognitionTable extends NameRecognition {

	public Optional<String> TableName()
	{
		return getName1();
	}
	
	public Optional<OrderedIntTuple> BoundariesTableName()
	{
		return getBoundaries1();
	}
	
	public Optional<String> TableAlias()
	{
		return getName2();
	}

	public Optional<OrderedIntTuple> BoundariesAlias()
	{
		return getBoundaries2();
	}
	
}