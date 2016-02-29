(ns arvid.thesis.plugin.clj.preprocessOld.store
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change])
  (:require [arvid.thesis.plugin.clj.preprocessOld.group :as group])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [arvid.thesis.plugin.clj.util :as util]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Store  [groups uniques])

(defn-
  get-unique-index
  "get a numeric index of a the given unique change within the store"
  [store unique]
  ; Note we consed' items to the front of uniques for performance reasons, we're recalculating the indices to compensate.
  (- (count (:uniques store)) (util/index-of unique (:uniques store))))

; Helper data structure for store instances
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Instance  [group change unique])

(defn-
  instance-make
  "Create a new instance."
  [group change unique]
  (Instance. group change unique))

(defn-
  instance-get-group
  "Get the group of given instance."
  [instance]
  (:group instance))

(defn-
  instance-get-change
  "Get the underlying (changenodes) change of the given instance."
  [instance]
  (:change instance))

(defn-
  instance-get-unique
  "Get the, according to the equality strategy, interned equivalent of the underlying change."
  [instance]
  (:unique instance))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  make
  []
  (Store. (hash-map) '()))

; Mutations
;;;;;;;;;;;

(defn
  intern-change
  "Intern a change, returning the a vector [new-store unique]."
  [store strategy-equals? change]
  (let [uniques (:uniques store)
        matches (filter (fn [unique] (strategy-equals? change unique)) uniques)]
    (if (empty? matches)
        [(Store. (:groups store) (cons change uniques)) change] ; Not in the list, add it
        [store (first matches)]))) ; Already in the list, nothing to be done

(defn
  find-or-create-group
  "Ensure a group exists in the store for the given container (AST node or nil). Returns the new store and the group
   of the container."
  [store container]
  (let [groups (:groups store)
        new-groups (if (contains? groups container)
                       groups
                       (assoc groups container (group/make container)))
        group (get new-groups container)]
    [(Store. new-groups (:uniques store)) group]))

(defn
  add-instance
  "Add a new instance (with given group, change and unique) to the store. This expects group to be an existing group
   in the store and unique an interned change of the store." 
  [store group change unique]
  (update-in store 
             [:groups (group/get-container group)] 
             (fn [group] (group/add-instance group 
                                             (instance-make group change unique)))))

; Accessors
;;;;;;;;;;;



(defn
  get-unique-indices-by-group
  "get the list of indices of the uniques in the given group"
  [store group]
  (let [numbers (sort (group/map-instances group (fn [instance] (get-unique-index store (instance-get-unique instance)))))
        unique-numbers (util/distinct-consequtive numbers)]
    ; Inform the user of data loss
    (when (not (= numbers unique-numbers)) 
      (println "! DATA LOSS DUE TO ITEMSET MINING: " numbers "<=>" unique-numbers))
    ; Return result
    unique-numbers))

(defn
  get-unique-by-index 
  "Get a unique by its index."
  [store unique-index]
  (nth (:uniques store) (- (count (:uniques store)) unique-index)))

(defn
  get-groups-with-uniques 
  "Get all groups with the given uniques."
  [store uniques]
  (filter (fn [group] 
            (let [uniques-in-group (group/map-instances group instance-get-unique)]
              (every? (fn [unique] (some #(= unique %) uniques-in-group)) uniques)))
          (vals (:groups store))))

(defn
  get-group-changes-by-uniques
  [store group uniques]
  (map (fn [instance] (instance-get-change instance))
       (filter (fn [instance] (some #(= (instance-get-unique instance) %) uniques))
               (group/map-instances group identity))))

(defn
  get-group-container-of-group-containing-change-with-copy
  [store node]
  (letfn [(test-instance [instance]
            (loop [current node]
						  (if (nil? current)
						      false
						      (if (= current (:copy (instance-get-change instance)))
						          true
						          (recur (astnode/owner current))))))]
	  (group/get-container 
	    (first
		    (filter (fn [group] (some true? (group/map-instances group test-instance)))
		            (vals (:groups store)))))))
