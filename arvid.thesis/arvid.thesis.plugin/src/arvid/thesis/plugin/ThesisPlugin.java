package arvid.thesis.plugin;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ThesisPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "arvid.thesis.plugin"; //$NON-NLS-1$
	private static final String CONSOLE_NAME = "Ekeko Console";

    //////////////////////////
	// The plugin singleton //
	//////////////////////////
	
	// The shared instance
	private static ThesisPlugin plugin;

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ThesisPlugin getDefault() {
		return plugin;
	}

	//////////////////////////
	// Weird pluginny stuff //
	//////////////////////////
 
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initMyConsoleStream();
	}
	
	/**
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/////////////////
	// The console //
	/////////////////
	
	private MessageConsoleStream myConsoleStream;
	
	/**
	 * Initializes the plugin's console stream and caches it
	 */
	private void initMyConsoleStream() {
		// Let's use the same console as ekeko
		String name = ThesisPlugin.CONSOLE_NAME;
		// Find my console, return if its already out there
		MessageConsole myConsole;
		IConsoleManager conMan = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
	         if (name.equals(existing[i].getName())) {
	     		myConsole = (MessageConsole) existing[i];
	     		myConsoleStream = myConsole.newMessageStream();
	     		return;
	         }
		} 
		// Create a new console
		myConsole = new MessageConsole(name, null);
	    conMan.addConsoles(new IConsole[]{myConsole});
	    myConsoleStream = myConsole.newMessageStream();
	}

	/**
	 * Returns the plugin's console stream, which must be used for showing messages.
	 * @return The plugin's console stream
	 */
	public MessageConsoleStream getConsoleStream() {
		return myConsoleStream;
	}

}
