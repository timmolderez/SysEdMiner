(ns arvid.thesis.plugin.algorithm.difference)

; Old code
;;;;;;;;;;

;(defn
;  stringify-operation
;  [operation]
;  (str "(" 
;      (astnode/ekeko-keyword-for-class-of operation) " "
;        (if (nil? (.getOriginal operation))
;            (astnode/ekeko-keyword-for-class-of (.getAffectedNode operation))
;            (astnode/ekeko-keyword-for-class-of (.getAffectedNode operation)))
;      ")\n"))

;(defn
;  changenodes-diff
;  [before after]
;  (let [cn (new changenodes.Differencer before after)]
;		(.difference cn)
;		(writeln (reduce str "Changenodes output:\n" (map stringify-operation (.getOperations cn))))))


; Code change: (<op kind> <AST node type>)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord 
  Change
  [operation node-type])

(defn
  make-change
  [operation node-type]
  (Change. operation node-type ))

(defn
  change-toString
  [change]
  (str (:operation change) " " (:node-type change)))

; Differencing
;;;;;;;;;;;;;;

(defn
  diff
  [before after]
  ; We ignore both before and after until we've got some usable input. 
  ; We'll use dummy changes (below) until that's the case.
  [(make-change :change :SimpleName)
   (make-change :change :PrimitiveType)
   (make-change :add :SimpleName)
   (make-change :add :SimpleName)
   (make-change :add :SimpleName)
   (make-change :add :MethodInvocation)
   (make-change :add :NumberLiteral)
   (make-change :add :ReturnStatement)
   (make-change :add :IfStatement)
   (make-change :add :SimpleName)
   (make-change :add :SimpleName)
   (make-change :add :SimpleName)
   (make-change :add :MethodInvocation)
   (make-change :add :NumberLiteral)
   (make-change :add :ReturnStatement)
   (make-change :add :IfStatement)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Bridge with the Java worlds
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn
  register-callbacks
  []
  (set! (arvid.thesis.plugin.ClojureBridge/FN_DIFFERENCE) diff))

(register-callbacks)