package manipulations.results;

import java.util.List;

import com.google.common.base.Optional;

import structures.TableInfo;

public interface TableInfoResolver {
	Optional<TableInfo> getTableInfo(String nameOrId);

	List<TableInfo> listTables();
}
