package test;

import static org.junit.Assert.*;

import org.junit.Test;

import interfacing.SqlCompletionType;
import manipulations.CursorContext;
import util.Op;

public class TestCursorContextExpr {

	private CursorContext debug(CursorContext c)
	{
		String ops = "";
		for ( SqlCompletionType o : c.completionOptions) 
			ops = ops + o.name() + " ";
		System.out.println("completion options: " + ops);
		
		return c;
	}
	
	@Test
	public void test() {
		String q = "Select a from s where x ";
		int index = q.indexOf("x");
		
		CursorContext c = debug(test.Util.getCursorContext(q, index));
		assertTrue(Op.eq(c.completionOptions, SqlCompletionType.column, SqlCompletionType.columnConditionExprAfterColumn));
		assertTrue(c.getModelElementType() == SqlCompletionType.column);
		
		
	}

}
