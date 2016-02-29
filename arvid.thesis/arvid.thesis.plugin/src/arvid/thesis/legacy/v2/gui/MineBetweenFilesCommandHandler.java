package arvid.thesis.plugin.gui;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import arvid.thesis.plugin.ClojureBridge;
import arvid.thesis.plugin.ThesisPlugin;

/**
 * Command handler MineBetweenFilesCommandHandler
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class MineBetweenFilesCommandHandler extends AbstractHandler {

	////////////////
	// Public API //
	////////////////
	
	/**
	 * the command has been executed, perform correct command handling.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageConsoleStream console = ThesisPlugin.getDefault().getConsoleStream();
		// Get selection and ensure two items are selected
		ISelection s = window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
		IStructuredSelection selection = (s instanceof IStructuredSelection ? (IStructuredSelection)s : null);
		if (selection == null || selection.size() != 2) {
			console.println("ERROR: Please select two Java files.");
			return null;
		}
		// Get selected CompilationUnits, ensure that in fact Java files are selected
		Iterator<?> selectionIterator = selection.iterator();
		Object firstSelection = selectionIterator.next();
		ICompilationUnit first = (firstSelection instanceof ICompilationUnit ? (ICompilationUnit)firstSelection : null);
		Object secondSelection = selectionIterator.next();
		ICompilationUnit second = (secondSelection instanceof ICompilationUnit ? (ICompilationUnit)secondSelection : null);
		if (first == null || second == null) {
			console.println("ERROR: Please select two Java files.");
			return null;
		}
		// Create a ChangeHistory
		ChangeHistory history = new ChangeHistory();
		try {
			history.addCommit("Original " + first.getElementName());
			history.addChangesToCurrentCommit(first.getSource(), second.getSource());
			history.addCommit("Original " + second.getElementName());
			history.addChangesToCurrentCommit(second.getSource(), first.getSource());
		} catch (JavaModelException e1) {
			console.println("ERROR: Unknown error.");
		}
		// Ask mining settings
		MiningDetailsDialog sbaad = new MiningDetailsDialog(window.getShell(), history);
		if(sbaad.open() == Window.CANCEL) {
			return null;
		}
		// Invoke the miner
		try {
			ClojureBridge.mine(
					sbaad.getMinSupport(), 
					sbaad.getSelectedGeneralizerDefinition(), sbaad.getSelectedGrouperDefintion(), 
					sbaad.getCommit());
		} catch (Exception e) {
			console.println("ERROR: Mining failed.");
			e.printStackTrace();
			return null;
		}
		// Done
		return null;
	}

}

