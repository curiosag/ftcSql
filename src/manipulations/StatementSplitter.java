package manipulations;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BufferedTokenStream;

import cg.common.check.Check;
import parser.FusionTablesSqlBaseListener;
import parser.FusionTablesSqlParser;

public class StatementSplitter extends FusionTablesSqlBaseListener {

	private final BufferedTokenStream tokens;
	public final List<String> splits = new ArrayList<String>();
	
	public StatementSplitter(BufferedTokenStream tokens) {
		Check.notNull(tokens);
		this.tokens = tokens;
	}

	@Override
	public void exitSql_stmt(FusionTablesSqlParser.Sql_stmtContext ctx) {
		splits.add(tokens.getText(ctx.start, ctx.stop));
	}

}
