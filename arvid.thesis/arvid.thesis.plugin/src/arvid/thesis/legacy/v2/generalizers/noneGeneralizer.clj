(ns arvid.thesis.plugin.clj.strategies.generalizers.noneGeneralizer)

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn
  run
  "Use the identity to generalize changes to... themselves."
  [change]
  change)

(defn
  to-string
  [descriptor]
  (System/identityHashCode descriptor))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(def definition {:name "No generalization", :run run, :to-string to-string})