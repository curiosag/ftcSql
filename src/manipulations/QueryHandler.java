package manipulations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import cg.common.check.Check;
import cg.common.core.Logging;
import interfacing.ColumnInfo;
import interfacing.Connector;
import interfacing.SyntaxElement;
import interfacing.TableInfo;
import manipulations.QueryPatching;
import manipulations.results.RefactoredSql;
import manipulations.results.ResolvedTableNames;

public class QueryHandler extends Observable {

	private boolean reload = true;
	public final boolean ADD_DETAILS = true;
	private final Logging logger;
	private final Connector connector;
	private final boolean preview = true;
	private final boolean execute = !preview;
	private final List<TableInfo> tableInfo = null;

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
		return new QueryManipulator(connector, logger, query);
	}

	private void reloadTableList() {
		internalGetTableList(reload);
	}

	private synchronized List<TableInfo> internalGetTableList(boolean reload) {
		if (tableInfo == null || reload)
			return connector.getTableInfo();
		else
			return tableInfo;
	}

	public List<TableInfo> getTableList(boolean addDetails) {
		List<TableInfo> info = internalGetTableList(!reload);
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

	public List<SyntaxElement> getHighlighting(String query)
	{
		return createManipulator(query).getSyntaxElements();
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
