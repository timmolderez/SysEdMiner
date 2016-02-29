(ns arvid.thesis.plugin.clj.strategies.generalizers.list
  (:require [arvid.thesis.plugin.clj.strategies.generalizers.negaraGeneralizer :as negaraGeneralizer])
  (:require [arvid.thesis.plugin.clj.strategies.generalizers.noneGeneralizer :as noneGeneralizer])
  (:require [arvid.thesis.plugin.clj.strategies.generalizers.arvidGeneralizer :as arvidGeneralizer]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

; A generalizer takes as input a Changenodes change, and it returns a "descriptor". Changes with the same
; descriptor belong to the same group. The format of the descriptor is irrelevant for the outside world.
(def generalizers [negaraGeneralizer/definition noneGeneralizer/definition arvidGeneralizer/definition])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn get-definitions 
  []
  generalizers)
