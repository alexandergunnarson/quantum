(ns quantum.core.print.prettier
  (:require
    [fipp.edn]
    [fipp.visit]
    [fipp.ednize]
    [quantum.core.ns :as ns]))

; TODO get rid of this fipp boilerplate
(in-ns 'fipp.ednize)

(defprotocol IOverride
  "Mark object as preferring its custom IEdn behavior.")

(defn override? [x]
  (satisfies? IOverride x))

(in-ns 'fipp.visit)

(defn transient-vector? [x]
  (instance? #?(:clj  clojure.lang.ITransientVector
                :cljs cljs.core/TransientVector) x))

;; TODO more efficient
;; TODO move?
#?(:clj
(deftype IntIndexedIterator
  [^long ^:unsynchronized-mutable i ^long ct ^clojure.lang.Indexed xs]
  java.util.Iterator
    (hasNext [this] (< i ct))
    (next    [this]
      (if (< i ct)
          (let [i-prev i]
            (set! i (unchecked-inc i))
            (.nth xs i-prev))
          (throw (java.util.NoSuchElementException.))))
    (remove  [this] (throw (UnsupportedOperationException.)))))

#?(:clj
(deftype IntIndexedIterable [^clojure.lang.Indexed xs]
  Iterable
    (iterator [this] (IntIndexedIterator. 0 (count xs) xs))))

(defn visit*
  "Visits objects, ignoring metadata."
  [visitor x]
  (cond
    (nil? x) (visit-nil visitor)
    (fipp.ednize/override? x) (visit-unknown visitor x)
    (boolean? x) (visit-boolean visitor x)
    (string? x) (visit-string visitor x)
    (char? x) (visit-character visitor x)
    (symbol? x)
      ;; Collapses symbols
      [:text (str (when-let [n (namespace x)]
                    (str (if-let [alias- (do #?(:clj (quantum.core.ns/ns-name->alias *ns* (symbol n)) :cljs false))]
                           (str "," alias-)
                           n)
                         "/")) (name x))]
      #_(visit-symbol visitor x)
    (keyword? x) (visit-keyword visitor x)
    (number? x) (visit-number visitor x)
    (seq? x) (visit-seq visitor x)
    (vector? x) (visit-vector visitor x)
    (record? x) (visit-record visitor x)
    (map? x) (visit-map visitor x)
    (set? x) (visit-set visitor x)
    (tagged-literal? x) (visit-tagged visitor x)
    (var? x) (visit-var visitor x)
    (regexp? x) (visit-pattern visitor x)
    (transient-vector? x)
      [:group "#" (pr-str '!+)
        (when (and (:print-meta visitor) (meta (:form visitor))) " ")
        (visit-vector visitor (IntIndexedIterable. x))]
    :else (visit-unknown visitor x)))

(in-ns 'quantum.core.print.prettier)

(defn extend-pretty-printing! []
  (extend-type java.util.ArrayList
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '! (vec this)))) ; TODO faster
  (extend-type it.unimi.dsi.fastutil.longs.LongArrayList
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '!l (vec this)))) ; TODO faster

  (extend-type (Class/forName "[Z")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '?<> (vec this)))) ; TODO faster
  (extend-type (Class/forName "[B")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'b<> (vec this)))) ; TODO faster
  (extend-type (Class/forName "[C")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'c<> (vec this)))) ; TODO faster
  (extend-type (Class/forName "[[C")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'c<><> (into [] this)))) ; TODO faster
  (extend-type (Class/forName "[S")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 's<> (vec this)))) ; TODO faster
  (extend-type (Class/forName "[I")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'i<> (vec this)))) ; TODO faster
  (extend-type (Class/forName "[J")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'l<> (vec this)))) ; TODO ->vec for this
  (extend-type (Class/forName "[F")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'f<> (vec this)))) ; TODO faster
  (extend-type (Class/forName "[D")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'd<> (vec this)))) ; TODO ->vec for this
  (extend-type (Class/forName "[[D")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'd<><> (into [] this)))) ; TODO faster
  (extend-type (Class/forName "[Ljava.lang.Object;")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '*<> (vec this))))  ; TODO faster
  (extend-type (Class/forName "[[Ljava.lang.Object;")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '*<><> (into [] this))))  ; TODO faster
  (extend-type (Class/forName "[Ljava.lang.Class;")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'class<> (vec this))))  ; TODO faster

  (extend-type java.util.HashMap
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '! (into {} this))))
  (extend-type java.util.concurrent.ConcurrentHashMap
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '!! (into {} this)))) ; TODO ->map
  )
