(ns arvid.thesis.plugin.clj.preprocess.generalization.gengroups
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup])
  (:require [arvid.thesis.plugin.clj.preprocess.grouping.groups :as groups]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

; Note: uniques is a list op tuples [group change].
(defrecord Gengroups [uniques gengroups-by-container])

(defn-
  make-from-groups
  [strategy-equals? groups]
	(groups/reduce-groups
	  groups
	  (fn [current-gengroups group]
	    (let [uniques (:uniques current-gengroups)
            gengroups-by-container (:gengroups-by-container current-gengroups)
            [new-uniques gengroup] (gengroup/make-from-group strategy-equals? uniques group)]
	      (Gengroups. new-uniques 
                    (assoc gengroups-by-container (gengroup/get-container gengroup) gengroup))))
	  (Gengroups. '() (hash-map))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  map-gengroups
  "map f over the gengroups in the given 'gengroups'"
  [gengroups f]
  (map f (vals (:gengroups-by-container gengroups))))

(defn
  generalize
  "Generalizes the changes in the 'groups' store using 'strategy-equals?', producing gengroups ready for mining."
  [strategy-equals? groups]
  (make-from-groups strategy-equals? groups))

(defn
  to-string
  "Returns a string representation of 'gengroups'."
  [gengroups]
  (str (if (empty? (:gengroups-by-container gengroups))
           "! Empty database"
           (clojure.string/join "\n" 
                                (map-gengroups gengroups gengroup/to-string)))
       "\n"))

(defn
  get-gengroups-with-genchange-ids
  [gengroups genchange-ids]
  (filter (fn [gengroup] (gengroup/has-all-genchange-ids gengroup genchange-ids))
          (vals (:gengroups-by-container gengroups))))