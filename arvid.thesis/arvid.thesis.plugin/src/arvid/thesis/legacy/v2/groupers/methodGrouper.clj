(ns arvid.thesis.plugin.clj.strategies.groupers.methodGrouper
  (:use damp.ekeko)
  (:use damp.ekeko.jdt.ast)
  (:require [clojure.core [logic :as l]])
  (:require [damp.ekeko [logic :as el]]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defn 
  ast-methoddeclaration
  [?ast ?m] 
   (l/fresh [?parent]
     (ast :MethodDeclaration ?ast) 
     (l/== ?ast ?m)))

(defn
  run
  [change]
  "Group them all together by parent method declaration. Changes not belonging to a parent method are grouped via descriptor nil."
  (let [; Determine the parent AST
        parent-ast (if (= (:operation change) :delete)
                       (.getParent (:original change)) ; right-parent is not set for node removals, use original's group instead.
                       (:right-parent change)) ; This isn't a node removal, just take the right parent
        ; Find the containing method by walking up the tree as long as necessary.
        result (ekeko [?method]
                  (l/conda [(ast-methoddeclaration parent-ast ?method)]
                           [(ast-methoddeclaration|encompassing parent-ast ?method)]))]
		(if (empty? result)
        nil
        (first (first result)))))


(defn
  to-string
  [descriptor]
  (if (nil? descriptor)
      "-"
      (.getIdentifier (.getName descriptor))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(def definition {:name "By parent method declaration", :run run, :to-string to-string})
