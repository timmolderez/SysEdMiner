(ns arvid.thesis.plugin.clj.strategies.helpers.equalities
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup])
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:import [java.util List]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn-
  equals-tree?
  "Returns whether the subtree rooted at the given node matches the given other node."
  ([short-circuit nil-processor primitive-processor ast-processor list-processor node1 node2]
    (equals-tree? 0 short-circuit nil-processor primitive-processor ast-processor list-processor node1 node2))
  ([depth short-circuit nil-processor primitive-processor ast-processor list-processor node1 node2]
    (if (short-circuit node1 node2 depth)
	      true 
			  (cond (astnode/nilvalue? node1)
			          (nil-processor node1 node2)
				      (astnode/primitivevalue? node1)
	              (primitive-processor node1 node2)
				      (astnode/ast? node1)
	              (ast-processor node1 
	                             node2 
                               (partial equals-tree? (+ depth 1)  short-circuit nil-processor primitive-processor ast-processor list-processor))
				      (astnode/lstvalue? node1) 
	              (list-processor node1 
	                              node2
                                (partial equals-tree? (+ depth 1)  short-circuit nil-processor primitive-processor ast-processor list-processor))
              :else 
                (println "Should not happen")))))

; note:
; - ChildListPropertyDescriptor (property-descriptor-list?) has no isMandatory()
; - SimplePropertyDescriptor (property-descriptor-simple?) has isMandatory()
; - ChildPropertyDescriptor (property-descriptor-child?) has isMandatory()
(defn-
  trivial-ast-equals?
  [only-mandatory node1 node2 the-equals?]
  (and (astnode/ast? node2) 
       (= (.getClass node1) (.getClass node2))
       (let [properties (filter (fn [property] (or (not only-mandatory) (astnode/property-descriptor-list? property) (.isMandatory property)))
                                (astnode/node-property-descriptors node1))
             children1 (map (partial astnode/node-property-value|reified node1) properties)
	           children2 (map (partial astnode/node-property-value|reified node2) properties)]
         (every? true? (map the-equals? children1 children2)))))

(defn-
  trivial-nil-equals?
  [node1 node2]
  (astnode/nilvalue? node2))

(defn-
  trivial-list-equals?
  [node1 node2 the-equals?] 
  (and (astnode/lstvalue? node2) 
       (let [children1 (astnode/value-unwrapped node1)
		         children2 (astnode/value-unwrapped node2)]
		     (and (= (count children1) (count children2))
	            (every? true? (map the-equals?
                                 children1 children2))))))

(defn-
  trivial-primitive-equals?
  [check-values node1 node2]
  (and (astnode/primitivevalue? node2)
       (let [value1 (astnode/value-unwrapped node1)
			       value2 (astnode/value-unwrapped node2)] 
         (if (or (nil? value1) (nil? value2))
             (do (println "PRIMITIVE VALUE IS NIL. SHOULD NOT HAPPEN. REMOVE COMMENTS.")
                 (and (nil? value1) (nil? value2)))
				     (and (= (.getClass value1) (.getClass value2))
		              (if check-values (= value1 value2) true))))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;


(defn
  context-equals-subtree?
  [check-values ignore-changes group1 node1 group2 node2]
  (equals-tree? 
    (fn [node1 node2 depth] 
      (if (not ignore-changes)
          false
          (or (not (nil? (gengroup/get-genchange-at group1 node1)))
              (not (nil? (gengroup/get-genchange-at group2 node2))))))
    trivial-nil-equals?
    (partial trivial-primitive-equals? check-values)
    (partial trivial-ast-equals? false)
    trivial-list-equals?
    node1
    node2))

(defn
  equals-subtree?
  [max-depth check-values only-mandatory node1 node2]
  "Returns whether the subtree rooted at the given node matches the given other node. Requires depth to be positive. Special cases:
   * With depth=0,check-values=/: Considers every two nodes equal.
   * With depth=1,check-values=false: Returns whether the type of the given AST is equal to the type of the given other node.
   * With depth=Integer/MAX_VALUE, check-values=false: Structural matching of the subtrees (does not consider values/simpleProperties)
   * With depth=Integer/MAX_VALUE, checkValues=true Full matching of the subtrees. (100% equality)"
  (equals-tree? 
    (fn [node1 node2 depth] 
      (>= depth max-depth))
    trivial-nil-equals?
    (partial trivial-primitive-equals? check-values)
    (partial trivial-ast-equals? only-mandatory)
    trivial-list-equals?
    node1
    node2))
