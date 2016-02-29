(ns arvid.thesis.plugin.clj.git.changedFile
  (:require [arvid.thesis.plugin.clj.changenodes.changes :as changes]))

;;;;;;;;;;;
; Internals
;;;;;;;;;;;

(defrecord ChangedFile [name before after])

;;;;;;;;;;;;
; Public API
;;;;;;;;;;;;

(defn 
  make
  "Create a new changed file. This is for internal use only. Consider using commit/get-changed-files instead."
  [name before after]
  (ChangedFile. name before after))

(defn
  get-before
  "Returns the original AST (before, left) of the given 'changed-file'."
  [changed-file]
  (:before changed-file))

(defn
  get-after
  "Returns the final AST (after, right) of the given 'changed-file'."
  [changed-file]
  (:after changed-file))

(defn
  get-name
  "Returns the name of the given 'changed-file'."
  [changed-file]
  (:name changed-file))

(defn 
  get-changes
  "Invokes changenodes differencer on the given 'changed-file'."
  [changed-file]
  (try
    (changes/get-all (get-before changed-file) (get-after changed-file))
    (catch Exception e
      (println "! Running changenodes failed. Ignoring file " (get-name changed-file))
      '())))