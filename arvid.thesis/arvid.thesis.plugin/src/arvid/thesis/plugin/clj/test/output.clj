(ns
  ^{:doc "Inspect the output of main/analyse-repository"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.output
  (:require 
    [arvid.thesis.plugin.clj.util :as util]
    [arvid.thesis.plugin.clj.test.main :as main]))

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

(defn pattern-string 
  "Convert all frequent change patterns in a commit to string"
  [repo-path commit]
  
  )

(defn- statement-index-from-changepath 
  [change-path]
  (let [matcher (re-matcher #":body :statements (\d+) .*" change-path)
        _ (.matches matcher)]
    (.group matcher 1)))

(comment
  
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
        stmt-no (statement-index-from-changepath  changepath)
        
        stmt (nth (.statements (.getBody method)) stmt-no)
        ]
    (inspector-jay.core/inspect method)
    stmt-no
    
    )
  
  ; Show the diff of one file..
  ; git diff 1a3ef09582f78ba0dcd21a91e23e366926fac0f4^ 1a3ef09582f78ba0dcd21a91e23e366926fac0f4 src/org/droidtv/quicksearchbox/activities/AbstractSearchActivityView.java
  )