(ns arvid.thesis.plugin.clj.strategies.helpers.templates
  (:require [damp.ekeko.snippets.operatorsrep :as operatorsrep])
  (:require [damp.ekeko.snippets.snippetgroup :as snippetgroup])
  (:require [damp.ekeko.snippets.snippet :as snippet])
  (:require [arvid.thesis.plugin.clj.mining.pattern :as pattern])
  (:require [arvid.thesis.plugin.clj.util :as util])
  (:require [damp.ekeko.snippets.matching :as matching]))

(defn
  apply-operator-simple
  "Apply the operator defined by string 'oper' to the given 'node' in 'snippet-group'"
  [snippet-group node oper]
  (let [snippet (first (snippetgroup/snippetgroup-snippetlist snippet-group))
        operator (first (filter 
                          (fn [op] 
                            (some #{(operatorsrep/operator-id op)} [oper]))
                          (operatorsrep/registered-operators)))
        bindings (operatorsrep/make-implicit-operandbinding-for-operator-subject snippet-group snippet node operator)]
    (if (operatorsrep/applicable? snippet-group snippet node operator)
        (operatorsrep/apply-operator-to-snippetgroup snippet-group snippet node operator [bindings])
        (do "! Operator not applicable. Ignoring!"
            snippet-group))))

(defn
  make-lhs 
  [updater pattern]
  (let [snippet-group (snippetgroup/add-snippet (snippetgroup/make-snippetgroup "-") 
                                                (matching/snippet-from-node (pattern/get-container pattern)))
        new-snippet-group (atom snippet-group)
        snippet (first (snippetgroup/snippetgroup-snippetlist @new-snippet-group))]
    (updater snippet (partial swap! new-snippet-group) pattern)
    @new-snippet-group))
