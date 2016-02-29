-(ns arvid.thesis.plugin.clj.git.repository
   (require [arvid.thesis.plugin.clj.git.commit :as commit])
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

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;
 
(defn
  get-commits
  "Get a list of maximum 'limit' commits (whose commit messages make the predicate 'message-filter' succeed) 
   in the git repository at the specified 'path'.

  The path variable must be a path to a .git folder of a repository.."
  ([path]
    (get-commits path (fn [msg] true)))
  ([path message-filter]
    (get-commits path message-filter Integer/MAX_VALUE))
  ([path message-filter limit]
	  (let [repository (.build (.findGitDir (.readEnvironment (.setGitDir (new FileRepositoryBuilder) (new File path)))))
	        git (new Git repository)
	        commits (.call (.all (.log git)))]
	    (map (fn [jgit-commit] (commit/make repository (.getShortMessage jgit-commit) jgit-commit))
           (take limit 
                 (filter (fn [jgit-commit] (message-filter (.getShortMessage jgit-commit))) commits))))))
