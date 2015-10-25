package manipulations;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;

public class CursorContextTableName extends CursorContext {

	public final Optional<String> tableName;
	public final Optional<OrderedIntTuple> boundariesTableName;
	
	public CursorContextTableName(CursorContextListener c) {
		super();
		Check.isTrue(c.atCursor.isPresent());
		Check.isTrue(c.atCursor.get() instanceof NameRecognitionTable);
		NameRecognitionTable atCursor = (NameRecognitionTable)c.atCursor.get();
		tableName = atCursor.TableName();
		boundariesTableName = atCursor.BoundariesTableName();
	}
	
}
