(ns arvid.thesis.plugin.clj.strategies.strategyFactory
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.grouping :as grouping])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.templates :as templates])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.equalities :as equalities])
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change])
  (:require [damp.ekeko.snippets.matching :as matching])
  (:require [damp.ekeko.snippets.snippet :as snippet])
  (:require [damp.ekeko.snippets.snippetgroup :as snippetgroup])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup])
  (:require [arvid.thesis.plugin.clj.preprocess.grouping.group :as group])
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern]))

; Equalities for operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn
  equals-operation-fully?
  "Returns whether the same type of operation is performaed."
  [group1 change1 group2 change2]
  (= (:operation change1) (:operation change2)))

(defn
  equals-operation-who-cares?
  "Returns true."
  [group1 change1 group2 change2]
  true)

; Equalities for change subjects
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn
  equals-subject-fully? 
  "Returns whether the subtree rooted at the given node fully matches the given other node." 
  [group1 change1 group2 change2]
  (let [node1 (change/get-subject change1)
			  node2 (change/get-subject change2)]
    (equalities/equals-subtree? Integer/MAX_VALUE true false node1 node2)))

(defn
  equals-subject-structurally? 
  "Returns whether the subtree rooted at the given node structurally matches the given other node: does not take
   propertyvalues of SimpleProperty's into consideration." 
  [group1 change1 group2 change2]
  (let [node1 (change/get-subject change1)
			  node2 (change/get-subject change2)]
    (equalities/equals-subtree? Integer/MAX_VALUE false false node1 node2)))

(defn
  equals-subject-structurally-only-mandatory? 
  "Returns whether the subtree rooted at the given node structurally matches the given other node: does not take
   propertyvalues of SimpleProperty's into consideration." 
  [group1 change1 group2 change2]
  (let [node1 (change/get-subject change1)
			  node2 (change/get-subject change2)]
    (equalities/equals-subtree? Integer/MAX_VALUE false true node1 node2)))

(defn
  equals-subject-depth-limited-structurally-2? 
  "Returns whether the subtree rooted at the given node partially structurally matches the given other node: does not take
   propertyvalues of SimpleProperty's into consideration. Only looks 2 levels deep." 
  [group1 change1 group2 change2]
  (let [node1 (change/get-subject change1)
			  node2 (change/get-subject change2)]
    (equalities/equals-subtree? 2 false false node1 node2)))

(defn
  equals-subject-type? 
  "Returns whether the node's type matches the given other node's type."
  [group1 change1 group2 change2]
  (let [node1 (change/get-subject change1)
			  node2 (change/get-subject change2)]
    (equalities/equals-subtree? 1 false false node1 node2)))

(defn
  equals-subject-who-cares? 
  "Considers all nodes equal."
  [group1 change1 group2 change2]
  (let [node1 (change/get-subject change1)
			  node2 (change/get-subject change2)]
    (equalities/equals-subtree? 0 false false node1 node2)))

; Context-introducing equalities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn
  equals-context-fully-changes-ignore?
  "Considers two nodes equal if their context within the group is fully equal."
  [group1 change1 group2 change2]
  (equalities/context-equals-subtree? true true group1 (gengroup/get-container group1) group2 (gengroup/get-container group2)))

(defn
  equals-context-structurally-changes-ignore?
  "Considers two nodes equal if their context within the group is structurally equal."
  [group1 change1 group2 change2]
  (equalities/context-equals-subtree? false true group1 (gengroup/get-container group1) group2 (gengroup/get-container group2)))

(defn
  equals-context-fully-changes-context?
  "Considers two nodes equal if their context within the group is fully equal."
  [group1 change1 group2 change2]
  (equalities/context-equals-subtree? true false group1 (gengroup/get-container group1) group2 (gengroup/get-container group2)))

(defn
  equals-context-structurally-changes-context?
  "Considers two nodes equal if their context within the group is structurally equal."
  [group1 change1 group2 change2]
  (equalities/context-equals-subtree? false false group1 (gengroup/get-container group1) group2 (gengroup/get-container group2)))

(defn
  equals-context-path-exact?
  "Considers two nodes equal if their exact path within the group is equal."
  [group1 change1 group2 change2]
  (let [path1 (group/get-path-of-change group1 change1)
        path2 (group/get-path-of-change group2 change2)]
    (= path1 path2)))

(defn 
  equals-context-path-no-indices?
  "Considers two nodes equal if their exact path within the group is equal, ignoring the paths indices."
  [group1 change1 group2 change2]
  (let [path1 (group/get-path-of-change group1 change1)
        path2 (group/get-path-of-change group2 change2)]
    (= (remove integer? path1) 
       (remove integer? path2))))

; TODO
;(defn
;  equals-context-path-relative?
;  "Considers two nodes equal if their relative path within the group is equal.
;   Note: the relative path does not include indices in lists."
;  [group1 change1 group2 change2]
;  true)

(defn
  equals-context-who-cares? 
  "Considers two nodes equal no matter their context within the group."
  [group1 change1 group2 change2]
  true)

; Template generation
;;;;;;;;;;;;;;;;;;;;;

