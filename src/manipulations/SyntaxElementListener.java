package manipulations;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import interfacing.SyntaxElement;
import interfacing.SyntaxElementType;
import parser.FusionTablesSqlParser;
import util.CollectionUtil;

public class SyntaxElementListener extends BaseFtListener implements OnError {
	private final boolean debug = true;

	private static String[] sql_keywords = { "ALTER", "AND", "OR", "AS", "ASC", "AVERAGE", "BY", "BETWEEN", "CASE",
			"COLUMN", "COUNT", "CREATE", "DELETE", "DESC", "DROP", "FROM", "GROUP", "HAVING", "IN", "INSERT", "INTO",
			"JOIN", "LEFT", "LIKE", "LIMIT", "ON", "ORDER", "OUTER", "RENAME", "SELECT", "SUM", "SET", "TABLE", "TO",
			"UPDATE", "VALUES", "VIEW", "WHERE" };

	private static String[] ft_keywords = { "CIRCLE", "WITH", "ST_DISTANCE", "ST_INTERSECTS", "STARTS", "CONTAINS",
			"RECTANGLE", "LATLNG", "DOES", "CONTAIN", "ENDS", "IGNORING", "NOT", "EQUAL", "OF", "OFFSET", "MATCHES",
			"MAXIMUM", "MINIMUM", };

	private static List<String> sqlkeywords = CollectionUtil.sort(CollectionUtil.toList(sql_keywords));
	private static List<String> ftkeywords = CollectionUtil.sort(CollectionUtil.toList(ft_keywords));

	private SyntaxElementType currentElementType = SyntaxElementType.unknown;
	public final List<SyntaxElement> syntaxElements = new LinkedList<SyntaxElement>();

	private static boolean isSqlKeyword(String s) {
		return Collections.binarySearch(sqlkeywords, s.toUpperCase()) >= 0;
	}

	private static boolean isFtKeyword(String s) {
		return Collections.binarySearch(ftkeywords, s.toUpperCase()) >= 0;
	}

	@Override
	public void notifyOnError(Token offendingToken, Token missingToken, IntervalSet tokensExpected) {

	}

	private void addElement(Token token, SyntaxElementType type) {
		syntaxElements.add(SyntaxElement.create(token.getText(), token.getStartIndex(), token.getStopIndex(), type));
		SyntaxElement e = syntaxElements.get(syntaxElements.size() - 1);
		if (debug)
			System.out.println(String.format("%d-%d %s %s", e.from, e.to, e.type.name(), e.value));
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		if (currentElementType != SyntaxElementType.unknown)
			addElement(node.getSymbol(), currentElementType);
		else if (isSqlKeyword(node.getText()))
			addElement(node.getSymbol(), SyntaxElementType.sql_keyword);
		else if (isFtKeyword(node.getText()))
			addElement(node.getSymbol(), SyntaxElementType.ft_keyword);
	}

	@Override
	public void enterIdentifier(FusionTablesSqlParser.IdentifierContext ctx) {
		setElementType(SyntaxElementType.identifier);
	}

	@Override
	public void exitIdentifier(FusionTablesSqlParser.IdentifierContext ctx) {
		unSetElementType(SyntaxElementType.identifier);
	}

	@Override
	public void enterTable_name(FusionTablesSqlParser.Table_nameContext ctx) {
		setElementType(SyntaxElementType.tableName);
	}

	@Override
	public void exitTable_name(FusionTablesSqlParser.Table_nameContext ctx) {
		unSetElementType(SyntaxElementType.tableName);
	}

	@Override
	public void enterColumn_name(FusionTablesSqlParser.Column_nameContext ctx) {
		setElementType(SyntaxElementType.columnName);
	}

	@Override
	public void exitColumn_name(FusionTablesSqlParser.Column_nameContext ctx) {
		unSetElementType(SyntaxElementType.columnName);
	}

	@Override
	public void enterView_name(FusionTablesSqlParser.View_nameContext ctx) {
		setElementType(SyntaxElementType.viewName);
	}

	@Override
	public void exitView_name(FusionTablesSqlParser.View_nameContext ctx) {
		unSetElementType(SyntaxElementType.viewName);
	}

	@Override
	public void enterTable_alias(FusionTablesSqlParser.Table_aliasContext ctx) {
		setElementType(SyntaxElementType.alias);
	}

	@Override
	public void exitTable_alias(FusionTablesSqlParser.Table_aliasContext ctx) {
		unSetElementType(SyntaxElementType.alias);
	}

	@Override
	public void enterOperator(FusionTablesSqlParser.OperatorContext ctx) {
		setElementType(SyntaxElementType.operator);
	}

	@Override
	public void exitOperator(FusionTablesSqlParser.OperatorContext ctx) {
		unSetElementType(SyntaxElementType.operator);
	}

	@Override
	public void enterNumeric_literal(FusionTablesSqlParser.Numeric_literalContext ctx) {
		setElementType(SyntaxElementType.numericLiteral);
	}

	@Override
	public void exitNumeric_literal(FusionTablesSqlParser.Numeric_literalContext ctx) {
		unSetElementType(SyntaxElementType.numericLiteral);
	}

	@Override
	public void enterString_literal(FusionTablesSqlParser.String_literalContext ctx) {
		setElementType(SyntaxElementType.stringLiteral);
	}

	@Override
	public void exitString_literal(FusionTablesSqlParser.String_literalContext ctx) {
		unSetElementType(SyntaxElementType.stringLiteral);
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		if (!isGenericError(node.getText()))
			addElement(node.getSymbol(), SyntaxElementType.error);
	}

	private void setElementType(SyntaxElementType to) {
		if (currentElementType == SyntaxElementType.unknown)
			currentElementType = to;
	}

	private void unSetElementType(SyntaxElementType from) {
		if (currentElementType == from)
			currentElementType = SyntaxElementType.unknown;
	}

}
