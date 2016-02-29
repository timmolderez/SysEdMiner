(ns arvid.thesis.plugin.clj.strategies.groupers.commitGrouper)

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn
  run
  "Group them all together. Note that we expect our input consists of only one commit."
  [change]
  ())

(defn
  to-string
  [descriptor]
  "*selected-commit*")

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(def definition {:name "By commit", :run run, :to-string to-string})
