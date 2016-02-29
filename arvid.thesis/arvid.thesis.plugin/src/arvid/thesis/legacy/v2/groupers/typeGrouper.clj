(ns arvid.thesis.plugin.clj.strategies.groupers.typeGrouper
  (:use damp.ekeko)
  (:use damp.ekeko.jdt.ast))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn
  run
  "Group them all together by their containing class/interface."
  [change]
  (ekeko [?type]
     (ast-typedeclaration|encompassing (:right-parent change) ?type)))

(defn
  to-string
  [descriptor]
  (if (empty? descriptor)
      "-"
      (.getIdentifier (.getName (first (first descriptor))))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(def definition {:name "By containing type", :run run, :to-string to-string})
