package arvid.thesis.legacy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.ui.console.MessageConsoleStream;

import clojure.lang.Cons;
import clojure.lang.IPersistentMap;
import arvid.thesis.plugin.ClojureBridge;
import arvid.thesis.plugin.ThesisPlugin;
import changenodes.Differencer;
import changenodes.matching.MatchingException;
import changenodes.operations.Delete;
import changenodes.operations.IOperation;
import changenodes.operations.Insert;
import changenodes.operations.Move;
import changenodes.operations.Update;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import ca.pfv.spmf.algorithms.frequentpatterns.apriori_close.AlgoAprioriClose;

@SuppressWarnings("restriction")
public class FlexibleMiner {

	private class GeneralizedChange {
		private String changeType;
		private String astType;
		public GeneralizedChange(IOperation operation) {
			this.changeType = operation.getClass().getSimpleName();
			this.astType = operation.getAffectedNode().getClass().getSimpleName();
		}
		public String toString() {
			return "(" + changeType + " " + astType + ")";
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((astType == null) ? 0 : astType.hashCode());
			result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GeneralizedChange other = (GeneralizedChange) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (astType == null) {
				if (other.astType != null)
					return false;
			} else if (!astType.equals(other.astType))
				return false;
			if (changeType == null) {
				if (other.changeType != null)
					return false;
			} else if (!changeType.equals(other.changeType))
				return false;
			return true;
		}
		private FlexibleMiner getOuterType() {
			return FlexibleMiner.this;
		}
		
	}

	private class GeneralizedChanges {
		private Map<GeneralizedChange, Integer> changes;
		private Map<Integer, GeneralizedChange> changesReversed;
		private Map<ASTNode, Set<Integer>> changesByMethod;
		
		public GeneralizedChanges(Collection<IOperation> operations) {
			changes = new HashMap<GeneralizedChange, Integer>();
			changesReversed = new HashMap<Integer, GeneralizedChange>();
			changesByMethod = new HashMap<ASTNode, Set<Integer>>();
			
			for(IOperation operation : operations) {
				// Ensure the change is in our changes map
				GeneralizedChange gc = new GeneralizedChange(operation);
				
				int changeId;
				if(!changes.containsKey(gc)) {
					changeId = changes.size()+1;
					changes.put(gc, changeId);	
					changesReversed.put(changeId, gc);	
				} else {
					changeId = changes.get(gc);
				}
				
				// Add it to the changesByMethodMap
				Set<Integer> integersSet;
				
				ASTNode functionNode;
				if(operation.getAffectedNode().getNodeType() == ASTNode.METHOD_DECLARATION)
				    functionNode = operation.getAffectedNode();
				else
			        functionNode = ASTNodes.getParent(operation.getAffectedNode(), ASTNode.METHOD_DECLARATION);
				
				if(functionNode == null) 
					continue;
				
				if(changesByMethod.containsKey(functionNode)) {
					integersSet = changesByMethod.get(functionNode);
				} else {
					integersSet = new HashSet<Integer>();
					changesByMethod.put(functionNode, integersSet);
				}
				integersSet.add(changeId);
				
				// Print it
				ThesisPlugin.getDefault().getConsoleStream().println(gc.toString() + " in method " + ((MethodDeclaration)functionNode).getName());
			}
			ThesisPlugin.getDefault().getConsoleStream().println();
		}

		public String toStringDb() {
			String test = "";
			
			for (Entry<ASTNode, Set<Integer>> entry : changesByMethod.entrySet())
			{
				for (Integer value : entry.getValue())
				{
				    test += value + " ";
				}
			    test += "\n";
			}

			return test;
		}

		public Object get(Integer id) {
			return changesReversed.get(id);
		}
	}
	
	private static int MIN_SUPPORT = 0;
	private GeneralizedChanges generalizedChanges;
	private Itemsets result;

