package manipulations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;

import com.google.common.base.Optional;

import cg.common.check.Check;
import cg.common.core.Logging;
import cg.common.http.HttpStatus;
import gc.common.structures.StackLight;
import structures.ClientSettings;
import structures.ColumnInfo;
import structures.QueryResult;
import interfaces.Connector;
import structures.TableInfo;
import uglySmallThings.Const;
import interfaces.SyntaxElement;
import interfaces.SyntaxElementType;
import manipulations.QueryPatching;
import manipulations.results.RefactoredSql;
import manipulations.results.ResolvedTableNames;
import manipulations.results.TableReference;
import parser.FusionTablesSqlLexer;
import util.StringUtil;

public class QueryHandler extends Observable {
	private boolean debug = Const.debugQueryHandler;

	private boolean reload = true;
	private final Logging logger;
	private final Connector connector;
	private final boolean preview = true;
	private final boolean execute = !preview;
	private final List<TableInfo> tableInfo = new LinkedList<TableInfo>();
	private final Map<String, TableInfo> tableNameToTableInfo = new HashMap<String, TableInfo>();
	private TableNameToIdMapper tableNameToIdMapper;
	private final ClientSettings settings;

	public QueryHandler(Logging logger, Connector connector, ClientSettings settings) {
		Check.notNull(logger);
		Check.notNull(connector);
		this.logger = logger;
		this.connector = connector;
		this.settings = settings;
	}

	
	public void reset(Dictionary<String, String> connectionInfo) {
		connector.reset(connectionInfo);
		reloadTableList();
	};

	private void log(String msg) {
		logger.Info(msg);
	}

	private QueryManipulator createManipulator(String query) {
		return new QueryManipulator(loadTableCaches(false), tableNameToIdMapper, logger, query);
	}

	private void reloadTableList() {
		loadTableCaches(reload);
	}
	
	private TableInfo getTableInfo(String tableName) {
		loadTableCaches(false);
		return tableNameToTableInfo.get(StringUtil.stripQuotes(tableName));
	}

	private synchronized List<TableInfo> loadTableCaches(boolean reload) {
		if (tableInfo.isEmpty() || reload) {
			tableInfo.clear();
			tableInfo.addAll(connector.getTableInfo());
			populateTableMaps(tableInfo);
		}
		return tableInfo;
	}

	private void populateTableMaps(List<TableInfo> tableInfo) {
		tableNameToTableInfo.clear();
		for (TableInfo t : tableInfo)
			tableNameToTableInfo.put(t.name, t);
		tableNameToIdMapper = new TableNameToIdMapper(tableInfo);
	}

	public List<TableInfo> getTableList() {
		return loadTableCaches(!reload);
	}

	public TableModel getTableInfo() {

		Vector<String> columns = new Vector<String>();
		columns.add("Id");
		columns.add("Name");

		Vector<Vector<String>> rows = new Vector<Vector<String>>();

		List<String> names = new ArrayList<String>();

		for (TableInfo i : getTableList()) {
			if (names.contains(i.name))
				log("Duplicate table name: '" + i.name + "' name to ID substitution may fail.");

			names.add(i.name);

			Vector<String> row = new Vector<String>();
			row.add(i.id);
			row.add(i.name);
			rows.add(row);
		}

		return new DefaultTableModel(rows, columns);
	}

	public TableModel getColumnInfo(TableInfo info) {
		Check.notNull(info);

		Vector<String> columns = new Vector<String>();
		columns.add("Column name");
		columns.add("Datatype");
	
		Vector<Vector<String>> rows = new Vector<Vector<String>>();

		for (ColumnInfo i : info.columns) {
			Vector<String> row = new Vector<String>();
			row.add(i.name);
			row.add(i.type);
			rows.add(row);
		}

		return new DefaultTableModel(rows, columns);
	}

	private QueryResult hdlAlterTable(QueryManipulator ftr, boolean preview) {
		ResolvedTableNames id = ftr.getAlterTableIdentifiers();

		String msg;
		if (id.problemsEncountered.isPresent())
			msg = id.problemsEncountered.get();
		else if (preview)
			msg = String.format("Api call rename to %s, table id %s", id.nameTo, id.idFrom.or(""));
		else {
			msg = connector.renameTable(id.idFrom.get(), id.nameTo);
			onStructureChanged();
		}
		return packQueryResult(msg);
	}

