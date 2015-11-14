package manipulations;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;
import interfacing.Completion;
import interfacing.Completions;
import interfacing.SyntaxElement;
import interfacing.SyntaxElementType;
import parser.FusionTablesSqlParser;
import util.CollectionUtil;
import util.StringUtil;

public class QueryPatching {
	public final Optional<CursorContext> context;
	public final Object contextAtCursor;
	public final int cursorPosition;
	public Optional<Integer> newCursorPosition = Optional.absent();
	private final String query;

	private boolean bareSelect(String q) {
		return q.toLowerCase().replace("select", "").replace(" ", "").length() == 0;
	}

	public QueryPatching(Optional<CursorContext> context, int cursorPosition, String query) {
		this.context = context;
		if (context.isPresent())
			contextAtCursor = context.get();
		else
			contextAtCursor = null;

		this.cursorPosition = cursorPosition;
		this.query = query;
	}

	private void setNewCursorPosition(int i) {
		newCursorPosition = Optional.of(Integer.valueOf(i));
	}

	public String patch(Optional<Object> itemValue, Optional<String> maybeTableName) {
		String result = query;
		boolean bareSelect = bareSelect(query);

		String value = null;
		if (itemValue.isPresent() && context.isPresent()) {
			if (itemValue.get() instanceof Completion)
				value = ((Completion) itemValue.get()).completion;
			else if (itemValue.get() instanceof String)
				value = (String) itemValue.get();
			else
				Check.fail("unexpected type : " + itemValue.get().getClass().getName());

			Optional<OrderedIntTuple> boundaries = context.get().boundaries;
			if (boundaries.isPresent()) {
				result = StringUtil.replace(query, boundaries.get(), value);
				int offsetNewCursor = value.length() - 1 - (boundaries.get().hi() - boundaries.get().lo());
				setNewCursorPosition(cursorPosition + offsetNewCursor);
			} else {
				result = StringUtil.insert(query, cursorPosition, value);
				setNewCursorPosition(cursorPosition + value.length());
			}
			if (bareSelect && maybeTableName.isPresent())
				result = result + "\nFROM " + maybeTableName.get() + ";";

		}
		return result;
	}

	private final static String locColumn = "{location_column}";
	private final static String geoCircle = "ST_INTERSECTS(" + locColumn
			+ ", CIRCLE(LATLNG({number}, {number}), {number})) ";
	private final static String geoRectangle = "ST_INTERSECTS(" + locColumn
			+ ", RECTANGLE(LATLNG({number}, {number}), LATLNG({number}, {number}))) ";

	public List<Completion> geoCompletions() {
		List<Completion> result = new LinkedList<Completion>();

		result.add(new Completion("intersects circle", geoCircle));
		result.add(new Completion("intersects rectangle", geoRectangle));

		return result;
	}

	public Completions getCompletions() {
		Check.isTrue(context.isPresent());
		CursorContext cursorContext = context.get();

		Completions result = new Completions();

		if (cursorContext.contextAtCursor instanceof FusionTablesSqlParser.FusionTablesSqlContext) {
			result.add("Alter table", "ALTER TABLE {table_name} RENAME TO {name};");
			result.add("Drop table", "DROP TABLE {table_name};");
			result.add("Insert single", "INSERT INTO {table_name} ({column_name}) \nVALUES ({value});");
			result.add("Insert multi",
					"INSERT INTO {table_name} ({column_name}, {column_name}) \nVALUES ({value}, {value});");
			result.add("Select", "SELECT ");
			result.add("Update single", "UPDATE TABLE {table_name} SET {column_name} = {value};");
			result.add("Update multi",
					"UPDATE TABLE {table_name} SET {column_name} = {value}, {column_name} = {value};");

		} else if (cursorContext.contextAtCursor instanceof FusionTablesSqlParser.ExprContext) {
			result.add("=", "= {value} ");
			result.add(">", "> {value} ");
			result.add("<", "< {value} ");
			result.add(">=", "=> {value} ");
			result.add("<=", "<= {value} ");

			result.add("in", "IN('{value}', '{value}') ");
			result.add("between", "BETWEEN '{value}' AND '{value}' ");
			result.add("like", "LIKE '{value}' ");
			result.add("matches", "MATCHES '{value}' ");
			result.add("starts with", "STARTS WITH '{value}' ");
			result.add("ends with", "ENDS WITH '{value}' ");
			result.add("contains", "CONTAINS '{value}' ");
			result.add("contains ignoring case", "CONTAINS IGNORING CASE '{value}' ");
			result.add("does not contain", "DOES NOT CONTAIN '{value}' ");
			result.add("not equal to", "NOT EQUAL TO '{value}' ");
			// the error is usually a field name only
			SyntaxElement lastToken = CollectionUtil.last(cursorContext.syntaxElements);
			if (!(lastToken.type == SyntaxElementType.error)) {
				result.add("intersects circle", geoCircle.replace(locColumn, lastToken.value));
				result.add("intersects rectangle", geoRectangle.replace(locColumn, lastToken.value));
			}
		}

		return result;
	}

}
