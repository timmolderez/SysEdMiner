(ns arvid.thesis.plugin.clj.util
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [damp.ekeko.snippets.operatorsrep :as operatorsrep])
  (:require [damp.ekeko.snippets.snippetgroup :as snippetgroup]))

(defn 
  ekekoConsolePrintln 
  "Print given arguments to the Ekeko console"
  [& args] 
  (.println (.getConsoleStream (arvid.thesis.plugin.ThesisPlugin/getDefault)) (apply str args)))

(defn index-of 
  "Get the index of 'e' in 'coll', in linear time."
  [e coll] 
  (first (keep-indexed #(if (= e %2) %1) coll)))

(defn
  apply-operator-simple
  "Apply the operator defined by string 'oper' to the given 'node' in 'snippet-group'"
  [snippet-group node oper]
  (let [snippet (first (snippetgroup/snippetgroup-snippetlist snippet-group))
        operator (first (filter 
                          (fn [op] 
                            (some #{(operatorsrep/operator-id op)} [oper]))
                          (operatorsrep/registered-operators)))
        bindings (operatorsrep/make-implicit-operandbinding-for-operator-subject snippet-group snippet node operator)]
    (if (operatorsrep/applicable? snippet-group snippet node operator)
        (operatorsrep/apply-operator-to-snippetgroup snippet-group snippet node operator [bindings])
        (do "! Operator not applicable. Ignoring!"
            snippet-group))))

(defn 
  ast-match?
  "Simple Clojure interface for Java ASTMatcher."
  ([left right]
    (let [matcher (new org.eclipse.jdt.core.dom.ASTMatcher)]
      (ast-match? left right matcher)))
  ([left right matcher]
    (.subtreeMatch left matcher right)))

(defn
  node-propertykeyword-value-safe
  "Variant of astnode/node-propertykeyword-value|reified failing in silence (returning nil) if the property is not valid."
  [node propertykeyword]
  (let [reifier (get (astnode/reifiers node) propertykeyword)]
    (if reifier
        (reifier node)
        nil)))

(defn distinct-consequtive 
  [sequence] 
  (map first (partition-by identity sequence)))

(defn node-to-oneliner
  [node]
  (let [to-stringed-node (.replace (.toString node) "\n" " ")]
    (str (.substring to-stringed-node 0 (min 80 (.length to-stringed-node)))
         (if (> (.length to-stringed-node) 80) "..." ""))))

(defn
  node-dft
  ([node node-processor]
    (node-dft node node-processor node-processor node-processor node-processor))
  ([node ast-processor list-processor primitive-processor nil-processor]
   (cond (astnode/nilvalue? node)
           (nil-processor node)
	       (astnode/primitivevalue? node)
           (primitive-processor node)
	       (astnode/ast? node)
           (do (ast-processor node)
               (map (fn [propval] (node-dft propval ast-processor list-processor primitive-processor nil-processor)) 
                    (astnode/node-propertyvalues node)))
	       (astnode/lstvalue? node) 
           (do (list-processor node)
               (map (fn [listitem] (node-dft listitem ast-processor list-processor primitive-processor nil-processor)) 
                    (astnode/value-unwrapped node)))
         :else
           (throw (Exception. "Woops. What's happening here?")))))

;(defn
;  is-comment
;  [change]
;  (let [subject (get-subject change)
;        parent (astnode/owner subject)]
;    (println subject parent)
;    (if (or (.isComment NodeClassifier subject)
;            (and (not (nil? parent)) (.isComment NodeClassifier parent)))
;        (println "DROP COMMENT NODE" (to-short-string subject)))
;    (or (.isComment NodeClassifier subject)
;        (and (not (nil? parent)) (.isComment NodeClassifier parent)))))
    