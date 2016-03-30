(ns
  ^{:doc "Frequent change pattern analysis of the Selenium data set"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.selenium
  (:require 
    [arvid.thesis.plugin.clj.git.repository :as repo]
    [arvid.thesis.plugin.clj.strategies.strategyFactory :as stratfac]
    [clojure.java.shell :as sh]
    [arvid.thesis.plugin.clj.test.main :as main]))

(def output-dir 
  "/Users/soft/desktop/selenium-freqchanges/")

; Paths to all local Selenium repositories
(def selenium-repos
  (for [repo ["atlas"
              "eeg-database" ; selenium phased out
              "TAMA-Web"
              "mifos-head" ; selenium phased out
              "open-lmis"
              "xwiki-enterprise"
              "zanata-server"
              "zimbra-sources"]]
    (str "/Users/soft/Documents/Github/" repo "/.git")))

; Assumption that all functional tests are in certain packages doesn't always make sense..
;(def selenium-packages [["uk.ac.ebi.atlas.acceptance.selenium" "uk.ac.ebi.atlas.experiments" "uk.ac.ebi.atlas.experimentpage"]
;                        ["cz.zcu.kiv.eegdatabase.ui"]
;                        ])

(def selenium-folders [["web/src/test/java/uk/ac/ebl/atlas/acceptance" 
                        "web/src/test/java/uk/ac/ebl/atlas/bloentity" 
                        "web/src/test/java/uk/ac/ebl/atlas/experiments"
                        "web/src/test/java/uk/ac/ebl/atlas/experimentpage"]
                       ["src/test/java/cz/zcu/klv/eegdatabase/ui"]
                       ["tama.web/src/test/java/org/motechproject/tamafunctionalframework"]
                       ["server-workspace/src/test/java/org/mifos/server/workspace"]
                       ["test-modules/functional-tests/src"]
                       ["xwiki-enterprise-test/xwiki-enterprise-test-selenium/src"]
                       ["zanata-server/functional-test/src"]
                       ["main/ZimbraSelenium/src/java"]])

(defn find-selenium-related-commits [repo-path]
  (let [all-commits (map
                      main/commit-id
                      (repo/get-commits repo-path))]
    (remove
      nil?
      (for [commit all-commits]
       (let [commit-files (rest (clojure.string/split-lines 
                                  (:out (sh/sh "git" "diff-tree" commit "--name-only" "-r" :dir (str repo-path "/..")))))
             selenium-files (filter
                              (fn [file]
                                (let [contents (:out (sh/sh "git" "show" (str commit ":" file) :dir (str repo-path "/..")))]
                                  (.contains contents "import org.openqa.selenium")))
                              commit-files)]
         (if (empty? selenium-files)
           nil
           [commit selenium-files])
         )))))

(defn spit-selenium-related-commits [filepath commit-map]
  (doseq [[commit files] commit-map]
    (main/append filepath (str "### " commit))
    (doseq [file files]
      (main/append filepath file))))

(defn slurp-selenium-related-commits [filepath]
  (with-open [rdr (clojure.java.io/reader filepath)]
    (loop [[line & rest-lines] (line-seq rdr)
           commit nil
           commit-map {}]
      (cond 
        (nil? line)
        commit-map
        
        (.startsWith line "###")
        (let [cur-commit (second (clojure.string/split line #" "))]
          (recur rest-lines cur-commit commit-map))
        
        :rest
        (let [old (get commit-map commit)
              new (conj old line)]
          (recur rest-lines commit (assoc commit-map commit new)))))))

(defn analyse-selenium-commits [repo-path strategy]
  "Analyse only the commits that concern Selenium-related files, and analyse only those files"
  (let [repo-name (main/repo-name-from-path repo-path)
        commit-map (slurp-selenium-related-commits (str output-dir repo-name "/selenium-commits.txt"))
        commit-count (count (keys commit-map))
        min-support 3
        verbosity 1]
    (map-indexed 
      (fn [idx commit-id]
        (println "Processing commit" idx "/" commit-count "(" repo-name ")")
        (try 
          (main/mine-commit 
            (main/find-commit-by-id repo-path commit-id) 
            strategy min-support verbosity 
            (str output-dir repo-name "/patterns.txt")
            (fn [file-path]
              (let [selenium-files (get commit-map commit-id)]
                (some (fn [sel-file] (= file-path sel-file)) selenium-files))))
          (catch Exception e
            (do
              (println "!! Failed to process commit" idx)
              (.printStackTrace e))))
        )
      (keys commit-map))))

(comment
  (def git-path (nth selenium-repos 5))
  
  (analyse-selenium-commits (nth selenium-repos 0) (stratfac/make-strategy))
  
  (spit-selenium-related-commits "/Users/soft/Desktop/sel.txt" selenium-commits)
  (inspector-jay.core/inspect (slurp-selenium-related-commits (str output-dir "atlas/selenium-commits.txt")))
  
  
  (def selenium-commits
    (find-selenium-related-commits git-path))
  
  (doseq [repo selenium-repos]
    (let [repo-name (main/repo-name-from-path repo)
          selenium-commits (find-selenium-related-commits repo)]
      (println repo-name)
      (.mkdir (java.io.File. (str output-dir repo-name)))
      (spit-selenium-related-commits (str output-dir repo-name "/selenium-commits.txt") selenium-commits)
      ))
  
)