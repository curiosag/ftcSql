package manipulations;

import java.util.List;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;

public class CursorContextColumnName extends CursorContext {

	public final Optional<String> columnName;
	public final Optional<String> tableName;
	public final Optional<OrderedIntTuple> boundariesColumnName;
	public final Optional<OrderedIntTuple> boundariesTableName;
	public final List<NameRecognitionTable> tableList;

	public CursorContextColumnName(CursorContextListener c) {
		Check.isTrue(c.atCursor.isPresent());
		Check.isTrue(c.atCursor.get() instanceof NameRecognitionColumn);
		NameRecognitionColumn atCursor = (NameRecognitionColumn) c.atCursor.get();

		columnName = atCursor.ColumnName();
		tableName = resolveTableName(atCursor.TableName(), c.tableList);
		boundariesColumnName = atCursor.BoundariesColumnName();
		boundariesTableName = atCursor.BoundariesTableName();
		tableList = c.tableList;
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
