package manipulations;

import com.google.common.base.Optional;

import gc.common.structures.OrderedIntTuple;

public class NameRecognitionColumn extends NameRecognition {
	
	public Optional<String> ColumnName() {
		switch (state) {
		case NAME1:
			return getName1();
		case NAME2:
			return getName2();
		default:
			return Optional.absent();
		}
	}
	
	public Optional<OrderedIntTuple> BoundariesColumnName()
	{
		switch (state) {
		case NAME1:
			return getBoundaries1();
		case NAME2:
			return getBoundaries2();
		default:
			return Optional.absent();
		}
	}
	
	public Optional<String> TableName() {
		if (state.in(NameRecognitionState.NAME2, NameRecognitionState.QUALIFIER)) {
			return getName1();
		} else
			return Optional.absent();
	}
	
	public Optional<OrderedIntTuple> BoundariesTableName() {
		if (state.in(NameRecognitionState.NAME2, NameRecognitionState.QUALIFIER)) {
			return getBoundaries1();
		} else
			return Optional.absent();
	}

}
