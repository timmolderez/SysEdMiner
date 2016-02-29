(ns arvid.thesis.plugin.clj.mining.mining
  (import java.io.ByteArrayInputStream)
  (import ca.pfv.spmf.algorithms.frequentpatterns.apriori_close.AlgoAprioriClose)
  (import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharm_Bitset)
  (import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase)
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroups :as gengroups])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.gengroup :as gengroup])
  (:require [arvid.thesis.plugin.clj.mining.patterns :as patterns]))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  make-spmf-db
  "Creates a by SPMF minable string database"
  [gengroups]
  ; Represent each group on a seperate line by a sorted list of all the indices of the unique's of the instances in the group
  (clojure.string/join "\n" 
                       (gengroups/map-gengroups
							           gengroups
							           (fn [gengroup]
							             (clojure.string/join " "  
                                                (sort (gengroup/get-genchange-ids gengroup)))))))

; Removed that slow stuff... :)
;(defn
;  spmf-mine-apriori
;  "Run SPMF's apriori_close algorithm with the given minimum support and input database string."
;  [min-support input-database-string]
;  (let [input (new ByteArrayInputStream (.getBytes input-database-string))]
;    (.runAlgorithm (new AlgoAprioriClose) min-support input nil)))
(defn
  spmf-mine
  "Run SPMF's CHARM algorithm with the given minimum support and input database string."
  [input-database-string min-support]
  (let [input (new ByteArrayInputStream (.getBytes input-database-string))
        tdb (new TransactionDatabase)]
    (.loadVirtualFile tdb input)
    (.runAlgorithm (new AlgoCharm_Bitset) nil tdb min-support true 10000)))

(defn
  spmf-result-to-string
  "Returns a string representation of the SPMF mining result."
  [spmf-result]
  (clojure.string/join "\n" 
       (mapcat (fn [kitemsets]
                 (map (fn [itemset] (str "* " (sort (into [] (.getItems itemset))) " support " (.getAbsoluteSupport itemset)))
                      kitemsets))
               (.getLevels spmf-result))))

(defn
  make-patterns-from-spmf-result
  "Converts an SPMF mining result into a clojure datastructure, being a list of tuples <support, items>."
  [gengroups spmf-result]
  (let [spmf-clj-result (mapcat (fn [kitemsets]
                                     (map (fn [itemset] 
                                            [(.getAbsoluteSupport itemset) (sort (into '() (.getItems itemset)))])
                                          kitemsets))
                                (.getLevels spmf-result))]
    (patterns/make gengroups spmf-clj-result)))
