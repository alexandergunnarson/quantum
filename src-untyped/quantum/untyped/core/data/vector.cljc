(ns quantum.untyped.core.data.vector
  #?(:clj (:import java.util.ArrayList)))

(defn alist-get
  #?(:clj  [^ArrayList xs ^long   i]
     :cljs [           xs ^number i])
  (#?(:clj .get :cljs aget) xs i))

(defn alist-set!
  #?(:clj  [^ArrayList xs ^long   i v]
     :cljs [           xs ^number i v])
  (#?(:clj .set :cljs aset) xs i v))

(defn alist-conj! [#?(:clj ^ArrayList xs :cljs xs) v]
  (doto xs (#?(:clj .add :cljs .push) v)))

(defn #?(:clj alist-count :cljs ^number alist-count) [#?(:clj ^ArrayList xs :cljs xs)]
  (#?(:clj .size :cljs alength) xs))

(defn #?(:clj alist-empty? :cljs ^boolean alist-empty?) [#?(:clj ^ArrayList xs :cljs xs)]
  (== (#?(:clj .size :cljs alength) xs) 0))

(defn alist-empty! [#?(:clj ^ArrayList xs :cljs xs)]
  #?(:clj (.clear xs) :cljs (set! (.-length xs) 0))
  xs)

(defn #?(:clj alist== :cljs ^boolean alist==)
  [#?(:clj ^ArrayList x :cljs x) #?(:clj ^ArrayList y :cljs y)]
  (let [len (if (nil? x) 0 (long (alist-count x)))]
    (and (== len (if (nil? y) 0 (long (alist-count y))))
         (loop [i 0]
           (or (== i len)
               (if (identical? (alist-get x i) (alist-get y i))
                   (recur (inc i))
                   false))))))

(defn #?(:clj ^ArrayList alist :cljs alist)
  ([]  #?(:clj (ArrayList.) :cljs #js []))
  ([x] #?(:clj (doto (ArrayList.) (.add x)) :cljs #js [x])))
