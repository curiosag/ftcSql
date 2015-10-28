package manipulations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

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
import parser.FusionTablesSqlParser.ExprContext;
import util.StringUtil;

public class CursorContextListener extends FusionTablesSqlBaseListener implements OnError {
	/**
	 * debug switch
	 */
	private final static boolean debug = true;

	private final int cursorIndex;

	public String[] expectedSymbols = new String[0];
	public Token offendingSymbol = null;
	private Token lastTerminalRead = null;
	private LinkedList<Token> errorTokensRead = new LinkedList<Token>();

	private Optional<NameRecognition> currentNameRecognition = Optional.absent();

	public Optional<ParserRuleContext> contextAtCursor = Optional.absent();
	public Stack<ParserRuleContext> contextStack = new Stack<ParserRuleContext>();
	public Optional<NameRecognition> nameAtCursor = Optional.absent();
	public List<NameRecognitionTable> tableList = new ArrayList<NameRecognitionTable>();

	private final FusionTablesSqlParser parser;

	public CursorContextListener(int cursorIndex, FusionTablesSqlParser parser) {
		this.cursorIndex = cursorIndex;
		this.parser = parser;
	}

	@Override
	public void notifyOnError(Token offendingToken, Token missingToken, IntervalSet tokensExpected) {
		if (offendingToken != null)
			offendingSymbol = offendingToken;
		if (tokensExpected.size() > 0)
			expectedSymbols = getTokenNames(tokensExpected);

		debugOnError();
	}

	private void debugOnError() {
		if (debug) {
			if (offendingSymbol != null)
				System.out.println("Offended by: " + offendingSymbol.getText());

			System.out.println("expected: " + StringUtil.ToCsv(expectedSymbols, ","));

		}
	}

	private String[] getTokenNames(IntervalSet s) {
		return criminalExtraction(s);
	}

	@SuppressWarnings("static-access")
	private String[] criminalExtraction(IntervalSet s) {
		String cont = s.toString(parser.VOCABULARY);
		if (debug)
			System.out.println(cont);
		return cont.split(",");
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
		startNameRecognition(new NameRecognitionTable(), ctx);
	}

	@Override
	public void exitTable_name_with_alias(FusionTablesSqlParser.Table_name_with_aliasContext ctx) {
		stopNameRecognition(ctx);
	}

