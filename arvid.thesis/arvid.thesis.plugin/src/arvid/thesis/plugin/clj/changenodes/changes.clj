(ns arvid.thesis.plugin.clj.changenodes.changes
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change])
  (:require [qwalkeko.clj.functionalnodes :as functionalnodes]))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn get-all
  "Invokes changenodes differencer on the given before/after AST."
  [before after]
  (:changes (functionalnodes/get-ast-changes before after)))

(defn to-string
  "Converts a list of changenodes changes to a string for informational purposes."
  [changes]
  (clojure.string/join "\n" (map change/to-string changes)))