	private static ASTNode sourceToAST(ICompilationUnit source) {
		// Create parser
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// Set parser source
		parser.setSource(source);
		// Set options
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, options);
		parser.setCompilerOptions(options);
		// Run it
		return parser.createAST(null);
	}
	
	public FlexibleMiner(ICompilationUnit before, ICompilationUnit after) {
		MessageConsoleStream consoleStream = ThesisPlugin.getDefault().getConsoleStream();

        // Diff 'em
		Differencer differencer = new Differencer(sourceToAST(before), sourceToAST(after));
		try {
			differencer.difference();
		} catch (MatchingException e) {
			consoleStream.println("ERROR: Differencing failed.");
			e.printStackTrace();
			return;
		}
		
		// Show differencing results
		Collection<IOperation> operations = differencer.getOperations();
		//printOperations(operations);
		generalizedChanges = new GeneralizedChanges(operations);
	}
	
	private void printOperations(Collection<IOperation> operations) {
		MessageConsoleStream consoleStream = ThesisPlugin.getDefault().getConsoleStream();

		for(IOperation operation: operations) {
			if(operation instanceof Update) {
				Update u = (Update)operation;
				consoleStream.println("UPDATE");
				consoleStream.println("Left parent: " + u.getLeftParent());
				consoleStream.println("Right parent: " + u.getRightParent());
				consoleStream.println("Original: " + u.getOriginal());
				consoleStream.println("Affected node: " + u.getAffectedNode());
				consoleStream.println();
			}
			else if(operation instanceof Insert) {
				Insert i = (Insert)operation;
				consoleStream.println("INSERT");
				consoleStream.println("Right parent: " + i.getRightParent());
				consoleStream.println("Right node: " + i.getRightNode());
				consoleStream.println("Original: " + i.getOriginal());
				consoleStream.println("Affected node: " + i.getAffectedNode());
				consoleStream.println("Index: " + i.getIndex());
				consoleStream.println("Mandatory Nodes: " + i.mandatoryNodes());
				consoleStream.println("Property: " + i.getProperty());
				consoleStream.println();
			}
			else if(operation instanceof Delete) {
				Delete d = (Delete)operation;
				consoleStream.println("DELETE");
				consoleStream.println("Original: " + d.getOriginal());
				consoleStream.println("Original Index: " + d.getOriginalIndex());
				consoleStream.println("Index: " + d.getIndex());
				consoleStream.println("Affected Node: " + d.getAffectedNode());
				consoleStream.println();
			} else if (operation instanceof Move){
				Move m = (Move)operation;
				consoleStream.println("MOVE");
				consoleStream.println("New parent: " + m.getNewParent());
				consoleStream.println("Right node: " + m.getRightNode());
				consoleStream.println("Original: " + m.getOriginal());
				consoleStream.println("Left node: " + m.getLeftNode());
				consoleStream.println("Index: " + m.getIndex());
				consoleStream.println("Affected Node: " + m.getAffectedNode());
				consoleStream.println();
			}
		}
	}

	public FlexibleMiner(String savedContent, String currentContent) {
		// TODO Auto-generated constructor stub
	}

	public void mine() throws IOException {
		InputStream input = new ByteArrayInputStream( generalizedChanges.toStringDb().getBytes() );
		
		// Applying the Apriori algorithm
		AlgoAprioriClose apriori = new AlgoAprioriClose();
		try {
			this.result = apriori.runAlgorithm(MIN_SUPPORT, input, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void printItemsets(Itemsets itemsets) {
		// for each level (a level is a set of itemsets having the same number of items)
		List<List<Itemset>> levels = itemsets.getLevels();
		ListIterator<List<Itemset>> levelsIterator = levels.listIterator(levels.size());
		while(levelsIterator.hasPrevious()) {
			List<Itemset> kItemsets = levelsIterator.previous();
			// for each itemset
			for (Itemset itemset : kItemsets) {
				Arrays.sort(itemset.getItems());
				// print the itemset and its support
				StringBuilder sb = new StringBuilder ();
				sb.append("FREQUENT PATTERN (support ");
				sb.append(itemset.getAbsoluteSupport());
				sb.append(")\n");
				for(int i=0; i < itemset.size(); i++){
					sb.append("* ");
					sb.append(generalizedChanges.get(itemset.get(i)).toString());
					sb.append('\n');
				}
				ThesisPlugin.getDefault().getConsoleStream().println(sb.toString());
			}
		}
	}

	public void printResults() {
		// Print results
		//Activator.getDefault().getConsoleStream().println("MINING: [\n" + generalizedChanges.toStringDb() + "]\n");
		printItemsets(this.result);
	}
}
