(ns arvid.thesis.plugin.clj.strategies.groupers.list
  (:require [arvid.thesis.plugin.clj.strategies.groupers.methodGrouper :as methodGrouper])
  (:require [arvid.thesis.plugin.clj.strategies.groupers.typeGrouper :as typeGrouper])
  (:require [arvid.thesis.plugin.clj.strategies.groupers.commitGrouper :as commitGrouper])
  (:require [arvid.thesis.plugin.clj.strategies.groupers.noneGrouper :as noneGrouper]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

; A grouper takes as input a Changenodes change, and it returns a "descriptor". Changes with the same
; descriptor belong to the same group. The format of the descriptor is irrelevant for the outside world.
(def groupers [noneGrouper/definition
               methodGrouper/definition
               typeGrouper/definition
               commitGrouper/definition])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn get-definitions 
  []
  groupers)
