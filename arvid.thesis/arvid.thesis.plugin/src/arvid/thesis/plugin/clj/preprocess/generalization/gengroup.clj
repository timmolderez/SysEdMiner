(ns arvid.thesis.plugin.clj.preprocess.generalization.gengroup
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change])
  (:require [arvid.thesis.plugin.clj.preprocess.generalization.genchange :as genchange])
  (:require [arvid.thesis.plugin.clj.preprocess.grouping.group :as group])
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:import [org.eclipse.jdt.core.dom MethodDeclaration]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Gengroup  [genchange-ids container changes-by-node genchanges group])

(defn- 
  map-genchanges
  "map f over the genchanges in the given 'gengroup'"
  [gengroup f]
  (map f (:genchanges gengroup)))

(defn
  intern-change
  "Intern a 'group'/'change' in 'uniques', returning a vector [new-uniques id]."
  [strategy-equals? uniques group change]
  (let [first-index (util/find-first-index (fn [[group2 change2]] (strategy-equals? group2 change2 group change)) uniques)]
    (if (nil? first-index)
        [(concat uniques (list [group change])) (count uniques)] ; Not in the list, add it
        [uniques first-index]))) ; Already in the list, nothing to be done

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  get-path-of-change
  [gengroup genchange]
  (group/get-path-of-change (:group gengroup) (:change genchange)))

(defn
  get-genchange-at
  [group node]
  (let [change (get (:changes-by-node group) node)]
    (if change
        (some #(when (= (genchange/get-change %) change) %) (:genchanges group))
        nil)))

(defn
  get-container
  "Get the container of the given 'gengroup'."
  [gengroup]
  (:container gengroup))

(defn
  make-from-group 
  [strategy-equals? uniques group]
    ;(println "! PROCESSING GROUP")
	  (group/reduce-changes
		  group
		  (fn [[old-uniques old-gengroup] change]
          ;(println "!! inject " (change/to-short-string change))
          (let [old-genchange-ids (:genchange-ids old-gengroup)
               [new-uniques genchange-id] (intern-change strategy-equals? old-uniques group change)]
           ; Check, informing the user in case of data loss
           (if (contains? old-genchange-ids genchange-id) 
               ; Should never happen
               (do (println "! DATA LOSS DUE TO ITEMSET MINING. IGNORING CHANGE" genchange-id "!")
                   [old-uniques old-gengroup])
               ; Everything ok, return
               (let [genchange (genchange/make change genchange-id)]
                 [new-uniques (Gengroup. (conj old-genchange-ids genchange-id)
		                                      (:container old-gengroup)
                                          (:changes-by-node old-gengroup)
                                          (cons genchange (:genchanges old-gengroup))
                                          (:group old-gengroup))]))))
		  [uniques (Gengroup. #{} 
                          (group/get-container group) 
                          (group/get-changes-by-node group)
                          '()
                          group)]))
  
(defn
  get-genchange-ids
  "Get the genchange-id's in the given 'gengroup'."
  [gengroup]
  (:genchange-ids gengroup))

(defn
  has-all-genchange-ids
  [gengroup genchange-ids]
  (clojure.set/subset? genchange-ids (:genchange-ids gengroup)))

(defn
  to-string
  [gengroup]
  (let [container (:container gengroup)
        container-string (if (instance? MethodDeclaration container) 
                             (.getIdentifier (.getName container))
                             (.getSimpleName (.getClass container)))]
    (str "* Gengroup " container-string "\n"
         (clojure.string/join "\n" 
                              (map-genchanges gengroup
                                              (fn [genchange] (str "    * " (genchange/to-string genchange) "\n"
                                                                   "      at " (vec (get-path-of-change gengroup genchange)))))))))
