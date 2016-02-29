

; Useful things
;;;;;;;;;;;;;;;

(use 'clojure.stacktrace)    
(print-stack-trace *e 40)

(inspector-jay.core/inspect *1)

; Verbosities:
; - 0: shut up!
; - 1: only results
; - 2: details about preprocessing and mining
; - 3: also includes changes

; Template generation is supported for:
; - #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?}
; - #{:equals-operation-fully? :equals-subject-structurally? :equals-context-structurally? :equals-context-path-exact?}

; Some notes:
;matching/snippet-from-node
;slurp-from-resource`
;(print-stack-trace *e)
;zie templategroup-matches in geneticsearch
;command-shift-R
;test/against-project-named
;query/query-by-snippetgroup: voert de query uit
;ge,etocs+^re^rpcess-templategroep: pas operator mee op roots
;operatorsrep/registered-operators <--- lijstje van operatoren
;operatorsrep/apply-operator-to-snippetgroup
;operatorsrep/operator-operands
;possibl-operand-values|valid
;(query-by-snippet*



; Requiring stuff
;;;;;;;;;;;;;;;;;

(defn 
  reqall
  ([] 
    (reqall false))
  ([hard-reset]
	  ; Helpers
		(require '[clojure.core.logic :as logic] :reload)
		(require '[damp.ekeko.jdt.ast :as jdt] :reload)
	  ; Global stuff
	  (require '[arvid.thesis.plugin.clj.main :as main] (if hard-reset  :reload-all  :reload))
	  (require '[arvid.thesis.plugin.clj.util :as util] :reload)
	  (require '[arvid.thesis.plugin.clj.strategies.strategy :as strategy] :reload)
	  (require '[arvid.thesis.plugin.clj.strategies.strategyFactory :as strategyFactory] :reload)
    (require '[arvid.thesis.plugin.clj.strategies.helpers.equalities :as equalities] :reload)
    (require '[arvid.thesis.plugin.clj.strategies.helpers.templates :as templates] :reload)
	  (require '[arvid.thesis.plugin.clj.strategies.helpers.grouping :as grouping] :reload)
	  ; Phase 1: GIT
	  (require '[arvid.thesis.plugin.clj.git.changedFile :as changed-file] :reload)
	  (require '[arvid.thesis.plugin.clj.git.commit :as commit] :reload)
	  (require '[arvid.thesis.plugin.clj.git.repository :as repository] :reload)
	  ; Phase 2: Differencing
	  (require '[arvid.thesis.plugin.clj.changenodes.change :as change] :reload)
	  (require '[arvid.thesis.plugin.clj.changenodes.changes :as changes] :reload)
	  ; Phase 3: preprocessing (grouping and generalization)
	  (require '[arvid.thesis.plugin.clj.preprocess.grouping.group :as group] :reload)
	  (require '[arvid.thesis.plugin.clj.preprocess.grouping.groups :as groups] :reload)
	  (require '[arvid.thesis.plugin.clj.preprocess.generalization.genchange :as genchange] :reload)
	  (require '[arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup] :reload)
	  (require '[arvid.thesis.plugin.clj.preprocess.generalization.gengroups :as gengroups] :reload)
	  (require '[arvid.thesis.plugin.clj.preprocess.preprocess :as preprocess] :reload)
	  ; Phase 4: mining
    (require '[arvid.thesis.plugin.clj.mining.instance :as instance] :reload)
    (require '[arvid.thesis.plugin.clj.mining.pattern :as pattern] :reload)
    (require '[arvid.thesis.plugin.clj.mining.patterns :as patterns] :reload)
    (require '[arvid.thesis.plugin.clj.mining.mining :as mining] :reload)
	  ; Phase 5: postprocessing (template creation)
    (require '[arvid.thesis.plugin.clj.postprocess.lhs :as lhs] :reload)
    (require '[arvid.thesis.plugin.clj.postprocess.postprocess :as postprocess] :reload)))
  
(reqall)

; Mine a specific commit in my repo
(let [strategy (strategyFactory/make-strategy :MethodDeclaration #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?})]
	(main/generate-templates
	  strategy
		(main/mine-commit-by-message 
		  "/Users/demeyerarvid/Desktop/Thesis/Repos/arvid.thesis.test/.git"
		  "Updated returnZeroIfArgumentsAreEqual"
		  strategy
		  2 
	    2)
	  true))
                
; Mine all pattern-containing commits in my repo
;(let [strategy (strategyFactory/make-strategy :MethodDeclaration #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?})]
;	(patterns/count
;		(main/mine-commits-by-message-predicate 
;		  "/Users/demeyerarvid/Desktop/Thesis/Repos/arvid.thesis.test/.git"
;		  (fn [msg] (.contains msg "Updated"))
;		  strategy
;		  2 
;	    0)))

; Experiment with equalities
;(doseq [subject-equality [:equals-fully? :equals-structurally? :equals-depth-limited-structurally-5? 
;                          :equals-depth-limited-structurally-3? :equals-depth-limited-structurally-2? 
;                          :equals-type? :equals-who-cares?]
;        context-equality [;:equals-context-fully? :equals-context-structurally? 
;                          ;:equals-context-structurally-to-changes? :equals-context-structurally-above-changes-exact?
;                          ;:equals-context-structurally-above-changes-relative? 
;                          :equals-context-path-exact? 
;                          ;:equals-context-path-relative? :equals-context-who-cares?
;                          ]]
;  (let [results (main/mine-commits-by-message-predicate 
;                  "/Users/demeyerarvid/School/Thesis/Repos/arvid.thesis.test/.git"
;                  (fn [msg] (.contains msg "Updated"))
;                  (strategyFactory/make-strategy :MethodDeclaration :equals-operation-fully? subject-equality context-equality)
;                  2 
;                  0)
;        numresults (patterns/count results)]
;   (println (format "%-40s%-60s%-5d" subject-equality context-equality numresults))))
    

;(doseq [subject-equality [:equals-fully?; :equals-structurally? :equals-depth-limited-structurally-5? 
;                          ;:equals-depth-limited-structurally-3? :equals-depth-limited-structurally-2? 
;                          ;:equals-type? :equals-who-cares?
;                          ]
;        context-equality [;:equals-context-fully? :equals-context-structurally? 
;                          ;:equals-context-structurally-to-changes? :equals-context-structurally-above-changes-exact?
;                          ;:equals-context-structurally-above-changes-relative? 
;                          :equals-context-path-exact? 
;                          ;:equals-context-path-relative? :equals-context-who-cares?
;                          ]]
;  (let [results (main/mine-commits-by-message-predicate 
;                  "/Users/demeyerarvid/Desktop/Thesis/Repos/jgit/.git"
;                  (fn [msg] (and (.contains (.toLowerCase msg) "refactor")
;                                 (not (.startsWith (.toLowerCase msg) "merge"))))
;                  20
;                  (strategyFactory/make-strategy :MethodDeclaration :equals-operation-fully? subject-equality context-equality)
;                  2
;                  0)
;        numresults (patterns/count results)]
;   (println (format "%-40s%-60s%-5d" subject-equality context-equality numresults))))

; Mine Coen's exapus thing


(let [strategy (strategyFactory/make-strategy :MethodDeclaration #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?})]
	(main/generate-templates 
    strategy
    (main/mine-commit-by-message 
	    "/Users/demeyerarvid/Desktop/Thesis/Repos/exapus/.git"
      "Incorporated partial program analysis"
      (fn [filename] (.contains filename "PackageLayer"))
	    strategy
	    2
      3)
    true)
  nil )

; Mine jgit refactorings

(let [strategy (strategyFactory/make-strategy :MethodDeclaration #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?})]
	(main/generate-templates 
    strategy
    (main/mine-commit-by-message 
	    "/Users/demeyerarvid/Desktop/Thesis/Repos/exapus/.git"
	    "Incorporated partial"
      (fn [filename] (.contains (.toLowerCase filename) "packagelayer"))
	    strategy
	    2
      1)
    true)
  nil )

; Mine current workspace
(reqall)
(let [strategy (strategyFactory/make-strategy :MethodDeclaration #{:equals-operation-fully? :equals-subject-structurally?})
      result  (first (logic/run 1 [?left ?right]
                       (logic/fresh [?lt ?ln ?rt ?rn]
                         (jdt/ast :CompilationUnit ?left)
                         (jdt/child+ ?left ?lt)
                         (jdt/ast :TypeDeclaration ?lt)
                         (jdt/has :name ?lt ?ln)
                         (jdt/name|simple-string ?ln "Test1")
                         (jdt/ast :CompilationUnit ?right)
                         (jdt/child+ ?right ?rt)
                         (jdt/ast :TypeDeclaration ?rt)
                         (jdt/has :name ?rt ?rn)
                         (jdt/name|simple-string ?rn "Test2"))))
      changes (changes/get-all (first result) (second result))]
  (println (changes/to-string changes))
  (main/generate-templates 
    strategy
    (main/mine-changes
      changes
      strategy
      2
      2)
    true)
  nil)
   
; Code for testing equalities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   
(defn getnodes []     
  (first (logic/run 1 [?md1 ?md2 ?md3 ?td]
            (logic/fresh [?cu ?td ?tdn ?mdn1 ?mdn2 ?mdn3]
              (jdt/ast :CompilationUnit ?cu)
              (jdt/child+ ?cu ?td)
              (jdt/ast :TypeDeclaration ?td)
              (jdt/has :name ?td ?tdn)
              (jdt/name|simple-string ?tdn "Test1")
              (jdt/ast :MethodDeclaration ?md1)
              (jdt/has :name ?md1 ?mdn1)
              (jdt/name|simple-string ?mdn1 "a")
              (jdt/ast :MethodDeclaration ?md2)
              (jdt/has :name ?md2 ?mdn2)
              (jdt/name|simple-string ?mdn2 "b")
              (jdt/ast :MethodDeclaration ?md3)
              (jdt/has :name ?md3 ?mdn3)
              (jdt/name|simple-string ?mdn3 "c")))))
(def nodes (getnodes))
(println 
  (equalities/equals-structurally? (nth nodes 0) (nth nodes 0))
  (equalities/equals-structurally? (nth nodes 0) (nth nodes 1))
  (equalities/equals-structurally? (nth nodes 0) (nth nodes 2)))
         

