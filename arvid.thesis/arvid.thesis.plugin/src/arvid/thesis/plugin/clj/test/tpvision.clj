(ns 
  ^{:doc "Frequent change pattern analysis of the TP Vision data set"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.tpvision
  (:require 
    [arvid.thesis.plugin.clj.git.repository :as repo]
    [arvid.thesis.plugin.clj.strategies.strategyFactory :as stratfac]
    [arvid.thesis.plugin.clj.test.main :as main]))

(def tpvision-repos
  ; Retrieve a list of all paths to TP Vision's git repositories (on the local filesystem)
  (memoize 
    (fn []
      (let [root-path "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/"
            rdr (clojure.java.io/reader (str root-path "repos.txt"))]
        (for [line (line-seq rdr)]
          (str root-path line))))))

(comment
  (def git-path (nth (tpvision-repos) 68))
  (count (repo/get-commits git-path))
  (repo-name-from-path git-path)
  
  ; Analyse multiple repos in parallel (Can produce a garbled mess in the console, but that's ok :) )
  (pmap 
    (fn [x] 
      (damp.ekeko.snippets.util/future-group 
       nil 
       (main/analyse-repository (nth (tpvision-repos) x) (stratfac/make-strategy))))
    (range 30 34))
  
  ; Analyse an entire repository (in a separate thread)
  (damp.ekeko.snippets.util/future-group nil 
    (main/analyse-repository git-path (stratfac/make-strategy))) 
  
  ; Pretty-print the entire support map
  (def supp-map (main/repo-support-map git-path))
  
  (doseq [support (sort (keys supp-map))]
    (let [val (get supp-map support)]
      (println support "," (:count val) "," (double (:avg-length val)) "," (:max-length val))))
  
  (inspector-jay.core/inspect supp-map)
  
  ; Open up all commits of a certain support level
  (doseq [commit (:commits (get supp-map 15))]
    (open-commit git-path commit))

  ; Find commit with a certain message
  (.indexOf (repo/get-commits git-path)
    (first (repo/get-commits git-path (fn [msg] (.startsWith msg "Dapq Face recognisation change")))))
  
  (inspector-jay.core/inspect (repo/get-commits git-path))
  
  (def changes (main/get-changes-in-commit (nth (repo/get-commits git-path) 7) (fn [f] true) 2))
  (inspector-jay.core/inspect changes)
  
  ; Get the changes of a specific commit
  (def changes (get-changes-by-commit-id git-path "42bea6059847ef149c6296de315ca696f013a49e"))
  (damp.ekeko.jdt.astnode/path-from-root (:original (nth changes 3)))
  
  ; Analyze a range of commits
  (analyse-commits 
    git-path
    "/Users/soft/desktop/freqchanges-contd6.txt"
    (stratfac/make-strategy)
    200)
  
  (mine-commit
    (find-commit-by-id git-path "240f127f65b6814a00c51538240ff54dd1d42ee8")
    (stratfac/make-strategy)
    3
    1
    "/Users/soft/desktop/test2.txt")
  
  (analyse-commits git-path "/Users/soft/desktop/tpv-freqchanges/ambihue/patterns-0.txt"
                         (stratfac/make-strategy) 200)
  
  ; Nested insert
  ;(inspector-jay.core/inspect (locate-change-in-body (nth changes 44) changes))
  ; Nested delete
  ;(inspector-jay.core/inspect (find-root-change (nth changes 747) changes))
  
  ; Create change dependency graph
  (time (def deps (qwal/create-dependency-graph {:changes changes})))
  (qwal/changes->graph {:changes changes})
  )