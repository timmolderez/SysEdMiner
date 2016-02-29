(ns arvid.thesis.plugin.clj.strategies.groupers.noneGrouper
  (:use damp.ekeko)
  (:use damp.ekeko.jdt.ast))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn
  run
  "Do not group changes: each changes gets its own private group."
  [change]
  change)

(defn
  to-string
  [descriptor]
  (System/identityHashCode descriptor))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(def definition {:name "Do not group", :run run, :to-string to-string})
