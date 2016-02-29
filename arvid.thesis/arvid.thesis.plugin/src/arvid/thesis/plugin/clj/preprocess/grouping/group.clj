(ns arvid.thesis.plugin.clj.preprocess.grouping.group
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:import [org.eclipse.jdt.core.dom MethodDeclaration]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Group  [container changes changes-by-node])

(defn-
  update-changes-by-node
  [changes-by-node change]
  (if (nil? changes-by-node) 
      changes-by-node
      (let [target (cond (= (:operation change) :insert)
                           (if (and (:original change) (:index change))
                               (astnode/node-propertykeyword-value|reified (change/get-original change) (:property change))
                               (change/get-original change))
                         :else
                           (change/get-original change))]
			  (if (contains? changes-by-node target)
			      (assoc changes-by-node target change)
			      changes-by-node))))

(defn-
  build-nodes-map
  [container]
  (cond (nil? container) 
          nil
        :else
				  (let [changes-by-container (atom {})]
				    (util/tree-dft container
				                   (fn [node]
				                     (swap! changes-by-container assoc node nil)))
				    @changes-by-container)))

(defn-
  get-root-insert-of-insert
  [group change]
  (loop [current (:copy change)]
	   (if (nil? current)
         (do (println "! get-root-insert-of-insert RETURNS NIL, SHOULD NOT HAPPEN!") nil) ; should not happen
         (let [corrent-change (first (filter (fn [change] (and (= (:copy change) current) (not (nil? (:original change)))))
                                             (:changes group)))]
		       (if corrent-change
		           corrent-change
		           (recur (astnode/owner current)))))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  get-container
  "Get the container of the given group."
  [group]
  (:container group))

(defn 
  make
  "Create an empty group (containing no changes) for given container (ASTNode or nil)."
  [container]
  (Group. container '() (build-nodes-map container)))

(defn
  add-change
  "Add a change to a group, returning the resulting group."
  [group change]
  (Group. (:container group)
          (cons change (:changes group))
          (update-changes-by-node (:changes-by-node group) change)))

(defn
  reduce-changes
  [group f val]
  (reduce f val (reverse (:changes group)))) ; keep order of changes, inserts-in-inserts have to occur after corresponding root inserts

(defn get-changes-by-node
  [group]
  (:changes-by-node group))

(defn
  contains-parent-insert-of-insert 
  [group child-change]
  (letfn [(is-child-of 
           [node parent]
           (loop [current node]
						  (if (nil? current)
						      false
						      (if (= current parent)
						          true
						          (recur (astnode/owner current))))))]
    (some true? 
          (map (fn [change] (is-child-of (:copy child-change) (:copy change)))
               (:changes group)))))

(defn
  get-path-of-change
  [group change]
  (let [group-container (get-container group)]
    (cond (= (:operation change) :update)
	          (util/get-path-between-nodes (change/get-original change) group-container)
	        (= (:operation change) :delete)
	          (util/get-path-between-nodes (change/get-subject change) group-container) 
	        (= (:operation change) :move)
	          (util/get-path-between-nodes (change/get-subject change) group-container) 
	        (= (:operation change) :insert)
	          (let [property (:property change)
	                index (:index change)
	                original (change/get-original change)]
	            (concat (if (nil? original) 
	                        (let [root-insert (get-root-insert-of-insert group change)
                                root-insert-property (:property root-insert)
							                  root-insert-index (:index root-insert)
							                  root-insert-original (change/get-original root-insert)]
                             (concat (util/get-path-between-nodes root-insert-original group-container) ; path of root-insert-original to the group-container
                                     (util/get-path-between-nodes (:left-parent change) (:left-parent root-insert)))) ; path of left-parent of the change to the left-parent of root-insert
	                        (util/get-path-between-nodes original group-container))
	                    (cons property (if (nil? index) index (cons index nil))))))))

(defn
  to-string
  [group]
  (let [container (:container group)
        changes (:changes group)
        container-string (cond (nil? container) 
                                 "*ignored-changes*"
                               (instance? MethodDeclaration container)
                                 (.getIdentifier (.getName container))
                               :else
                                 (.getSimpleName (.getClass container)))]
    (str "* Group " container-string "\n"
         (clojure.string/join "\n" 
                              (map (fn [change] (str "    * " (change/to-short-string change) ))
                                   changes)))))