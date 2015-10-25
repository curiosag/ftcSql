package manipulations;

import cg.common.check.Check;

public enum NameRecognitionState {

	INITIAL {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			NameRecognitionState result;
			if (input.toUpperCase().equals("ST_INTERSECTS"))
				result = EXPR_INTERSECTS;
			else if (isSeparator(input))
				result = QUALIFIER;
			else if (emptyExpressionStatement(input))
				result = EXPR_LEFT_SIDE_COMPLETE;
			else
				result = NAME1;

			debug(input, this, result);
			return result;
		}

		private boolean emptyExpressionStatement(String input) {
			return eqSym(input);
		}
	},

	EXPR_INTERSECTS {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			NameRecognitionState result;
			if (input.equals("("))
				result = EXPR_LPAREN;
			else
				result = ERROR;

			debug(input, this, result);
			return result;
		}
	},

	EXPR_LPAREN {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			NameRecognitionState result;
			if (input.equals(")") || input.equals("="))
				result = EXPR_LEFT_SIDE_COMPLETE;
			else if (input.equals("."))
				result = QUALIFIER;
			else
				result = NAME1;

			debug(input, this, result);
			return result;
		}
	},

	NAME1 {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			NameRecognitionState result;
			if (isSeparator(input))
				result = QUALIFIER;
			else if (eqSym(input))
				result = EXPR_LEFT_SIDE_COMPLETE;
			else if (input.equals(")"))
				result = MAYBE_EXPR_LEFT_SIDE_COMPLETE;
			else
				result = ERROR;

			debug(input, this, result);
			return result;
		}
	},

	QUALIFIER {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			NameRecognitionState result;
			if (isSeparator(input))
				result = ERROR;
			else if (eqSym(input))
				result = EXPR_LEFT_SIDE_COMPLETE;
			else if (input.equals(")"))
				result = MAYBE_EXPR_LEFT_SIDE_COMPLETE;

			else
				result = NAME2;

			debug(input, this, result);
			return result;
		}

	},

	NAME2 {
		@Override
		public NameRecognitionState next(String input) {
			NameRecognitionState result;

			if (eqSym(input))
				result = EXPR_LEFT_SIDE_COMPLETE;
			else if (input.equals(")"))
				result = MAYBE_EXPR_LEFT_SIDE_COMPLETE;
			else
				result = ERROR;
			debug(input, this, result);
			return result;
		}
	},

	EXPR_LEFT_SIDE_COMPLETE {
		@Override
		public NameRecognitionState next(String input) {
			NameRecognitionState result = EXPR_LEFT_SIDE_COMPLETE;
			debug(input, this, result);
			return result;
		}
	},

	ERROR {
		@Override
		public NameRecognitionState next(String input) {
			NameRecognitionState result = ERROR;
			debug(input, this, result);
			return result;
		}
	},

	MAYBE_EXPR_LEFT_SIDE_COMPLETE {
		@Override
		public NameRecognitionState next(String input) {
			NameRecognitionState result = MAYBE_EXPR_LEFT_SIDE_COMPLETE;
			debug(input, this, result);
			return result;
		}
	};

	public abstract NameRecognitionState next(String input);

	private static boolean debug = true;

	private static void debug(String input, NameRecognitionState from, NameRecognitionState to) {
		if (debug)
			System.out.println(String.format("input: %s from: %s to: %s ", input, from.name(), to.name()));
	}

	private static boolean eqSym(String input) {
		return input.equals("=");
	}

	public boolean in(NameRecognitionState... states) {
		for (NameRecognitionState state : states)
			if (this == state)
				return true;
		return false;
	}

	private static boolean isSeparator(String s) {
		return s != null && (s.equals(".") || s.toUpperCase().equals("AS"));
	}

}
