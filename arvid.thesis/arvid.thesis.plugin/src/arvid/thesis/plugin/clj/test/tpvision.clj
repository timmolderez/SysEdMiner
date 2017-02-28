(ns 
  ^{:doc "Frequent change pattern analysis of the TP Vision data set"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.tpvision
  (:require 
    [arvid.thesis.plugin.clj.git.repository :as repo]
    [arvid.thesis.plugin.clj.strategies.strategyFactory :as stratfac]
    [arvid.thesis.plugin.clj.test.main :as main]
    [arvid.thesis.plugin.clj.test.output :as output]))

(def tpvision-repos
  ; Retrieve a list of all paths to TP Vision's git repositories (on the local filesystem)
  (memoize 
    (fn []
      (let [root-path "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/"
            rdr (clojure.java.io/reader (str root-path "repos.txt"))]
        (for [line (line-seq rdr)]
          (str root-path line))))))


(def default-strategy (stratfac/make-strategy))
(def custom-strategy
    (stratfac/make-strategy 
      :CompilationUnit
      #{:equals-operation-fully? :equals-subject-structurally? :equals-context-path-exact?}))
(def git-path (nth (tpvision-repos) 30))

(defn average
  [numbers]
  (if (= (count numbers) 0)
    0
    (/ (apply + numbers) (count numbers)))
)

(comment
  
  (for [repo-path (tpvision-repos)]
    (let [repo-name (main/repo-name-from-path repo-path)]
      (if (.exists (clojure.java.io/as-file (str main/output-dir repo-name)))
        (let []
          (try 
            (output/generate-sample-systematic-edits repo-path 1)
            (catch Exception e (println "!")))))))
  
  ; Count commits for all projects
  (for [repo-path (tpvision-repos)]
    (let [repo-name (main/repo-name-from-path repo-path)]
      (if (.exists (clojure.java.io/as-file (str main/output-dir repo-name)))
        (let [] 
          (println repo-name)
          (println (count (repo/get-commits repo-path)))))))
  
  (int (count (remove nil? (for [repo-path (tpvision-repos)]
                          (let [repo-name (main/repo-name-from-path repo-path)]
                            (if (.exists (clojure.java.io/as-file (str main/output-dir repo-name)))
                              (count (repo/get-commits repo-path))))))))
 
  
  
  
  (count (repo/get-commits git-path))
  (main/repo-name-from-path git-path)
  
  (doseq [gp (tpvision-repos)]
    (println gp)
;    (println (count (repo/get-commits gp)))
    )
  
  (def default-strategy (stratfac/make-strategy))
  
  
  (inspector-jay.core/inspect (tpvision-repos))
  
  ; Analyse multiple repos in parallel (Can produce a garbled mess in the console, but that's ok :) )
  (pmap 
    (fn [x] 
      (damp.ekeko.snippets.util/future-group 
       nil 
       (main/analyse-repository (nth (tpvision-repos) x) (stratfac/make-strategy))))
    (range 0 75))
  
  ; Non-parallel
  (map 
    (fn [x] 
      (main/analyse-repository (nth (tpvision-repos) x) (stratfac/make-strategy)))
    (range 0 10))
  
  
  ; Analyse an entire repository (in a separate thread)
  (damp.ekeko.snippets.util/future-group nil 
    (main/analyse-repository git-path default-strategy)) 
  
  ; Pretty-print the entire support map
  (def supp-map (main/repo-support-map (main/repo-name-from-path git-path)))
  
  (/ (with-open [rdr (clojure.java.io/reader "/Users/soft/Desktop/timing-m.txt")]
           (reduce (fn [cur y] (+ cur (java.lang.Double/parseDouble y))) 0 (line-seq rdr))) 1000)
  
  ;!!!!!!!!!!!!!!!!!
  (inspector-jay.core/inspect supp-map)
  
  (let [massive-maps
        (for [repo-path (tpvision-repos)]
        (let [repo-name (main/repo-name-from-path repo-path)]
          (if (.exists (clojure.java.io/as-file (str main/output-dir repo-name )))
        
        
            (let [supmap (main/repo-support-map repo-name)
             
                  output
                  (for [support (sort (keys supmap))]
                    (let [val (get supmap support)
                          lengths (for [pattern (get val :patterns)]
                                                     (count (output/changepath-intersection pattern)))
                          ]
                      [(:count val) lengths]
                  
                      ))
                  output-map (zipmap (sort (keys supmap)) output)
                  ]
              ;(println output-map)
              output-map
              ))))
        
        reduced-maps (reduce (fn [map1 map2]
                               (reduce (fn [current support]
                                         (let [v1 (get current support)
                                               v1_ (if (nil? v1) [0 []] v1)
                                               v2 (get map2 support)
                                               ;tmp (println v1_)
                                               new-count (concat (second v1) (second v2))
                                               
                                               ]
                                           (assoc current support [0 new-count]))
                                         
                                         ) map1 (keys map2))
                               
                               ) massive-maps)]
    (doseq [support (sort (keys reduced-maps))]
      (println support "--" 
               (clojure.string/join "," (second (get reduced-maps support)))               ))
    )
  
  
  (let [massive-maps
        (for [repo-path (tpvision-repos)]
        (let [repo-name (main/repo-name-from-path repo-path)]
          (if (.exists (clojure.java.io/as-file (str main/output-dir repo-name )))
        
        
            (let [supmap (main/repo-support-map repo-name)
             
                  output
                  (for [support (sort (keys supmap))]
                    (let [val (get supmap support)
                          avg (double (average (for [pattern (get val :patterns)]
                                                 (count (output/changepath-intersection pattern)))))
                          max (apply max (cons 0 (for [pattern (get val :patterns)]
                                                  (count (output/changepath-intersection pattern)))))
                          ]
                      [(:count val) avg max]
                  
                      ))
                  output-map (zipmap (sort (keys supmap)) output)
                  ]
              ;(println output-map)
              output-map
              ))))
        
        reduced-maps (reduce (fn [map1 map2]
                               (reduce (fn [current support]
                                         (let [v1 (get current support)
                                               v1_ (if (nil? v1) [0 0 0] v1)
                                               v2 (get map2 support)
                                               ;tmp (println v1_)
                                               new-count (+ (first v1_) (first v2))
                                               new-avg (+ 
                                                         (* (/ (first v1_) new-count) (second v1_))
                                                         (* (/ (first v2) new-count) (second v2)))
                                               
                                               bad-avg (average [(second v1_) (second v2)])
                                               new-max (max (nth v1_ 2) (nth v2 2))
                                               ]
                                           (assoc current support [new-count new-avg new-max]))
                                         
                                         ) map1 (keys map2))
                               
                               ) massive-maps)]
    (doseq [support (sort (keys reduced-maps))]
      (println support "," 
               (first (get reduced-maps support)) ","
               (second (get reduced-maps support)) ","
               (nth (get reduced-maps support) 2)))
    )
  
  
  
  
  
  (doseq [support (sort (keys supp-map))]
    (let [val (get supp-map support)
;          tmp (println (keys val))
          avg (double (average (for [pattern (get val :patterns)]
                                           (let []
;                                        (println (count (output/changepath-intersection pattern)))
                                        (count (output/changepath-intersection pattern))))))
          max (apply max (for [pattern (get val :patterns)]
                                      (let []
;                                        (println (count (output/changepath-intersection pattern)))
                                        (count (output/changepath-intersection pattern)))))
          ]
      (println support "," (:count val) "," avg "," max)))
  
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