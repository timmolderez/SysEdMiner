(ns arvid.thesis.plugin.clj.preprocess.grouping.groups
  (:require [arvid.thesis.plugin.clj.preprocess.grouping.group :as group]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Groups [groups-by-container])
  
(defn-
  get-container-of-insert-in-insert
  "Nasty inserts inside inserts do not have a 'left'... but the insert containing them does...
   We suppose our mother-insert is already present (which is the case), and search for it)."
  [groups change]
  (group/get-container
    (first 
      (filter (fn [group] (group/contains-parent-insert-of-insert group change))
              (vals (:groups-by-container groups))))))

(defn-
  ensure-group-exists-for-container
  "Ensure a group exists in 'groups' for the given 'container' (AST node or nil). Returns the new groups."
  [groups container]
  (let [groups-by-container (:groups-by-container groups)]
    (if (contains? groups-by-container container)
	      groups
	      (Groups. (assoc groups-by-container container (group/make container))))))

(defn- 
  map-groups-including-ignored
  "map f over the groups in the given 'groups'"
  [groups f]
  (map f (vals (:groups-by-container groups))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  make
  "Create an empty groups store (containing no groups)."
  []
  (Groups. (hash-map)))

(defn
  add-change
  [groups strategy-group-container change]
  ; Determine the group containe
  (let [container  (if (= strategy-group-container :unknown)
                       ; It's an insert inside an insert, update the group of the parent insert
                       (get-container-of-insert-in-insert groups change)
                       ; Is a group for this container already present?
                       strategy-group-container)
        groups (ensure-group-exists-for-container groups container)]
	  (update-in groups 
	             [:groups-by-container container] 
	             (fn [group] (group/add-change group change)))))

(defn 
  reduce-groups
  "Reduce the groups in 'groups'"
  [groups f val]
  (reduce f 
          val
          (filter (fn [group] ((complement nil?) (group/get-container group)))
                  (vals (:groups-by-container groups)))))

(defn
  to-string
  "Returns a string representation of 'groups'."
  [groups]
  (str (if (empty? (:groups-by-container groups))
           "! No groups"
           (clojure.string/join "\n" 
                                (map-groups-including-ignored groups group/to-string)))
       "\n"))
