(ns 
  ^{:doc "Helper functions to run the Frequent change pattern minor on entire git repositories, and inspect the results"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.main
  (:require 
    [arvid.thesis.plugin.clj.git.repository :as repo]
    [arvid.thesis.plugin.clj.strategies.strategyFactory :as stratfac]
    [arvid.thesis.plugin.clj.main :as main]
    [arvid.thesis.plugin.clj.preprocess.grouping.group :as group]
    [clojure.java.shell :as sh]
    [qwalkeko.clj.functionalnodes :as qwal]
    [arvid.thesis.plugin.clj.util :as util]
    [arvid.thesis.plugin.clj.changenodes.change :as change]
    [arvid.thesis.plugin.clj.changenodes.changes :as changes]
    [qwalkeko.clj.functionalnodes :as functionalnodes]
    [damp.ekeko.jdt.astnode :as astnode])
  (:import
    [org.eclipse.jdt.core.dom ASTNode ChildListPropertyDescriptor MethodDeclaration Block Expression]))

(defn commit-id
  "Get the commit ID of a JGit object"
  [commit]
  (second (clojure.string/split 
            (.toString (:jgit-commit commit)) 
            #" ")))

(def output-dir "/Users/soft/desktop/tpv-freqchanges/")
(def COMMIT-TIMEOUT 300000)

;(def output-dir "/Users/soft/desktop/calcul-ipp-study/")

(defn append
  "Appends a line of text to a file"
  [filepath text]
  (spit filepath (str text "\n") :append true :create true))

(defn remove-redundant-changes 
  "Removes any changes that aren't very interesting to consider when looking for systematic changes
   As ChangeNodes produces a change for every single AST node that is produced, we're often only interested in the root node
   when modifying an entire tree. For example, when adding a statement return 4; , the change that inserts the 4 isn't all that interesting.

  In general, all nodes that are more fine-grained than statements are removed, unless they are the root of a modification.
  (All roots are definitely kept to include any fine-grained/below-statement-level systematic changes..)"
  
  [changes]
  (let [dependencies (functionalnodes/create-dependency-graph {:changes changes})]
    (remove 
      (fn [change]
        (let [original (:original change) ; Is nil for nested changes
              copy (:copy change)] 
          (and (nil? original) (instance? Expression copy))))
      changes)))

(defn make-strategy [container]
  (stratfac/make-strategy 
    container
    #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?})
  )

(defn add-suffix [path suffix]
  "Add a suffix to a filename, assuming the file has an extension"
  (let [split (clojure.string/split path #"\.")
        fname (str (nth split (- (count split ) 2)) suffix)]
    (clojure.string/join "."
      (assoc split (- (count split ) 2) fname))))

(defn write-results [commit changes patterns results-path]
  (if (not (empty? (:patterns-list  patterns)))
      (do 
        (append results-path (str "CommitID:" (.toString (:jgit-commit commit))))
        (append results-path (str "CommitMsg:" (:message commit)))
        (append results-path " ")
        ; For each pattern
        (doseq [pattern (:patterns-list patterns)]
          (append results-path (str "Support:" (:support pattern)))
          ; Write change types (insert, delete, ..)
          (let [inst-changes (:changes (:group (:gengroup (first (:instances pattern)))))
                change-types (clojure.string/join " " (for [change inst-changes] (name (:operation change))))]
            (append results-path (str "ChangeTypes:" change-types))
            (append results-path " "))
          ; For each instance of the pattern
          (doseq [instance (:instances pattern)]
            (let [gengroup (:gengroup instance)
                  group (:group gengroup)
                  container (:container gengroup)]
              (append results-path (str "ContainerMethod:"
                                        (.getName (.getPackage (.getRoot container))) "."
                                        (.getName (first (.types (.getRoot container)))) "."
                                        (.getName container)))
              (append results-path (str "ChangeIDs:" 
                                        (clojure.string/join 
                                          " "
                                          (for [change (:changes group)]
                                            (.indexOf changes change)))))
              (doseq [change (:changes group)]
                (append results-path (str "ChangePath-" (.indexOf changes change) ":"
                                          (clojure.string/join 
                                            " "
                                            (for [element (group/get-path-of-change group change)]
                                              (.toString element))))))
              (append results-path (str "ChangeNodeTypes:" 
                                        (clojure.string/join 
                                          " "
                                          (for [change (:changes group)]
                                            (.getName (.getClass (:copy change)))))))
              (append results-path "------")
              ))
          (append results-path "======")
          ))))

(defn mine-commit
  "Look for frequent change patterns in a single commit
   @param commit        JGit Commit instance
   @param strategy      Determines how to group changes, and the equality relation between two changes
   @param verbosity     Console output verbosity level [0-3]
   @param results-path  Path to results file (created if it doesn't exist)
   @return              Pair containing all distilled changes + all frequent patterns"
  ([commit strategy min-support verbosity results-path]
    (mine-commit commit strategy min-support verbosity results-path (fn [filename] true)))
  ([commit strategy min-support verbosity results-path file-filter]
    (let [timing-path (str (clojure.string/join 
                            "/" 
                            (butlast (clojure.string/split results-path #"/"))) 
                           "/timing.txt")
        start-time (. System (nanoTime))
        changes (main/get-changes-in-commit commit file-filter verbosity)
        _ (append timing-path (util/time-elapsed start-time))
        
        start-time2 (. System (nanoTime))
        patterns (main/mine-changes changes strategy min-support verbosity)
        tmp (inspector-jay.core/inspect patterns)
        _2 (append (add-suffix timing-path "-m") (util/time-elapsed start-time2))
        
        start-time3 (. System (nanoTime))
        patterns-cu (main/mine-changes changes (make-strategy :CompilationUnit) min-support verbosity)
        _3 (append (add-suffix timing-path "-cu") (util/time-elapsed start-time3))
        
        start-time4 (. System (nanoTime))
        patterns-stmt (main/mine-changes changes (make-strategy :Statement) min-support verbosity)
        _4 (append (add-suffix timing-path "-stmt") (util/time-elapsed start-time4))
        ]
    ; Update results file if any patterns are found
    (write-results commit changes patterns results-path)
    (write-results commit changes patterns-cu (add-suffix results-path "-cu"))
    (write-results commit changes patterns-stmt (add-suffix results-path "-stmt"))
    [changes patterns])))

(defn mine-source-change
  "Look for frequent change patterns by comparing two pieces of source code 
   (variant of mine-commit)
   @param before-code   source code (String) before it was changed
   @param after-code    source code after it was changed
   @param strategy      Determines how to group changes, and the equality relation between two changes
   @param verbosity     Console output verbosity level [0-3]
   @param results-path  Path to results file (created if it doesn't exist)
   @return              Pair containing all distilled changes + all frequent patterns"
  ([before-code after-code strategy min-support verbosity results-path]
    (mine-source-change before-code after-code strategy min-support verbosity results-path (fn [filename] true)))
  ([before-code after-code strategy min-support verbosity results-path file-filter]
  (let [before-ast (util/source-to-ast before-code)
        after-ast (util/source-to-ast after-code)
        changes (changes/get-all before-ast after-ast)
        patterns (main/mine-changes changes strategy min-support verbosity)]
    ; Update results file if any patterns are found
    (if (not (empty? (:patterns-list  patterns)))
      (do 
        (append results-path " ")
        ; For each pattern
        (inspector-jay.core/inspect patterns)
        (doseq [pattern (:patterns-list patterns)]
          (append results-path (str "Support:" (:support pattern)))
          ; Write change types (insert, delete, ..)
          (let [inst-changes (:changes (:group (:gengroup (first (:instances pattern)))))
                change-types (clojure.string/join " " (for [change inst-changes] (name (:operation change))))]
            (append results-path (str "ChangeTypes:" change-types))
            (append results-path " "))
          ; For each instance of the pattern
          (doseq [instance (:instances pattern)]
            (let [gengroup (:gengroup instance)
                  group (:group gengroup)
                  container (:container gengroup)]
              (append results-path (str "ContainerMethod:"
                                        (.getName (.getPackage (.getRoot container))) "."
                                        (.getName (first (.types (.getRoot container)))) "."
                                        (.getName container)))
              (append results-path (str "ChangeIDs:" 
                                        (clojure.string/join 
                                          " "
                                          (for [change (:changes group)]
                                            (.indexOf changes change)))))
              (doseq [change (:changes group)]
                (append results-path (str "ChangePath-" (.indexOf changes change) ":"
                                          (clojure.string/join 
                                            " "
                                            (for [element (group/get-path-of-change group change)]
                                              (.toString element))))))
              (append results-path (str "ChangeNodeTypes:" 
                                        (clojure.string/join 
                                          " "
                                          (for [change (:changes group)]
                                            (.getName (.getClass (:copy change)))))))
              (append results-path "------")
              ))
          (append results-path "======")
          )))
    [changes patterns])))

(defn find-commit-by-id [repo-path commit-id]
  (let [all-commits (repo/get-commits repo-path)
        commit (some
                 (fn [commit]
                   (if (.contains (.toString (:jgit-commit commit)) commit-id)
                     commit))
                 all-commits)]
    commit))

(defn get-changes-by-commit-id [repo-path commit-id]
  (main/get-changes-in-commit 
    (find-commit-by-id repo-path commit-id) 
    (fn [f] true) 1))

(defn repo-name-from-path
  "Extract the git repository name from the path to its .git directory"
  [repo-path]
  (let [split-path (clojure.string/split repo-path #"/")]
    (nth split-path (- (count split-path) 2))))

(defn analyse-commits
  "Look for change patterns in multiple commits
   @param repo-path     File path to the .git directory of a repository
   @param results-path  Path to results file (created if it doesn't exist)
   @param strategy      Preprocessing strategy (see strategyFactory.clj)
   @param start-idx     Start the analysis at the given index, and continue until the last commit"
  [repo-path results-path strategy start-idx]
  (let [repo-name (repo-name-from-path repo-path)
        all-commits (repo/get-commits repo-path)
        commit-no (count all-commits)
        commits (take-last (- commit-no start-idx) all-commits)
        min-support 3
        verbosity 1]
    (map-indexed 
      (fn [idx commit]
        (println "Processing commit" (+ start-idx idx) "/" commit-no "(" repo-name ")")
        (try 
          (util/with-timeout 
            COMMIT-TIMEOUT
            (mine-commit commit strategy min-support verbosity results-path))
          (catch Exception e
            (do
              (println "!! Failed to process commit" (+ start-idx idx) "/" commit-no)
              (.printStackTrace e)))))
      commits)))

(defn analyse-repository
  "Look for change patterns across all commits in a repository"
  [repo-path strategy]
  (let [all-commits (repo/get-commits repo-path)
        commit-no (count all-commits) 
        repo-name (repo-name-from-path repo-path)
        pattern-folder (str output-dir repo-name "/")
        batch-size 200]
    (.mkdir (java.io.File. pattern-folder))
    (dotimes [i (Math/ceil (/ commit-no batch-size))]
      (let [start-commit (* i batch-size)]
        (doall (analyse-commits repo-path (str pattern-folder "patterns-" i ".txt")
                               strategy start-commit))))))

(defn open-commit
  "Retrieve the diff of a commit, store it to file, and open it in a text editor"
  [repo-path commit]
  (let [diff (:out (sh/sh "git" "show" commit "-U100" :dir (str repo-path "/..")))
        file-path (str output-dir "commits/" commit ".diff")]
    (spit file-path diff)
    (sh/sh "open" "-a" "/Applications/Sublime Text 2.app" (str commit ".diff") :dir (str output-dir "commits/"))))

(defn open-class-in-commit
  "Retrieve the diff of a commit, store it to file, and open it in a text editor"
  [repo-path commit class-name]
  (let [short-name (last (clojure.string/split class-name #"\."))
        class-file (str "src/"
                        (clojure.string/replace class-name #"\." "/")
                        ".java")
        
        
        file-contents (:out (sh/sh "git" "show" (str commit ":" class-file) :dir (str repo-path "/..")))
        file-path (str output-dir "files/" short-name ".java")]
    (spit file-path file-contents)
    (sh/sh "open" "-a" "/Applications/Sublime Text 2.app" (str short-name ".java") :dir (str output-dir "files/"))))

;(open-class-in-commit git-path "bfead7403d778a662a1fd4dda85067cafce00a01" "org.droidtv.epg.bcepg.epgui.NowNextOverview")

(defn build-support-map [init-support-map results-path]
  "Produce a support map, which maps each support level to
   various information regarding all frequent patterns that have this support level.
   @param init-support-map  The produced support map is merged with this one.
   @param results-path      Path to a file produced by analyse-commits"
  (with-open [rdr (clojure.java.io/reader results-path)]
    (let [lines (line-seq rdr)]
      (loop [line (first lines) 
             rest-lines (rest lines) 
             support-map init-support-map
             pattern {}
             instance {}
             support 0
             commit nil]
        (if (empty? rest-lines)
          support-map
          (cond
            ; Update the current commit ID
            (.startsWith line "CommitID")
            (let [new-commit (second (clojure.string/split line #" "))]
              (recur (first rest-lines) (rest rest-lines) support-map pattern instance support new-commit))
            
            (.startsWith line "Support")
            (let [supp (java.lang.Integer/parseInt (second (clojure.string/split line #":")))
                  length (count (clojure.string/split (first rest-lines) #" ")) ; Count number of entries in the ChangeTypes: line
                  cur-val (if (nil? (get support-map supp))
                            {:commits #{} :count 0 :avg-length 0 :max-length 0 :patterns []}
                            (get support-map supp))
                  cnt (:count cur-val)
                  new-val {:commits (conj (:commits cur-val) commit)
                           :count (inc cnt)
                           :avg-length (if (= cnt 0)
                                         length
                                         (+
                                           (* (/ (dec cnt) cnt) (:avg-length cur-val))
                                           (* (/ 1 cnt) length)))
                           :max-length (if (> length (:max-length cur-val))
                                         length
                                         (:max-length cur-val))
                           :patterns (:patterns cur-val)}
                  ]
              (recur (first rest-lines) (rest rest-lines) (assoc support-map supp new-val) pattern instance supp commit))
            
            (.startsWith line "ChangeTypes")
            (let [change-types (clojure.string/split
                                 (second (clojure.string/split line #":")) 
                                 #" ")]
              (recur (first rest-lines) (rest rest-lines) support-map (assoc pattern :change-types change-types) instance support commit))
            
            (.startsWith line "ContainerMethod")
            (let [container-method (second (clojure.string/split line #":"))]
              (recur (first rest-lines) (rest rest-lines) support-map pattern (assoc instance :container-method container-method) support commit))
            
            (.startsWith line "ChangeIDs")
            (let [changeids (map
                              (fn [str] (java.lang.Integer/parseInt str))
                              (clojure.string/split
                                (second (clojure.string/split line #":")) 
                                #" "))]
              (recur (first rest-lines) (rest rest-lines) support-map pattern (assoc instance :change-ids changeids) support commit))
            
            (.startsWith line "ChangePath")
            (let [cur-paths (instance :change-paths)
                  matcher (re-matcher #"ChangePath-(\d+):(.*)" line)
                  _ (.matches matcher) ; Needed for .group calls (side-effect)
                  changeid (.group matcher 1)
                  path (.group matcher 2)
                  ]
              (recur (first rest-lines) (rest rest-lines) 
                     support-map pattern 
                     (assoc instance :change-paths
                            (assoc cur-paths changeid path)) 
                     support commit))
            
            ; Disabled for quicker browsing..
;            (.startsWith line "ChangeNodeTypes")
;            (let [change-node-types (clojure.string/split
;                                      (second (clojure.string/split line #":")) 
;                                      #" ")]
;              (recur (first rest-lines) (rest rest-lines) support-map pattern (assoc instance :change-node-types change-node-types) support commit))
            
            ; End of a pattern instance; flush the current instance to the current pattern
            (.startsWith line "------")
            (recur (first rest-lines) (rest rest-lines) support-map (assoc pattern :instances (conj (pattern :instances) instance)) {} support commit)
            
            ; End of a pattern; flush the current pattern to the support map
            (.startsWith line "======")
            (let [cur-patterns (:patterns (get support-map support))
                  new-patterns (conj cur-patterns (assoc pattern :commit commit))
                  new-pattern-map (assoc (get support-map support) :patterns new-patterns)]
              (recur (first rest-lines) (rest rest-lines) (assoc support-map support new-pattern-map) {} {} 0 commit))
            
            :else
            (recur (first rest-lines) (rest rest-lines) support-map pattern instance support commit)))))))

(defn repo-support-map
  "Reads all result files produced by analyze-repository"
  [repo-name]
  (let [pattern-folder (str output-dir repo-name "/")]
    (loop [i 0
           support-map {}]
      (let [pattern-file (str pattern-folder "patterns-" i ".txt")]
        (if (.exists (clojure.java.io/as-file pattern-file))
          (let [new-support-map (build-support-map support-map pattern-file)]
            (recur (inc i) new-support-map))
          support-map)))))

(defn find-root-change
  "In case a change is nested, find the root change
   , as well as the path relating the root change to the change"
  [change all-changes]
  (if (and (= (:operation change) :insert) (nil? (:original change)))
    (loop [current (:copy change)
           path []]
	   (if (nil? current)
         nil ; Shouldn't happen
         (let [current-change (first (filter (fn [change] (and (= (:copy change) current) (not (nil? (:original change))))) all-changes))]
           (if current-change
		           [current-change path]
		           (recur (damp.ekeko.jdt.astnode/owner current)
                    (let [^ASTNode parent (damp.ekeko.jdt.astnode/owner current)
                          lip (.getLocationInParent current)
                          index (if (instance? ChildListPropertyDescriptor lip)
                                  (.indexOf  ^java.util.AbstractList (.getStructuralProperty parent lip) current))]
                      (cons [lip index] path))
                    )))))
    [change []]))

(defn locate-change-in-body [change changes]
  (let [node (if (= (:operation change) :insert)
               (let [[root-change path] (find-root-change change changes)
                     copy (:copy root-change)]
                 (damp.ekeko.jdt.astnode/node-from-path copy path))
               (:original change))
        path (astnode/path-from-root node)
        stmt-index (loop [cur node]
                     (let [parent (astnode/owner cur)]
                       (if (nil? parent)
                         -1
                         (let [grandparent (astnode/owner parent)]
                           (if (and (instance? MethodDeclaration grandparent) (instance? Block parent))
                             (let [lip (.getLocationInParent cur)
                                   index (.indexOf  ^java.util.AbstractList (.getStructuralProperty parent lip) cur)]
                               index)
                             (recur (astnode/owner cur))))))
                     )]
    [stmt-index node path]
    ))

(comment
  
  (let [repo-path "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/./common/app/quicksearchbar/.git"
        repo-name (repo-name-from-path repo-path)
        commit (find-commit-by-id repo-path "db2ee203daf15c881db6f285e217eb8ff8a19e6c")]
    (mine-commit commit (stratfac/make-strategy) 3 1 "/Users/soft/desktop/tpv-freqchanges/tmp/tmp.txt")
    )
  
  (inspector-jay.core/inspect 
    (repo-support-map "quicksearchbar"))
  
  
  
  (let [repo-path "/Users/soft/Documents/Github/calcul_fast_2015/.git/"
        all-commits (repo/get-commits repo-path)]
    (doall (map-indexed
            (fn [index commit]
              (if (.startsWith (.toString (commit-id commit)) "14d63c4")
                (println index)))
            all-commits))    

;    (println (.indexOf all-commits "14d63c4"))
;    (count all-commits)
;    (for [commit all-commits]
;      (commit-id commit))
;    (inspector-jay.core/inspect (first all-commits))
    )
  
  (count (repo/get-commits "/Users/soft/Documents/Github/calcul_fast_2015/.git/"))
  
  ; Open up several commits in Sublime
  (let [repo-path "/Users/soft/Documents/Github/calcul_fast_2015/.git/"
        start 747
        all-commits (repo/get-commits repo-path)
        commits (take-last (- (count all-commits) start) all-commits)
        slice (take 10 commits)]
    (for [commit slice]
      (open-commit repo-path (commit-id commit))))
  
  (nth 117 (repo/get-commits repo-path))
  
  (open-commit "/Users/soft/Documents/Github/calcul_fast_2015/.git/" "1799ffbf023c49c75963422f3d6c62f62dfe3f83")
  
  (open-commit "/Users/soft/Documents/Github/calcul_fast_2015/.git/" "862b17092e146215203852065451a5820a15c95a")
  (open-commit "/Users/soft/Documents/Github/calcul_2015_full/.git/" "2543c4e83648b9eb12b18203f59465388e55fe64")
  (open-commit "/Users/soft/Documents/Github/calcul_2015_full/.git/" "ef28f2a")

  (let [repo-path "/Users/soft/Documents/Github/calcul_fast_2015/.git/"]
    (with-open [rdr (clojure.java.io/reader "/Users/soft/Desktop/commits.txt")]
    (let [lines (line-seq rdr)]
      (doall 
        (for [line lines]
         (open-commit repo-path line)
         )))))  
  
  ; Find patterns between two files
  (let [a (util/source-to-ast (slurp "/Users/soft/Desktop/a.txt"))
        b (util/source-to-ast (slurp "/Users/soft/Desktop/b.txt"))
        changes (changes/get-all a b)
        
        ]
    (println (changes/to-string changes)))
  
  (mine-source-change
    (slurp "/Users/soft/Desktop/c.txt")
    (slurp "/Users/soft/Desktop/d.txt")
    (stratfac/make-strategy)
    1
    0
    "/Users/soft/desktop/output.txt"
    )
  
  ; Run the distiller
  (let [a (util/source-to-ast (slurp "/Users/soft/Desktop/a.txt"))
        b (util/source-to-ast (slurp "/Users/soft/Desktop/b.txt"))
        changes (changes/get-all a b)
        deps (functionalnodes/create-dependency-graph {:changes changes})]
    (println (changes/to-string changes)))
  
  )