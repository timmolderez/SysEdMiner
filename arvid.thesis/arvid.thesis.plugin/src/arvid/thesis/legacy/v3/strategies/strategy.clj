(ns arvid.thesis.plugin.clj.strategies.strategy)

(defrecord Strategy 
  [equals? get-group-container get-group-container-old make-lhs])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  make
  [equals? get-group-container get-group-container-old make-lhs]
  (Strategy. equals? get-group-container get-group-container-old make-lhs))
  
(defn
  equals?
  [strategy change1 change2]
  ((:equals? strategy) change1 change2))

(defn
  get-group-container
  [strategy change]
  ((:get-group-container strategy) change))

(defn
  get-group-container-old
  [strategy get-group-container-of-group-containing-change-with-copy change]
  ((:get-group-container-old strategy) get-group-container-of-group-containing-change-with-copy change))

(defn
  make-lhs
  [strategy container changes-in-container]
  ((:make-lhs strategy) container changes-in-container))
