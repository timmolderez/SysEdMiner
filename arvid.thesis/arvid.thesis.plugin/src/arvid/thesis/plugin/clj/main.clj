;helpers/equalities/context-equals-subtree?
;* correct processing of inserts?
;* kleuren in group/update-changes-by-node: nu op original, iets complexer?
;
;strategyFactory/equals-context-path-absolute?
;=> Welke indices in pad opnemen (echte of "virtuele")
;
;strategyFactory/templatestuff
;* updater-1: :equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?
;* updater-2: :equals-operation-fully? :equals-subject-structurally? :equals-context-structurally?
;
;Coen's exapus thing proberen
;* todo

(ns arvid.thesis.plugin.clj.main
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy])
  (:require [arvid.thesis.plugin.clj.strategies.strategyFactory :as strategyFactory])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.grouping :as grouping])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.templates :as templates])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.equalities :as equalities])
  ; Phase 1: GIT
  (:require [arvid.thesis.plugin.clj.git.changedFile :as changed-file])
  (:require [arvid.thesis.plugin.clj.git.commit :as commit])
  (:require [arvid.thesis.plugin.clj.git.repository :as repository])
  ; Phase 2: Differencing
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change])
  (:require [arvid.thesis.plugin.clj.changenodes.changes :as changes])
  ; Phase 3: preprocessing (grouping and generalization)
  (:require [arvid.thesis.plugin.clj.preprocess.grouping.group :as group])
  (:require [arvid.thesis.plugin.clj.preprocess.grouping.groups :as groups])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.genchange :as genchange])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroups :as gengroups])
  (:require [arvid.thesis.plugin.clj.preprocess.preprocess :as preprocess])
  ; Phase 4: mining
  (:require [arvid.thesis.plugin.clj.mining.instance :as instance])
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern])
  (:require [arvid.thesis.plugin.clj.mining.patterns :as patterns])
  (:require [arvid.thesis.plugin.clj.mining.mining :as mining])
  ; Phase 5: postprocessing (template creation)
  (:require [arvid.thesis.plugin.clj.postprocess.lhs :as lhs])
  (:require [arvid.thesis.plugin.clj.postprocess.postprocess :as postprocess]))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

; Some low-level stuff
;;;;;;;;;;;;;;;;;;;;;;

(defn
  mine
  [gengroups min-support verbosity]
  (let [spmf-db (mining/make-spmf-db gengroups)]
    (when (>= verbosity 2) 
      (println "MINING DATABASE") 
      (println spmf-db "\n"))
    (let [spmf-result (mining/spmf-mine spmf-db min-support)]
	    (when (>= verbosity 2)
	          (println "MINING RESULT")
	          (println (mining/spmf-result-to-string spmf-result))
            (println))
      (let [patterns (mining/make-patterns-from-spmf-result gengroups spmf-result)]
	      (when (> verbosity 0) 
	        (println "PATTERNS")
          (println (patterns/to-string patterns))
          (println))
	      patterns))))
    
(defn
  preprocess
  [strategy changes verbosity]
  (when (>= verbosity 2) (println "BUILDING DATABASE..."))
  (let [gengroups (preprocess/preprocess strategy changes)]
    (when (>= verbosity 2) (println (gengroups/to-string gengroups)))
    gengroups))

(defn
  get-changes-in-commit
  ([commit verbosity]
    (get-changes-in-commit commit (fn [filename] true) verbosity))
  ([commit file-filter verbosity]
   ; Print some generic information
   (when (>= verbosity 1)
     (println "==================================================")
     (println "MINING FOR FREQUENT CHANGE PATTERNS...")
     (println "* Commit: " (commit/get-message commit))
     (println "--------------------------------------------------")
     (println))
   (when (>= verbosity 2) (println "RUNNING CHANGENODES..."))
   (when (= verbosity 2) (println))
   ; Return files
   (let [changes (commit/get-changes commit file-filter)]
     ; Visualisation in verbose mode
	   (when (= verbosity 3) (println (changes/to-string changes)))
	   ; Return chagnes
     changes)))

; High-level helpers
;;;;;;;;;;;;;;;;;;;;

(defn
  mine-changes
  [changes strategy min-support verbosity]
  (try 
    (let [gengroups (preprocess strategy changes verbosity)]
		 	(mine gengroups min-support verbosity))
    (catch Exception e (do (println "* EXCEPTION:" e)
                           (println (clojure.stacktrace/print-stack-trace e))
                           []))))

(defn
  mine-commit
  ([commit strategy min-support verbosity]
  	(mine-commit commit (fn [filename] true) strategy min-support verbosity))
  ([commit file-filter strategy min-support verbosity]
  	(let [changes (get-changes-in-commit commit file-filter verbosity)]
      (mine-changes changes strategy min-support verbosity))))

(defn
  mine-commit-by-message
  ([repo commit-message strategy min-support verbosity]
   (mine-commit-by-message repo commit-message (fn [filename] true) strategy min-support verbosity ))
  ([repo commit-message file-filter strategy min-support verbosity]
	 (let [commit (first (repository/get-commits repo (fn [msg] (.startsWith msg commit-message))))]
     (mine-commit commit file-filter strategy min-support verbosity))))
  
(defn
  mine-commits-by-message-predicate
  ([repo commit-message-predicate strategy min-support verbosity]
     (mine-commits-by-message-predicate repo commit-message-predicate Integer/MAX_VALUE strategy min-support verbosity))
  ([repo commit-message-predicate limit strategy min-support verbosity]
     ; Run it
     (let [commits (repository/get-commits repo commit-message-predicate limit)]
       (patterns/concat (map (fn [commit]
	                             (mine-commit commit strategy min-support verbosity))
	                           commits)))))
      
; Template construction
;;;;;;;;;;;;;;;;;;;;;;;

(defn
  generate-templates
  [strategy patterns print-em]
  ; Construct lhs'es
	(let [lhses (postprocess/make-lhses-from-patterns strategy patterns)]
	  ; Print lhs'es if necessary
	  (when print-em
      (println "TEMPLATES")
	    (println (postprocess/lhses-to-string lhses))
	  ; Return lhs'es
	  lhses)))