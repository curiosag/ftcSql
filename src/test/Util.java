package test;

import java.util.List;

import cg.common.core.SystemLogger;
import interfacing.SyntaxElement;
import manipulations.CursorContext;
import manipulations.QueryManipulator;
import manipulations.QueryPatching;
import util.Op;
import util.StringUtil;

public class Util {

	public static QueryManipulator getManipulator(String query) {
		return new QueryManipulator(MockConnector.instance(null), new SystemLogger(), query);
	}
	
	private static void debug(String objectRequested, String query, int cursorPosition)
	{
		query = StringUtil.nonNull(query);
		query = cursorPosition < query.length() ? StringUtil.insert(query, cursorPosition, "|") : query;		
		System.out.println(String.format("-- Requesting %s for index %d query {%s}", objectRequested, cursorPosition, query));
	}
	
	public static CursorContext getCursorContext(String query, int cursorPosition) {
		debug("CursorContext", query, cursorPosition);	
		return getManipulator(query).getCursorContext(cursorPosition);
	}

	public static QueryPatching getPatcher(String query, int cursorPosition) {
		debug("QueryPatching", query, cursorPosition);	
		return getManipulator(query).getPatcher(cursorPosition);
	}

	public static void debugTokens(String intro, int cursorPos, CursorContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append(intro + ": ");
		for (SyntaxElement e : context.getSyntaxElements()) {
			if (Op.between(e.from, cursorPos, e.to))
				sb.append("|");
			sb.append(String.format(" %s ", e.value));
		}
		
		System.out.println(sb.toString() + "\n");
	}
	
	public static void debugTokens(String intro, int cursorPos, List<SyntaxElement> l) {
		StringBuilder sb = new StringBuilder();
		sb.append(intro + ": ");
		for (SyntaxElement e : l) 
			sb.append(String.format(" %s (%d,%d)", e.value, e.from, e.to));
		
		
		System.out.println(sb.toString() + "\n");
	}
	
}
