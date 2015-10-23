package manipulations;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.google.common.base.Optional;

import cg.common.core.Op;
import gc.common.structures.IntTuple;
import gc.common.structures.OrderedIntTuple;
import parser.FusionTablesSqlBaseListener;
import parser.FusionTablesSqlParser;

public class CursorContextListener extends FusionTablesSqlBaseListener {
	private final boolean debug = true;

	private final int cursorIndex;

	private Optional<NameRecognition> currentRecognition = Optional.absent();

	public Optional<NameRecognition> atCursor = Optional.absent();
	public List<NameRecognitionTable> tableList = new ArrayList<NameRecognitionTable>();

	public CursorContextListener(int cursorIndex) {
		this.cursorIndex = cursorIndex;
	}

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
	public void enterQualified_column_name(FusionTablesSqlParser.Qualified_column_nameContext ctx) {
		//startRecognition(new NameRecognitionColumn(), ctx);
	}

	@Override
	public void exitQualified_column_name(FusionTablesSqlParser.Qualified_column_nameContext ctx) {
		//stopRecognition(ctx);
	}

	private void startRecognition(NameRecognition current, ParserRuleContext ctx) {
		currentRecognition = Optional.of(current);

		OrderedIntTuple range = OrderedIntTuple.instance(getStart(ctx), getStop(ctx));

		if (cursorWithinBoundaries(range))
			atCursor = currentRecognition;

		if (current.getClass() == NameRecognitionTable.class)
			tableList.add((NameRecognitionTable) current);

		debugStopRecognition(range);
	}

	private void stopRecognition(ParserRuleContext ctx) {
		currentRecognition = Optional.absent();
	}

	private int getStop(ParserRuleContext ctx) {
		return ctx.stop.getStopIndex();
	}

	private int getStart(ParserRuleContext ctx) {
		return ctx.start.getStartIndex();
	}

	private void debugStopRecognition(OrderedIntTuple range) {
		if (debug) {
			String within = cursorWithinBoundaries(range) ? " match" : " no match";
			System.out.println(String.format("Recognition boundaries lo:%d hi:%d cursor at: %d -> %s", range.lo(),
					range.hi(), cursorIndex, within));
		}
	}

	private boolean cursorWithinBoundaries(OrderedIntTuple o) {
		return Op.between(o.lo(), cursorIndex, o.hi());
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		recognize(node.getText(), getStop(node));
		debugTerminal(node);
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		recognize(node.getText(), getStop(node));
		debugErrorNode(node);
	}

	private void recognize(String token, int stopIndex) {
		if (currentRecognition.isPresent()) {
			currentRecognition.get().digest(token);
			debugRecognize(token);
		}
	}

	private void debugRecognize(String token) {
		if (debug)
			System.out.println("recognizing " + token);

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

	// debug stuff from here on

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
