package manipulations;

import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;
import interfacing.SyntaxElement;

public class CursorContext {

	public final CursorContextType contextType;

	public final ParserRuleContext contextAtCursor;
	public final Stack<ParserRuleContext> contextStack;

	public final Optional<String> name;
	public final Optional<OrderedIntTuple> boundaries;
	public final Optional<String> otherName;
	public final Optional<OrderedIntTuple> otherBoundaries;
	public final List<NameRecognitionTable> tableList;
	public final List<SyntaxElement> syntaxElements;

	public CursorContext(CursorContextListener c) {
		Check.isTrue(c.contextAtCursor.isPresent());
	
		tableList = c.tableList;
		this.contextAtCursor = c.contextAtCursor.get();
		this.contextStack = c.contextStack;
		this.syntaxElements = c.syntaxElements;

		if (!c.nameAtCursor.isPresent()) {
			contextType = CursorContextType.anyRule;
			name = Optional.absent();
			boundaries = Optional.absent();
			otherName = Optional.absent();
			otherBoundaries = Optional.absent();
		} else if (c.nameAtCursor.get() instanceof NameRecognitionTable) {
			contextType = CursorContextType.tableName;
			NameRecognitionTable atCursor = (NameRecognitionTable) c.nameAtCursor.get();

			name = resolveTableName(atCursor.TableName(), c.tableList);
			boundaries = atCursor.BoundariesTableName();
			otherName = Optional.absent();
			otherBoundaries = Optional.absent();

		} else if (c.nameAtCursor.get() instanceof NameRecognitionColumn) {
			contextType = CursorContextType.columnName;
			NameRecognitionColumn atCursor = (NameRecognitionColumn) c.nameAtCursor.get();
			name = atCursor.ColumnName();
			boundaries = atCursor.BoundariesColumnName();
			otherName = resolveTableName(atCursor.TableName(), c.tableList);
			otherBoundaries = atCursor.BoundariesTableName();

		} else
			throw new RuntimeException("invalid case: " + c.nameAtCursor.get().getClass().getName());
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

		if (context.contextAtCursor.isPresent())
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
