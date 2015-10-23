package manipulations;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

import cg.common.check.Check;

public class NameRecognition {

	public NameRecognitionState state = NameRecognitionState.INITIAL;

	protected Map<NameRecognitionState, String> names = new HashMap<NameRecognitionState, String>();

	/**
	 * reads token sequences <name1> [<separator> <name2>] where separator is
	 * either "." or "AS" like for "field_name", "table_name.field_name",
	 * "table_name" or "table_name AS alias"
	 */

	public NameRecognition() {
		names.put(NameRecognitionState.NAME1, null);
		names.put(NameRecognitionState.NAME2, null);
	}

	public void digest(String t) {
		Check.notEmpty(t);

		if (state == NameRecognitionState.ERROR)
			return;

		state = state.next(t);
		if (state.in(NameRecognitionState.NAME1, NameRecognitionState.NAME2))
			names.put(state, t);
	};

	public Optional<String> getName1() {
		return Optional.fromNullable(names.get(NameRecognitionState.NAME1));
	}

	public Optional<String> getName2() {
		return Optional.fromNullable(names.get(NameRecognitionState.NAME2));
	}

}
