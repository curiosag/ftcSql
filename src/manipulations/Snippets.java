package manipulations;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;
import interfaces.SqlCompletionType;
import structures.Completions;


public class Snippets {

	private final static boolean extended_dml = false;
	
	private static Optional<OrderedIntTuple> noBoundaries = Optional.absent();
	private static Snippets instance = null;
	private Map<SqlCompletionType, Completions> completionMap = new HashMap<SqlCompletionType, Completions>();

	public Completions get(SqlCompletionType t)
	{
		Completions c = completionMap.get(t);
		if (c == null)
			c = new Completions(noBoundaries);
		
		return c;
	}
	
	private Completions getAggregateExpressions() {
		Completions result = new Completions(noBoundaries);

		result.addSnippet(SqlCompletionType.aggregate, "sum", "SUM(${c})");
		result.addSnippet(SqlCompletionType.aggregate, "count", "COUNT(${c})");
		result.addSnippet(SqlCompletionType.aggregate, "average", "AVERAGE(${c})");
		result.addSnippet(SqlCompletionType.aggregate, "maximum", "MAXIMUM(${c})");
		result.addSnippet(SqlCompletionType.aggregate, "minimum", "MINIMUM(${c})");

		return result;
	}

	/**
	 * ${cursor} params aren't really what's needed here
	 * every parameter starting with "c" is considered to be a column
	 * triggering column retrieval in completions
	 * @return
	 */
	private Completions getSqlStatementExpressions() {
		Completions result = new Completions(noBoundaries);

		result.addSnippet(SqlCompletionType.ftSql, "alter table", "ALTER TABLE ${t} RENAME TO ${name}; ");
		result.addSnippet(SqlCompletionType.ftSql, "drop table", "DROP TABLE ${t}; ");
		result.addSnippet(SqlCompletionType.ftSql, "select", "SELECT ${c} FROM ${t};");
		result.addSnippet(SqlCompletionType.ftSql, "insert single", "INSERT INTO ${t} (${c}) \nVALUES (${value});");
		result.addSnippet(SqlCompletionType.ftSql, "insert multi",
				"INSERT INTO ${t} (${c1}, ${c2}) \nVALUES (${value1}, ${value2});");
		
		result.addSnippet(SqlCompletionType.ftSql, "delete",
				"DELETE FROM ${t} WHERE ${c}=${value}; ");
		result.addSnippet(SqlCompletionType.ftSql, "delete all",
				"DELETE FROM ${t}; ");
		if (extended_dml) {
			result.addSnippet(SqlCompletionType.ftSql, "update single", "UPDATE ${t} SET ${c} = ${value};");
			result.addSnippet(SqlCompletionType.ftSql, "update multi",
					"UPDATE ${t} SET ${c1} = ${value1}, ${c2} = ${value2}; ");	
		}
		
		result.addSnippet(SqlCompletionType.ftSql, "describe table", "DESCRIBE ${t};");
		return result;
	}
		
	private Completions getColumnConditionExpressions(SqlCompletionType t) {
		Completions result = new Completions(noBoundaries);
		
		result.addSnippet(t, "=", "${c} = ${value} ");
		result.addSnippet(t, ">", "${c} > ${value} ");
		result.addSnippet(t, "<", "${c} < ${value} ");
		result.addSnippet(t, ">=", "${c} => ${value} ");
		result.addSnippet(t, "<=", "${c} <= ${value} ");

		result.addSnippet(t, "in", "${c} IN('${value}', '${value}') ");
		result.addSnippet(t, "between", "${c} BETWEEN '${value}' AND '${value}' ");
		result.addSnippet(t, "like", "${c} LIKE '${value}' ");
		result.addSnippet(t, "matches", "${c} MATCHES '${value}' ");
		result.addSnippet(t, "starts with", "${c} STARTS WITH '${value}' ");
		result.addSnippet(t, "ends with", "${c} ENDS WITH '${value}' ");
		result.addSnippet(t, "contains", "${c} CONTAINS '${value}' ");
		result.addSnippet(t, "contains ignoring case", "${c} CONTAINS IGNORING CASE '${value}' ");
		result.addSnippet(t, "does not contain", "${c} DOES NOT CONTAIN '${value}' ");
		result.addSnippet(t, "not equal to", "${c} NOT EQUAL TO '${value}' ");

		if (t == SqlCompletionType.columnConditionExpr){
			result.addSnippet(t, "geo condition circle", "ST_INTERSECTS(${c}, CIRCLE(LATLNG(${number}, ${number})), ${number}))) ");
			result.addSnippet(t, "geo condition rectangle", "ST_INTERSECTS(${c}, RECTANGLE(LATLNG(${number}, ${number}), LATLNG(${number}, ${number}))) ");
		}
			
		return result;
	}

	private Completions getOrderByExpressions() {
		Completions result =  new Completions(noBoundaries);
		SqlCompletionType t = SqlCompletionType.orderBy;
		result.addSnippet(t, "order by ", "ORDER BY ${c} ");
		result.addSnippet(t, "order by, descending", "ORDER BY ${c} DESC");
		result.addSnippet(t, "order by spatial distance", "ORDER BY ST_DISTANCE(${c}, LATLNG(${number}, ${number})) ");
		
		return result;
	}

	private Completions getGroupByExpressions() {
		Completions result =  new Completions(noBoundaries);
		SqlCompletionType t = SqlCompletionType.groupBy;
		result.addSnippet(t, "group by", "GROUP BY ${c} ");
		return result;
	}
	
	private Snippets() {
		completionMap.put(SqlCompletionType.ftSql, getSqlStatementExpressions());
		completionMap.put(SqlCompletionType.columnConditionExpr, getColumnConditionExpressions(SqlCompletionType.columnConditionExpr));
		completionMap.put(SqlCompletionType.columnConditionExprAfterColumn, getColumnConditionExpressions(SqlCompletionType.columnConditionExprAfterColumn));
		completionMap.put(SqlCompletionType.aggregate, getAggregateExpressions());
		completionMap.put(SqlCompletionType.groupBy, getGroupByExpressions());
		completionMap.put(SqlCompletionType.orderBy, getOrderByExpressions());
		completionMap.put(SqlCompletionType.keywordWhere, Completions.create(noBoundaries, SqlCompletionType.keywordWhere, "where - keyword", "WHERE "));
	}
	
	public static Snippets instance()
	{
		if (Snippets.instance == null)
			Snippets.instance = new Snippets();
		
		return instance;
	}
}
