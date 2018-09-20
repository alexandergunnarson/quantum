(ns quantum.core.data.meta
  "Functions related to metadata."
  (:refer-clojure :exclude
    [reset-meta! with-meta])
  (:require
    [quantum.core.data.map :as map]
    [quantum.core.type :as t
      :refer [defnt]]))

(def meta?         (t/? map/+map?))
(def metable?      (t/isa? #?(:clj clojure.lang.IMeta :cljs cljs.core/IMeta)))
(def with-metable? (t/isa? #?(:clj clojure.lang.IObj  :cljs cljs.core/IWithMeta)))

(defnt >meta
  "Returns the (possibly nil) metadata of ->`x`."
  > meta?
  [x metable?] (#?(:clj .meta :cljs cljs.core/-meta) x))

(defnt with-meta
  "Returns an object of the same type and value as ->`x`, with ->`meta'` as its metadata."
  > with-metable?
           ([x with-metable?, meta' meta? > (t/* with-metable?) #_(TODO TYPED (t/value-of x))]
             (#?(:clj .withMeta :cljs cljs.core/-with-meta) x meta'))
  #?(:cljs ([x goog/isFunction, meta' meta?]
             (cljs.core/MetaFn. x meta'))))

(defnt reset-meta!
  "Atomically resets ->`x`'s metadata to be ->`meta'`."
  > meta?
  [x (t/isa? #?(:clj clojure.lang.IReference :cljs (TODO))) meta' meta?]
  (#?(:clj .resetMeta :cljs (set! (.-meta x) m)) x meta'))

;; TODO TYPED
#_(defnt update-meta
  "Returns an object of the same type and value as ->`x`, with its metadata updated by ->`f`."
  ;; TODO `f` should more specifically be able to handle the args arity and specs
  [x (t/and with-metable? metable?) f (t/fn meta? [& (t/type-of %args)]) & args _]
  (with-meta x (apply f (meta x) args)))

;; TODO TYPED
#_(defnt merge-meta
  {:alternate-implementations #{'cljs.tools.reader/merge-meta}}
  [x (t/and with-metable? metable?) meta- meta? > (t/spec-of x)]
  (update-meta x merge meta-))

;; TODO TYPED
#_(defnt merge-meta-from [to (t/and with-metable? metable?), from metable?]
  (update-meta to merge (>meta from)))

(defnt replace-meta-from > with-metable? [to with-metable?, from metable?]
  (with-meta to (>meta from)))
