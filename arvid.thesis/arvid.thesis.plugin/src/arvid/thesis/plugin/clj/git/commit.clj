(ns arvid.thesis.plugin.clj.git.commit
  (require [arvid.thesis.plugin.clj.git.changedFile :as changed-file])
  (import java.io.File)
  (import org.eclipse.jgit.api.Git)
	(import org.eclipse.jgit.api.errors.GitAPIException)
	(import org.eclipse.jgit.api.errors.InvalidRefNameException)
	(import org.eclipse.jgit.diff.DiffEntry)
	(import org.eclipse.jgit.diff.DiffEntry$ChangeType)
	(import org.eclipse.jgit.diff.DiffFormatter)
	(import org.eclipse.jgit.diff.RawTextComparator)
	(import org.eclipse.jgit.errors.CorruptObjectException)
	(import org.eclipse.jgit.errors.IncorrectObjectTypeException)
	(import org.eclipse.jgit.errors.MissingObjectException)
	(import org.eclipse.jgit.lib.Repository)
	(import org.eclipse.jgit.revwalk.RevCommit)
	(import org.eclipse.jgit.storage.file.FileRepositoryBuilder)
	(import org.eclipse.jgit.util.io.DisabledOutputStream)
  (import org.eclipse.jdt.core.JavaCore)
  (import org.eclipse.jdt.core.dom.ASTParser)
  (import org.eclipse.jdt.core.dom.AST))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord Commit [repository message jgit-commit])

(defn- 
  source-to-ast
  "Internal helper to convert a string into its corresponding AST"
  [source]
  (let [parser (ASTParser/newParser AST/JLS8)
        options (JavaCore/getOptions)]
    (JavaCore/setComplianceOptions JavaCore/VERSION_1_5 options)
    (.setKind parser ASTParser/K_COMPILATION_UNIT)
    (.setSource parser (.toCharArray source))
    (.setCompilerOptions parser options)
    (.createAST parser nil)))

(defn- 
  get-changed-files
  "Get a lazy sequence of changed files within the specified 'commit'. Is limited to files for which predicate
   'filename-filter' returns true given the corresponding filename."
  [commit filename-filter]
   (let [jgit-commit (:jgit-commit commit)
         repository (:repository commit)]
	   (if (= (.getParentCount jgit-commit) 0)
	       '()
	       (let [parent (.getParent jgit-commit 0)
	             diffFormatter (new DiffFormatter DisabledOutputStream/INSTANCE)]
	         (.setRepository diffFormatter repository)
	         (.setDiffComparator diffFormatter RawTextComparator/DEFAULT)
	         (.setDetectRenames diffFormatter true)
	         (let [diff-entries (.scan diffFormatter (.getTree parent) (.getTree jgit-commit))
                 useful-diff-entries (filter (fn [entry] 
                                                 (and (or (= (.getChangeType entry) DiffEntry$ChangeType/MODIFY)
                                                          (= (.getChangeType entry) DiffEntry$ChangeType/RENAME)) 
						                                          (.endsWith (.getNewPath entry) ".java")
                                                      (filename-filter (.getNewPath entry))))
	                                           diff-entries)]
              ;(map (fn [entry] (println (.getNewPath entry))) useful-diff-entries)
              (map (fn [entry]  (changed-file/make (.getNewPath entry)
                                                  (source-to-ast (new String (.getBytes (.open repository (.toObjectId (.getOldId entry)))) "UTF-8"))
                                                  (source-to-ast (new String (.getBytes (.open repository (.toObjectId (.getNewId entry)))) "UTF-8"))))
                   useful-diff-entries))))))

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  make
  [repository message jgit-commit]
  "Create a new commit. This is for internal use only. Consider using repository/get-commits instead."
  (Commit. repository message jgit-commit))

(defn 
  get-changes
  "Invokes changenodes differencer on the files (for which predicate 'filename-filter' succeeds with their 
   corresponding filenames) in the given 'commit'."
  ([commit]
   (get-changes commit (fn [filename] true)))
  ([commit filename-filter]
   [commit]
   (mapcat changed-file/get-changes
           (get-changed-files commit filename-filter))))
  
(defn
  get-message
  "Get the commit message of this commit"
  [commit]
  (:message commit))
