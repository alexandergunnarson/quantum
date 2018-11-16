(ns quantum.core.data.meta
  "Functions related to metadata."
  (:refer-clojure :exclude
    [reset-meta! with-meta])
  (:require
    [quantum.core.data.map :as map]
    [quantum.core.type :as t]))

(def meta?         (t/? map/+map?))
(def metable?      (t/isa?|direct #?(:clj clojure.lang.IMeta :cljs cljs.core/IMeta)))
(def with-metable? (t/isa?|direct #?(:clj clojure.lang.IObj  :cljs cljs.core/IWithMeta)))

(t/defn ^:inline >meta
  "Returns the (possibly nil) metadata of ->`x`."
  > meta?
  ([x metable?] (#?(:clj .meta :cljs cljs.core/-meta) x))
  ([x t/any?] nil))

(t/defn ^:inline with-meta
  "Returns an object of the same type and value as ->`x`, with ->`meta'` as its metadata."
  > with-metable?
           ([x with-metable?, meta' meta? > (t/run with-metable?) #_(TODO TYPED (t/value-of x))]
             (#?(:clj .withMeta :cljs cljs.core/-with-meta) x meta'))
  #?(:cljs ([x (t/isa? js/Function), meta' meta?]
             (cljs.core/MetaFn. x meta'))))

(t/defn ^:inline reset-meta!
  "Atomically resets ->`x`'s metadata to be ->`meta'`."
  > meta?
  [x (t/isa? #?(:clj clojure.lang.IReference :cljs (TODO))) meta' meta?]
  (#?(:clj .resetMeta :cljs (set! (.-meta x) m)) x meta'))

;; TODO TYPED
#_(t/defn update-meta
  "Returns an object of the same type and value as ->`x`, with its metadata updated by ->`f`."
  ;; TODO `f` should more specifically be able to handle the args arity and specs
  {:incorporated '{clojure.core/vary-meta "9/2018"
                   cljs.core/vary-meta    "9/2018"}}
  [x (t/and with-metable? metable?) f (t/fn meta? [& (t/type args)]) & args _]
  (with-meta x (apply f (meta x) args)))

;; TODO TYPED
#_(t/defn merge-meta
  {:incorporated #{'cljs.tools.reader/merge-meta}}
  [x (t/and with-metable? metable?) meta- meta? > (t/value-of x)]
  (update-meta x merge meta-))

;; TODO TYPED
#_(t/defn merge-meta-from [to (t/and with-metable? metable?), from metable?]
  (update-meta to merge (>meta from)))

(t/defn replace-meta-from > with-metable? [to with-metable?, from metable?]
  (with-meta to (>meta from)))
