(ns arvid.thesis.plugin.clj.strategies.helpers.lhsConstruction
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [damp.ekeko.snippets.snippet :as snippet])
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change]))

(defn
  find-corresponding-change
  [snippet path changes]
  (letfn [(according-to-path-lst [current parent-of-current prop idx] 
              (let [list (util/node-propertykeyword-value-safe parent-of-current prop)]
                (do ;(println "      " (util/node-propertykeyword-value-safe parent-of-current prop))
                    (if (astnode/lstvalue? list)
                        (let [jlist (astnode/value-unwrapped list)]
                          (if (> (.size jlist) idx)
                              (= (.get jlist idx) current)
                              nil))
                        nil))))
          (according-to-path [current parent-of-current prop] 
            (do (= (util/node-propertykeyword-value-safe parent-of-current prop) current)))]
    (first 
      (filter  
        (fn [change] 
          (loop [current (change/get-left change)
                 current-path (if (= (:operation change) :insert)
			                            path
			                            path)]
            (if (empty? current-path)
                (do (if (and (astnode/ast? current) 
                              (util/ast-match? current (snippet/snippet-root snippet)))
	                       (snippet/snippet-root snippet)
	                       nil))
                (if (not current)
                    nil
			              (let [parent-of-current (astnode/owner current)]
			                (cond (nil? parent-of-current) ; we do not have an owner but must have one... BOEM!
			                        nil
		                        (not (if (number? (first current-path))
		                                 (according-to-path-lst current parent-of-current (second current-path) (first current-path))
		                                 (according-to-path current parent-of-current (first current-path))))
		                          nil
			                      :else ; Recursively travel up
		                          (recur parent-of-current (drop (if (number? (first current-path)) 2 1) current-path))))))))
		      changes))))
