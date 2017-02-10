(ns
  ^{:doc "Inspect the output of main/analyse-repository"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.output
  (:require 
    [arvid.thesis.plugin.clj.util :as util]
    [arvid.thesis.plugin.clj.test.main :as main]))

; Find a method with a certain name within a class (also checks nested classes)
(defn find-method-named [cu method-name]
;  (println cu)
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


;(defn show-systematic-changepath [repo-path method changepath output-dir]
;  (let [
;        output-file (str output-dir "/sysedit-" (.getName method)  ".txt")
;        stmt-no (Integer/parseInt (statement-index-from-changepath changepath))
;        stmt (.get (.statements (.getBody method)) stmt-no)
;        ]
;    (util/append output-file (str "---------------------\n" changepath "\n" (.toString stmt)))
;    )
;  )

(defn show-systematic-instance [repo-path commit instance output-dir cps]
  (let [repo-name (main/repo-name-from-path repo-path)
        method (:container-method instance)
        _ (println method)
        
        method-split (clojure.string/split method #"\.")
        method-name (last method-split)
        cls-path (clojure.string/join #"/" (butlast method-split))
        
        source (util/get-file-in-commit (str repo-path "/..") (str "src/" cls-path ".java") commit)
        
        diff (util/get-file-diff (str repo-path "/..") (str "src/" cls-path ".java") commit)
        
;        prev-source (util/get-file-in-commit (str repo-path "/..") (str "src/" cls-path ".java") (str commit "^"))
        file-path (str repo-path "/../src/" cls-path ".java")
        ast (util/source-to-ast source)
;        prev-ast (util/source-to-ast prev-source)
        method-node (find-method-named ast method-name)
        output-file (str output-dir "/sysedit-" (.getName method-node)  ".txt")
        
        stmtno-to-changes
        (loop [changepaths (vals (:change-paths instance))
               stmtno-to-changes {}] ; Maps stmt number to change-paths in that stmt
          (if (empty? changepaths)
            stmtno-to-changes
            (if (or (not (contains? cps (first changepaths))) (empty? (first changepaths)) (= ":body" (first changepaths)))
              (recur (rest changepaths) stmtno-to-changes)
              (let [changepath (first changepaths)
                  stmtno (Integer/parseInt (statement-index-from-changepath  changepath))
                  cur-paths (get stmtno-to-changes stmtno)]
              (recur 
                (rest changepaths)
                (assoc stmtno-to-changes stmtno
                       (conj cur-paths changepath))))))
          )]
    (util/append output-file method)
    (doseq [stmt-no (sort (keys stmtno-to-changes))]
      (let [stmt (.get (.statements (.getBody method-node)) stmt-no)
            changepaths (get stmtno-to-changes stmt-no)]
        (util/append output-file (str "---------------------"))
        (doseq [changepath changepaths]
          (util/append output-file changepath)
          )
        (util/append output-file (.toString stmt))
        (util/append output-file "")
        (util/append output-file "")
        (util/append output-file diff)))
    
;    (doseq [changepath (vals (:change-paths instance))]
;      (show-systematic-changepath repo-path method changepath output-dir))
    )
  )

(defn changepath-intersection [pattern]
  (loop [instances (:instances pattern)
         intersection (into #{} (vals (:change-paths (first instances))))]
    (if (empty? instances)
      intersection
      (let [instance (first instances)
            changepaths (into #{} (vals (:change-paths instance)))]
        (recur (rest instances) (clojure.set/intersection intersection changepaths))))))

(defn show-systematic-edit [repo-path support pattern-no]
  (let [repo-name (main/repo-name-from-path repo-path)
        _ (println repo-name)
        output-dir (str main/output-dir "/" repo-name "/" repo-name "-" support "-" pattern-no)
        supmap (main/repo-support-map repo-name)
        pattern (nth (:patterns (get supmap support)) pattern-no)
        commit (:commit pattern)
        
        
        
        inter (changepath-intersection pattern) ]
    (if (.exists (clojure.java.io/as-file output-dir))
      (println support "-" pattern-no " already exists!")
      (do 
        (.mkdir (java.io.File. output-dir))
        (println "****** " commit)
        (doseq [instance (:instances pattern)]
;          (show-systematic-instance repo-path commit instance output-dir)
          (try 
            (show-systematic-instance repo-path commit instance output-dir inter)
            (catch Exception e (println "!!!" (.getMessage e))))
          
          ))
      )))

(defn support-list [repo-name]
  (let [supmap (main/repo-support-map repo-name)]
    (keys supmap)))

(defn generate-sample-systematic-edits [repo-path sample-size]
  "Export a few systematic edits for each available support value of a repository"
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
        tmp (println changepath)
        stmt-no (Integer/parseInt (statement-index-from-changepath  changepath))
        
        tmp (println (.statements (.getBody method)))
        stmt (.get (.statements (.getBody method)) stmt-no)
        ]
;    (inspector-jay.core/inspect method)
    stmt-no
    
    )
  
  ; Show the diff of one file..
  ; git diff 1a3ef09582f78ba0dcd21a91e23e366926fac0f4^ 1a3ef09582f78ba0dcd21a91e23e366926fac0f4 src/org/droidtv/quicksearchbox/activities/AbstractSearchActivityView.java
  )