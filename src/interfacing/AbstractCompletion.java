package interfacing;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;

import cg.common.check.Check;
import gc.common.structures.OrderedIntTuple;

public abstract class AbstractCompletion {

	public final String displayName;

	public final SqlCompletionType completionType;
	public final AbstractCompletion parent;
	public final List<AbstractCompletion> children = new LinkedList<AbstractCompletion>();

	public AbstractCompletion(SqlCompletionType completionType, String displayName, AbstractCompletion parent) {
		this.displayName = displayName;
		this.completionType = completionType;
		this.parent = parent;
	}

	public void addChild(AbstractCompletion c) {
		Check.notNull(c);
		children.add(c);
	}

	public AbstractCompletion addAsChildren(Completions completions) {
		Check.notNull(completions);
		for (AbstractCompletion c : completions.getAll())
			children.add(c);
		return this;
	}

	@Override
	public String toString() {
		return displayName;
	}

}
