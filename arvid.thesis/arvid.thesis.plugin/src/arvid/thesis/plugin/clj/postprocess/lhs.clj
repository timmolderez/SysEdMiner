(ns arvid.thesis.plugin.clj.postprocess.lhs
  (:use damp.ekeko)
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern])
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy])
  (import damp.ekeko.snippets.gui.TemplatePrettyPrinter)
  (import damp.ekeko.snippets.data.TemplateGroup))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  to-string
  [lhs]
  (.prettyPrint (new TemplatePrettyPrinter (TemplateGroup/newFromClojureGroup lhs))))

(defn
  make
  "Create a new left-hand side based on a pattern."
  [strategy pattern]
  (strategy/make-lhs strategy pattern))