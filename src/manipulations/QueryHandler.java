package manipulations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cg.common.check.Check;
import cg.common.core.Logging;
import interfeces.Connector;
import interfeces.TableInfo;
import manipulations.results.RefactoredSql;
import manipulations.results.ResolvedTableNames;

public class QueryHandler {

	private final Logging logger;
	private final Connector connector;
	private final boolean preview = true;
	private final boolean execute = !preview;

	public QueryHandler(Logging logger, Connector c) {
		Check.notNull(logger);
		Check.notNull(c);
		this.logger = logger;
		this.connector = c;
	}

	private void log(String msg) {
		logger.Info(msg);
	}

	private QueryManipulator createRefactoring(String query) {
		return new QueryManipulator(connector, logger, query);
	}

	public List<TableInfo> getTableList() {
		return connector.getTableInfo();
	}

	public String getTableInfo() {
		StringBuilder sb = new StringBuilder();
		List<String> names = new ArrayList<String>();

		for (TableInfo i : getTableList()) {
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
			return String.format("Api call rename to %s, table id %s", id.nameTo, id.idFrom);
		else
			return connector.renameTable(id.idFrom.get(), id.nameTo);
	}

	private String hdlQuery(String query, QueryManipulator ftr, boolean preview) {
		RefactoredSql r = createRefactoring(query).refactorQuery();
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
				return "dropped '" + id.idFrom.get() + "'";
			} catch (IOException e) {
				return e.getMessage();
			}
	}

	public String getQueryResult(String query) {
		logger.Info("processing :" + query);

		try {
			QueryManipulator ftr = createRefactoring(query);
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
		QueryManipulator ftr = createRefactoring(query);

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

}