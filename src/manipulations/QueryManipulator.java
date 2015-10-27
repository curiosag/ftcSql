package manipulations;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.xpath.XPath;

import com.google.common.base.Optional;

import cg.common.check.Check;
import cg.common.core.Logging;
import interfeces.Connector;
import manipulations.Util.Stuff;
import manipulations.results.ParseResult;
import manipulations.results.RefactoredSql;
import manipulations.results.ResolvedTableNames;
import manipulations.results.Splits;
import parser.FusionTablesSqlParser;
import util.StringUtil;

public class QueryManipulator {

	public class QueryPatcher {
		public final Optional<CursorContext> context;
		public final int cursorPosition;
		private final String query;

		public QueryPatcher(Optional<CursorContext> context, int cursorPosition, String query) {
			this.context = context;
			this.cursorPosition = cursorPosition;
			this.query = query;
		}

		public String patch(Optional<String> value) {
			String result = query;
			if (value.isPresent() && context.isPresent())
				if (context.get().boundaries.isPresent())
					result = StringUtil.replace(query, context.get().boundaries.get(), value.get());
				else 
					result = StringUtil.insert(query, cursorPosition, value.get());
			
			return result;
		}

	}

	private class DiggedAliases extends ParseResult {
		Map<String, String> aliases;

		public DiggedAliases(Map<String, String> aliases, String problemsEncountered) {
			super(problemsEncountered);

			this.aliases = aliases;
		}
	}

	private final Connector connector;
	private final TableNameToIdMapper tableNameToIdMapper;
	private final ParseTreeWalker walker = new ParseTreeWalker();
	private final String query;
	public final StatementType statementType;

	public QueryManipulator(Connector c, Logging log, String query) {
		Check.notNull(c);
		Check.notNull(log);

		this.connector = c;
		this.tableNameToIdMapper = new TableNameToIdMapper(connector.getTableNameToIdMap());
		connector.getTableNameToIdMap();
		this.query = query;

		statementType = getStatementType(getParser());
	}

	private StatementType getStatementType(FusionTablesSqlParser parser) {
		String xpath = "//" + Const.rulename_sql_stmt;

		Collection<ParseTree> s = XPath.findAll(parser.fusionTablesSql(), xpath, parser);
		if (s.size() == 0)
			return StatementType.UNKNOWN;
		else
			return Util.getSqlStatementType(s.iterator().next(), parser);
	}

	private FusionTablesSqlParser getParser() {
		return Util.getParser(query).parser;
	}

	public RefactoredSql refactorQuery() {
		Stuff stuff = Util.getParser(query);
		VerboseErrorListener errorListener = Util.addVerboseErrorListener(stuff.parser);

		DiggedAliases tableAliasToName = getTableAliases();

		NameToIDSubstitution substi = new NameToIDSubstitution(stuff.parser, stuff.tokenStream, tableNameToIdMapper,
				tableAliasToName.aliases);

		walker.walk(substi, stuff.parser.fusionTablesSql());

		return new RefactoredSql(query, substi.tuted(),
				StringUtil.concat(tableAliasToName.problemsEncountered.orNull(), errorListener.getErrors()));
	}

	private String getNextTerminal(Iterator<ParseTree> iter, FusionTablesSqlParser parser) {
		if (iter.hasNext())
			return Util.getTerminalValue(iter.next(), parser);
		else
			return null;
	}

	private ResolvedTableNames resolveTableNames(String nameFrom, String nameTo, String problemsTillNow) {
		String problem = null;

		Optional<String> tableId = tableNameToIdMapper.resolveTableId(nameFrom);
		if (!tableId.isPresent())
			problem = "Could not resolve id for table name '" + nameFrom + "'\r\n";

		return new ResolvedTableNames(nameFrom, tableId, nameTo, StringUtil.concat(problem, problemsTillNow));

	}

	public ResolvedTableNames getAlterTableIdentifiers() {
		Check.isTrue(statementType == StatementType.ALTER);

		FusionTablesSqlParser parser = getParser();
		VerboseErrorListener errorListener = Util.addVerboseErrorListener(parser);

		String xpath = "//alter_table_stmt//string_literal";
		Iterator<ParseTree> names = XPath.findAll(parser.fusionTablesSql(), xpath, parser).iterator();

		String nameFrom = null;
		String nameTo = null;

		nameFrom = getNextTerminal(names, parser);
		nameTo = getNextTerminal(names, parser);

		return resolveTableNames(nameFrom, nameTo, errorListener.getErrors());
	}

	public DiggedAliases getTableAliases() {
		Map<String, String> result = new HashMap<String, String>();
		FusionTablesSqlParser parser = getParser();
		VerboseErrorListener errorListener = Util.addVerboseErrorListener(parser);

		String xpath = "//table_name_with_alias";
		Iterator<ParseTree> trees = XPath.findAll(parser.fusionTablesSql(), xpath, parser).iterator();
		while (trees.hasNext()) {
			ParseTree parseTree = trees.next();
			addAlias(result, parseTree);
		}
		return new DiggedAliases(result, errorListener.getErrors());
	}

	private void addAlias(Map<String, String> result, ParseTree t) {
		TerminalNode tName = Util.digTerminal(t);
		TerminalNode tAlias = null;

		if (tName != null && t.getChildCount() > 1) {
			tAlias = Util.digTerminal(t.getChild(t.getChildCount() - 1));
			if (tAlias != null)
				result.put(tName.getText(), tAlias.getText());
		}
	}

	public ResolvedTableNames getTableNameToDrop() {
		Check.isTrue(statementType == StatementType.DROP);

		FusionTablesSqlParser parser = getParser();
		VerboseErrorListener errorListener = Util.addVerboseErrorListener(parser);

		String xpath = "//string_literal";

		Iterator<ParseTree> name = XPath.findAll(parser.fusionTablesSql(), xpath, parser).iterator();
		String tableName = getNextTerminal(name, parser);

		return resolveTableNames(tableName, null, errorListener.getErrors());
	}

	public Splits splitStatements() {
		Stuff stuff = Util.getParser(query);
		VerboseErrorListener errorListener = Util.addVerboseErrorListener(stuff.parser);

		StatementSplitter splitter = new StatementSplitter(stuff.tokenStream);

		walker.walk(splitter, stuff.parser.fusionTablesSql());
		return new Splits(splitter.splits, errorListener.getErrors());
	}

	public CursorContextListener getCursorContextListener(int cursorPosition) {
		Stuff stuff = Util.getParser(query);

		CursorContextListener cursorContextListener = new CursorContextListener(cursorPosition, stuff.parser);
		stuff.parser.removeErrorListeners();
		stuff.parser.setErrorHandler(new RecognitionErrorStrategy(cursorContextListener));

		walker.walk(cursorContextListener, stuff.parser.fusionTablesSql());
		return cursorContextListener;
	}

	private boolean symBoundary(char what)
	{
		char[] boundarySyms = new char[] {' ', '('};
		for (char c : boundarySyms) 
			if (what == c)
				return true;
		return false;
	}
	
	private int placeInValidTokenRange(String query, int cursorPos) {
		if (! symBoundary(query.charAt(cursorPos - 1)))
			cursorPos --;
		return cursorPos;
	}
	
	public Optional<CursorContext> getCursorContext(int cursorPosition) {
		return CursorContext.instance(getCursorContextListener(placeInValidTokenRange(query, cursorPosition)));
	}

	public QueryPatcher getPatcher(int cursorPosition){
		return new QueryPatcher(getCursorContext(cursorPosition), cursorPosition, query); 
	}
	
}
