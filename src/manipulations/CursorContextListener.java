package manipulations;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.google.common.base.Optional;

import cg.common.core.Op;
import gc.common.structures.OrderedIntTuple;
import parser.FusionTablesSqlBaseListener;
import parser.FusionTablesSqlParser;
import util.StringUtil;

public class CursorContextListener extends FusionTablesSqlBaseListener implements OnError {
	/**
	 * debug switch
	 */
	private final static boolean debug = false;

	private final int cursorIndex;

	public String[] expectedSymbols = new String[0];
	public Token offendingSymbol = null;
	
	private Optional<NameRecognition> currentRecognition = Optional.absent();

	public Optional<NameRecognition> atCursor = Optional.absent();
	public List<NameRecognitionTable> tableList = new ArrayList<NameRecognitionTable>();

	private final FusionTablesSqlParser parser;

	public CursorContextListener(int cursorIndex, FusionTablesSqlParser parser) {
		this.cursorIndex = cursorIndex;
		this.parser = parser;
	}

	@Override
	public void notify(Token offendingToken, Token missingToken, IntervalSet tokensExpected) {
			if (offendingToken != null)
			  offendingSymbol = offendingToken;
			if (tokensExpected.size() > 0)
			  expectedSymbols = getTokenNames(tokensExpected);
	}

	private String[] getTokenNames(IntervalSet s) {
		return criminalExtraction(s);
	}

	@SuppressWarnings("static-access")
	private String[] criminalExtraction(IntervalSet s) {
		String cont = s.toString(parser.VOCABULARY);
		if (debug)
			System.out.println(cont);
		return StringUtil.peel(cont).split(",");
	}

	
	/**
	 * there are repeated calls for startRecognition / stopRecognition in
	 * successive nested rules r0->r1-r2, say. It covers the cases
	 * 
	 * - from a correct input where all rules work as expected until r2 picking
	 * its content from a terminal nodes - to errors in the input causing any of
	 * r2, r1 to fail, picking some to all of its content from error nodes. -
	 * the empty case, where no information from the desired context could be
	 * picked, except that it was the context - if r0 fails, nothing can be said
	 * at all
	 * 
	 * the cases:
	 * 
	 * Result_columnContext ... empty and error cases where no column names
	 * follow
	 * 
	 * Ordering_termContext ... same for ordering term
	 * 
	 * ExprContext ... and for expressions, though this remains ambiguous e.g.
	 * how do you want to tell on a merely syntactic level an incomplete numeric
	 * literal("1.") from a pathologically incomplete
	 * Qualified_column_nameContext where the table name is "1"?
	 * 
	 * Qualified_column_in_expressionnameContext ... introduced for the
	 * st_intersects branch in expressions and then used everywhere, to
	 * eliminate at least one differentiation
	 * 
	 * Result_columnContext -> Aggregate_expContext ->
	 * Qualified_column_nameContext ... aggregate comes with extra tokens "AVG("
	 * or "SUM(" that need to be skipped - or treated in class
	 * "NameRecognitionState" which would be more complicated
	 * 
	 * Qualified_column_nameContext ... stand alone version of the previous case
	 * 
	 * 
	 */

	@Override
	public void enterTable_name_with_alias(FusionTablesSqlParser.Table_name_with_aliasContext ctx) {
		startRecognition(new NameRecognitionTable(), ctx);
	}

	@Override
	public void exitTable_name_with_alias(FusionTablesSqlParser.Table_name_with_aliasContext ctx) {
		stopRecognition(ctx);
	}

