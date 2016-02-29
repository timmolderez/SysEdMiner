(ns arvid.thesis.plugin.clj.strategies.strategyFactory
  (:require [arvid.thesis.plugin.clj.strategies.strategy :as strategy])
  (:require [damp.ekeko.jdt.astnode :as astnode])
  (:require [damp.ekeko.snippets.snippet :as snippet])
  (:require [damp.ekeko.snippets.snippetgroup :as snippetgroup])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.equalities :as equalities])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.grouping :as grouping])
  (:require [arvid.thesis.plugin.clj.strategies.helpers.lhsConstruction :as lhsConstruction])
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:require [arvid.thesis.plugin.clj.changenodes.change :as change]))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn
  make-strategy 
  ([]
   (make-strategy :MethodDeclaration :equals-operation-fully? :equals-fully? :equals-context-path-exact?))
  ([group-container-type operation-equality subject-equality context-equality]
	  (let [operation-equality-fn (equalities/get-operation-equality operation-equality)
          subject-equality-fn (equalities/get-subject-equality subject-equality)
          context-equality-fn (equalities/get-context-equality context-equality)]
     
	    ; Used for grouping
			(defn
			  get-group-container-old
			  [get-group-container-of-group-containing-change-with-copy change]
			  "Group them all together by parent method declaration. Changes not belonging to a parent method are grouped via descriptor nil."
			  (if (change/get-left change)
			      (grouping/get-ancestor-of-type group-container-type (change/get-left change))
			      (get-group-container-of-group-containing-change-with-copy (:left-parent change)))) ; Nasty inserts inside inserts do not have a "left"... but the insert containing them does...
			
			(defn
			  get-group-container
        "Returns an AST, nil (no group container) or :unknown (couldn't determine with the information I have)"
        [change]
        (if (change/get-original change)
            (grouping/get-ancestor-of-type group-container-type (change/get-original change))
            :unknown))
   
   
		  ; Used for generalization
			(defn
			  generalizer-equals
			  "Perform generalization based on passed equalities (operation, subject and context)"
			  [change1 change2]
			  (let [node1 (change/get-subject change1)
						  node2 (change/get-subject change2)]
			    (and (operation-equality-fn (:operation change1) (:operation change2))
						   (subject-equality-fn node1 node2)
			         (context-equality-fn group-container-type node1 node2))))
	  
			; Used for template generation
			(defn
			  make-lhs 
			  [snippet-group changes]
			  (let [new-snippet-group (atom snippet-group)
			        snippet (first (snippetgroup/snippetgroup-snippetlist @new-snippet-group))]
			    (letfn
			      [(recursive-descent
			         [snippet current path]
			         (cond (snippet/snippet-value-list? snippet current)
			                                        (let [lst (snippet/snippet-value-list-unwrapped snippet current)
			                                              count (count lst)]
			                                          (map-indexed (fn [index child] (snippet-traveller snippet child (cons (- (- count 1) index) path)))
			                                                       (reverse lst)))
			                                      (snippet/snippet-value-node? snippet current)
			                                        (map (fn [child] (snippet-traveller snippet child (cons (astnode/ekeko-keyword-for-property-descriptor (astnode/owner-property child)) path))) 
			                                             (snippet/snippet-node-children snippet current))
			                                      :else
			                                        [false]))
			       (snippet-traveller
			         [snippet current path]
			         (let [change (lhsConstruction/find-corresponding-change snippet path changes)]
			           (cond (and change (= (:operation change) :insert)) ; NOTE: current now is the PARENT of where the insert happened
			                      (do ;(swap! new-snippet-group util/apply-operator-simple current "remove-node")
			                          (let [recres (recursive-descent snippet current path)]
							                    (some true? (doall recres))))
			                 (and change (or (= (:operation change) :delete) 
			                                 (= (:operation change) :update)))
			                      (do ; (println "UPDATE OR DELETE")
			                          true) 
			                 (and change (= (:operation change) :move))
			                      (do ;(println "MOVE")
			                          true) 
			                 :else
			                   (let [recres (recursive-descent snippet current path)]
			                     (if (some true? (doall recres))
			                         true
			                         (do (swap! new-snippet-group util/apply-operator-simple current "replace-by-wildcard")
			                             false))))))]
					   (snippet-traveller snippet (snippet/snippet-root snippet) '())
				     @new-snippet-group)))
			
		    ; Export the strategy
			  (strategy/make generalizer-equals get-group-container get-group-container-old make-lhs))))
		
