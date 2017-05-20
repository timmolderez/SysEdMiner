![SysEdMiner logo](https://raw.githubusercontent.com/timmolderez/SysEdMiner/master/documents/SysEdMiner.png)

Systematic Edit Miner is an Eclipse plugin that finds "systematic edits", groups of similar code changes, in Java projects.
There are several different scenarios that involve making several similar changes: 
making changes to an API, migrating to a different library, refactoring, fixing multiple occurrences of a bug, or performing routine code maintenance tasks.

Because systematic edits can be tedious and error-prone to perform manually, being able to find them in existing projects can be valuable to:
- Find error-prone areas in the source code
- Make informed refactoring decisions
- Generate transformations/scripts to automate a systematic edit

For more information on how SysEdMiner works, have a look at our [MSR 2017 paper](http://soft.vub.ac.be/Publications/2017/vub-soft-tr-17-04.pdf).

### Installation

- If you haven't already, first install the [Eclipse](http://www.eclipse.org/) IDE. Note that the chosen Eclipse package should include the Eclipse Plugin Development Environment. (This is the case for e.g. "Eclipse IDE for Java EE Developers" and "Eclipse for RCP and RAP Developers")
- Install the [Counterclockwise](http://doc.ccw-ide.org/) Eclipse plugin, which adds support for the Clojure language. Counterclockwise can be found in the Eclipse marketplace. (Help > Eclipse Marketplace...)
- Clone the Github repositories of SysEdminer's dependencies to your computer: [Ekeko](https://github.com/cderoove/damp.ekeko), [ChangeNodes](https://github.com/ReinoutStevens/ChangeNodes) and [Qwalkeko](https://github.com/ReinoutStevens/damp.qwalkeko)
- From these cloned repositories, import the following projects into Eclipse:
  - damp.ekeko.feature
  - damp.ekeko.plugin
  - damp.ekeko.plugin.test
  - damp.libs
  - damp.changenodes.plugin
  - damp.qwalkeko.feature
  - damp.qwalkeko.plugin
- Clone the SysEdMiner repository, and import its projects into Eclipse.

### Running SysEdMiner

- To load the SysEdMiner plugin in a separate Eclipse instance, right-click the damp.ekeko.plugin project > Run As > Eclipse Application. Before starting, it may be necessary to change the run configuration: (Right-click the project > Run As > Run Configurations...) In the Main tab, check the "Location:" textbox. In the Arguments tab, check the "Working directory:" and the "VM arguments:".
- Once the new Eclipse instance has launched, SysEdMiner can be used via a Clojure REPL. To open a REPL, go to the Ekeko menu > Start nREPL. Now go to the other Eclipse instance (from which SysEdMiner was launched) and check its console. It should show a link to the new REPL. Click this link to open the REPL.
- Open the arvid.thesis.plugin.clj.test.main.clj file (in the arvid.thesis.plugin.clj project). Load it into the REPL via the Clojure menu > Load file in REPL.
- Enter the following to analyse each commit of a git repository: ```(analyse-repository "/path/to/a/cloned/git-repository/.git" (stratfac/make-strategy))```

### Acknowledgements

We would like to thank Arvid De Meyer for his contributions to the initial prototype of the SysEdMiner tool in the context of his master’s thesis.
