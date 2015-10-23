package manipulations;

import org.antlr.v4.runtime.tree.RuleNode;

import parser.FusionTablesSqlBaseVisitor;

public class ContiVisitor extends FusionTablesSqlBaseVisitor<String> {

	public ContiVisitor() {
	}

	@Override
	public String visitChildren(RuleNode node) {
		Object p = node.getPayload();
		
		return p.toString();
	}
	
}
