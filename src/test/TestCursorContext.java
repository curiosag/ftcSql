package test;

import org.junit.Test;

import com.google.common.base.Optional;

import static org.junit.Assert.*;

import manipulations.CursorContextColumnName;
import manipulations.CursorContextListener;
import manipulations.CursorContextTableName;
import manipulations.QueryManipulator;

public class TestCursorContext {

	private int shift(int i) {
		return i + 0;
	}

	// @Test
	public void debugContextListener() {
		String query = "Select x. from AA as b;";
		QueryManipulator m = test.Util.getManipulator(query);
		int indexBeforeCursor = query.indexOf('x');
		// indexBeforeCursor = 6;

		CursorContextListener c = m.getCursorContextListener(indexBeforeCursor);
		c.debug(query, indexBeforeCursor);
	}

	@Test
	public void testCursorOutsideColumnContext() {
		Optional<CursorContextColumnName> c = test.Util.getCursorContextColumnName("Select a from  s", shift(11));
		assertFalse(c.isPresent());
	}

	@Test
	public void testResultColumnEmpty() {
		Optional<CursorContextColumnName> c;

		c = test.Util.getCursorContextColumnName("Select ", shift(4));
		assertFalse(c.isPresent());

		// context starts at index 5??
		c = test.Util.getCursorContextColumnName("Select ", shift(5)); 
		assertTrue(c.isPresent());
		
		c = test.Util.getCursorContextColumnName("Select ", shift(6)); 
		assertTrue(c.isPresent());
	}

	@Test
	public void testColumnJoker() {
		Optional<CursorContextColumnName> c;

		c = test.Util.getCursorContextColumnName("Select *", shift(7));
		assertTrue(c.isPresent());
		assertEquals("*", c.get().columnName.get());

		c = test.Util.getCursorContextColumnName("Select a.*  from o;", shift(7));
		assertTrue(c.isPresent());
		assertEquals("*", c.get().columnName.get());
		assertEquals("a", c.get().tableName.get());
	}

	@Test
	public void testContextResultColumn() {
		testPermutations("select %s");
		testPermutations("select %s from a;");
	}

	@Test
	public void testTableAliasResolution() {
		Optional<CursorContextColumnName> c;

		String query = "Select a.x from u left outer join b as a on u.id = a.id";
		c = test.Util.getCursorContextColumnName(query, shift(query.indexOf("a")));

		assertTrue(c.isPresent());
		assertTrue(c.get().columnName.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("x", c.get().columnName.get());
		assertEquals("b", c.get().tableName.get());

	}

	@Test
	public void testContextExpression() {
		testPermutations("Select a from b where %s;");
		testPermutations("Select a from b where %s=1");
	}

	@Test
	public void testContextGroupingClause() {
		testPermutations("Select a from b group by %s;");
		testPermutations("Select a from b group by %s");
	}

	@Test
	public void testContextOrderingClause() {
		testPermutations("Select a from b order by %s;");
		testPermutations("Select a from b order by %s");
	}
	
	@Test
	public void testContextAggregateClause() {
		testPermutations("Select b, sum(%s) from b as A group by A.x order by a.y;");
		testPermutations("Select b, sum(%s");
	}

	// @Test
	public void testCursorContextTableName() {
		Optional<CursorContextTableName> c = test.Util.getCursorContextTableName("Select x from  s", shift(11));
		assertFalse(c.isPresent());

		String query = "Select  from ";
		c = test.Util.getCursorContextTableName(query, shift(13));
		assertTrue(c.isPresent());
		assertFalse(c.get().tableName.isPresent());

		query = "Select  from x";
		c = test.Util.getCursorContextTableName(query, shift(query.indexOf("x")));
		assertTrue(c.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("x", c.get().tableName.get());

		query = "Select c from x as y";
		c = test.Util.getCursorContextTableName(query, shift(query.indexOf("x")));
		assertTrue(c.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("x", c.get().tableName.get());

		query = "Select a.x from A left outer join X as E on ";
		c = test.Util.getCursorContextTableName(query, shift(query.indexOf("X")));
		assertTrue(c.isPresent());
		assertTrue(c.get().tableName.isPresent());
		assertEquals("X", c.get().tableName.get());
	}

	/**
	 * 
	 * @param queryTemplate
	 *            smthg like "select %s from x where y=0" or
	 *            "select a from x where %s=0"
	 * 
	 *            %s will be replaced by all of these combinations, which are
	 *            expected when checking the results.
	 * 
	 *            Table Column %s T v "T.v" T null "T." null v ".v" null null ""
	 * 
	 */

	private final boolean emptyContext = true;

	public void testPermutations(String queryTemplate) {
		testPermutatation(queryTemplate, "T", "v");
		testPermutatation(queryTemplate, "T", ""); // pass "" rather than null
													// to indicate non-existence
		testPermutatation(queryTemplate, "", "v");
		testPermutatation(queryTemplate, "", "");
		testPermutatation(queryTemplate, "", "", emptyContext);

	}

	private void testPermutatation(String queryTemplate, String tableE, String columnE, boolean... emptyContext) {
		int pos = queryTemplate.indexOf("%s");
		
		String separator = ".";
		if (emptyContext.length > 0 && emptyContext[0])
			separator = "";
		
		String query = queryTemplate.replace("%s", tableE + separator + columnE);
		Optional<CursorContextColumnName> c = test.Util.getCursorContextColumnName(query, pos);
		checkPermutation(c, tableE, columnE, query, emptyContext);
	}

	private void checkPermutation(Optional<CursorContextColumnName> c, String tableE, String columnE, String query,
			boolean... emptyContext) {
		debugPermutation(c, tableE, columnE, query, emptyContext);

		assertTrue(c.isPresent());
		checkString(c.get().tableName, tableE);
		checkString(c.get().columnName, columnE);
	}

	private void debugPermutation(Optional<CursorContextColumnName> c, String tableE, String columnE, String query,
			boolean... emptyContext) {
		String fmtString;
		
		if (emptyContext.length > 0 && emptyContext[0])
			System.out.println("Checking for empty context, query '" + query + "'");
		else {
			fmtString = "Checking for table='%s', column='%s' in query '%s'";
			System.out.println(String.format(fmtString, tableE, columnE, query));
		}
		String present = c.isPresent() ? "y" : "n";
		String col = "";
		String tab = "";
		if (c.isPresent()) {
			col = c.get().columnName.or("absent");
			tab = c.get().tableName.or("absent");
		}
		fmtString = "recognized: present='%s', table='%s', column='%s'";
		System.out.println(String.format(fmtString, present, tab, col));
	}

	private void checkString(Optional<String> s, String valueExpected) {
		if (valueExpected == "")
			assertFalse(s.isPresent());
		else {
			assertTrue(s.isPresent());
			assertEquals(valueExpected, s.get());
		}
	}
}
