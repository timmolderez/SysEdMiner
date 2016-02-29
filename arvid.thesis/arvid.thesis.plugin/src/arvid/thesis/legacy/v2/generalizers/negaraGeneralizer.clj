(ns arvid.thesis.plugin.clj.strategies.generalizers.negaraGeneralizer
  (:require [damp.ekeko.jdt [astnode :as astnode]]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn
  run
  "Negara-based generalization: only consider operation (Insert, Update, Delete) and type of AST node."
  [change]
  (let [operation (:operation change)
        type (if (:copy change)
	               (if (astnode/value? (:copy change))
	                   (.getClass (astnode/value? (:copy change)))
	                   (.getClass (:copy change)))
	               nil)]
    [operation type]))

(defn
  to-string
  [descriptor]
  (let [operation (first descriptor)
        type (second descriptor)]
	  (str operation " "
	       (if type (.getSimpleName type) "-"))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(def definition {:name "Negara et al. style", :run run, :to-string to-string})