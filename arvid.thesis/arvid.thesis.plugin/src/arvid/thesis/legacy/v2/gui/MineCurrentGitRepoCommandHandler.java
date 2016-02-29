package arvid.thesis.plugin.gui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;

import arvid.thesis.plugin.ClojureBridge;
import arvid.thesis.plugin.ThesisPlugin;

public class MineCurrentGitRepoCommandHandler extends AbstractHandler {

	////////////////
	// Public API //
	////////////////
	
	/**
	 * the command has been executed, perform correct command handling.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MessageConsoleStream console = ThesisPlugin.getDefault().getConsoleStream();
		// Get selected project's directory
		IWorkbenchWindow window =
			    PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
		IJavaProject javaProject = (IJavaProject)selection.getFirstElement();
		IProject project = javaProject.getProject();
		// Create ChangeHistory based on git commits
		ChangeHistory history;
		try {
			String folder = getGitDirectory(project.getLocation()).toString();
			history = HistoryBuilder.build(folder);
		} catch (IOException | GitAPIException e) {
			console.println("Processing git repo failed");
			return null;
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

	//////////////////////
	// Internal helpers //
	//////////////////////
	
	/**
	 * Start from the given path and climb up the tree until we find a /.git subdirectory to return.
	 * NOTE: maybe there's a better way to do this.
	 * @throws IOException
	 */
	private IPath getGitDirectory(IPath path) throws IOException {
		// Are we at the root? We've failed :(
		if(path.isRoot()) throw new IOException();
		// Check if a "/.git" directory exists in path, if so, we're done.
		File currentDirectory = path.append("/.git").toFile();
		if( currentDirectory.exists() && currentDirectory.isDirectory() ) {
			return path.append("/.git");
		}
		// Try again recursively with our parent.
		return getGitDirectory(path.removeLastSegments(1));
	}
	
}
