package test;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

import manipulations.NameRecognitionColumn;
import manipulations.NameRecognition;
import manipulations.NameRecognitionState;
import manipulations.NameRecognitionTable;

public class TestNameRecognition {

	private NameRecognition getNR() {
		return new NameRecognition();
	};

	private String[] sa(String... values) {
		return values;
	}

	private void digest(NameRecognition r, String[] tokens)
	{
		for (int i = 0; i < tokens.length; i++)
			r.digest(new MockToken(tokens[i]));
	}
	
	private void check(String[] tokens, NameRecognitionState stateExpected, String name1Expected,
			String name2Expected) {
		System.out.println("---------------------------");
		NameRecognition r = getNR();
		digest(r, tokens);
		assertEquals(stateExpected, r.state);
		assertTrue(r.getName1().equals(Optional.fromNullable(name1Expected)));
		assertTrue(r.getName2().equals(Optional.fromNullable(name2Expected)));
	}

	private void testNameRecognition(String separator) {
		check(sa(), NameRecognitionState.INITIAL, null, null);
		check(sa(separator), NameRecognitionState.QUALIFIER, null, null);
		check(sa("a", "b"), NameRecognitionState.ERROR, "a", null);
		check(sa("a", separator), NameRecognitionState.QUALIFIER, "a", null);
		check(sa("a", separator, "b", separator), NameRecognitionState.ERROR, "a", "b");
		check(sa("a", separator, "b", "c"), NameRecognitionState.ERROR, "a", "b");
		check(sa("a"), NameRecognitionState.NAME1, "a", null);
		check(sa("a", separator, "b", "="), NameRecognitionState.EXPR_LEFT_SIDE_COMPLETE, "a", "b");
		check(sa("a", separator, "="), NameRecognitionState.EXPR_LEFT_SIDE_COMPLETE, "a", null);
		check(sa("a", "="), NameRecognitionState.EXPR_LEFT_SIDE_COMPLETE, "a", null);
		check(sa("="), NameRecognitionState.EXPR_LEFT_SIDE_COMPLETE, null, null);
		check(sa("ST_INTERSECTS", "(", "a", separator, "b", ")"), NameRecognitionState.MAYBE_EXPR_LEFT_SIDE_COMPLETE, "a", "b");
		check(sa("ST_INTERSECTS", "(", separator, "b", ")"), NameRecognitionState.MAYBE_EXPR_LEFT_SIDE_COMPLETE, null, "b");
		check(sa("ST_INTERSECTS", "(", "a", separator, ")"), NameRecognitionState.MAYBE_EXPR_LEFT_SIDE_COMPLETE, "a", null);
		check(sa("ST_INTERSECTS", "(", "a", ")"), NameRecognitionState.MAYBE_EXPR_LEFT_SIDE_COMPLETE, "a", null);
		check(sa("ST_INTERSECTS", "(", "a"), NameRecognitionState.NAME1, "a", null);
	}
	
	@Test
	public void testNameRecognition() {
		testNameRecognition(".");
	}

	
	@Test
	public void testColumnNameRecognition() {
		NameRecognitionColumn r = new NameRecognitionColumn();
		digest(r, sa("a", ".", "b"));
		assertTrue(r.TableName().equals(Optional.of("a")));
		assertTrue(r.ColumnName().equals(Optional.of("b")));
	}
	
	@Test
	public void testTableNameRecognition() {
		NameRecognitionTable r = new NameRecognitionTable();
		digest(r, sa("a", ".", "b"));
		assertTrue(r.TableName().equals(Optional.of("a")));
		assertTrue(r.TableAlias().equals(Optional.of("b")));
	}
	
	@Rule
    public ExpectedException thrown= ExpectedException.none();
	
	@Test
	public void testFailState(){
		thrown.expect(AssertionError.class);
		check(sa("."), NameRecognitionState.INITIAL, null, null);
	}
	
	@Test
	public void testFailName1(){
		thrown.expect(AssertionError.class);
		check(sa("."), NameRecognitionState.ERROR, "a", null);
	}
	
	@Test
	public void testFailName2(){
		thrown.expect(AssertionError.class);
		check(sa("a"), NameRecognitionState.ERROR, "b", null);
	}
	
	@Test
	public void testFailName3(){
		thrown.expect(AssertionError.class);
		check(sa("a"), NameRecognitionState.ERROR, null, null);
	}	
	
}
