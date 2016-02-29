(ns arvid.thesis.plugin.clj.strategies.strategy
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change]))
(defrecord Strategy 
  [equals? get-group-container make-lhs])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  make
  [equals? get-group-container make-lhs]
  (Strategy. equals? get-group-container make-lhs))
  
(defn
  equals?
  [strategy group1 change1 group2 change2]
  ((:equals? strategy) group1 change1 group2 change2))

(defn
  get-group-container
  [strategy change]
  ((:get-group-container strategy) change))

(defn
  make-lhs
  [strategy pattern]
  ((:make-lhs strategy) pattern))
