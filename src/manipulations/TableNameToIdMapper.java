package manipulations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Optional;

import cg.common.check.Check;
import structures.TableInfo;
import util.StringUtil;
public class TableNameToIdMapper {

	private final Map<String, String> namesToIds;

	public TableNameToIdMapper(Map<String, String> namesToIds) {
		Check.notNull(namesToIds);

		this.namesToIds = namesToIds;
	}

	public TableNameToIdMapper(List<TableInfo> tables) {
		Check.notNull(tables);

		this.namesToIds = new HashMap<String, String>();
		for (TableInfo t : tables)
			namesToIds.put(t.name, t.id);
	}

	public boolean isId(String identifier) {
		return namesToIds.containsValue(identifier);
	}

	public boolean isName(String identifier) {
		return namesToIds.containsKey(StringUtil.stripQuotes(identifier));
	}

	public Optional<String> idForName(String value) {
		if (isName(value))
			return Optional.of(namesToIds.get(StringUtil.stripQuotes(value)));
		else
			return Optional.absent();
	}

	public Optional<String> nameForId(String value) {
		for (Entry<String, String> e : namesToIds.entrySet())
			if (e.getValue().equals(value))
				return Optional.of(e.getKey());

		return Optional.absent();
	}

	public Optional<String> resolveTableId(String value) {
		Optional<String> result;

		if (isId(value))
			result = Optional.of(value);
		else
			result = idForName(StringUtil.stripQuotes(value));

		return result;
	}

}
