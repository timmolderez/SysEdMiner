(ns arvid.thesis.plugin.clj.postprocess.lhs
  (:use damp.ekeko)
  (:require [damp.ekeko.snippets.matching :as matching])
  (:require [damp.ekeko.snippets.snippetgroup :as snippetgroup])
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
  "Create a new left-hand side based on a pattern or a container and its changes."
  ([strategy container changes-in-container]
   (if container
       (strategy/make-lhs strategy 
                          (snippetgroup/add-snippet (snippetgroup/make-snippetgroup "-") 
                                                    (matching/snippet-from-node container)) changes-in-container)
       nil))
  ([strategy pattern]
   (make strategy (pattern/get-container pattern) (pattern/get-changes-in-container pattern))))