package test;

import com.google.common.base.Optional;

import cg.common.core.SystemLogger;
import manipulations.CursorContext;
import manipulations.QueryManipulator;
import manipulations.QueryManipulator.QueryPatcher;

public class Util {

	public static QueryManipulator getManipulator(String query) {
		return new QueryManipulator(MockConnector.instance(null), new SystemLogger(), query);
	}

	public static Optional<CursorContext> getCursorContext(String query, int cursorPosition) {
		return getManipulator(query).getCursorContext(cursorPosition);
	}

	public static QueryPatcher getPatcher(String query, int cursorPosition) {
		return getManipulator(query).getPatcher(cursorPosition);
	}

}
