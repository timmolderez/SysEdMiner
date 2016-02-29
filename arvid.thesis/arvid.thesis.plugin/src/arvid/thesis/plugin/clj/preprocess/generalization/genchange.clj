(ns arvid.thesis.plugin.clj.preprocess.generalization.genchange
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Genchange  [change genchange-id])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  make
  "Create a new genchange."
  [change genchange-id]
  (Genchange. change genchange-id))

(defn
  get-change
  "Get the underlying (changenodes) change of the given 'genchange'."
  [genchange]
  (:change genchange))

(defn
  genchange-id
  "Get the genchange-id of the given genchange. This id uniquely identifies its generalization"
  [genchange]
  (:genchange-id genchange))

(defn 
  to-string
  "Returns a string representation of 'genchange'."
  [genchange]
  (str "[" (:genchange-id genchange) "]  "
       (change/to-short-string (:change genchange))))