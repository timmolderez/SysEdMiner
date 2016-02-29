(ns arvid.thesis.plugin.clj.mining.pattern
  (:require [arvid.thesis.plugin.clj.mining.instance :as instance])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.genchange :as genchange])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroups :as gengroups]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Pattern [support genchange-ids instances])

(defn- in? 
  "true if seq contains elm"
  [seq elm]  
  (some #(= elm %) seq))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  make
  [gengroups spmf-clj-result-entry]
  (let [[support genchange-ids] spmf-clj-result-entry]
    (Pattern. support
              genchange-ids 
              (map (partial instance/make genchange-ids)
                   (gengroups/get-gengroups-with-genchange-ids gengroups genchange-ids)))))
  
(defn
  get-container
  [pattern]
  (gengroup/get-container (:gengroup (first (:instances pattern)))))

(defn
  get-change-in-container
  [pattern node]
  (let [genchange (gengroup/get-genchange-at (:gengroup (first (:instances pattern))) node)]
    (if (in? (:genchange-ids pattern) (genchange/genchange-id genchange) )
        (genchange/get-change genchange)
        nil)))

;(defn- 
;  to-pattern 
;  "Internal helper"
;  [store support unique-indices]
;  (let [uniques (map (partial store/get-unique-by-index store) unique-indices)
;        group (first (store/get-groups-with-uniques store uniques))
;        container (group/get-container group)
;        changes-in-container (store/get-group-changes-by-uniques store group uniques)]
;    (pattern/make support container changes-in-container)))

(defn
  to-string
  [pattern]
  (str "* Pattern " (:genchange-ids pattern) "\n"
       "  With support " (:support pattern) ", in groups: \n"
       (clojure.string/join "\n" 
                            (map (fn [instance] (str "  - " (instance/to-string instance)))
                                 (:instances pattern)))))