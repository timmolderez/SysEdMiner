(ns arvid.thesis.plugin.clj.preprocess.preprocess
  (:require [arvid.thesis.plugin.clj.preprocess.grouping.groups :as groups])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroups :as gengroups])
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn-
  safe-empty?
  "Simple wrapper around empty?, which catches exceptions. Changenodes is able to throw these at weird times somehow."
	[changes]
	(try 
	  (empty? changes)
	  (catch Exception e (do (println "! CHECKING IF THERE ARE MORE CHANGES FAILED, IGNORING.") true))))

(defn- 
  generalize
  "Generalizes the changes in the 'groups' store using 'strategy-equals?', producing gengroups ready for mining."
  [strategy-equals? groups]
  (gengroups/generalize strategy-equals? groups))

(defn- 
  grouping
  "Create a new groups based on the given 'changes', by inserting each change using the grouping strategy
   dictated by 'strategy-get-group-container'."
  [strategy-get-group-container changes]
  (loop [current-changes changes
	         groups (groups/make)]
	    (if (safe-empty? current-changes)
	        groups 
	        (let [current-change (first current-changes)
                new-groups (groups/add-change groups (strategy-get-group-container current-change) current-change)]
	          (recur (rest current-changes) new-groups)))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  preprocess
  "Build a store filled with given changes using the given strategy for grouping and generalization."
  [strategy changes]
  ; Process each changenodes change: add each change to the store. This also immediatly performs 
  ; generalization and grouping.
  (let [strategy-get-group-container (partial strategy/get-group-container strategy)
        strategy-equals? (partial strategy/equals? strategy)]
	  (generalize strategy-equals?
                (grouping strategy-get-group-container 
                          changes))))
