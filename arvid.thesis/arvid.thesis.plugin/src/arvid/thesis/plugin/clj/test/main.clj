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
    [damp.ekeko.jdt.astnode :as astnode]
    )
  (:import
    [org.eclipse.jdt.core.dom ASTNode ChildListPropertyDescriptor MethodDeclaration Block]))

(defn commit-id
  "Get the commit ID "
  [commit]
  (second (clojure.string/split 
            (.toString (:jgit-commit commit)) 
            #" ")))

(def output-dir 
  "/Users/soft/desktop/tpv-freqchanges/")

(defn append
  "Appends a line of text to a file"
  [filepath text]
  (spit filepath (str text "\n") :append true :create true))

(defn mine-commit
  "Look for frequent change patterns in a single commit
   @param commit        JGit Commit instance
   @param strategy      Determines how to group changes, and the equality relation between two changes
   @param verbosity     Console output verbosity level [0-3]
   @param results-path  Path to results file (created if it doesn't exist)
   @reutrn              Pair containing all distilled changes + all frequent patterns"
  [commit strategy min-support verbosity results-path]
  (let [file-filter (fn [filename] true)
        changes (main/get-changes-in-commit commit file-filter verbosity)
        patterns (main/mine-changes changes strategy min-support verbosity)]
    ; Update results file if any patterns are found
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
          )))
    [changes patterns]))

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
          (mine-commit commit strategy min-support verbosity results-path)
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
  [repo-path]
  (let [repo-name (repo-name-from-path repo-path)
        pattern-folder (str output-dir repo-name "/")]
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