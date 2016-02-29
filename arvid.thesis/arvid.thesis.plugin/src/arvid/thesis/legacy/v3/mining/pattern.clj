(ns arvid.thesis.plugin.clj.mining.pattern
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Pattern [support container changes-in-container])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  make
  [support container changes-in-container]
  (Pattern. support container changes-in-container))

(defn
  get-changes-in-container
  [pattern]
  (:changes-in-container pattern))

(defn
  get-container ; nil or an AST node
  [pattern]
  (:container pattern))

(defn
  to-string
  [pattern]
  (str "* Pattern with support " (:support pattern) "\n"
       (clojure.string/join "\n" 
                            (map (fn [change] (str "  * " (change/to-very-short-string change)))
                                 (:changes-in-container pattern)))
       "\n"))
