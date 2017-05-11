(ns
  ^{:doc "Inspect the output of main/analyse-repository"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.output
  (:require 
    [arvid.thesis.plugin.clj.util :as util]
    [arvid.thesis.plugin.clj.test.main :as main]
    [damp.ekeko.jdt.astnode :as astnode])
  (:import
    [org.eclipse.jdt.core.dom ASTNode ChildListPropertyDescriptor MethodDeclaration CompilationUnit Statement Block Expression]))

; Find a method with a certain name within a class (also checks nested classes)
(defn find-method-named [cu method-name]
  (let [type (first (.types cu))
        methods (.getMethods type)
        
        nestedtypes (.getTypes type)
        nestedmethods (apply concat (for [nested nestedtypes] (.getMethods nested)))]
    (some
      (fn [meth] 
        (if (= method-name (.getIdentifier (.getName meth)))
          meth))
      (concat methods nestedmethods))))


(defn- statement-index-from-changepath 
  [change-path]
  (let [matcher (re-matcher #":body :statements (\d+).*" change-path)
        _ (.matches matcher)]
    (.group matcher 1)))

(defn- info-from-changepath [ast container-path change-path]
  "Given an AST of compilation unit, and a change-path in this AST, fetch the following:
   - the node being changed
   - from there, the closest parent node that is a statement
   - ..and the furtherst parent node that is a statement"
  (let [path (concat container-path change-path)
        change-node (util/follow-node-path ast path)
        
        [furthest-stmt furthest-stmt-idx]
        (loop [cur-node ast
               cur-path path]
          (if (empty? cur-path)
            nil
            (let [[property idx] (first cur-path)
                  child-tmp (astnode/value-unwrapped 
                              (astnode/ekeko-keyword-for-property-descriptor cur-node property))
                  child (if (nil? idx)
                          child-tmp
                          (nth child-tmp idx))]
              (if (instance? Statement child)
                [child idx]
                (recur child (rest cur-path))))))
        
        closest-stmt
        (loop [cur-node change-node]
          (if (or (nil? cur-node) (instance? Statement cur-node))
            cur-node
            (recur (astnode/owner cur-node))))]
    [change-path change-node furthest-stmt closest-stmt furthest-stmt-idx]))

(defn show-systematic-instance [repo-path commit pattern instance output-dir]
  (let [repo-name (main/repo-name-from-path repo-path)
        container-pkg (:container-package instance)
        container-type (:container-type instance)
        container-path (util/parse-node-path (:container-path instance))
        container-desc (:container-description instance)
        
        
;        method-split (clojure.string/split method #"\.")
;        method-name (last method-split)
;        cls-path (clojure.string/join #"/" (butlast method-split))
        
        file-path (str "src/" (clojure.string/replace container-pkg #"\." "/") "/" container-type ".java")

        source (util/get-file-in-commit (str repo-path "/../") file-path commit) ; Code after commit
        prev-source (util/get-file-in-commit (str repo-path "/../") file-path (str commit "^")) ; Code before commit
        
        diff (util/get-file-diff (str repo-path "/..") file-path commit)
        
        ast (util/source-to-ast source)
        prev-ast (util/source-to-ast prev-source)
        
        container (util/follow-node-path ast container-path)
        
;        method-node (find-method-named ast method-name)
        
        output-file (str output-dir "/sysedit-" container-desc  ".txt")
        
        stmtno-to-change-info
        (loop [changepaths (vals (:change-paths pattern))
               info-map {}] ; Maps stmt number to change-paths in that stmt
          (if (empty? changepaths)
            info-map
            
            (let [changepath (util/parse-node-path (first changepaths))
                  info (info-from-changepath ast container-path changepath)
                  furthest-stmt-idx (nth info 4)
                  cur-vals (get info-map furthest-stmt-idx)]
              (recur 
                (rest changepaths)
                (assoc info-map furthest-stmt-idx
                       (conj cur-vals changepath))))
            
            ))]
    (util/append output-file container-desc)
    (doseq [stmt-no (sort (keys stmtno-to-change-info))]
      (let [info-list (get stmtno-to-change-info stmt-no)
            [_ _ furthest-stmt _ furthest-stmt-idx] (first info-list)]
        (util/append output-file (str "---------------------"))
        (doseq [[change-path change-node furthest-stmt closest-stmt furthest-stmt-idx] info-list]
          (util/append output-file "--ChangePath:")
          (util/append output-file change-path)
          (util/append output-file "--Node:")
          (util/append output-file (.toString change-node)))
        (util/append output-file "--Stmt:")
        (util/append output-file (str furthest-stmt-idx "---" (.toString furthest-stmt)))
        (util/append output-file "")
        (util/append output-file "")
        (util/append output-file diff)))))

(defn show-systematic-edit 
  "Pretty-print a systematic edit to a text file. The sys. edit is indicated by its support level and an index number."
  [repo-path support pattern-no]
  (let [repo-name (main/repo-name-from-path repo-path)
        _ (println repo-name)
        output-dir (str main/output-dir "/" repo-name "/" repo-name "-" support "-" pattern-no)
        supmap (main/repo-support-map repo-name)
        pattern (nth (:patterns (get supmap support)) pattern-no)
        commit (:commit pattern)
        tmp (println "Commit: " commit)]
    ; If this sys. edit was already outputted, remove the old version
    (if (.exists (clojure.java.io/as-file output-dir))
      (util/delete-recursively output-dir))
    (do
      (.mkdir (java.io.File. output-dir))
      (println "****** " commit)
      (doseq [instance (:instances pattern)]
        (show-systematic-instance repo-path commit pattern instance output-dir)))))

(defn support-list [repo-name]
  (let [supmap (main/repo-support-map repo-name)]
    (keys supmap)))

(defn generate-sample-systematic-edits 
  "Export a few systematic edits for each available support value of a repository
   More specifically, for each support level, we export 'sample-size' randomly chosen systematic edits."
  [repo-path sample-size]
  (let [repo-name (main/repo-name-from-path repo-path)
        supmap (main/repo-support-map repo-name)]
    (doseq [support (keys supmap)]
      (let [pattern-count (count (:patterns (get supmap support)))]
        (doseq [x (range 0 sample-size)]
          (show-systematic-edit repo-path support (rand-int pattern-count)))))))



(comment
  (generate-sample-systematic-edits "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/./common/app/xtv/.git" 2)
  (show-systematic-edit "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/./common/app/gesturecontrol/.git" 4 2)
  (show-systematic-edit "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/./common/app/quicksearchbar/.git" 3 0)

  
  (let [repo-path "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/./common/app/quicksearchbar/.git"
        supmap (main/repo-support-map "quicksearchbar")
        pattern (first (:patterns (get supmap 3)))
        commit (:commit pattern)
        instance (first (:instances pattern))
        method (:container-method instance)
        method-split (clojure.string/split method #"\.")
        method-name (last method-split)
        cls-path (clojure.string/join #"/" (butlast method-split))
        file-path (str repo-path "/../src/" cls-path ".java")
        ast (util/source-to-ast (slurp file-path))
        method (find-method-named ast method-name)
        
        changepath (first (vals (:change-paths instance)))
        stmt-no (Integer/parseInt (statement-index-from-changepath  changepath))
        
        tmp (println (.statements (.getBody method)))
        stmt (.get (.statements (.getBody method)) stmt-no)
        ]
    stmt-no)
  
  ; Show the diff of one file..
  ; git diff 1a3ef09582f78ba0dcd21a91e23e366926fac0f4^ 1a3ef09582f78ba0dcd21a91e23e366926fac0f4 src/org/droidtv/quicksearchbox/activities/AbstractSearchActivityView.java
  )