	@Override
	public void enterResult_column(FusionTablesSqlParser.Result_columnContext ctx) {
		startNameRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitResult_column(FusionTablesSqlParser.Result_columnContext ctx) {
		stopNameRecognition(ctx);
	}

	@Override
	public void enterOrdering_term(FusionTablesSqlParser.Ordering_termContext ctx) {
		startNameRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitOrdering_term(FusionTablesSqlParser.Ordering_termContext ctx) {
		stopNameRecognition(ctx);
	}

	@Override
	public void enterExpr(FusionTablesSqlParser.ExprContext ctx) {
		startNameRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitExpr(FusionTablesSqlParser.ExprContext ctx) {
		stopNameRecognition(ctx);
		evaluateExprContextMatchOnExitRule(ctx);
	}

	@Override
	public void enterQualified_column_name(FusionTablesSqlParser.Qualified_column_nameContext ctx) {
		startNameRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitQualified_column_name(FusionTablesSqlParser.Qualified_column_nameContext ctx) {
		stopNameRecognition(ctx);
	}

	@Override
	public void enterQualified_column_name_in_expression(
			FusionTablesSqlParser.Qualified_column_name_in_expressionContext ctx) {
		startNameRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitQualified_column_name_in_expression(
			FusionTablesSqlParser.Qualified_column_name_in_expressionContext ctx) {
		stopNameRecognition(ctx);
	}

	@Override
	public void enterTable_name_in_ddl(FusionTablesSqlParser.Table_name_in_ddlContext ctx) {
		startNameRecognition(new NameRecognitionTable(), ctx);
	}

	@Override
	public void exitTable_name_in_ddl(FusionTablesSqlParser.Table_name_in_ddlContext ctx) {
		stopNameRecognition(ctx);
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		recognize(node.getSymbol(), getStop(node));

		lastTerminalRead = node.getSymbol();
		if (errorTokensRead.size() > 0)
			errorTokensRead.clear();

		debugTerminal(node);
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		if (!isGenericError(node.getText()))
			recognize(node.getSymbol(), getStop(node));

		errorTokensRead.add(node.getSymbol());

		debugErrorNode(node);
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		onEnterRule(ctx);
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		onExitRule(ctx);
	}

	private OrderedIntTuple getLastErrorBoundaries(int stretchBy) {
		return OrderedIntTuple.instance(errorTokensRead.getLast().getStartIndex(),
				errorTokensRead.getLast().getStopIndex() + stretchBy);
	}

	private void evaluateExprContextMatchOnExitRule(ParserRuleContext ctx) {
		// smthg like "WHERE a " will give a terminal "WHERE" plus one error
		// node "a" the cursor is 1 after a in this case, token
		// boundary ends with a, therefore stretchBy
		int stretchBy = 2;
		if (lastTerminalRead != null
				&& StringUtil.equalsAny(lastTerminalRead.getText().toUpperCase(), "WHERE", "AND", "OR")
				&& errorTokensRead.size() == 1 && cursorWithinBoundaries(getLastErrorBoundaries(stretchBy))) {
			contextAtCursor = Optional.of(ctx);
			if (debug)
				System.out.println("-> matched in hindsight");
		}

	}

	private void onEnterRule(ParserRuleContext ctx) {
		debugContext(ctx, "enter");

		pushContext(ctx);
		if (cursorWithinBoundaries(ctx))
			contextAtCursor = Optional.of(ctx);
	}

	private void onExitRule(ParserRuleContext ctx) {
		debugContext(ctx, "exit");
		popContext();
	}

	private boolean isGenericError(String errorString) {
		return errorString.startsWith("<") && errorString.endsWith(">");
	}

	private void startNameRecognition(NameRecognition current, ParserRuleContext ctx) {
		currentNameRecognition = Optional.of(current);

		OrderedIntTuple range = getRuleRange(ctx);

		if (cursorWithinBoundaries(range))
			nameAtCursor = currentNameRecognition;

		if (current.getClass() == NameRecognitionTable.class)
			tableList.add((NameRecognitionTable) current);

		debugStartRecognition(range);

	}

	private OrderedIntTuple getRuleRange(ParserRuleContext ctx) {
		return OrderedIntTuple.instance(getStart(ctx), getStop(ctx));
	}

	private void stopNameRecognition(ParserRuleContext ctx) {
		if (currentNameRecognition.isPresent()) {
			currentNameRecognition = Optional.absent();
		}
	}

	private int getStop(ParserRuleContext ctx) {
		if (ctx.stop == null)
			return -1;
		else
			return ctx.stop.getStopIndex();
	}

	private int getStart(ParserRuleContext ctx) {
		if (ctx.start == null)
			return -1;
		else
			return ctx.start.getStartIndex();
	}

	private void debugStartRecognition(OrderedIntTuple range) {
		if (debug) {
			String within = cursorWithinBoundaries(range) ? " match" : " no match";
			System.out.println(String.format("Recognition boundaries lo:%d hi:%d cursor at: %d -> %s", range.lo(),
					range.hi(), cursorIndex, within));
		}
	}

	private boolean cursorWithinBoundaries(ParserRuleContext c) {
		return cursorWithinBoundaries(getRuleRange(c));
	}

	private boolean cursorWithinBoundaries(OrderedIntTuple o) {
		return Op.between(o.lo(), cursorIndex, o.hi());
	}

	private void recognize(Token token, int stopIndex) {
		if (currentNameRecognition.isPresent()) {
			currentNameRecognition.get().digest(token);
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

	private void debugContext(ParserRuleContext ctx, String inOrOut) {
		if (debug)
			say(inOrOut + " rule: " + ctx.getClass().getSimpleName(), getBoundaries(ctx));
	}

	private OrderedIntTuple getBoundaries(ParserRuleContext ctx) {
		return getRuleRange(ctx);
	}

	private OrderedIntTuple getBoundaries(TerminalNode n) {
		return OrderedIntTuple.instance(n.getSymbol().getStartIndex(), n.getSymbol().getStopIndex());
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
		if (nameAtCursor.isPresent())
			say(nameAtCursor.get());
		for (NameRecognitionTable t : tableList)
			say(t);
	}

	public void debug(String query, int cursorPos) {
		say();
		System.out.println(String.format("'%s' cursor after pos: %d", query, cursorPos));
		System.out.println(markPos(cursorPos));
	}

	private void pushContext(ParserRuleContext c) {
		contextStack.push(c);
	}

	private void popContext() {
		if (!contextAtCursor.isPresent())
			contextStack.pop();
	}
}
