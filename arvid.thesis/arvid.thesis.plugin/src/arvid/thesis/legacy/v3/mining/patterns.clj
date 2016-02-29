(ns arvid.thesis.plugin.clj.mining.patterns
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern]))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  to-string
  "Convert the mining result to a human-readable string."
  [patterns]
  (if (empty? patterns)
    "! No results"
	  (apply str 
	    (map (fn [pattern] (pattern/to-string pattern))
	         patterns))))
