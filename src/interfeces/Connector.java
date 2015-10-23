
package interfeces;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface Connector {

  List<TableInfo> getTableInfo();

  Map<String, String> getTableNameToIdMap();

  String executeSql(String query) throws IOException;

  String execSql(String query);

  void deleteTable(String tableId) throws IOException;

  String renameTable(String tableId, String newName);

}
