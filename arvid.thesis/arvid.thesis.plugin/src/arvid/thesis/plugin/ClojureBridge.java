package arvid.thesis.plugin;

import org.osgi.framework.Bundle;

import ccw.util.osgi.ClojureOSGi;
import ccw.util.osgi.RunnableWithException;
import clojure.lang.IFn;
import clojure.lang.IPersistentVector;
import clojure.lang.Keyword;
import clojure.lang.RT;
import clojure.lang.Symbol;

public class ClojureBridge {
	
	public static void loadClojureCode() {
		Bundle b = ThesisPlugin.getDefault().getBundle();
		
		try {
			ClojureOSGi.withBundle(b, new RunnableWithException() {
				public Object run() throws Exception {
					ClojureOSGi.require(b, "arvid.thesis.plugin.clj.main"); 	
					return null;
				}
			});
		} catch (Exception Ex) {
		    Ex.printStackTrace();
			throw new RuntimeException(Ex);
		}
	}
	
}


