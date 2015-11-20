package manipulations;

import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;
import interfacing.AbstractCompletion;
import interfacing.CodeSnippetCompletion;
import interfacing.ColumnInfo;
import interfacing.Completions;
import interfacing.ModelElementCompletion;
import interfacing.SqlCompletionType;
import interfacing.TableInfo;
import util.StringUtil;

public class QueryPatching {
	public final CursorContext cursorContext;
	public final Optional<ParserRuleContext> parserRuleContext;
	public final int cursorPosition;
	public Optional<Integer> newCursorPosition = Optional.absent();
	private final String query;
	private final List<TableInfo> tableInfo;

	private boolean bareSelect(String q) {
		return q.toLowerCase().replace("select", "").replace(" ", "").length() == 0;
	}

	public QueryPatching(List<TableInfo> tableInfo, CursorContext context, int cursorPosition, String query) {
		this.cursorContext = context;
		parserRuleContext = context.getParserRuleContext();

		this.cursorPosition = cursorPosition;
		this.query = query;
		this.tableInfo = tableInfo;
	}

	private void setNewCursorPosition(int i) {
		newCursorPosition = Optional.of(Integer.valueOf(i));
	}

	public String patch(AbstractCompletion completion) {
		String result = query;
		boolean bareSelect = bareSelect(query);

		String value = patchFromCompletion(completion);
		Optional<OrderedIntTuple> boundaries = cursorContext.boundaries;

		if (completion.completionType == SqlCompletionType.columnConditionExprAfterColumn)
			result = appendCompletion(value.replace("{column_name}", ""));
		else {
			if (boundaries.isPresent()) {
				result = StringUtil.replace(query, boundaries.get(), value);
				int offsetNewCursor = value.length() - 1 - (boundaries.get().hi() - boundaries.get().lo());
				setNewCursorPosition(cursorPosition + offsetNewCursor);
			} else
				result = appendCompletion(value);
		}

		if (bareSelect && completion.completionType == SqlCompletionType.column && completion.parent != null)
			result = result + "\nFROM " + completion.parent.displayName + ";";

		return result;
	}

	private String appendCompletion(String value) {
		String result;
		result = StringUtil.insert(query, cursorPosition, value);
		setNewCursorPosition(cursorPosition + value.length());
		return result;
	}

	private String patchFromCompletion(AbstractCompletion completion) {
		String result = null;
		if (completion instanceof ModelElementCompletion)
			result = ((ModelElementCompletion) completion).displayName;
		else if (completion instanceof CodeSnippetCompletion)
			result = ((CodeSnippetCompletion) completion).snippet;
		else
			Check.fail("unexpected type : " + completion.getClass().getName());
		return result;
	}

	private final static boolean addColumnDetails = true;

	public Completions getCompletions() {
		Completions result = new Completions();

		for (SqlCompletionType c : cursorContext.completionOptions)
			switch (c) {
			case table:
				result.addAll(toCompletions(tableInfo, !addColumnDetails));
				break;

			case column:
				if (cursorContext.underlyingTableName.isPresent()) {
					Optional<TableInfo> i = findTableInfo(cursorContext.underlyingTableName.get());
					if (i.isPresent()) {
						result.addAll(columnsToCompletions(i.get()));
						break;
					}
				}
				result.addAll(toCompletions(tableInfo, addColumnDetails));
				break;

			case unknown:
				;

			default:
				result.add(Snippets.instance().get(c));
			}

		return result;
	}

	private Optional<TableInfo> findTableInfo(String tableName) {
		for (TableInfo i : tableInfo)
			if (i.name.equals(tableName))
				return Optional.of(i);

		return Optional.absent();
	}

	private Completions toCompletions(List<TableInfo> tableInfo, boolean addDetails) {
		Completions result = new Completions();
		for (TableInfo t : tableInfo)
			result.add(fromTable(t, addDetails));

		return result;
	}

	private Completions columnsToCompletions(TableInfo tableInfo) {
		Completions result = new Completions();
		for (ColumnInfo c : tableInfo.columns)
			result.add(new ModelElementCompletion(SqlCompletionType.column, c.name, null));

		return result;
	}

	private ModelElementCompletion fromTable(TableInfo t, boolean addDetails) {
		ModelElementCompletion result = new ModelElementCompletion(SqlCompletionType.table, t.name, null);

		if (addDetails)
			for (ColumnInfo c : t.columns)
				result.addChild(new ModelElementCompletion(SqlCompletionType.column, c.name, result));

		return result;
	}

}
