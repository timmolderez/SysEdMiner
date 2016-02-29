(ns arvid.thesis.plugin.algorithm.orderedMap
  (:require [clojure.string :as s])
  (:import (clojure.lang IPersistentSet ITransientSet IEditableCollection
                         IPersistentMap ITransientMap ITransientAssociative
                         IPersistentVector ITransientVector IPersistentCollection
                         Associative SeqIterator Reversible IFn IObj
                         MapEquivalence Reversible MapEntry)
           (java.util Set Collection)
           (java.util Map Map$Entry)))
                         
; NS
;;;;

(defn var-name
  "Get the namespace-qualified name of a var."
  [v]
  (apply symbol (map str ((juxt (comp ns-name :ns)
                                :name)
                          (meta v)))))

(defn alias-var
  "Create a var with the supplied name in the current namespace, having the same
  metadata and root-binding as the supplied var."
  [name ^clojure.lang.Var var]
  (apply intern *ns* (with-meta name (merge {:dont-test (str "Alias of " (var-name var))}
                                            (meta var)
                                            (meta name)))
         (when (.hasRoot var) [@var])))

(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  [dst src]
  `(alias-var (quote ~dst) (var ~src)))

(defn alias-ns
  "Create vars in the current namespace to alias each of the public vars in
  the supplied namespace."
  [ns-name]
  (require ns-name)
  (doseq [[name var] (ns-publics (the-ns ns-name))]
    (alias-var name var)))

; DELEGATE
;;;;;;;;;

(defn canonical-name
  "Resolve a symbol in the current namespace; but intead of returning its value,
   return a canonical name that can be used to name the same thing in any
   namespace."
  [sym]
  (if-let [val (resolve sym)]
    (condp instance? val
      java.lang.Class (symbol (pr-str val))
      clojure.lang.Var (var-name val)
      (throw (IllegalArgumentException.
              (format "%s names %s, an instance of %s, which has no canonical name."
                      sym val (class val)))))
    sym))

(defn parse-deftype-specs
  "Given a mess of deftype specs, possibly with classes/interfaces specified multiple times,
  collapse it into a map like {interface => (method1 method2...)}.
  Needed because core.deftype only allows specifying a class ONCE, so our delegating versions would
  clash with client's custom methods."
  [decls]
  (loop [ret {}, curr-key nil, decls decls]
    (if-let [[x & xs] (seq decls)]
      (if (seq? x)
        (let [mname (symbol (name (first x)))
              nargs (count (second x))]
          (recur (assoc-in ret [curr-key [mname nargs]] x),
                 curr-key, xs))
        (let [interface-name (canonical-name x)]
          (recur (update-in ret [interface-name] #(or % {})),
                 interface-name, xs)))
      ret)))

(defn emit-deftype-specs
  "Given a map returned by aggregate, spit out a flattened deftype body."
  [specs]
  (apply concat
         (for [[interface methods] specs]
           (cons interface
                 (for [[[method-name num-args] method] methods]
                   method)))))

(letfn [;; Output the method body for a delegating implementation
        (delegating-method [method-name args delegate]
          `(~method-name [~'_ ~@args]
             (. ~delegate (~method-name ~@args))))

        ;; Create a series of Interface (method...) (method...) expressions,
        ;; suitable for creating the entire body of a deftype or reify.
        (type-body [delegate-map other-args]
          (let [our-stuff (for [[send-to interfaces] delegate-map
                                [interface which] interfaces
                                :let [send-to (vary-meta send-to
                                                         assoc :tag interface)]
                                [name args] which]
                            [interface (delegating-method name args send-to)])]
            (emit-deftype-specs
             (parse-deftype-specs
              (apply concat other-args our-stuff)))))]

  (defmacro delegating-deftype
    "Shorthand for defining a new type with deftype, which delegates the methods you name to some
    other object or objects. Delegates are usually a member field, but can be any expression: the
    expression will be evaluated every time a method is delegated. The delegate object (or
    expression) will be type-hinted with the type of the interface being delegated.
    The delegate-map argument should be structured like:
      {object-to-delegate-to {Interface1 [(method1 [])
                                          (method2 [foo bar baz])]
                              Interface2 [(otherMethod [other])]},
       another-object {Interface1 [(method3 [whatever])]}}.
    This will cause your deftype to include an implementation of Interface1.method1 which does its
    work by forwarding to (.method1 object-to-delegate-to), and likewise for the other
    methods. Arguments will be forwarded on untouched, and you should not include a `this`
    parameter. Note especially that you can have methods from Interface1 implemented by delegating
    to multiple objects if you choose, and can also include custom implementations for the remaining
    methods of Interface1 if you have no suitable delegate.
    Arguments after `delegate-map` are as with deftype, although if deftype ever has options defined
    for it, delegating-deftype may break with them."
    [cname [& fields] delegate-map & deftype-args]
    `(deftype ~cname [~@fields]
       ~@(type-body delegate-map deftype-args)))

  (defmacro delegating-defrecord
    "Like delegating-deftype, but creates a defrecod body instead of a deftype."
    [cname [& fields] delegate-map & deftype-args]
    `(defrecord ~cname [~@fields]
       ~@(type-body delegate-map deftype-args)))

  (defmacro delegating-reify
    "Like delegating-deftype, but creates a reify body instead of a deftype."
    [delegate-map & reify-args]
    `(reify ~@(type-body delegate-map reify-args))))

; COMMON
;;;;;;;;

(defmacro change! [field f & args]
  `(set! ~field (~f ~field ~@args)))

