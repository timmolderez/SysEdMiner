(ns arvid.thesis.plugin.clj.mining.mining
  (import java.io.ByteArrayInputStream)
  (import ca.pfv.spmf.algorithms.frequentpatterns.apriori_close.AlgoAprioriClose)
  (import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharm_Bitset)
  (import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase)
  (:require [arvid.thesis.plugin.clj.preprocessOld.store :as store])
  (:require [arvid.thesis.plugin.clj.preprocessOld.group :as group])
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn- 
  to-pattern 
  "Internal helper"
  [store support unique-indices]
  (let [uniques (map (partial store/get-unique-by-index store) unique-indices)
        group (first (store/get-groups-with-uniques store uniques))
        container (group/get-container group)
        changes-in-container (store/get-group-changes-by-uniques store group uniques)]
    (pattern/make support container changes-in-container)))

; Removed that slow stuff... :)
;(defn-
;  spmf-mine-apriori
;  "Run SPMF's apriori_close algorithm with the given minimum support and input database string."
;  [min-support input-database-string]
;  (let [input (new ByteArrayInputStream (.getBytes input-database-string))]
;    (.runAlgorithm (new AlgoAprioriClose) min-support input nil)))

(defn-
  spmf-mine
  "Run SPMF's CHARM algorithm with the given minimum support and input database string."
  [min-support input-database-string]
  (let [input (new ByteArrayInputStream (.getBytes input-database-string))
        tdb (new TransactionDatabase)]
    (.loadVirtualFile tdb input)
    (.runAlgorithm (new AlgoCharm_Bitset) nil tdb min-support true 10000)))

(defn-
  spmf-process-result
  "Converts an SPMF mining result into a clojure datastructure, being a list of tuples <support, items>."
  [store mining-result]
  (apply concat 
       (map (fn [kitemsets]
              (map (fn [itemset] 
                     (to-pattern store (.getAbsoluteSupport itemset) (sort (into '() (.getItems itemset)))))
                   kitemsets))
            (.getLevels mining-result))))


;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  spmf-make-db
  "Creates a by SPMF minable string database"
  [store]
  ; Represent each group on a seperate line by a sorted list of all the indices of the unique's of the instances in the group
  (clojure.string/join 
    "\n" 
    (store/map-groups store
      (fn [group]
        (clojure.string/join
          " "
          (store/get-unique-indices-by-group store group))))))

(defn
  run
  "Mines the given database for patterns using threshold min-support. Returns a list of patterns."
  [store min-support]
  (spmf-process-result store (spmf-mine min-support (spmf-make-db store)))) 
