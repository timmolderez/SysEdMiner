(ns arvid.thesis.plugin.clj.preprocessOld.preprocess
  (:require [arvid.thesis.plugin.clj.preprocessOld.store :as store])
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn- 
  preprocess-change
  "Create a new store based on the given 'store', with the new 'change' inserted after generalization/grouping.
   Uses strategy to determine grouping and generalization."
  [strategy store change]
  ; Intern the change, getting a "unique equivalent" of the change.
  (let [[store unique] (store/intern-change store (partial strategy/equals? strategy) change)]
    ; Determine the change container, create (or find) the corresponding group in the store.
    (let [container (strategy/get-group-container-old strategy (partial store/get-group-container-of-group-containing-change-with-copy store) change)
          [store group] (store/find-or-create-group store container)]
      ; Add the instance to the store.
      (store/add-instance store group change unique))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  preprocess
  "Build a store filled with given changes using the given strategy for grouping and generalization."
  [strategy changes]
  ; Process each changenodes change: add each change to the store. This also immediatly performs 
  ; generalization and grouping.
  (letfn [(empty-changes?
            [changes]
            (try 
              (empty? changes)
              (catch Exception e (do (println "! CHECKING IF THERE ARE MORE CHANGES FAILED, IGNORING.") true))))]
    (loop [current-changes changes
           store (store/make)]
      (if (empty-changes? current-changes)
          store 
          (let [new-store (preprocess-change strategy store (first current-changes))]
            (recur (rest current-changes) new-store))))))