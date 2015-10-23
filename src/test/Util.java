package test;

import cg.common.core.SystemLogger;
import manipulations.QueryManipulator;

public class Util {

	public static QueryManipulator getManipulator(String query) {
		return new QueryManipulator(MockConnector.instance(null), new SystemLogger(), query);
	}

}
