(ns arvid.thesis.plugin.clj.strategies.helpers.equalities
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy])
  (:use damp.ekeko)
  (:use damp.ekeko.jdt.ast)
  (:require [clojure.core.logic :as l])
  (:require [damp.ekeko.logic :as el])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [damp.ekeko.snippets.snippet :as snippet])
  (:require [damp.ekeko.snippets.matching :as matching])
  (:require [damp.ekeko.snippets.snippetgroup :as snippetgroup])
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern])
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:import [org.eclipse.jdt.core.dom MethodDeclaration])
  (:import [java.util List]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

; Helpers
;;;;;;;;;

(defn-
  equals-subtree?
  [depth check-values node1 node2]
  "Returns whether the subtree rooted at the given node matches the given other node. Requires depth to be positive. Special cases:
   * With depth=0,check-values=/: Considers every two nodes equal.
   * With depth=1,check-values=false: Returns whether the type of the given AST is equal to the type of the given other node.
   * With depth=Integer/MAX_VALUE, check-values=false: Structural matching of the subtrees (does not consider values/simpleProperties)
   * With depth=Integer/MAX_VALUE, checkValues=true Full matching of the subtrees. (100% equality)"
  (if (= depth 0)
      true 
		  (cond (astnode/nilvalue? node1)
		          (astnode/nilvalue? node2)
			      (astnode/primitivevalue? node1)
              (and (astnode/primitivevalue? node2)
					            (let [value1 (astnode/value-unwrapped node1)
					                  value2 (astnode/value-unwrapped node2)] 
                         (cond (nil? value1)
                                 (nil? value2)
                               (nil? value2)
                                 (nil? value1)
				                       :else
                                 (and (= (.getClass value1) (.getClass value2))
		                                      (if check-values (= value1 value2) true)))))
			      (astnode/ast? node1)
			        (and (astnode/ast? node2)
			             (let [propvals1 (astnode/node-propertyvalues node1)
			                   propvals2 (astnode/node-propertyvalues node2)]
			               (and (= (.getClass node1) (.getClass node2)) ; Same type
                          (= (count propvals1) (count propvals2)) ; Trivial I suppose it'l be always true due to same type
			                    (every? true? (map (partial equals-subtree? (- depth 1) check-values) propvals1 propvals2)))))
			      (astnode/lstvalue? node1) 
			        (and (astnode/lstvalue? node2)
			             (let [lst1 (astnode/value-unwrapped node1)
			                   lst2 (astnode/value-unwrapped node2)]
			               (and (= (count lst1) (count lst2))
					                (every? true? (map (partial equals-subtree? (- depth 1) check-values) lst1 lst2))))))))

(defn- path-to-container-relative
  [container-type current]
  (loop [current-path '()
         current current]
    (if (or (nil? current)
            (instance? (astnode/class-for-ekeko-keyword container-type) current))
        current-path
        (let [parent-of-current (astnode/owner current)
              next-path (if parent-of-current
                            (cons (astnode/ekeko-keyword-for-property-descriptor (astnode/owner-property current)) current-path)
                            current-path)]
            (recur next-path parent-of-current)))))

(defn- path-to-container-exact
  [container-type current]
  (loop [current-path '()
         current current]
    (if (or (nil? current)
            (instance? (astnode/class-for-ekeko-keyword container-type) current))
        current-path
        (let [parent-of-current (astnode/owner current)
              next-path (if parent-of-current
                            (let [owner-property (astnode/owner-property current)]
                              (if (not (astnode/property-descriptor-list? owner-property))
                                  (cons (astnode/ekeko-keyword-for-property-descriptor owner-property) current-path)
                                  (let [lst (astnode/node-property-value|reified parent-of-current owner-property)
                                         lst-raw (astnode/value-unwrapped lst)]
                                         (cons (.indexOf ^List lst-raw current)
                                               (cons (astnode/ekeko-keyword-for-property-descriptor owner-property) 
                                                     current-path)))))
                            current-path)]
            (recur next-path parent-of-current)))))

; Equalities for operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn 
  equals-operation-fully?
  "Returns whether the same type of operation is performaed."
  [operation1 operation2]
  (= operation1 operation2))

(defn 
  equals-operation-who-cares?
  "Returns true."
  [operation1 operation2]
  true)

; Equalities for change subjects
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def equals-fully? 
  "Returns whether the subtree rooted at the given node fully matches the given other node." 
  (partial equals-subtree? Integer/MAX_VALUE true))

(def equals-structurally? 
  "Returns whether the subtree rooted at the given node structurally matches the given other node: does not take
   propertyvalues of SimpleProperty's into consideration." 
  (partial equals-subtree? Integer/MAX_VALUE false))

(def equals-depth-limited-structurally-5? 
  "Returns whether the subtree rooted at the given node partially structurally matches the given other node: does not take
   propertyvalues of SimpleProperty's into consideration. Only looks 5 levels deep." 
  (partial equals-subtree? 5 false))

