package manipulations;

import java.util.List;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;

public class CursorContext {

	public final CursorContextType contextType;

	public final Optional<String> name;
	public final Optional<OrderedIntTuple> boundaries;
	public final Optional<String> otherName;
	public final Optional<OrderedIntTuple> otherBoundaries;
	public final List<NameRecognitionTable> tableList;

	public CursorContext(CursorContextListener c) {
		Check.isTrue(c.atCursor.isPresent());

		tableList = c.tableList;
		if (c.atCursor.get() instanceof NameRecognitionTable) {
			contextType = CursorContextType.table;
			NameRecognitionTable atCursor = (NameRecognitionTable) c.atCursor.get();

			name = resolveTableName(atCursor.TableName(), c.tableList);
			boundaries = atCursor.BoundariesTableName();
			otherName = Optional.absent();
			otherBoundaries = Optional.absent();

		} else if (c.atCursor.get() instanceof NameRecognitionColumn) {
			contextType = CursorContextType.column;
			NameRecognitionColumn atCursor = (NameRecognitionColumn) c.atCursor.get();
			name = atCursor.ColumnName();
			boundaries = atCursor.BoundariesColumnName();
			otherName = resolveTableName(atCursor.TableName(), c.tableList);
			otherBoundaries = atCursor.BoundariesTableName();

		} else
			throw new RuntimeException("invalid case: " + c.atCursor.get().getClass().getName());
	}

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

	public static Optional<CursorContext> instance(CursorContextListener context) {
		Check.notNull(context);
		
		if (context.atCursor.isPresent())
			return Optional.of(new CursorContext(context));
		else
			return Optional.absent();
	}

	private Optional<String> resolveTableName(Optional<String> tableNameRecognized,
			List<NameRecognitionTable> tableList) {

		if (!tableNameRecognized.isPresent())
			return Optional.absent();

		for (NameRecognitionTable r : tableList)
			if (r.TableAlias().isPresent() && tableNameRecognized.equals(r.TableAlias()))
				return r.TableName();

		return tableNameRecognized;
	}

}
