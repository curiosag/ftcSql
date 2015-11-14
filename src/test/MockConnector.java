package test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cg.common.check.Check;
import interfacing.Connector;
import interfacing.TableInfo;

import static org.junit.Assert.*;

public class MockConnector implements Connector {

	private Map<String, String> tableNameToIdMap;

	public static MockConnector instance(Map<String, String> tableNameToIdMap) {

		if (tableNameToIdMap == null) {
			tableNameToIdMap = new HashMap<String, String>();

			for (Integer i = 0; i < 10; i++)
				tableNameToIdMap.put("table" + i.toString(), "ID" + i.toString());
		}
		return new MockConnector(tableNameToIdMap);
	}

	private MockConnector(Map<String, String> tableNameToIdMap) {
		Check.notNull(tableNameToIdMap);
		this.tableNameToIdMap = tableNameToIdMap;
	}

	@Override
	public List<TableInfo> getTableInfo() {
		fail("no call expected");
		return null;
	}

	@Override
	public Map<String, String> getTableNameToIdMap() {
		return tableNameToIdMap;
	}

	@Override
	public String executeSql(String query) throws IOException {
		fail("no call expected");
		return null;
	}

	@Override
	public String execSql(String query) {
		fail("no call expected");
		return null;
	}

	@Override
	public void deleteTable(String tableId) throws IOException {
		fail("no call expected");
	}

	@Override
	public String renameTable(String tableId, String newName) {
		fail("no call expected");
		return null;
	}

}