	@Override
	public void enterResult_column(FusionTablesSqlParser.Result_columnContext ctx) {
		startRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitResult_column(FusionTablesSqlParser.Result_columnContext ctx) {
		stopRecognition(ctx);
	}

	@Override
	public void enterOrdering_term(FusionTablesSqlParser.Ordering_termContext ctx) {
		startRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitOrdering_term(FusionTablesSqlParser.Ordering_termContext ctx) {
		stopRecognition(ctx);
	}

	@Override
	public void enterExpr(FusionTablesSqlParser.ExprContext ctx) {
		startRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitExpr(FusionTablesSqlParser.ExprContext ctx) {
		stopRecognition(ctx);
	}

	@Override
	public void enterQualified_column_name(FusionTablesSqlParser.Qualified_column_nameContext ctx) {
		startRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitQualified_column_name(FusionTablesSqlParser.Qualified_column_nameContext ctx) {
		stopRecognition(ctx);
	}

	@Override
	public void enterQualified_column_name_in_expression(
			FusionTablesSqlParser.Qualified_column_name_in_expressionContext ctx) {
		startRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitQualified_column_name_in_expression(
			FusionTablesSqlParser.Qualified_column_name_in_expressionContext ctx) {
		stopRecognition(ctx);
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		recognize(node.getSymbol(), getStop(node));
		debugTerminal(node);
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		if (!isGenericError(node.getText()))
			recognize(node.getSymbol(), getStop(node));

		debugErrorNode(node);
	}

	private boolean isGenericError(String errorString) {
		return errorString.startsWith("<") && errorString.endsWith(">");
	}

	private void startRecognition(NameRecognition current, ParserRuleContext ctx) {
		currentRecognition = Optional.of(current);

		OrderedIntTuple range = OrderedIntTuple.instance(getStart(ctx), getStop(ctx));

		if (cursorWithinBoundaries(range))
			atCursor = currentRecognition;

		if (current.getClass() == NameRecognitionTable.class)
			tableList.add((NameRecognitionTable) current);

		debugStartRecognition(range);

	}

	private void stopRecognition(ParserRuleContext ctx) {
		if (currentRecognition.isPresent()) {
			currentRecognition = Optional.absent();
		}
	}

	private int getStop(ParserRuleContext ctx) {
		return ctx.stop.getStopIndex();
	}

	private int getStart(ParserRuleContext ctx) {
		return ctx.start.getStartIndex();
	}

	private void debugStartRecognition(OrderedIntTuple range) {
		if (debug) {
			String within = cursorWithinBoundaries(range) ? " match" : " no match";
			System.out.println(String.format("Recognition boundaries lo:%d hi:%d cursor at: %d -> %s", range.lo(),
					range.hi(), cursorIndex, within));
		}
	}

	private boolean cursorWithinBoundaries(OrderedIntTuple o) {
		return Op.between(o.lo(), cursorIndex, o.hi());
	}

	private void recognize(Token token, int stopIndex) {
		if (currentRecognition.isPresent()) {
			currentRecognition.get().digest(token);
			debugRecognize(token);
		}
	}

	private void debugRecognize(Token token) {
		if (debug)
			System.out.println("recognizing " + token.getText());

	}

	private int getStop(TerminalNode node) {
		return node.getSymbol().getStopIndex();
	}

	private int getStop(ErrorNode node) {
		return node.getSymbol().getStopIndex();
	}

	private void debugTerminal(TerminalNode node) {
		if (debug)
			say("Terminal: " + qt(node.getText()), getBoundaries(node));
	}

	private void debugErrorNode(ErrorNode node) {

		if (debug)
			say("Error: " + node.getText(), getBoundaries(node));
	}

	private String qt(String s) {
		return ">>" + s + "<<";
	}

	private void say(String what, OrderedIntTuple o) {
		String swapped = o.swap ? " (indices swapped)" : "";
		String withinBoundaries = cursorWithinBoundaries(o) ? "+" : "-";
		System.out.println(String.format("%s %s from %d to %d %s", withinBoundaries, what, o.lo(), o.hi(), swapped));
	}

	private void sayContext(ParserRuleContext ctx, String inOrOut) {
		if (debug)
			say(inOrOut + " rule: " + ctx.getClass().getSimpleName(), getBoundaries(ctx));
	}

	private OrderedIntTuple getBoundaries(ParserRuleContext ctx) {
		return OrderedIntTuple.instance(getStart(ctx), getStop(ctx));
	}

	private OrderedIntTuple getBoundaries(TerminalNode n) {
		return OrderedIntTuple.instance(n.getSymbol().getStartIndex(), n.getSymbol().getStopIndex());
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		sayContext(ctx, "enter");
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		sayContext(ctx, "exit");
	}

	private String markPos(int pos) {
		String result = " "; // for 1st quote around query
		for (int i = 0; i < pos; i++)
			result = result + ' ';
		result = result + "^|";
		return result;
	}

	private void say(NameRecognitionColumn t) {
		System.out.println("Column: " + t.ColumnName().or("") + " of table: " + t.TableName().or(""));
	}

	private void say(NameRecognitionTable t) {
		System.out.println("Table: " + t.TableName().or("") + " alias: " + t.TableAlias().or(""));
	}

	private void say(NameRecognition r) {
		if (r instanceof NameRecognitionColumn)
			say((NameRecognitionColumn) r);
		if (r instanceof NameRecognitionTable)
			say((NameRecognitionTable) r);
		System.out.println("recognition state: " + r.state.name());
	}

	private void say() {
		if (atCursor.isPresent())
			say(atCursor.get());
		for (NameRecognitionTable t : tableList)
			say(t);
	}

	public void debug(String query, int cursorPos) {
		say();
		System.out.println(String.format("'%s' cursor after pos: %d", query, cursorPos));
		System.out.println(markPos(cursorPos));
	}


}
