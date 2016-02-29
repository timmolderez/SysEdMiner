(ns arvid.thesis.plugin.algorithm.negara
  (:require [damp.ekeko.jdt [astnode :as astnode]])
  (:use [arvid.thesis.plugin.algorithm [orderedMap :as orderedMap]])
  (:use [arvid.thesis.plugin.algorithm.difference :only (change-toString)]))

(def ^:dynamic *max-pattern-length* 4)
(def ^:dynamic *window-size* (* *max-pattern-length*  2))

(defn 
  writeln 
  [& args] 
  (.println (.getConsoleStream (arvid.thesis.plugin.ThesisPlugin/getDefault)) (apply str args)))

(defn 
  write 
  [& args] 
  (.print (.getConsoleStream (arvid.thesis.plugin.ThesisPlugin/getDefault)) (apply str args)))

; Transaction database: a set of transactions
; Here represented in VDF, as a set of tuples <change, tidset> with tidset a set
; of transaction identifiers that contain the corresponding item
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; The database
(defrecord 
  Tdb
  [entries num-adds])

(defn
  make-tdb
  []
  (Tdb. (orderedMap/ordered-map) 0))


(defn 
  add-tid-to-entry
  [entry tid occurrence-number]
  (if (contains? entry tid)
    (assoc entry tid (conj (get entry tid) occurrence-number))
    (assoc entry tid (sorted-set occurrence-number))))

(defn 
  add-tids-to-entry
  [entry tids occurrence-number]
  (write "- occurrence " occurrence-number ": add tids " tids " to entry " (if (nil? entry) "nil" entry))
  (loop [current-entry entry
         current-tid (first tids)
         rest-tids (drop 1 tids)]
    (let [new-entry (add-tid-to-entry (if (nil? current-entry) 
                                          (sorted-map) 
                                          current-entry) 
                                      current-tid 
                                      occurrence-number)] ; Intersect the tidsets
      (if (> (count rest-tids) 0)
        (recur new-entry
               (first rest-tids)
               (drop 1 rest-tids))
        new-entry))))

(defn
  tdb-add-item
  [tdb item]
  (let [entries (:entries tdb)
        num-adds (:num-adds tdb)
        tids (if (< num-adds *max-pattern-length*)
                 (list 1)
                 (list (+ (quot num-adds *window-size*) 1), (+ (quot num-adds *window-size*) 2)))]
    (let [new-entry (add-tids-to-entry (get entries item) tids num-adds)
          new-entries (assoc entries item new-entry)]
       (writeln " => " new-entry)
			 (Tdb. new-entries (+ num-adds 1)))))

(defn
  tdb-toString
  [tdb]
  (reduce 
    (fn [prev new] 
      (format "%s%-30s => %s\n" 
              prev 
              (change-toString (get new 0)) 
              (get new 1)))
    "TDB: \n" 
    (:entries tdb)))

; The algorithm
;;;;;;;;;;;;;;;

(defn
  iter
  [itemset tidset remaining-entries depth]
  (writeln (str depth " #{" (reduce #(str %1 "(" (change-toString %2) ") ") "" itemset) "} ->" tidset))
  (if (> (count remaining-entries) 0)
    (loop [next-entry (first remaining-entries)
           rest-next-entries (drop 1 remaining-entries)]
      (let [next-iteration-tidset (clojure.set/intersection tidset (get next-entry 1))] ; Intersect the tidsets
        (if (> (count next-iteration-tidset) 1) ; Threshold
	        (iter (conj itemset (get next-entry 0)) ; Union the itemset
	              next-iteration-tidset
		            rest-next-entries
		            (str depth \*))))
       (if (> (count rest-next-entries) 0)
	       (recur (first rest-next-entries)
                (drop 1 rest-next-entries))))))

(defn
  run
  [tdb]
  (let [entries (:entries tdb)]
    (loop [current-entry (first entries)
           rest-entries (drop 1 entries)]
      (iter (hash-set (get current-entry 0)) ; Current itemset
            (get current-entry 1); Current tidset
            rest-entries ; Remaining
            "*") ; Depth (purely for visualisation)
      (if (> (count rest-entries) 0)
	      (recur (first rest-entries)
	             (drop 1 rest-entries))))))

;;;;;;;;;;;
; Main code
;;;;;;;;;;;

(defn
  run-negara
  [changes] 
  (let [tdb (reduce tdb-add-item (make-tdb) changes)]
    ; Print the tdb
    (writeln (tdb-toString tdb))))
    ; Run the algoithm
    ;(run tdb)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Bridge with the Java worlds
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

 (defn
  register-callbacks
  []
  (set! (arvid.thesis.plugin.ClojureBridge/FN_RUN_NEGARA) run-negara))

(register-callbacks)
