package manipulations;

import java.util.HashMap;
import java.util.Map;

import cg.common.check.Check;
import interfacing.AbstractCompletion;
import interfacing.CodeSnippetCompletion;
import interfacing.Completions;
import interfacing.SqlCompletionType;

public class Snippets {

	private static Snippets instance = null;
	private Map<SqlCompletionType, Completions> completionMap = new HashMap<SqlCompletionType, Completions>();

	public AbstractCompletion get(SqlCompletionType t)
	{
		Completions c = completionMap.get(t);
		Check.notNull(c);
		
		return new CodeSnippetCompletion(SqlCompletionType.categorySnippet, t.name() + "-snippets", "").addAsChildren(c);
	}
	
	private Completions getAggregateExpressions() {
		Completions result = new Completions();

		result.addSnippet(SqlCompletionType.aggregate, "sum", "SUM({column_name})");
		result.addSnippet(SqlCompletionType.aggregate, "count", "COUNT({column_name})");
		result.addSnippet(SqlCompletionType.aggregate, "average", "AVERAGE({column_name})");
		result.addSnippet(SqlCompletionType.aggregate, "maximum", "MAXIMUM({column_name})");
		result.addSnippet(SqlCompletionType.aggregate, "minimum", "MINIMUM({column_name})");

		return result;
	}

	private Completions getSqlStatementExpressions() {
		Completions result = new Completions();

		result.addSnippet(SqlCompletionType.ftSql, "alter table", "ALTER TABLE {table_name} RENAME TO {name};");
		result.addSnippet(SqlCompletionType.ftSql, "drop table", "DROP TABLE {table_name};");
		result.addSnippet(SqlCompletionType.ftSql, "insert single", "INSERT INTO {table_name} ({column_name}) \nVALUES ({value});");
		result.addSnippet(SqlCompletionType.ftSql, "insert multi",
				"INSERT INTO {table_name} ({column_name}, {column_name}) \nVALUES ({value}, {value});");
		result.addSnippet(SqlCompletionType.ftSql, "select", "SELECT ");
		result.addSnippet(SqlCompletionType.ftSql, "update single", "UPDATE TABLE {table_name} SET {column_name} = {value};");
		result.addSnippet(SqlCompletionType.ftSql, "update multi",
				"UPDATE TABLE {table_name} SET {column_name} = {value}, {column_name} = {value};");
		return result;
	}
		
	private Completions getColumnConditionExpressions(SqlCompletionType t) {
		Completions result = new Completions();
		
		result.addSnippet(t, "=", "{column_name} = {value} ");
		result.addSnippet(t, ">", "{column_name} > {value} ");
		result.addSnippet(t, "<", "{column_name} < {value} ");
		result.addSnippet(t, ">=", "{column_name} => {value} ");
		result.addSnippet(t, "<=", "{column_name} <= {value} ");

		result.addSnippet(t, "in", "{column_name} IN('{value}', '{value}') ");
		result.addSnippet(t, "between", "{column_name} BETWEEN '{value}' AND '{value}' ");
		result.addSnippet(t, "like", "{column_name} LIKE '{value}' ");
		result.addSnippet(t, "matches", "{column_name} MATCHES '{value}' ");
		result.addSnippet(t, "starts with", "{column_name} STARTS WITH '{value}' ");
		result.addSnippet(t, "ends with", "{column_name} ENDS WITH '{value}' ");
		result.addSnippet(t, "contains", "{column_name} CONTAINS '{value}' ");
		result.addSnippet(t, "contains ignoring case", "{column_name} CONTAINS IGNORING CASE '{value}' ");
		result.addSnippet(t, "does not contain", "{column_name} DOES NOT CONTAIN '{value}' ");
		result.addSnippet(t, "not equal to", "{column_name} NOT EQUAL TO '{value}' ");

		if (t == SqlCompletionType.columnConditionExpr){
			result.addSnippet(t, "geo condition circle", "ST_INTERSECTS({location_column}, CIRCLE(LATLNG({number}, {number})), {number}))) ");
			result.addSnippet(t, "geo condition rectangle", "ST_INTERSECTS({location_column}, RECTANGLE(LATLNG({number}, {number}), LATLNG({number}, {number}))) ");
		}
			
		return result;
	}

	private Completions getOrderByExpressions() {
		Completions result =  new Completions();
		SqlCompletionType t = SqlCompletionType.orderBy;
		result.addSnippet(t, "order by ", "ORDER BY {column_name} ");
		result.addSnippet(t, "order by, descending", "ORDER BY {column_name} DESC");
		result.addSnippet(t, "order by spatial distance", "ORDER BY ST_DISTANCE({location_column}, LATLNG({number}, {number})) ");
		
		return result;
	}

	private Completions getGroupByExpressions() {
		Completions result =  new Completions();
		SqlCompletionType t = SqlCompletionType.groupBy;
		result.addSnippet(t, "group by single", "GROUP BY {column_name} ");
		result.addSnippet(t, "group by multi", "GROUP BY {column_name}, {column_name} ");
		return result;
	}
	
	private Snippets() {
		completionMap.put(SqlCompletionType.ftSql, getSqlStatementExpressions());
		completionMap.put(SqlCompletionType.columnConditionExpr, getColumnConditionExpressions(SqlCompletionType.columnConditionExpr));
		completionMap.put(SqlCompletionType.columnConditionExprAfterColumn, getColumnConditionExpressions(SqlCompletionType.columnConditionExprAfterColumn));
		completionMap.put(SqlCompletionType.aggregate, getAggregateExpressions());
		completionMap.put(SqlCompletionType.groupBy, getGroupByExpressions());
		completionMap.put(SqlCompletionType.orderBy, getOrderByExpressions());
	}
	
	public static Snippets instance()
	{
		if (Snippets.instance == null)
			Snippets.instance = new Snippets();
		
		return instance;
	}
}
