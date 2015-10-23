package manipulations.results;

import com.google.common.base.Optional;

import util.OptionalUtil;

public class ParseResult {
	public final Optional<String> problemsEncountered;

	public ParseResult(String problemsEncountered) {
		this.problemsEncountered = OptionalUtil.of(problemsEncountered);
	}
}