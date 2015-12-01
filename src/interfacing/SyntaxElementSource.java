package interfacing;

import java.util.List;

public interface SyntaxElementSource {
	public List<SyntaxElement> get(String query);
	public Completions get(String query, int cursorPos);
}
