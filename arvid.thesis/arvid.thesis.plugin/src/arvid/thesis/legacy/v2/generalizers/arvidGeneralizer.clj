(ns arvid.thesis.plugin.clj.strategies.generalizers.arvidGeneralizer
  (:require [damp.ekeko.jdt [astnode :as astnode]])
  (:import [org.eclipse.jdt.core.dom MethodDeclaration]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

; Bug: only works with method grouping
(defn path-to-method
  [current]
  (loop [current-path '()
         current current]
    (let [parent-of-current (astnode/owner current)]
      (if (nil? parent-of-current)
          current-path
          (let [next-path (cons (astnode/ekeko-keyword-for-property-descriptor (astnode/owner-property current)) current-path)]
            (if (instance? MethodDeclaration current)
                next-path
                (recur next-path parent-of-current)))))))
           
(defn
  run
  "Arvid-based generalization: consider path, operation (Insert, Update, Delete) and type of AST node."
  [change]
  (let [node (:copy change)
        operation (:operation change)
        type (if node
	               (if (astnode/value? node)
					           (.getClass (astnode/value-unwrapped node))
					           (.getClass node))
	               nil)
        path (if node
                 (if (astnode/value? node)
					            '()
					            (path-to-method node))
                 nil)]
    [operation type path]))

(defn
  to-string
  [descriptor]
  (let [operation (first descriptor)
        type (second descriptor)
        path (nth descriptor 2)]
	  (str operation " "
	       (if type (.getSimpleName type) "-") 
         " in " path)))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(def definition {:name "Arvid-style", :run run, :to-string to-string})