	private QueryResult hdlQuery(StatementType statementType, String query, QueryManipulator ftr, boolean preview) {
		RefactoredSql r = createManipulator(query).refactorQuery();

		String prepared = r.refactored;
		if (statementType != StatementType.DESCRIBE)
			prepared = addLimit(prepared);
	
		if (r.problemsEncountered.isPresent())
			return packQueryResult(r.problemsEncountered.get());
		
		else if (preview)
			return packQueryResult(prepared);
		else
			return connector.fetch(prepared);
	}

	private String addLimit(String refactored) {
		String q = refactored.toUpperCase();
		if (q.indexOf("LIMIT") >= 0)
			return refactored;

		refactored = refactored.replace(";", "");
		if (q.indexOf("OFFSET") < 0)
			refactored = refactored + "\nOFFSET 0";

		refactored = refactored + String.format("\nLIMIT %d;", settings.defaultQueryLimit);

		return refactored;
	}

	private QueryResult hdlDropTable(QueryManipulator ftr, boolean preview) {
		ResolvedTableNames id = ftr.getTableNameToDrop();
		String msg;
		if (id.problemsEncountered.isPresent())
			msg = id.problemsEncountered.get();
		else if (preview)
			msg = "Api call delete, table id: " + id.idFrom.get();
		else
			try {
				connector.deleteTable(id.idFrom.get());
				onStructureChanged();
				msg = "dropped '" + id.idFrom.get() + "'";
			} catch (IOException e) {
				msg = e.getMessage();
			}
		return packQueryResult(msg);
	}

