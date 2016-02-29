package arvid.thesis.plugin.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import arvid.thesis.plugin.ClojureBridge;
import arvid.thesis.plugin.ThesisPlugin;

/**
 * Command handler MineCurrentDocumentChangesCommandHandler
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class MineCurrentDocumentChangesCommandHandler extends AbstractHandler {

	////////////////
	// Public API //
	////////////////
	
	/**
	 * the command has been executed, perform correct command handling.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageConsoleStream console = ThesisPlugin.getDefault().getConsoleStream();
		// Get the active editor
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor == null) {
			console.println("ERROR: No active editor.");
			return null;
		}
		// Get the editor's current contents (currentContent)
	    IDocument doc = ((ITextEditor)activeEditor).getDocumentProvider().getDocument(activeEditor.getEditorInput());
	    if (doc == null) {
			console.println("ERROR: No document in active editor.");
			return null;
	    }
	    String currentContent = doc.get();
		// Get the file's contents (savedContent)
	    String savedContent = null;
		IFile file = (IFile) activeEditor.getEditorInput().getAdapter(IFile.class);
		if (file == null) {
			console.println("ERROR: File not saved yet.");
			return null;
		}
		try {
			InputStream is = file.getContents();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    byte[] buffer = new byte[1024];
		    int length = 0;
		    while ((length = is.read(buffer)) != -1) {
		        baos.write(buffer, 0, length);
		    }
		    savedContent = new String(baos.toByteArray(), file.getCharset());
		} catch (CoreException | IOException e1) {
			console.println("ERROR: Could not get the contents of the saved file.");
			e1.printStackTrace();
			return null;
		}
		// Create a ChangeHistory
		ChangeHistory history = new ChangeHistory();
		history.addCommit("Current document changes");
		history.addChangesToCurrentCommit(savedContent, currentContent);
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
