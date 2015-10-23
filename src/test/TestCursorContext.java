package test;

import org.junit.Test;

import com.google.common.base.Optional;

import static org.junit.Assert.*;

import cg.common.check.Check;
import manipulations.CursorContext;
import manipulations.CursorContextColumnName;
import manipulations.CursorContextListener;
import manipulations.CursorContextTableName;
import manipulations.QueryManipulator;

public class TestCursorContext {

	//@Test
	public void debugContextListener() {
		String query = "Select x. from AA as b;";
		QueryManipulator m = test.Util.getManipulator(query);
		int indexBeforeCursor = query.indexOf('x');
		// indexBeforeCursor = 6;

		CursorContextListener c = m.getCursorContextListener(indexBeforeCursor);
		c.debug(query, indexBeforeCursor);
	}

	private Optional<CursorContext> getCursorContext(String query, int cursorPosition) {
		QueryManipulator m = test.Util.getManipulator(query);
		Optional<CursorContext> c = m.getCursorContext(cursorPosition);
		return c;
	}

	private Optional<CursorContextColumnName> getCursorContextColumnName(String query, int cursorPosition)
	{
		Optional<CursorContext> c = getCursorContext(query, cursorPosition);
		
		if (!c.isPresent())
			return Optional.absent();
		
		assertTrue(c.get() instanceof CursorContextColumnName);
		return Optional.of((CursorContextColumnName) c.get());
	}

	
	private Optional<CursorContextTableName> getCursorContextTableName(String query, int cursorPosition)
	{
		Optional<CursorContext> c = getCursorContext(query, cursorPosition);
		
		if (!c.isPresent())
			return Optional.absent();
		
		assertTrue(c.get() instanceof CursorContextTableName);
		return Optional.of((CursorContextTableName) c.get());
	}

	@Test
	public void testCursorOutsideColumnContext() {
		Optional<CursorContextColumnName> c = getCursorContextColumnName("Select a from  s", 11);
		assertFalse(c.isPresent());
	}
	
	@Test
	public void testResultColumnEmpty() {
		Optional<CursorContextColumnName> c;
	
		c = getCursorContextColumnName("Select ", 4);  
		assertFalse(c.isPresent());
		
		c = getCursorContextColumnName("Select ", 5); // why does the context start at 5??? 
	    assertTrue(c.isPresent());
	}
	
	@Test
	public void  testResultColumnWithTableNameOnly() {
		Optional<CursorContextColumnName> c;
		String query;
		query = "Select a.";
		c = getCursorContextColumnName(query, query.indexOf(".")); // why does the context start at 5??? 
		assertTrue(c.isPresent());
		assertFalse(c.get().columnName.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("a", c.get().tableName.get());	
	
		query = "Select a. from b as a";
		c = getCursorContextColumnName(query, query.indexOf(".")); // why does the context start at 5??? 
		assertTrue(c.isPresent());
		assertFalse(c.get().columnName.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("b", c.get().tableName.get());
	}
	
	@Test
	public void testContextResultColumn() {
		Optional<CursorContextColumnName> c;
		
		String query = "Select x";
		c = getCursorContextColumnName(query, query.indexOf("x")); // why does the context start at 5??? 
		assertTrue(c.isPresent());
		assertTrue(c.get().columnName.isPresent());
		assertFalse(c.get().tableName.isPresent());
		assertEquals("x", c.get().columnName.get());
			
		query = "Select a.x from u left outer join b as a on u.id = a.id";
		c = getCursorContextColumnName(query, query.indexOf("a")); // why does the context start at 5??? 
		assertTrue(c.isPresent());
		assertTrue(c.get().columnName.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("x", c.get().columnName.get());
		assertEquals("b", c.get().tableName.get());
		
		query = "Select a.x from b as a";
		c = getCursorContextColumnName(query, query.indexOf("a")); // why does the context start at 5??? 
		assertTrue(c.isPresent());
		assertTrue(c.get().columnName.isPresent());
		assertTrue(c.get().tableName.isPresent());
		
		assertEquals("x", c.get().columnName.get());
		assertEquals("b", c.get().tableName.get());
	}

	@Test
	public void  testQualifiedColumnNameOrderingClause() {
		Optional<CursorContextColumnName> c;
		String query;
		query = "Select a from b as a group by a.x having sum(a.y) > 10 order by a.x";
		c = getCursorContextColumnName(query, query.indexOf("x")); // why does the context start at 5??? 
		assertTrue(c.isPresent());
		assertTrue(c.get().columnName.isPresent());
		assertTrue(c.get().tableName.isPresent());
		
		assertEquals("x", c.get().columnName.get());
		assertEquals("b", c.get().tableName.get());
	}

	@Test
	public void  testQualifiedColumnNameAggregateClause() {
		Optional<CursorContextColumnName> c;
		String query;
		query = "Select a from b as a group by a.x having sum(a.x) > 10 order by a.y";
		c = getCursorContextColumnName(query, query.indexOf("a.x")); 
		assertTrue(c.isPresent());
		assertTrue(c.get().columnName.isPresent());
		assertTrue(c.get().tableName.isPresent());
		
		assertEquals("x", c.get().columnName.get());
		assertEquals("b", c.get().tableName.get());
	}
	
	//@Test
	public void testCursorContextTableName() {
		Optional<CursorContextTableName> c = getCursorContextTableName("Select x from  s", 11);
		assertFalse(c.isPresent());
		
		String query = "Select  from ";
		c = getCursorContextTableName(query, 13);
		assertTrue(c.isPresent());
		assertFalse(c.get().tableName.isPresent());
		
		query = "Select  from x";
		c = getCursorContextTableName(query, query.indexOf("x"));
		assertTrue(c.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("x", c.get().tableName.get());
		
		query = "Select c from x as y";
		c = getCursorContextTableName(query, query.indexOf("x"));
		assertTrue(c.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("x", c.get().tableName.get());
		
		query = "Select a.x from A left outer join X as E on ";
		c = getCursorContextTableName(query, query.indexOf("X"));
		assertTrue(c.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("X", c.get().tableName.get());
	}
}
