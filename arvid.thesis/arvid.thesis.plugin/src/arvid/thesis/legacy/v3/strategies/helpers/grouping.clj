(ns arvid.thesis.plugin.clj.strategies.helpers.grouping
  (:require [damp.ekeko.jdt.astnode :as astnode]))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  get-ancestor-of-type
  [type node]
  "Look up closest ancestor of the given type."
  (loop [current node]
	  (if (nil? current)
	      nil
	      (if (instance? (astnode/class-for-ekeko-keyword type) current)
	          current
	          (recur (astnode/owner current))))))
