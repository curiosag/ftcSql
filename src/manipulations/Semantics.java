package manipulations;

import java.util.List;
import com.google.common.base.Optional;

import cg.common.check.Check;
import interfacing.SyntaxElement;
import interfacing.SyntaxElementType;
import manipulations.results.TableReference;
import util.Op;

public class Semantics {

	List<NameRecognition> columnReferences;
	private final Optional<TableReference> tableReference;

	public Semantics(Optional<TableReference> tableReference, List<NameRecognition> names) {

		Check.notNull(names);
		Check.notNull(tableReference);

		this.columnReferences = names;
		this.tableReference = tableReference;
	}

	/**
	 * 
	 * @param tokens
	 * @param allNames2
	 * @return tokens mutated
	 */
	public List<SyntaxElement> setSemanticAttributes(List<SyntaxElement> tokens) {

		for (SyntaxElement e : tokens)
			e.setSemanticError(! hasSemanticError(e));

		return tokens;
	}

	private boolean hasSemanticError(SyntaxElement e) {
		if (!Op.in(e.type, SyntaxElementType.columnName, SyntaxElementType.tableName))
			return true;

		if (!tableReference.isPresent())
			return false;
		TableReference tableRef = tableReference.get();

		if (e.type == SyntaxElementType.columnName)
			return findColumnName(e.value, tableRef.columnNames);
		
		// tableName may occur also in qualified column names as <tableName>.<columnName>
		// where it also may be an alias or the table Id
		if (e.type == SyntaxElementType.tableName)
			return Op.in(e.value, tableRef.tableName, tableRef.tableAlias.or(""), tableRef.tableId);

		return true;

	}

	private boolean findColumnName(String value, List<String> columnNames) {
		for (String s : columnNames)
			if (s.equals(value))
				return true;
		
		return false;
	}

}
