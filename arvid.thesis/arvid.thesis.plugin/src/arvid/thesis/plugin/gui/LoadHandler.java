package arvid.thesis.plugin.gui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import arvid.thesis.plugin.ClojureBridge;

public class LoadHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ClojureBridge.loadClojureCode();
		return null;
	}

}
