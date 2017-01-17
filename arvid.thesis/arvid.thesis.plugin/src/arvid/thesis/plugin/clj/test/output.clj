(ns
  ^{:doc "Inspect the output of main/analyse-repository"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.output
  (:require 
    [arvid.thesis.plugin.clj.test.main :as main]))

(comment
  
  
  
  (let [supmap (main/repo-support-map "quicksearchbar")
        pattern (first (:patterns (get supmap 3)))
        
        
        ]
    pattern
    
    )
  )