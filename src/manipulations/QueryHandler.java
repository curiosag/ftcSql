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
	public final boolean ADD_DETAILS = true;
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
	
	public void reset(Dictionary<String, String> connectionInfo) 
	{
		connector.reset(connectionInfo);
		reloadTableList();
	};
	
	
	private void log(String msg) {
		logger.Info(msg);
	}

	private QueryManipulator createManipulator(String query) {
		return new QueryManipulator(internalGetTableInfo(false), tableNameToIdMapper, logger, query);
	}

	private void reloadTableList() {
		internalGetTableInfo(reload);
	}

	private TableInfo getTableInfo(String tableName) {
		internalGetTableInfo(false);
		return tableNameToTableInfo.get(tableName);
	}

	private synchronized List<TableInfo> internalGetTableInfo(boolean reload) {
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

	public List<TableInfo> getTableList(boolean addDetails) {
		List<TableInfo> info = internalGetTableInfo(!reload);
		List<TableInfo> result;
		if (addDetails)
			result = info;
		else
			result = flatten(info);
		return result;
	}

	private List<TableInfo> flatten(List<TableInfo> info) {
		List<ColumnInfo> noColumns = new ArrayList<ColumnInfo>();
		List<TableInfo> result = new ArrayList<TableInfo>();
		for (TableInfo t : info)
			result.add(new TableInfo(t.name, t.id, t.description, noColumns));
		return result;
	}

	public String _getTableInfo() {
		StringBuilder sb = new StringBuilder();
		List<String> names = new ArrayList<String>();

		for (TableInfo i : getTableList(ADD_DETAILS)) {
			if (names.contains(i.name))
				log("Duplicate table name: '" + i.name + "' name to ID substitution may fail.");

			sb.append("ID: " + i.id + " " + "  NAME: " + i.name + "\n");
		}

		return sb.toString();
	}
	
	public TableModel getTableInfo() {
		
		Vector<String> columns = new Vector<String>();
		columns.add("Id");
		columns.add("Name");
		
		Vector<Vector<String>> rows = new Vector<Vector<String>>();
		
				
		List<String> names = new ArrayList<String>();

		for (TableInfo i : getTableList(ADD_DETAILS)) {
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

	private String hdlAlterTable(QueryManipulator ftr, boolean preview) {
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
		return msg;
	}

	private QueryResult hdlQuery(String query, QueryManipulator ftr, boolean preview) {
		RefactoredSql r = createManipulator(query).refactorQuery();
		if (r.problemsEncountered.isPresent())
			return packQueryResult(r.problemsEncountered.get());
		else if (preview)
			return packQueryResult(addLimit(r.refactored));
		else
			return connector.fetch(addLimit(r.refactored));
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

	private String hdlDropTable(QueryManipulator ftr, boolean preview) {
		ResolvedTableNames id = ftr.getTableNameToDrop();
		String msg;
		if (id.problemsEncountered.isPresent())
			return id.problemsEncountered.get();
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
		return msg;
	}

	public QueryResult getQueryResult(String query) {
		logger.Info(String.format("running: '%s'", query));

		try {
			QueryManipulator ftr = createManipulator(query);
			switch (ftr.statementType) {
			case ALTER:
				return packQueryResult(hdlAlterTable(ftr, execute));

			case SELECT:
				return hdlQuery(query, ftr, execute);

			case INSERT:
				return hdlQuery(query, ftr, preview);
				
			case UPDATE:
				return hdlQuery(query, ftr, preview);
				
			case DELETE:
				return hdlQuery(query, ftr, preview);					
				
			case CREATE_VIEW:
				return hdlQuery(query, ftr, execute);

			case DROP:
				return packQueryResult(hdlDropTable(ftr, execute));

			default:
				return packQueryResult("Statement not covered: " + query);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return packQueryResult(e.getMessage());
		}

	}

	private QueryResult packQueryResult(String msg) {
		return new QueryResult(HttpStatus.SC_ACCEPTED, null, msg);
	}

	public String previewExecutedSql(String query) {
		QueryManipulator ftr = createManipulator(query);

		switch (ftr.statementType) {

		case ALTER:
			return hdlAlterTable(ftr, preview);

		case SELECT:
			return hdlQuery(query, ftr, preview).message.or("");
			
		case INSERT:
			return hdlQuery(query, ftr, preview).message.or("");
			
		case UPDATE:
			return hdlQuery(query, ftr, preview).message.or("");
			
		case DELETE:
			return hdlQuery(query, ftr, preview).message.or("");	

		case CREATE_VIEW:
			return hdlQuery(query, ftr, preview).message.or("");

		case DROP:
			return hdlDropTable(ftr, preview);

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
		//Check.isTrue(l.tableList.size() <= 1);

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

		if (t.getChannel() == FusionTablesSqlLexer.WHITESPACE)
		{
			if (t.getText().equals("\n"))
				type = SyntaxElementType.newline;
			else
				type = SyntaxElementType.whitespace;
		}
		else if (t.getChannel() == FusionTablesSqlLexer.HIDDEN)
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
