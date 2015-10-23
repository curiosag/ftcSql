package manipulations;

import com.google.common.base.Optional;

import cg.common.check.Check;

public class CursorContext {

	/**
	 * 
	 * @return absent: if the cursorIdex doesen't reflect a table or column
	 *         context otherwise: either an Optional of TableNameRecognition or
	 *         ColumnNameRecognition
	 * 
	 *         In both cases name properties may be incomplete or entirely
	 *         absent, indicating, that it actually is a column or table
	 *         context, but no names have been typed yet
	 * 
	 *         If it is a ColumnNameRecognition, the associated table name, if
	 *         any, reflects the actual table name as defined in the query with
	 *         aliases resolved
	 * 
	 *         Examples: cursorIndex = 2 "SELECT a from A" -> absent
	 *         (cursorIndex missed)
	 * 
	 *         assume that cursorIndex is in the column range
	 *         "SELECT A.a from X as A;" -> TableName = "X", ColumnName = "a"
	 *         "SELECT a from A;" -> TableName = "A", ColumnName = "a"
	 *         "SELECT a " -> TableName = absent, ColumnName = "a" "SELECT A.a "
	 *         -> TableName = absent, ColumnName = "a"
	 * 
	 *         assume that cursorIndex is in the table name range
	 *         "SELECT a  from A;" -> TableName = "A", TableAlias = absent
	 *         "SELECT   from A as X;" -> TableName = "A", TableAlias = "X"
	 */
	
	@SuppressWarnings("unused")
	public static Optional<CursorContext> instance(CursorContextListener contextListener) {
		Check.notNull(contextListener);
		
		if (! contextListener.atCursor.isPresent())
			return Optional.absent();
		
		String recognition = contextListener.atCursor.get().getClass().getName();
		CursorContext result = null;
		
		if (NameRecognitionTable.class.getName().equals(recognition)) 
			result = new CursorContextTableName(contextListener); 
		else if (NameRecognitionColumn.class.getName().equals(recognition))
			result = new CursorContextColumnName(contextListener);
	
		if (result == null)
			throw new RuntimeException("invalid case");
		
		return Optional.of(result);		
	}

}
