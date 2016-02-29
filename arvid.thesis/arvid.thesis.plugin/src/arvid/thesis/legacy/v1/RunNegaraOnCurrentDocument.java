package arvid.thesis.legacy;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import arvid.thesis.plugin.ClojureBridge;
import changenodes.operations.IOperation;

public class RunNegaraOnCurrentDocument {
	
	/**
	 * Internal helper, parsing a string, resulting in its corresponding AST
	 */
	private static ASTNode sourceToAST(String source) {
		// Create parser
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// Set parser source
		parser.setSource(source.toCharArray());
		// Set options
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, options);
		parser.setCompilerOptions(options);
		// Run it
		return parser.createAST(null);
	}

	/**
	 * Run negara's algorithm on the current document. Changes are determined by differencing the saved 
	 * an the active version.
	 * @throws Exception
	 */
	public static void runNegaraOnCurrentDocument(String savedContent, String currentContent) throws Exception {
	    // Let's parse
		ASTNode before = sourceToAST(savedContent);
		ASTNode after = sourceToAST(currentContent);
		// Let our friend Mister Clojure perform the differencing
		//Collection<IOperation> result = ClojureBridge.difference(before, after);
		// Run negara
		//ClojureBridge.runNegara(result);
	}
}