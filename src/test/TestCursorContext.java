package test;

import org.junit.Test;

import com.google.common.base.Optional;

import static org.junit.Assert.*;

import manipulations.CursorContext;
import manipulations.CursorContextListener;
import manipulations.CursorContextType;
import manipulations.QueryManipulator;
import manipulations.QueryManipulator.QueryPatcher;

public class TestCursorContext {

	private final boolean debug = false;
	
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
		Optional<CursorContext> c = test.Util.getCursorContext("Select a from  s", shift(11));
		assertTrue(c.isPresent());
		assertTrue(c.get().contextType == CursorContextType.anyRule);
		assertFalse(c.get().name.isPresent());
	}

	@Test
	public void testResultColumnEmpty() {
		Optional<CursorContext> c;

		c = test.Util.getCursorContext("Select ", shift(4));
		assertTrue(c.isPresent());
		assertTrue(c.get().contextType == CursorContextType.anyRule);
		
		c = test.Util.getCursorContext("Select ", shift(7)); 
		assertTrue(c.isPresent());
		assertTrue(c.get().contextType == CursorContextType.columnName);
	}

	@Test
	public void testColumnJoker() {
		Optional<CursorContext> c;

		c = test.Util.getCursorContext("Select *", shift(7));
		assertTrue(c.isPresent());
		assertEquals("*", c.get().name.get());

		c = test.Util.getCursorContext("Select a.*  from o;", shift(7));
		assertTrue(c.isPresent());
		assertEquals("*", c.get().name.get());
		assertEquals("a", getTableName(c).get());
	}

	@Test
	public void testContextResultColumn() {
		testPermutations("select %s");
		testPermutations("select %s from a;");
	}

	@Test
	public void testTableAliasResolution() {
		Optional<CursorContext> c;

		String query = "Select a.x from u left outer join b as a on u.id = a.id";
		c = test.Util.getCursorContext(query, shift(query.indexOf("a")));

		assertTrue(c.isPresent());
		assertTrue(c.get().name.isPresent());
		assertTrue(getTableName(c).isPresent());
		assertEquals("x", c.get().name.get());
		assertEquals("b", getTableName(c).get());

	}

	@Test
	public void testContextExpression() {
		testPermutations("Select a from b where %s;");
		testPermutations("Select a from b where A=1 and %s=2");
		testPermutations("Select a from b where st_intersects(%s, circle(latlang(1,1), 1))");
		testPermutations("Select a from b where st_intersects(%s, circ");
		testPermutations("Select a from b where st_intersects(%s");
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
		Optional<CursorContext> c = test.Util.getCursorContext("Select x from  s", shift(11));
		assertFalse(c.isPresent());
		assertEquals(CursorContextType.tableName, c.get().contextType);

		String query = "Select  from ";
		c = test.Util.getCursorContext(query, shift(13));
		assertTrue(c.isPresent());
		assertFalse(c.get().name.isPresent());

		query = "Select  from x";
		c = test.Util.getCursorContext(query, shift(query.indexOf("x")));
		assertTrue(c.isPresent());
		assertTrue(c.get().name.isPresent());
		assertEquals("x", c.get().name.get());

		query = "Select c from x as y";
		c = test.Util.getCursorContext(query, shift(query.indexOf("x")));
		assertTrue(c.isPresent());
		assertTrue(c.get().name.isPresent());
		assertEquals("x", c.get().name.get());

		query = "Select a.x from A left outer join X as E on ";
		c = test.Util.getCursorContext(query, shift(query.indexOf("X")));
		assertTrue(c.isPresent());
		assertTrue(c.get().name.isPresent());
		assertEquals("X", c.get().name.get());
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

		testPatching(queryTemplate);
		
	}

	private void testPermutatation(String queryTemplate, String tableE, String columnE, boolean... emptyContext) {

		int pos = queryTemplate.indexOf("%s");
				
		String separator = ".";
		
		String query = queryTemplate.replace("%s", tableE + separator + columnE);
		
		QueryPatcher c = test.Util.getPatcher(query, pos);
				
		checkPermutation(c.context, tableE, columnE, query, emptyContext(emptyContext));
		
	}

	public void testPatching(String queryTemplate) {
		testPatching(queryTemplate, "v");
		testPatching(queryTemplate, ""); 
	}

	
	private void testPatching(String queryTemplate, String value) {
		System.out.println("template: " + queryTemplate);
		
		int pos = queryTemplate.indexOf("%s");
	
		String query = queryTemplate.replace("%s", value);
		QueryPatcher c = test.Util.getPatcher(query, pos);
		String patched = c.patch(Optional.of("XXX"), Optional.of("SomeTable"));
		
		System.out.println("patched: " + patched);
	}


	private void checkPermutation(Optional<CursorContext> c, String tableE, String columnE, String query,
			boolean emptyContext) {
		debugPermutation(c, tableE, columnE, query, emptyContext);

		assertTrue(c.isPresent());
		assertEquals(CursorContextType.columnName, c.get().contextType);
		checkString(c.get().name, columnE);
		checkString(getTableName(c), tableE);
	}

	private Optional<String> getTableName(Optional<CursorContext> c) {
		return c.get().otherName;
	}

	private void debugPermutation(Optional<CursorContext> c, String tableE, String columnE, String query,
			boolean emptyContext) {
		
		if (! debug)
			return;
		
		String fmtString;
		
		if (emptyContext)
			System.out.println("Checking for empty context, query '" + query + "'");
		else {
			fmtString = "Checking for table='%s', column='%s' in query '%s'";
			System.out.println(String.format(fmtString, tableE, columnE, query));
		}
		String present = c.isPresent() ? "y" : "n";
		String col = "";
		String tab = "";
		if (c.isPresent()) {
			col = c.get().name.or("absent");
			tab = getTableName(c).or("absent");
		}
		fmtString = "recognized: present='%s', table='%s', column='%s'";
		System.out.println(String.format(fmtString, present, tab, col));
	}

	private boolean emptyContext(boolean... emptyContext) {
		return emptyContext.length > 0 && emptyContext[0];
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
