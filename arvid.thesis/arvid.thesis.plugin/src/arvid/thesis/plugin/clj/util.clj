(ns arvid.thesis.plugin.clj.util
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [damp.ekeko.snippets.operatorsrep :as operatorsrep])
  (:require [damp.ekeko.snippets.snippetgroup :as snippetgroup])
  (:import [java.util List]))

(defn 
  ekekoConsolePrintln 
  "Print given arguments to the Ekeko console"
  [& args] 
  (.println (.getConsoleStream (arvid.thesis.plugin.ThesisPlugin/getDefault)) (apply str args)))

(defn find-first-index 
  "Get the index of the first element satisfying 'pred' in 'coll', in linear time. nil otherwise." 
  [pred coll] 
  (let [indices (keep-indexed (fn [idx x] (when (pred x) idx)) coll)]
    (if (= (count indices) 1)
        (first indices)
        nil)))

(defn node-to-oneliner
  [node]
  (if (nil? node)
      "nil"
      (let [to-stringed-node (.replace (.toString node) "\n" " ")]
        (str (.substring to-stringed-node 0 (min 130 (.length to-stringed-node)))
             (if (> (.length to-stringed-node) 130) "..." "")))))

(defn
  get-path-between-nodes
  [node container-node]
  (loop [current-path '()
         current node]
    (if (or (nil? current)
            (= container-node current))
        current-path
        (let [parent-of-current (astnode/owner current)
              next-path (if parent-of-current
                            (let [owner-property (astnode/owner-property current)]
                              (if (not (astnode/property-descriptor-list? owner-property))
                                  (cons (astnode/ekeko-keyword-for-property-descriptor owner-property) current-path)
                                  (let [lst (astnode/node-property-value|reified parent-of-current owner-property)
                                         lst-raw (astnode/value-unwrapped lst)]
                                         (cons (astnode/ekeko-keyword-for-property-descriptor owner-property) 
                                               (cons (.indexOf ^List lst-raw current)
                                                     current-path)))))
                            current-path)]
            (recur next-path parent-of-current)))))

(defn
  tree-dft
  ([node node-processor]
    (tree-dft node node-processor node-processor node-processor node-processor))
  ([node ast-processor list-processor primitive-processor nil-processor]
   (cond (astnode/nilvalue? node)
           (nil-processor node)
	       (astnode/primitivevalue? node)
           (primitive-processor node)
	       (astnode/ast? node)
           (do (doall (map (fn [propval] (tree-dft propval ast-processor list-processor primitive-processor nil-processor)) 
                           (astnode/node-propertyvalues node)))
               (ast-processor node))
	       (astnode/lstvalue? node) 
           (do (doall (map (fn [listitem] (tree-dft listitem ast-processor list-processor primitive-processor nil-processor)) 
                           (astnode/value-unwrapped node)))
               (list-processor node))
         :else
           (throw (Exception. "Woops. What's happening here?" node)))))

