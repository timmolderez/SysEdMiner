package arvid.thesis.plugin.gui;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import arvid.thesis.plugin.ClojureBridge;
import arvid.thesis.plugin.git.ChangeHistory;
import arvid.thesis.plugin.git.Commit;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.Keyword;

public class MiningDetailsDialog extends Dialog {
	
	// Variables containing possible options
	private final IPersistentVector generalizers;
	private final IPersistentVector groupers;
	private ChangeHistory history;
	// Variables containing entered data
	private Commit selectedCommit;
	private IPersistentMap selectedGeneralizer;
	private IPersistentMap selectedGrouper;
	private int minSupport;
	
	//////////////////////////////
	// Constructor & Public API //
	//////////////////////////////
	
	public MiningDetailsDialog(Shell parentShell, ChangeHistory history) {
		super(parentShell);
		// Save passed arguments in this object
		this.generalizers = ClojureBridge.getGeneralizerDefinitions();
		this.groupers = ClojureBridge.getGrouperDefinitions();
		this.history = history;
		// Set default values
		this.selectedGeneralizer = (generalizers.length() > 1 ? (IPersistentMap)generalizers.nth(0) : null);
		this.selectedGrouper = (groupers.length() > 0 ? (IPersistentMap)groupers.nth(1) : null);
		this.selectedCommit = (history.getCommits().size() > 0 ? history.getCommits().get(0) : null);
		this.minSupport = 2;
	}

	/**
	 * Get the selected generalizer.
	 */
	public IPersistentMap getSelectedGeneralizerDefinition() {
		return this.selectedGeneralizer;
	}

	/**
	 * Get the selected grouper.
	 */
	public IPersistentMap getSelectedGrouperDefintion() {
		return this.selectedGrouper;
	}

	/**
	 * Get the specified minimum support.
	 */
	public int getMinSupport() {
		return this.minSupport;
	}

	/**
	 * Get the commit specified to use for mining.
	 */
	public Commit getCommit() {
		return this.selectedCommit;
	}
	
    ////////////////////////////////////////////////
	// Appearance and area component construction //
    ////////////////////////////////////////////////
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Specify details"); // Sets title bar
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
        // Create the container
	    Composite container = new Composite(area, SWT.NONE);
	    container.setLayoutData(new GridData(GridData.FILL_BOTH));
	    container.setLayout(new GridLayout(1, false));
	    // Add controls
	    createCommitDropdown(container);
	    createGeneralizerDropdown(container);
	    createGrouperDropdown(container);
	    createMinSupportInput(container);
		// Ready ;)
		return container;
	}
		
	protected void createCommitDropdown(Composite container) {
		List<Commit> commits = this.history.getCommits();
		// Get string array of commit descriptions
	    String[] items = new String[commits.size()];
	    String selectedItem = null;
	    for(int i = 0; i < commits.size(); i++) {
	    	items[i] = commits.get(i).getMessage();
	    	// Is it selected?
	    	if(commits.get(i).equals(this.selectedCommit)) {
	    		selectedItem = items[i];
	    	}
	    }
	    // Create the dropdown
		createDropdown(container, "Commit:", items, selectedItem, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = ((Combo)e.getSource()).getSelectionIndex();
				selectedCommit  = commits.get(index);
			}
		});
	}

	protected void createGeneralizerDropdown(Composite container) {
		// Get string array of generalizer names, in order of occurrence in the underlying IPersistentVector
	    String[] items = new String[this.generalizers.length()];
	    String selectedItem = null;
	    for(int i = 0; i < this.generalizers.length(); i++) {
	    	// Convert item in generalizers
	    	IPersistentMap generalizer = (IPersistentMap) this.generalizers.nth(i);
	    	Object generalizerName = generalizer.entryAt(Keyword.intern("name")).getValue();
	    	items[i] = generalizerName.toString();
	    	// Is it selected?
	    	if(generalizer.equals(this.selectedGeneralizer)) {
	    		selectedItem = items[i];
	    	}
	    }
	    // Create the dropdown
		createDropdown(container, "Generalizer:", items, selectedItem, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = ((Combo)e.getSource()).getSelectionIndex();
				selectedGeneralizer  = (IPersistentMap)generalizers.nth(index);
			}
		});
	}

	protected void createGrouperDropdown(Composite container) {
		// Get string array of grouper names, in order of occurrence in the underlying IPersistentVector
	    String[] items = new String[this.groupers.length()];
	    String selectedItem = null;
	    for(int i = 0; i < this.groupers.length(); i++) {
	    	// Convert item in groupers
	    	IPersistentMap grouper = (IPersistentMap) this.groupers.nth(i);
	    	Object generalizerName = grouper.entryAt(Keyword.intern("name")).getValue();
	    	items[i] = generalizerName.toString();
	    	// Is it selected?
	    	if(grouper.equals(this.selectedGrouper)) {
	    		selectedItem = items[i];
	    	}
	    }
	    // Create the dropdown
		createDropdown(container, "Grouper:", items, selectedItem, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = ((Combo)e.getSource()).getSelectionIndex();
				selectedGrouper  = (IPersistentMap)groupers.nth(index);
			}
		});
	}
	
	protected void createMinSupportInput(Composite container) {
		// Create label
	    Label label = new Label(container, SWT.NONE);
	    label.setText("Minimum support:");
	    // Create dropdown
	    Text text = new Text(container, SWT.SINGLE | SWT.BORDER);
	    text.setLayoutData(this.getFillGridData());
	    text.setText(Integer.toString(this.minSupport)); // Set initial value
	    text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Integer value;
				// Ensure its a floating point number within interval [0..1]
	            try {
		            value = new Integer(text.getText());
		            if(value < 0 || value > 100) {
		            	throw new Exception();
		            }
		        } catch (Exception ex) {
		            value = -1;
		        }
	            minSupport = value;
			}
	      });
	}
	
	protected void createDropdown(Composite container, String labelText, String[] items, String selectedItem, SelectionAdapter selectionListener) {
		// Create label
	    Label label = new Label(container, SWT.NONE);
	    label.setText(labelText);
	    // Create dropdown
	    Combo dropdown = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		dropdown.setLayoutData(this.getFillGridData());
		dropdown.setItems(items);
		if(selectedItem != null) {
			dropdown.setText(selectedItem);
		}
		dropdown.addSelectionListener(selectionListener); 
	}
	
	private GridData getFillGridData() {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
	    return gridData;
	}
	
	////////////////////
	// Event handling //
	////////////////////
	
	@Override
	protected void okPressed() {
		// Cancel form submission if no choice was made in the dropdown
		if(this.selectedCommit != null
		   && this.selectedGeneralizer != null
		   && this.selectedGrouper != null
		   && this.minSupport != -1)
			super.okPressed();
	}
	
} 