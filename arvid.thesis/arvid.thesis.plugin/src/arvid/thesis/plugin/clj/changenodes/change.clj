(ns arvid.thesis.plugin.clj.changenodes.change
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:import [changenodes.matching NodeClassifier]))

; * Known bugs: moves between methods: translate 'em to delete/inserts?

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  get-original
  [change]
  (:original change))

(defn
  get-subject
  [change]
  (cond (= (:operation change) :delete) 
          (:original change) ; Left, what we removed
        (= (:operation change) :update)
          (:copy change) ; Left, what we updated
        (= (:operation change) :insert) 
          (:copy change) ; Right, what we inserted
        (= (:operation change) :move) 
          (:original change))) ; Right, what we moved

(defn 
  to-very-short-string
  "Converts a changenodes change to a very short string for informational purposes."
  [change]
  (let [node (get-subject change)
        operation (:operation change)
        type (cond (nil? node) "unknown"
						       (astnode/value? node) (.getSimpleName (.getClass (astnode/value-unwrapped node)))
						       :else (.getSimpleName (.getClass node)))]
    (str operation " of " type)))

(defn 
  to-short-string
  "Converts a changenodes change to a relatively short string for informational purposes."
  [change]
  (let [node (get-subject change)]
    (str (format "%-30s" (to-very-short-string change)) 
         "<=  " 
         (util/node-to-oneliner node))))

(defn 
  to-string
  "Converts a changenodes change to a string for informational purposes."
  [change]
  (letfn [(node-to-string [node]
            (if node
                (format "%-30s%s"
                        (.getSimpleName (.getClass node)) 
                        (util/node-to-oneliner node))
                "-"))]
    (let [index (:index change)]
	    (str "* " (:operation change) "\n"
	         "    PROPERTY " (:property change) "\n"
	         "    INDEX    " (if index index "-") "\n"
	         "    ORIGINAL " (node-to-string (:original change) ) "\n"
	         "    COPY     " (node-to-string (:copy change)) "\n"
	         "    LEFTP    " (node-to-string (:left-parent change)) "\n"
	         "    RIGHTP   " (node-to-string (:right-parent change))))))