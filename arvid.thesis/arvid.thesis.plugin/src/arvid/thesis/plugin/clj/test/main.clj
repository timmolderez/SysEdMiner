(ns 
  ^{:doc "Sandbox to figure out how the change pattern miner works.."
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.main
  (:require 
    [arvid.thesis.plugin.clj.git.repository :as repo]
    [arvid.thesis.plugin.clj.strategies.strategyFactory :as stratfac]
    [arvid.thesis.plugin.clj.main :as main]
    [clojure.java.shell :as sh]))

(def tpvision-repos
  ; Retrieve a list of all paths to TP Vision's git repositories (on the local filesystem)
  (memoize 
    (fn []
      (let [root-path "/Volumes/Disk Image/tpv/tpv-extracted/tpvision/"
            rdr (clojure.java.io/reader (str root-path "repos.txt"))]
        (for [line (line-seq rdr)]
          (str root-path line))))))

(def output-dir 
  "/Users/soft/desktop/tpv-freqchanges/")

(defn append
  "Appends a line of text to a file"
  [filepath text]
  (spit filepath (str text "\n") :append true :create true))

(defn mine-commit
  "Look for frequent change patterns in a single commit
   @param commit        JGit Commit instance
   @param strategy      Determines how to group changes, and the equality relation between two changes
   @param verbosity     Console output verbosity level [0-3]
   @param results-path  Path to results file (created if it doesn't exist)
   @reutrn              Pair containing all distilled changes + all frequent patterns"
  [commit strategy min-support verbosity results-path]
  (let [file-filter (fn [filename] true)
        changes (main/get-changes-in-commit commit file-filter verbosity)
        patterns (main/mine-changes changes strategy min-support verbosity)]
    
    ; Update results file if any patterns are found
    (if (not (empty? (:patterns-list  patterns)))
      (do 
        (append results-path (str "CommitID:" (.toString (:jgit-commit commit))))
        (append results-path (str "CommitMsg:" (:message commit)))
        (append results-path " ")
        ; For each pattern
        (doseq [pattern (:patterns-list patterns)]
          (append results-path (str "Support:" (:support pattern)))
          ; Write change types (insert, delete, ..)
          (let [inst-changes (:changes (:group (:gengroup (first (:instances pattern)))))
                change-types (clojure.string/join " " (for [change inst-changes] (name (:operation change))))]
            (append results-path (str "ChangeTypes:" change-types))
            (append results-path " "))
          ; For each instance of the pattern
          (doseq [instance (:instances pattern)]
            (let [gengroup (:gengroup instance)
                  group (:group gengroup)
                  container (:container gengroup)]
              (append results-path (str "ContainerMethod:"
                                        (.getName (.getPackage (.getRoot container))) "."
                                        (.getName (first (.types (.getRoot container)))) "."
                                        (.getName container)))
              (append results-path (str "ChangeIDs:" 
                                        (clojure.string/join 
                                          " "
                                          (for [change (:changes group)]
                                            (.indexOf changes change)))))
              (append results-path (str "ChangeNodeTypes:" 
                                        (clojure.string/join 
                                          " "
                                          (for [change (:changes group)]
                                            (.getName (.getClass (:copy change)))))))
              (append results-path "------")
              ))
          (append results-path "======")
          )))
    [changes
     (main/mine-changes changes strategy min-support verbosity)]))

(defn analyse-commits
  "Look for change patterns in multiple commits
   @param repo-path     File path to the .git directory of a repository
   @param results-path  Path to results file (created if it doesn't exist)
   @param strategy      Preprocessing strategy (see strategyFactory.clj)
   @param start-idx     Start the analysis at the given index, and continue until the last commit"
  [repo-path results-path strategy start-idx]
  (let [all-commits (repo/get-commits repo-path)
        commit-no (count all-commits)
        commits (take-last (- commit-no start-idx) all-commits)
        min-support 3
        verbosity 1]
    (map-indexed 
      (fn [idx commit]
        (println "Processing commit" (+ start-idx idx) "/" commit-no)
        (try 
          (mine-commit commit strategy min-support verbosity results-path)
          (catch Exception e
            (do
              (println "!! Failed to process commit" (+ start-idx idx) "/" commit-no)
              (.printStackTrace e)))))
      commits)))

(defn- repo-name-from-path
  [repo-path]
  (let [split-path (clojure.string/split repo-path #"/")]
    (nth split-path (- (count split-path) 2))))

(defn analyse-repository
  "Look for change patterns across all commits in a repository"
  [repo-path strategy]
  (let [all-commits (repo/get-commits repo-path)
        commit-no (count all-commits)
        split-path (clojure.string/split repo-path #"/") 
        repo-name (nth split-path (- (count split-path) 2))
        pattern-folder (str output-dir repo-name "/")
        batch-size 200]
    (.mkdir (java.io.File. pattern-folder))
    (dotimes [i (Math/ceil (/ commit-no batch-size))]
      (let [start-commit (* i batch-size)]
        (analyse-commits repo-path (str pattern-folder "patterns-" i ".txt")
                         strategy start-commit)))))

(defn open-commit
  "Retrieve the diff of a commit, store it to file, and open it in a text editor"
  [repo-path commit]
  (let [diff (:out (sh/sh "git" "show" commit :dir (str repo-path "/..")))
        file-path (str output-dir "commits/" commit ".diff")]
    (spit file-path diff)
    (sh/sh "open" "-a" "/Applications/Sublime Text 2.app" (str commit ".diff") :dir (str output-dir "commits/"))))

(defn build-support-map [init-support-map results-path]
  "Produce a support map, which maps each support level to
   various information regarding all frequent patterns that have this support level.
   @param init-support-map  The produced support map is merged with this one.
   @param results-path      Path to a file produced by analyse-commits"
  (with-open [rdr (clojure.java.io/reader results-path)]
    (let [lines (line-seq rdr)]
      (loop [line (first lines) 
             rest-lines (rest lines) 
             support-map init-support-map
             commit nil]
        (if (empty? rest-lines)
          support-map
          (cond
            ; Update the current commit ID
            (.startsWith line "CommitID")
            (let [new-commit (second (clojure.string/split line #" "))]
              (recur (first rest-lines) (rest rest-lines) support-map new-commit))
            
            ; Update the support map
            (.startsWith line "Support")
            (let [supp (java.lang.Integer/parseInt (second (clojure.string/split line #":")))
                  length (count (clojure.string/split (first rest-lines) #" ")) ; Count number of entries in the ChangeTypes: line
                  cur-val (if (nil? (get support-map supp))
                            {:commits #{} :count 0 :avg-length 0 :max-length 0}
                            (get support-map supp))
                  cnt (:count cur-val)
                  new-val {:commits (conj (:commits cur-val) commit)
                           :count (inc cnt)
                           :avg-length (if (= cnt 0)
                                         length
                                         (+
                                           (* (/ (dec cnt) cnt) (:avg-length cur-val))
                                           (* (/ 1 cnt) length)))
                           :max-length (if (> length (:max-length cur-val))
                                         length
                                         (:max-length cur-val))}
                  ]
              (recur (second rest-lines)(rest (rest rest-lines))
                     (assoc support-map supp new-val) commit))
            :else
            (recur (first rest-lines) (rest rest-lines) support-map commit)))))))

(defn repo-support-map
  [repo-path]
  (let [repo-name (repo-name-from-path repo-path)
        pattern-folder (str output-dir repo-name "/")]
    (loop [i 0
           support-map {}]
      (let [pattern-file (str pattern-folder "patterns-" i ".txt")]
        (if (.exists (clojure.java.io/as-file pattern-file))
          (let [new-support-map (build-support-map support-map pattern-file)]
            (recur (inc i) new-support-map))
          support-map)))))



(comment
  (def git-path (nth (tpvision-repos) 1))
  (analyze-repository git-path )
  
  ; Pretty-print the entire support map
  (def supp-map (repo-support-map git-path))
  
  (doseq [support (sort (keys supp-map))]
    (let [val (get supp-map support)]
      (println support "," (:count val) "," (double (:avg-length val)) "," (:max-length val))))
  
  (inspector-jay.core/inspect supp-map)
  
  ; Open up all commits of a certain support level
  (doseq [commit (:commits (get supp-map 21))]
    (open-commit git-path commit))

  ; Total number of commits
  (count (repo/get-commits git-path))
  
  ; Find commit with a certain message
  (.indexOf (repo/get-commits git-path)
    (first (repo/get-commits git-path (fn [msg] (.startsWith msg "IPEPG Removed for TVJAR movement")))))
  
  (def changes (main/get-changes-in-commit (nth (repo/get-commits git-path) 7) (fn [f] true) 2))
  
  (analyse-commits 
    git-path
    "/Users/soft/desktop/freqchanges-contd6.txt"
    (stratfac/make-strategy)
    1410))