	public QueryResult getQueryResult(String query) {
		logger.Info(String.format("running: '%s'", query));

		try {
			QueryManipulator ftr = createManipulator(query);
			switch (ftr.statementType) {
			case ALTER:
				return hdlAlterTable(ftr, execute);

			case SELECT:
				return hdlQuery(ftr.statementType, query, ftr, execute);

//			case INSERT:
//				return hdlQuery(query, ftr, preview);
//
//			case UPDATE:
//				return hdlQuery(query, ftr, preview);
//
//			case DELETE:
//				return hdlQuery(query, ftr, preview);

			case CREATE_VIEW:
				return hdlQuery(ftr.statementType, query, ftr, execute);

			case DROP:
				return hdlDropTable(ftr, execute);

			case DESCRIBE:
				return hdlQuery(ftr.statementType, query, ftr, execute);
				//return hdlDescribeTable(query, execute);

			default:
				return packQueryResult("Statement not covered: " + query);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return packQueryResult(e.getMessage());
		}

	}

	private QueryResult hdlDescribeTable(String query, boolean preview) {
		if (preview)
			return packQueryResult(query);
		
		Optional<String> val = createManipulator(query).getCursorContext(query.trim().length() - 2).underlyingTableName;
		TableInfo info = null;
		if (val.isPresent())
			info = getTableInfo(val.get());

		if (info != null)
			return packQueryResult(getColumnInfo(info));
		else
			return packQueryResult("table not found");
	}

	private QueryResult packQueryResult(String msg) {
		return new QueryResult(HttpStatus.SC_BAD_REQUEST, null, msg);
	}

	private QueryResult packQueryResult(TableModel model) {
		return new QueryResult(HttpStatus.SC_OK, model, null);
	}
	
	public String previewExecutedSql(String query) {
		QueryManipulator ftr = createManipulator(query);

		switch (ftr.statementType) {

		case ALTER:
			return hdlAlterTable(ftr, preview).message.or("");

		case SELECT:
			return hdlQuery(ftr.statementType, query, ftr, preview).message.or("");

		case INSERT:
			return hdlQuery(ftr.statementType, query, ftr, preview).message.or("");

		case UPDATE:
			return hdlQuery(ftr.statementType, query, ftr, preview).message.or("");

		case DELETE:
			return hdlQuery(ftr.statementType, query, ftr, preview).message.or("");

		case CREATE_VIEW:
			return hdlQuery(ftr.statementType, query, ftr, preview).message.or("");

		case DROP:
			return hdlDropTable(ftr, preview).message.or("");

		case DESCRIBE:
			return hdlDescribeTable(query, preview).message.or("");

		default:
			return "Statement not covered: " + query;
		}

	}

	public QueryPatching getPatcher(String query, int cursorPos) {
		return createManipulator(query).getPatcher(cursorPos);
	}

	private String lastQuery = null;
	List<SyntaxElement> lastHighlighting = null;

	public List<SyntaxElement> getHighlighting(String query) {
		if (lastHighlighting != null && StringUtil.nullableEqual(query, lastQuery))
			return lastHighlighting;

		List<SyntaxElement> result = getSyntaxElements(createManipulator(query).getCursorContextListener(0));
		lastQuery = query;
		lastHighlighting = result;

		return result;
	}

	private List<SyntaxElement> getSyntaxElements(CursorContextListener l) {
		// Check.isTrue(l.tableList.size() <= 1);

		Optional<TableReference> tableReference;
		if (l.tableList.size() >= 1)
			tableReference = resolveTable(l.tableList.get(l.tableList.size() - 1));
		else
			tableReference = Optional.absent();

		Semantics semantics = new Semantics(tableReference, l.allNames);
		semantics.setSemanticAttributes(l.syntaxElements);

		List<SyntaxElement> complete = addNonSyntaxTokens(l.syntaxElements, l.tokens);

		if (debug)
			debug(complete);

		return complete;
	}

	private List<SyntaxElement> addNonSyntaxTokens(List<SyntaxElement> syntaxElements, BufferedTokenStream tokens) {

		StackLight<SyntaxElement> regularElements = new StackLight<SyntaxElement>(syntaxElements);

		List<SyntaxElement> result = new LinkedList<SyntaxElement>();

		for (Token t : tokens.getTokens()) {
			if (!regularElements.empty() && regularElements.peek().tokenIndex == t.getTokenIndex())
				result.add(regularElements.pop());
			else {
				SyntaxElementType type = getIrregularType(t);
				if (type != SyntaxElementType.unknown)
					result.add(SyntaxElement.create(t, type));
			}
		}

		return result;
	}

	private SyntaxElementType getIrregularType(Token t) {
		SyntaxElementType type = SyntaxElementType.unknown;

		if (t.getChannel() == FusionTablesSqlLexer.WHITESPACE) {
			if (t.getText().equals("\n"))
				type = SyntaxElementType.newline;
			else
				type = SyntaxElementType.whitespace;
		} else if (t.getChannel() == FusionTablesSqlLexer.HIDDEN)
			type = SyntaxElementType.comment;

		return type;
	}

	private void debug(List<SyntaxElement> syntaxElements) {
		System.out.println("--- syntax elements ---");
		for (SyntaxElement s : syntaxElements)
			System.out.println(String.format("%s %s %d-%d %s", s.value.replace("\n", "NL"), s.type.name(), s.from, s.to,
					s.hasSemanticError() ? "<bad>" : "ok"));
	}

	private static int lengthTableId = 41;

	private Optional<TableReference> resolveTable(NameRecognitionTable tableRecognized) {

		if (tableRecognized.TableName().isPresent()) {
			String tableName = tableRecognized.TableName().get();
			Optional<String> id = resolveTableId(tableName);

			if (nameIsActuallyAnId(tableName, id)) {
				Optional<String> maybeTableName = tableNameToIdMapper.nameForId(tableName);
				if (maybeTableName.isPresent()) {
					id = Optional.of(tableName);
					tableName = maybeTableName.get();
				}
			}

			if (id.isPresent()) {
				TableInfo t = getTableInfo(tableName);
				Check.notNull(t);
				return Optional.of(new TableReference(tableName, tableRecognized.TableAlias(), id.get(),
						getColumnNames(t.columns)));
			}
		}
		return Optional.absent();
	}

	private boolean nameIsActuallyAnId(String tableName, Optional<String> id) {
		return !id.isPresent() && tableName.length() == lengthTableId;
	}

	private Optional<String> resolveTableId(String tableName) {
		return tableNameToIdMapper.idForName(tableName);
	}

	private List<String> getColumnNames(List<ColumnInfo> columns) {
		Check.notNull(columns);
		List<String> result = new ArrayList<String>();

		for (ColumnInfo c : columns)
			result.add(c.name);

		return result;
	}

	private void onStructureChanged() {
		new Thread(new Runnable() {
			public void run() {
				reloadTableList();
				notifyObservers();
			}
		}).start();
	}

}
