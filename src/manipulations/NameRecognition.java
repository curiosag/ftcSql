package manipulations;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.Token;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;

public class NameRecognition {

	public NameRecognitionState state = NameRecognitionState.INITIAL;

	protected Map<NameRecognitionState, String> names = new HashMap<NameRecognitionState, String>();
	protected Map<NameRecognitionState, OrderedIntTuple> boundaries = new HashMap<NameRecognitionState, OrderedIntTuple>();
	
	/**
	 * reads token sequences <name1> [<separator> <name2>] where separator is
	 * either "." or "AS" like for "field_name", "table_name.field_name",
	 * "table_name" or "table_name AS alias"
	 */

	public NameRecognition() {
		names.put(NameRecognitionState.NAME1, null);
		names.put(NameRecognitionState.NAME2, null);
		boundaries.put(NameRecognitionState.NAME1, null);
		boundaries.put(NameRecognitionState.NAME2, null);
	}

	public void digest(Token token) {
		Check.notNull(token);
		Check.notEmpty(token.getText());

		if (state == NameRecognitionState.ERROR)
			return;

		state = state.next(token.getText());
		if (state.in(NameRecognitionState.NAME1, NameRecognitionState.NAME2))
		{
			names.put(state, token.getText());
			boundaries.put(state, OrderedIntTuple.instance(token.getStartIndex(), token.getStopIndex()));
		}
	};

	public Optional<String> getName1() {
		return Optional.fromNullable(names.get(NameRecognitionState.NAME1));
	}

	public Optional<String> getName2() {
		return Optional.fromNullable(names.get(NameRecognitionState.NAME2));
	}

	public Optional<OrderedIntTuple> getBoundaries1() {
		return Optional.fromNullable(boundaries.get(NameRecognitionState.NAME1));
		}

	public Optional<OrderedIntTuple> getBoundaries2() {
		return Optional.fromNullable(boundaries.get(NameRecognitionState.NAME2));
	}
	
}
