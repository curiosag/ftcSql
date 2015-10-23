package test;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;

import cg.common.core.SystemLogger;
import manipulations.CursorContext;
import manipulations.CursorContextColumnName;
import manipulations.CursorContextTableName;
import manipulations.QueryManipulator;

public class Util {

	public static QueryManipulator getManipulator(String query) {
		return new QueryManipulator(MockConnector.instance(null), new SystemLogger(), query);
	}
	
	public static Optional<CursorContext> getCursorContext(String query, int cursorPosition) {
		QueryManipulator m = test.Util.getManipulator(query);
		Optional<CursorContext> c = m.getCursorContext(cursorPosition);
		return c;
	}

	public static Optional<CursorContextColumnName> getCursorContextColumnName(String query, int cursorPosition)
	{
		Optional<CursorContext> c = getCursorContext(query, cursorPosition);
		
		if (!c.isPresent())
			return Optional.absent();
		
		assertTrue(c.get() instanceof CursorContextColumnName);
		return Optional.of((CursorContextColumnName) c.get());
	}

	
	public static Optional<CursorContextTableName> getCursorContextTableName(String query, int cursorPosition)
	{
		Optional<CursorContext> c = getCursorContext(query, cursorPosition);
		
		if (!c.isPresent())
			return Optional.absent();
		
		assertTrue(c.get() instanceof CursorContextTableName);
		return Optional.of((CursorContextTableName) c.get());
	}


}
