package manipulations;

import com.google.common.base.Optional;

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

	public Optional<String> TableName() {
		if (state.in(NameRecognitionState.NAME2, NameRecognitionState.QUALIFIER)) {
			return getName1();
		} else
			return Optional.absent();
	}

}
