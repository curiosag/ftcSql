package manipulations;

import java.util.Map;

import com.google.common.base.Optional;

import cg.common.check.Check;

public class TableNameToIdMapper {

	private final Map<String, String> namesToIds;

	public TableNameToIdMapper(Map<String, String> namesToIds) {
		Check.notNull(namesToIds);

		this.namesToIds = namesToIds;
	}

	public boolean isId(String identifier) {
		return namesToIds.containsValue(identifier);
	}

	public boolean isName(String identifier) {
		return namesToIds.containsKey(identifier);
	}

	public Optional<String> idForName(String value) {
		if (isName(value))
			return Optional.of(namesToIds.get(value));
		else
			return Optional.absent();
	}

	public Optional<String> resolveTableId(String value) {
		Optional<String> result;

		if (isId(value))
			result = Optional.of(value);
		else
			result = idForName(value);

		return result;
	}

}
