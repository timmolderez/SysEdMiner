(ns arvid.thesis.plugin.clj.mining.instance
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup])
  (:import [org.eclipse.jdt.core.dom MethodDeclaration]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Instance [genchange-ids gengroup])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  make
  [genchange-ids gengroup]
  (Instance. genchange-ids gengroup))

(defn
  to-string
  [instance]
  (let [container (gengroup/get-container (:gengroup instance))
        container-string (if (instance? MethodDeclaration container) 
	                            (.getIdentifier (.getName container))
	                            (.getSimpleName (.getClass container)))]
    container-string))