(ns arvid.thesis.plugin.clj.mining.patterns
  (:refer-clojure :rename {count clj-count concat clj-concat})
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern]))

(defrecord Patterns [patterns-list])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  to-string
  "Convert the patterns to a human-readable string."
  [patterns]
  (let [patterns-list (:patterns-list patterns)]
	  (if (empty? patterns-list)
	    "! No results"
		  (clojure.string/join "\n" 
									 		     (map (fn [pattern] (pattern/to-string pattern))
											          patterns-list)))))

(defn 
  make 
  [gengroups spmf-clj-result]
  (let [patterns-list (map (partial pattern/make gengroups) spmf-clj-result)]
    (Patterns. patterns-list)))

(defn
  count
  [patterns]
  (clj-count (:patterns-list patterns)))

(defn
  map-patterns
  [patterns f]
  (map f (:patterns-list patterns)))

(defn
  concat
  [seq-of-patterns]
  (Patterns. (mapcat (fn [patterns] (:patterns-list patterns))
                     seq-of-patterns)))
             