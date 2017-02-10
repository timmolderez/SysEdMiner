(ns 
  ^{:doc "Some helper functions to set up different data sets (Eclipse SWT bug data set, FODFIN systematic changes)"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.experiments
  (:require 
    [arvid.thesis.plugin.clj.git.repository :as repo]
    [arvid.thesis.plugin.clj.strategies.strategyFactory :as stratfac]
    [arvid.thesis.plugin.clj.test.tpvision :as experiments]
    [clojure.java.shell :as sh]
    [arvid.thesis.plugin.clj.test.main :as main]))

(def eclipse-swt-path "/Users/soft/Documents/Github/eclipse.platform.swt/")

; Systematic bug fixes, as identified in the LASE paper (ICSE 2013)
(def swt-systematic-fixes
  ["114007"
   "139329"
   "142947"
   "91937"
   "103863"
   "129314"
   "134091"
   "139329"
   "139329" ; Appears twice in the paper? I suppose two different systematic changes in one bug ID..
   "142947"
   "76182"
   "77194"
   "86079"
   "95409"
   "97981"
   "74139"
   "76391"
   "89785"
   "79107"
   "86079"
   "95116"
   "98198"])

(defn open-bug-webpage [bug-id]
  (clojure.java.browse/browse-url (str "https://bugs.eclipse.org/bugs/show_bug.cgi?id=" bug-id)))

(defn get-commits-of-bug [bug-id]
  (let [cmd (:out (sh/sh "git" "log" "--all" (str "--grep=" bug-id)  :dir eclipse-swt-path))]
    (reduce
      (fn [commits line]
        (if (.startsWith line "commit")
          (conj commits (subs line 7))
          commits))
      []
      (clojure.string/split-lines cmd))))

(defn fodfin-translate-commit 
  [commit-id src-repo tgt-repo]
  (let [commit-date-line 
        (nth (clojure.string/split-lines
                (:out (sh/sh "git" "show"  commit-id  :dir src-repo)))
              2)
        
        commit-date (subs commit-date-line 7)
        tmp (println commit-date)
        commit-in-tgt
        (:out (sh/sh "git" "log" 
                     (str "--after=\"" commit-date "\"") 
                     (str "--before=\"" commit-date "\"")
                     :dir tgt-repo))]
    (first (clojure.string/split-lines commit-in-tgt))))

(defn commit-messages [repo id]
  (first (clojure.string/split-lines 
                              (:out (sh/sh "git" "log" "--format=%B" "HEAD" :dir repo))))
  )

(defn all-commit-messages [repo]
  (let [first-commit (first (clojure.string/split-lines 
                              (:out (sh/sh "git" "rev-list" "--max-parents=0" "HEAD" :dir repo))))
        all-messages (:out (sh/sh "git" "log" "--pretty=oneline" (str first-commit "...HEAD") :dir repo))]
    (clojure.string/split-lines all-messages)))

(defn tax-code-map [repo]
  (reduce 
    (fn [tax-map line]
      (let [split (clojure.string/split line #" ")
            commit (nth split 0)
            code (re-find #"\d\d\d\d\d" (clojure.string/join (rest split)))]
        (if (not (nil? code))
          (let [old-val (get tax-map code)
                commit-date 
                (first (clojure.string/split-lines 
                         (:out (sh/sh "git" "show" "-s" "--format=%ci" commit :dir repo))))
                
                new-val (if (nil? old-val)
                          [1 [commit-date]]
                          [(inc (first old-val)) (conj (second old-val) commit-date)])] 
            (assoc tax-map code new-val))
          tax-map)))
    {}
    (all-commit-messages repo)))


(comment
  (all-commit-messages "/Users/soft/Documents/Github/calcul_fast_2015")
  (inspector-jay.core/inspect (tax-code-map "/Users/soft/Documents/Github/calcul_fast_2015"))
  
  (open-bug-webpage (first swt-systematic-fixes))
  (get-commits-of-bug (first swt-systematic-fixes))
  
  (all-commit-messages "/Users/soft/Documents/Github/calcul_fast_2015")
  
  (fodfin-translate-commit 
    "bb2f3992a8d88eb04164a3ce86d4ccbbd3b4c3c1"
    "/Users/soft/Documents/Github/calcul_fast_2015" 
    "/Users/soft/Documents/Github/calcul_2015_full")
  (main/open-commit "/Users/soft/Documents/Github/calcul_fast_2015/.git" "3e12e5f8f6d74ab28dc5cd49cee6c7a1902f028e")
  (main/open-commit "/Users/soft/Documents/Github/calcul_2015_full/.git" "ef28f2abec9a4af7efa3d671f680f1f35d0ac328")
  
  (def git-path (nth (tpvision-repos) 68))
  (count (repo/get-commits git-path))
  (repo-name-from-path git-path)
  
  (inspector-jay.core/inspect (tpvision-repos))
  
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