(defn
  updater
  [s update-snippet pattern]
  (letfn [(structural-generalizer
           [s current]
           (cond (snippet/snippet-value-list? s current)
                   (doall (map (fn [child] (structural-generalizer s child))
                               (snippet/snippet-value-list-unwrapped s current))
                 (snippet/snippet-value-node? s current)
                   (doall (map (fn [child] (structural-generalizer s child))
                               (snippet/snippet-node-children s current)))
                 (snippet/snippet-value-primitive? s current)
                   (update-snippet templates/apply-operator-simple current "replace-by-wildcard"))))
          (snippet-traveller
           [s current current2]
           (let [change (pattern/get-change-in-container pattern current2)]
             ;(println current)(newline)
             (if (and change (not (= (:operation change) :insert)))
                 (do (structural-generalizer s current)
                     true)
                 (cond (snippet/snippet-value-null? s current)
                         (do (update-snippet templates/apply-operator-simple current "replace-by-wildcard")
                             false)
                       (snippet/snippet-value-list? s current)
                         (if (or (some true?
                                       (doall (map (fn [child child2] (snippet-traveller s child child2))
                                                   (snippet/snippet-value-list-unwrapped s current)
                                                   (astnode/value-unwrapped current2))))
                                 (not (nil? change)))
                             true
                             (update-snippet templates/apply-operator-simple current "replace-by-wildcard"))
                       (snippet/snippet-value-node? s current)
                         (if (or (some true?
                                       (doall (map (fn [child child2] (snippet-traveller s child child2))
                                                   (snippet/snippet-node-children s current)
                                                   (astnode/node-propertyvalues current2))))
                                 (not (nil? change)))
                             true
                             (update-snippet templates/apply-operator-simple current "replace-by-wildcard"))
                       (snippet/snippet-value-primitive? s current)
                         (do (update-snippet templates/apply-operator-simple current "replace-by-wildcard")
                             false)))))]
    (snippet-traveller s 
                       (snippet/snippet-root s)
                       (pattern/get-container pattern))))

;(defn
;  updater-old
;  [s update-snippet pattern]
;  (letfn 
;    [(recursive-descent
;			 [snippet current path]
;		   (cond (snippet/snippet-value-list? snippet current)
;			                                 (let [lst (snippet/snippet-value-list-unwrapped snippet current)
;			                                       count (count lst)]
;			                                   (map-indexed (fn [index child] (snippet-traveller snippet child (cons (- (- count 1) index) path)))
;			                                                (reverse lst)))
;			                               (snippet/snippet-value-node? snippet current)
;			                                 (map (fn [child] (snippet-traveller snippet child (cons (astnode/ekeko-keyword-for-property-descriptor (astnode/owner-property child)) path))) 
;			                                      (snippet/snippet-node-children snippet current))
;			                               :else
;			                                 [false]))
;		 (snippet-traveller
;       [s current path]
;			 (let [change (lhsConstruction/find-corresponding-change s path changes)]
;			   (cond (and change (= (:operation change) :insert)) ; NOTE: current now is the PARENT of where the insert happened
;			              (do ;(swap! new-snippet-group util/apply-operator-simple current "remove-node")
;			                  (let [recres (recursive-descent s current path)]
;							            (some true? (doall recres))))
;			         (and change (or (= (:operation change) :delete) 
;			                         (= (:operation change) :update)))
;			              (do ; (println "UPDATE OR DELETE")
;			                  true)
;			         (and change (= (:operation change) :move))
;			              (do ;(println "MOVE")
;			                  true)
;			         :else
;			           (let [recres (recursive-descent s current path)]
;			             (if (some true? (doall recres))
;			                 true
;			                 (do (update-snippet util/apply-operator-simple current "replace-by-wildcard")
;			                     false))))))]
;		  (snippet-traveller s (snippet/snippet-root s) '())))

;;;;;;;;;;;;;;;;;;;;;;;;;;
; Interface, private stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;

; Valid equalities:
;   :equals-operation-fully?
;   :equals-operation-who-cares?
;   :equals-subject-fully? 
;   :equals-subject-structurally? 
;   :equals-subject-structurally-only-mandatory?
;   :equals-subject-depth-limited-structurally-2? 
;   :equals-subject-type? 
;   :equals-subject-who-cares? 
;   :equals-context-fully-changes-ignore?
;   :equals-context-structurally-changes-ignore?
;   :equals-context-fully-changes-context?
;   :equals-context-structurally-changes-context?
;   :equals-context-path-exact?
;   :equals-context-who-cares? 
(defn-
  get-equality 
  [equality-keyword] 
  (resolve (symbol "arvid.thesis.plugin.clj.strategies.strategyFactory" (name equality-keyword))))

; Lookup table for lhs generation
(defn 
  get-lhs-updater 
  [equalities]
  (cond (= equalities #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?})
          updater
        :else
          nil))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  make-strategy 
  ([]
   (make-strategy :MethodDeclaration #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-no-indices?}))
  ([equalities]
   (make-strategy :MethodDeclaration equalities))
  ([group-container-type equalities]
	  (let [equality-functions (map get-equality equalities)] 
      (letfn [; Generalization strategy
              (generalizer-equals?
							  [group1 change1 group2 change2]
                ; Perform generalization based on all passed equalities (operation, subject and context)
							  (every? true? 
                        (map (fn [f] (f group1 change1 group2 change2))
                             equality-functions)))
              ; Grouping strategy
              (get-group-container
                [change]
                ; Return an AST, nil (no group container) or :unknown (couldn't determine with the information I received)
                (if (change/get-original change)
                    (grouping/get-ancestor-of-type group-container-type (change/get-original change))
                    :unknown))
              ; LHS construction
              (make-lhs
                [pattern]
                (let [lhs-updater (get-lhs-updater equalities)]
                  (if lhs-updater
                      (templates/make-lhs lhs-updater pattern)
                      (println "! LHS CONSTRUCTION IS NOT IMPLEMENTED FOR THIS STRATEGY. IGNORING PATTERN."))))]
			  ; Export the strategy
				(strategy/make generalizer-equals? get-group-container make-lhs)))))
		
