package manipulations;

import parser.FusionTablesSqlBaseListener;

public class BaseFtListener extends FusionTablesSqlBaseListener {

	protected boolean isGenericError(String errorString) {
		return errorString.startsWith("<") && errorString.endsWith(">");
	}

}