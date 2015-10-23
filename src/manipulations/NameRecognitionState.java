package manipulations;

import cg.common.check.Check;

public enum NameRecognitionState {

	INITIAL {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			if (isSeparator(input))
				return QUALIFIER;
			else
				return NAME1;
		}
	},

	NAME1 {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			if (isSeparator(input))
				return QUALIFIER;
			else
				return ERROR;
		}
	},

	QUALIFIER {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);

			if (isSeparator(input))
				return ERROR;
			else
				return NAME2;
		}
	},

	NAME2 {
		@Override
		public NameRecognitionState next(String input) {
			Check.notEmpty(input);
			return ERROR;
		}
	},

	ERROR {
		@Override
		public NameRecognitionState next(String input) {
			return ERROR;
		}
	};

	public abstract NameRecognitionState next(String input);

	public boolean in(NameRecognitionState ... states) {
		for (NameRecognitionState state : states) 
			if (this == state)
				return true;
		return false;
	}

	private static boolean isSeparator(String s) {
		return s != null && (s.equals(".") || s.toUpperCase().equals("AS"));
	}

}
