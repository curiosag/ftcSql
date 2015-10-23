package manipulations;

import com.google.common.base.Optional;

import cg.common.check.Check;

public class CursorContextTableName extends CursorContext {

	public final Optional<String> tableName;
	
	public CursorContextTableName(CursorContextListener c) {
		super();
		Check.isTrue(c.atCursor.isPresent());
		Check.isTrue(c.atCursor.get() instanceof NameRecognitionTable);
		NameRecognitionTable atCursor = (NameRecognitionTable)c.atCursor.get();
		tableName = atCursor.TableName();
	}
	
}
