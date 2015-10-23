package manipulations;

import java.util.List;

import com.google.common.base.Optional;

import cg.common.check.Check;

public class CursorContextColumnName extends CursorContext {

	public final Optional<String> columnName;
	public final Optional<String> tableName;

	public CursorContextColumnName(CursorContextListener c) {
		Check.isTrue(c.atCursor.isPresent());
		Check.isTrue(c.atCursor.get() instanceof NameRecognitionColumn);
		NameRecognitionColumn atCursor = (NameRecognitionColumn) c.atCursor.get();

		columnName = atCursor.ColumnName();
		tableName = resolveTableName(atCursor.TableName(), c.tableList);
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
