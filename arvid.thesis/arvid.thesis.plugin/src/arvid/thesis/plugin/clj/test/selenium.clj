(ns
  ^{:doc "Frequent change pattern analysis of the Selenium data set"
    :author "Tim Molderez"}
  arvid.thesis.plugin.clj.test.selenium
  (:require 
    [arvid.thesis.plugin.clj.git.repository :as repo]
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

(comment
  (def git-path (nth selenium-repos 5))
  (count (repo/get-commits git-path))
  (nth (repo/get-commits git-path) 2)
  (repo-name-from-path git-path)
  
  (spit-selenium-related-commits "/Users/soft/Desktop/sel.txt" selenium-commits)
  (inspector-jay.core/inspect (slurp-selenium-related-commits "/Users/soft/Desktop/sel.txt"))
  
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