(defprotocol Compactable
  (compact [this]))

; SET
;;;;;
(declare transient-ordered-set)

(deftype OrderedSet [^IPersistentMap k->i
                     ^IPersistentVector i->k]
  IPersistentSet
  (disjoin [this k]
    (if-let [i (.valAt k->i k)]
      (OrderedSet. (dissoc k->i k)
                   (assoc i->k i ::empty))
      this))
  (cons [this k]
    (if-let [i (.valAt k->i k)]
      this
      (OrderedSet. (.assoc ^Associative k->i k (.count i->k))
                   (.cons i->k k))))
  (seq [this]
    (seq (remove #(identical? ::empty %) i->k)))
  (empty [this]
    (OrderedSet. (-> {} (with-meta (meta k->i)))
                 []))
  (equiv [this other]
    (.equals this other))
  (get [this k]
    (when (.valAt k->i k) k))
  (count [this]
    (.count k->i))

  IObj
  (meta [this]
    (.meta ^IObj k->i))
  (withMeta [this m]
    (OrderedSet. (.withMeta ^IObj k->i m)
                 i->k))

  Compactable
  (compact [this]
    (into (empty this) this))

  Object
  (toString [this]
    (str "#{" (clojure.string/join " " (map str this)) "}"))
  (hashCode [this]
    (reduce + (map hash (.seq this))))
  (equals [this other]
    (or (identical? this other)
        (and (instance? Set other)
             (let [^Set s other]
               (and (= (.size this) (.size s))
                    (every? #(.contains s %) (.seq this)))))))

  Set
  (iterator [this]
    (SeqIterator. (.seq this)))
  (contains [this k]
    (.containsKey k->i k))
  (containsAll [this ks]
    (every? #(.contains this %) ks))
  (size [this]
    (.count this))
  (isEmpty [this]
    (zero? (.count this)))
  (toArray [this dest]
    (reduce (fn [idx item]
              (aset dest idx item)
              (inc idx))
            0, (.seq this))
    dest)
  (toArray [this]
    (.toArray this (object-array (.count this))))

  Reversible
  (rseq [this]
    (seq (remove #(identical? ::empty %) (rseq i->k))))

  IEditableCollection
  (asTransient [this]
    (transient-ordered-set this))
  IFn
  (invoke [this k] (when (.contains this k) k)))

(def ^{:private true,
       :tag OrderedSet} empty-ordered-set (empty (OrderedSet. nil nil)))

(defn ordered-set
  ([] empty-ordered-set)
  ([& xs] (into empty-ordered-set xs)))

(deftype TransientOrderedSet [^{:unsynchronized-mutable true
                                :tag ITransientMap} k->i,
                              ^{:unsynchronized-mutable true
                                :tag ITransientVector} i->k]
  ITransientSet
  (count [this]
    (.count k->i))
  (get [this k]
    (when (.valAt k->i k) k))
  (disjoin [this k]
    (let [i (.valAt k->i k)]
      (when i
        (change! k->i .without k)
        (change! i->k .assocN i ::empty)))
    this)
  (conj [this k]
    (let [i (.valAt k->i k)]
      (when-not i
        (change! ^ITransientAssociative k->i .assoc k (.count i->k))
        (change! i->k conj! k)))
    this)
  (contains [this k]
    (boolean (.valAt k->i k)))
  (persistent [this]
    (OrderedSet. (.persistent k->i)
                 (.persistent i->k))))

(defn transient-ordered-set [^OrderedSet os]
  (TransientOrderedSet. (transient (.k->i os))
                        (transient (.i->k os))))

(defn into-ordered-set
  [items]
  (into empty-ordered-set items))

(defmethod print-method OrderedSet [o ^java.io.Writer w]
  (.write w "#ordered/set ")
  (print-method (seq o) w))

; MAP
;;;;;

(defn entry [k v i]
  (MapEntry. k (MapEntry. i v)))

(declare transient-ordered-map)

(delegating-deftype OrderedMap [^IPersistentMap backing-map
                                ^IPersistentVector order]
  {backing-map {IPersistentMap [(count [])]
                Map [(size [])
                     (containsKey [k])
                     (isEmpty [])
                     (keySet [])]}}
  ;; tagging interfaces
  MapEquivalence

  IPersistentMap
  (equiv [this other]
    (and (instance? Map other)
         (= (.count this) (.size ^Map other))
         (every? (fn [^MapEntry e]
                   (= (.val e) (.get ^Map other (.key e))))
                 (.seq this))))
  (entryAt [this k]
    (let [v (get this k ::not-found)]
      (when (not= v ::not-found)
        (MapEntry. k v))))
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (if-let [^MapEntry e (.get ^Map backing-map k)]
      (.val e)
      not-found))

  IFn
  (invoke [this k]
    (.valAt this k))
  (invoke [this k not-found]
    (.valAt this k not-found))

  Map
  (get [this k]
    (.valAt this k))
  (containsValue [this v]
    (boolean (seq (filter #(= % v) (.values this)))))
  (values [this]
    (map (comp val val) (.seq this)))

  Object
  (toString [this]
    (str "{" (s/join ", " (for [[k v] this] (str k " " v))) "}"))
  (equals [this other]
    (.equiv this other))
  (hashCode [this]
    (reduce (fn [acc ^MapEntry e]
              (let [k (.key e), v (.val e)]
                (unchecked-add ^Integer acc ^Integer (bit-xor (hash k) (hash v)))))
            0 (.seq this)))
  IPersistentMap
  (empty [this]
    (OrderedMap. (-> {} (with-meta (meta backing-map))) []))
  (cons [this obj]
    (condp instance? obj
      Map$Entry (let [^Map$Entry e obj]
                  (.assoc this (.getKey e) (.getValue e)))
      IPersistentVector (if (= 2 (count obj))
                          (.assoc this (nth obj 0) (nth obj 1))
                          (throw (IllegalArgumentException.
                                  "Vector arg to map conj must be a pair")))
      (persistent! (reduce (fn [^ITransientMap m ^Map$Entry e]
                             (.assoc m (.getKey e) (.getValue e)))
                           (transient this)
                           obj))))

  (assoc [this k v]
    (if-let [^MapEntry e (.get ^Map backing-map k)]
      (let [old-v (.val e)]
        (if (= old-v v)
          this
          (let [i (.key e)]
            (OrderedMap. (.cons backing-map (entry k v i))
                         (.assoc order i (MapEntry. k v))))))
      (OrderedMap. (.cons backing-map (entry k v (.count order)))
                   (.cons order (MapEntry. k v)))))
  (without [this k]
    (if-let [^MapEntry e (.get ^Map backing-map k)]
      (OrderedMap. (.without backing-map k)
                   (.assoc order (.key e) nil))
      this))
  (seq [this]
    (seq (keep identity order)))
  (iterator [this]
    (clojure.lang.SeqIterator. (.seq this)))
  (entrySet [this]
    ;; not performant, but i'm not going to implement another whole java interface from scratch just
    ;; because rich won't let us inherit from AbstractSet
    (apply ordered-set this))

  IObj
  (meta [this]
    (.meta ^IObj backing-map))
  (withMeta [this m]
    (OrderedMap. (.withMeta ^IObj backing-map m) order))

  IEditableCollection
  (asTransient [this]
    (transient-ordered-map this))

  Reversible
  (rseq [this]
    (seq (keep identity (rseq order))))

  Compactable
  (compact [this]
    (into (empty this) this)))

(def ^{:private true,
       :tag OrderedMap} empty-ordered-map (empty (OrderedMap. nil nil)))

(defn ordered-map
  ([] empty-ordered-map)
  ([coll]
     (into empty-ordered-map coll))
  ([k v & more]
     (apply assoc empty-ordered-map k v more)))

;; contains? is broken for transients. we could define a closure around a gensym
;; to use as the not-found argument to a get, but deftype can't be a closure.
;; instead, we pass `this` as the not-found argument and hope nobody makes a
;; transient contain itself.

(delegating-deftype TransientOrderedMap [^{:unsynchronized-mutable true, :tag ITransientMap} backing-map,
                                         ^{:unsynchronized-mutable true, :tag ITransientVector} order]
  {backing-map {ITransientMap [(count [])]}}
  ITransientMap
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (if-let [^MapEntry e (.valAt backing-map k)]
      (.val e)
      not-found))
  (assoc [this k v]
    (let [^MapEntry e (.valAt backing-map k this)
          vector-entry (MapEntry. k v)
          i (if (identical? e this)
              (do (change! order .conj vector-entry)
                  (dec (.count order)))
              (let [idx (.key e)]
                (change! order .assoc idx vector-entry)
                idx))]
      (change! backing-map .conj (entry k v i))
      this))
  (conj [this e]
    (let [[k v] e]
      (.assoc this k v)))
  (without [this k]
    (let [^MapEntry e (.valAt backing-map k this)]
      (when-not (identical? e this)
        (let [i (.key e)]
          (change! backing-map dissoc! k)
          (change! order assoc! i nil)))
      this))
  (persistent [this]
    (OrderedMap. (.persistent backing-map)
                 (.persistent order))))

(defn transient-ordered-map [^OrderedMap om]
  (TransientOrderedMap. (.asTransient ^IEditableCollection (.backing-map om))
                        (.asTransient ^IEditableCollection (.order om))))

(defmethod print-method OrderedMap [o ^java.io.Writer w]
  (.write w "#ordered/map ")
  (print-method (seq o) w))