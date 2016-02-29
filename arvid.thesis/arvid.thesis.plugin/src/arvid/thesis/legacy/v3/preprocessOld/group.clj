(ns arvid.thesis.plugin.clj.preprocessOld.group
  (:import [org.eclipse.jdt.core.dom MethodDeclaration]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Group  [container instances])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  make
  [container]
  "Create an empty group (containing no instances) for given container (ASTNode or nil)."
  (Group. container '()))

(defn
  add-instance
  [group instance]
  "Add an instance to a group, returning the resulting group."
  (Group. (:container group) (cons instance (:instances group))))

(defn
  get-container
  [group]
  "Get the container of the given group."
  (:container group))

(defn
  map-instances
  [group f]
  (map f (:instances group)))

(defn
  to-string
  [group]
  (let [container (:container group)]
    (cond (nil? container) 
            "*ignored-changes*"
          (instance? MethodDeclaration container)
            (.getIdentifier (.getName container))
          :else
            (.getSimpleName (.getClass container)))))