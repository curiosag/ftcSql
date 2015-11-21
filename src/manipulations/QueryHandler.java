package manipulations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import com.google.common.base.Optional;

import cg.common.check.Check;
import cg.common.core.Logging;
import interfacing.ColumnInfo;
import interfacing.Connector;
import interfacing.SyntaxElement;
import interfacing.TableInfo;
import manipulations.QueryPatching;
import manipulations.results.RefactoredSql;
import manipulations.results.ResolvedTableNames;
import manipulations.results.TableReference;

public class QueryHandler extends Observable {
	private boolean debug = false;
	
	private boolean reload = true;
	public final boolean ADD_DETAILS = true;
	private final Logging logger;
	private final Connector connector;
	private final boolean preview = true;
	private final boolean execute = !preview;
	private final List<TableInfo> tableInfo = new LinkedList<TableInfo>();
	private final Map<String, TableInfo> tableNameToTableInfo = new HashMap<String, TableInfo>();
	private TableNameToIdMapper tableNameToIdMapper;

	public QueryHandler(Logging logger, Connector c) {
		Check.notNull(logger);
		Check.notNull(c);
		this.logger = logger;
		this.connector = c;
	}

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

	public String getTableInfo() {
		StringBuilder sb = new StringBuilder();
		List<String> names = new ArrayList<String>();

		for (TableInfo i : getTableList(ADD_DETAILS)) {
			if (names.contains(i.name))
				log("Duplicate table name: '" + i.name + "' name to ID substitution may fail.");

			sb.append("ID: " + i.id + " " + "  NAME: " + i.name + "\n");
		}

		return sb.toString();
	}

	private String hdlAlterTable(QueryManipulator ftr, boolean preview) {
		ResolvedTableNames id = ftr.getAlterTableIdentifiers();

		if (id.problemsEncountered.isPresent())
			return id.problemsEncountered.get();
		else if (preview)
			return String.format("Api call rename to %s, table id %s", id.nameTo, id.idFrom.or(""));
		else {
			String result = connector.renameTable(id.idFrom.get(), id.nameTo);
			onStructureChanged();
			return result;
		}
	}

	private String hdlQuery(String query, QueryManipulator ftr, boolean preview) {
		RefactoredSql r = createManipulator(query).refactorQuery();
		if (r.problemsEncountered.isPresent())
			return r.problemsEncountered.get();
		else if (preview)
			return r.refactored;
		else
			return connector.execSql(r.refactored);
	}

	private String hdlDropTable(QueryManipulator ftr, boolean preview) {
		ResolvedTableNames id = ftr.getTableNameToDrop();

		if (id.problemsEncountered.isPresent())
			return id.problemsEncountered.get();
		else if (preview)
			return "Api call delete, table id: " + id.idFrom.get();
		else
			try {
				connector.deleteTable(id.idFrom.get());
				onStructureChanged();
				return "dropped '" + id.idFrom.get() + "'";
			} catch (IOException e) {
				return e.getMessage();
			}
	}

	public String getQueryResult(String query) {
		logger.Info("processing :" + query);

		try {
			QueryManipulator ftr = createManipulator(query);
			switch (ftr.statementType) {
			case ALTER:
				return hdlAlterTable(ftr, execute);

			case SELECT:
				return hdlQuery(query, ftr, execute);

			case CREATE_VIEW:
				return hdlQuery(query, ftr, execute);

			case DROP:
				return hdlDropTable(ftr, execute);

			default:
				return "Statement not covered: " + query;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public String previewExecutedSql(String query) {
		QueryManipulator ftr = createManipulator(query);

		switch (ftr.statementType) {

		case ALTER:
			return hdlAlterTable(ftr, preview);

		case SELECT:
			return hdlQuery(query, ftr, preview);

		case CREATE_VIEW:
			return hdlQuery(query, ftr, preview);

		case DROP:
			return hdlDropTable(ftr, preview);

		default:
			return "Statement not covered: " + query;
		}

	}

	public QueryPatching getPatcher(String query, int cursorPos) {
		return createManipulator(query).getPatcher(cursorPos);
	}

	public List<SyntaxElement> getHighlighting(String query) {
		return getSyntaxElements(createManipulator(query).getCursorContextListener(0));
	}

	private List<SyntaxElement> getSyntaxElements(CursorContextListener l) {
		Check.isTrue(l.tableList.size() <= 1);

		Optional<TableReference> tableReference;
		if (l.tableList.size() == 1)
			tableReference = resolveTable(l.tableList.get(0));
		else
			tableReference = Optional.absent();

		Semantics semantics = new Semantics(tableReference, l.allNames);
		semantics.setSemanticAttributes(l.syntaxElements);

		if (debug)
			debug(l.syntaxElements);

		return l.syntaxElements;
	}

	private void debug(List<SyntaxElement> syntaxElements) {
		System.out.println("--- syntax elements ---");
		for (SyntaxElement s : syntaxElements)
			System.out
					.println(String.format("%s %s %s", s.value, s.type.name(), s.hasSemanticError() ? "<bad>" : "ok"));
	}

	private static int lengthTableId = 41;
	private Optional<TableReference> resolveTable(NameRecognitionTable tableRecognized) {

		if (tableRecognized.TableName().isPresent()) {
			String tableName = tableRecognized.TableName().get();
			Optional<String> id = resolveTableId(tableName);

			if (nameMayActuallyBeAnId(tableName, id)){
				Optional<String> maybeTableName = tableNameToIdMapper.nameForId(tableName);
				if (maybeTableName.isPresent())
				{
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

	private boolean nameMayActuallyBeAnId(String tableName, Optional<String> id) {
		return ! id.isPresent() && tableName.length() == lengthTableId;
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
