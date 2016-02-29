(ns arvid.thesis.plugin.clj.postprocess.postprocess
  (:require [arvid.thesis.plugin.clj.postprocess.lhs :as lhs])
  (:require [arvid.thesis.plugin.clj.mining.patterns :as patterns]))
  
;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn make-lhses-from-patterns
  [strategy patterns]
  ; Remove nil values, which lhs/make returns in case of lhs construction failure.
  (remove nil? (patterns/map-patterns patterns (partial lhs/make strategy))))

(defn
  lhses-to-string
  [lhses]
	(clojure.string/join "\n" 
                       (map (fn [lhs] (str "* "  (lhs/to-string lhs)))
                            lhses)))