(def equals-depth-limited-structurally-3? 
  "Returns whether the subtree rooted at the given node partially structurally matches the given other node: does not take
   propertyvalues of SimpleProperty's into consideration. Only looks 3 levels deep." 
  (partial equals-subtree? 3 false))

(def equals-depth-limited-structurally-2? 
  "Returns whether the subtree rooted at the given node partially structurally matches the given other node: does not take
   propertyvalues of SimpleProperty's into consideration. Only looks 2 levels deep." 
  (partial equals-subtree? 2 false))

(def equals-type? 
  "Returns whether the node's type matches the given other node's type."
  (partial equals-subtree? 1 false))

(def equals-who-cares? 
  "Considers all nodes equal."
  (partial equals-subtree? 0 false))

; Context-introducing equalities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn equals-context-fully?
  "Considers two nodes equal if their context within the group is fully equal."
  [container-type node1 node2]
  true)

(defn equals-context-structurally?
  "Considers two nodes equal if their context within the group is structurally equal."
  [container-type node1 node2]
  true)


(defn equals-context-structurally-to-changes?
  "Considers two nodes equal if their context within the group is structurally equal up to the level where changes occur."
  [container-type node1 node2]
  true)

(defn equals-context-structurally-above-changes-exact?
  "Considers two nodes equal if their context within the group is structurally equal up to the parents of the level where
   changes occur. Exactly identifies change locations within their corresponding parent nodes."
   [container-type node1 node2]
  true)
 
(defn equals-context-structurally-above-changes-relative?
  "Considers two nodes equal if their context within the group is structurally equal up to the parents of the level where
   changes occur. Relatively identifies change locations within their corresponding parent nodes.
   Note: the relative path does not include indices in lists."
  [container-type node1 node2]
  true)

(defn equals-context-path-exact?
  "Considers two nodes equal if their exact path within the group is equal."
  [container-type node1 node2]
  (letfn [(get-path [node]
            (cond (nil? node) nil
			            (astnode/value? node) nil)
			            :else (path-to-container-exact container-type node))]
    (= (get-path node1) (get-path node2))))

(defn equals-context-path-relative?
  "Considers two nodes equal if their relative path within the group is equal.
   Note: the relative path does not include indices in lists."
  [container-type node1 node2]
  (letfn [(get-path [node]
            (cond (nil? node) nil
			            (astnode/value? node) nil)
			            :else (path-to-container-relative container-type node))]
    (= (get-path node1) (get-path node2))))

(defn equals-context-who-cares? 
  "Considers two nodes equal no matter their context within the group."
  [container-type node1 node2]
  true)

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

; Valid equalities:
;   :equals-operation-fully?
;   :equals-operation-who-cares?
(defn 
  get-operation-equality 
  [equality-keyword] 
  (if (contains? #{:equals-operation-fully? :equals-operation-who-cares?}
                 equality-keyword)
      (resolve (symbol "arvid.thesis.plugin.clj.strategies.helpers.equalities" (name equality-keyword)))
      (do (println "! OPERATION EQUALITY" equality-keyword "DOES NOT EXIST")
          nil)))
; Valid equalities:
;   :equals-fully? 
;   :equals-structurally? 
;   :equals-depth-limited-structurally-5? 
;   :equals-depth-limited-structurally-3? 
;   :equals-depth-limited-structurally-2? 
;   :equals-type? 
;   :equals-who-cares? 
(defn 
  get-subject-equality 
  [equality-keyword] 
  (if (contains? #{:equals-fully? :equals-structurally? :equals-depth-limited-structurally-5? 
                   :equals-depth-limited-structurally-3? :equals-depth-limited-structurally-2?
                   :equals-type? :equals-who-cares?} 
                 equality-keyword)
      (resolve (symbol "arvid.thesis.plugin.clj.strategies.helpers.equalities" (name equality-keyword)))
      (do (println "! SUBJECT EQUALITY" equality-keyword "DOES NOT EXIST")
          nil)))

; Valid equalities:
;   :equals-context-fully?
;   :equals-context-structurally?
;   :equals-context-structurally-to-changes?
;   :equals-context-structurally-above-changes-exact?
;   :equals-context-structurally-above-changes-relative?
;   :equals-context-path-exact?
;   :equals-context-path-relative?
;   :equals-context-who-cares? 
(defn 
  get-context-equality
  [equality-keyword] 
  (if (contains? #{:equals-context-fully? :equals-context-structurally? 
                   :equals-context-structurally-to-changes? 
                   :equals-context-structurally-above-changes-exact? 
                   :equals-context-structurally-above-changes-relative? 
                   :equals-context-path-exact? :equals-context-path-relative? 
                   :equals-context-who-cares?} 
                 equality-keyword)
      (resolve (symbol "arvid.thesis.plugin.clj.strategies.helpers.equalities" (name equality-keyword)))
      (do (println "! CONTEXT EQUALITY" equality-keyword "DOES NOT EXIST")
          nil)))