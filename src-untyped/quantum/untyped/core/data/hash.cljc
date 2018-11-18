(ns quantum.untyped.core.data.hash
         (:refer-clojure :exclude
           [hash])
         (:require
           [clojure.core                   :as core])
#?(:cljs (:require-macros
           [quantum.untyped.core.data.hash :as self])))

(def ^:const default -1)

(def hash core/hash)

(defn code [x]
  #?(:clj  (clojure.lang.Util/hash x)
     :cljs (hash                   x)))

(def unordered hash-unordered-coll)
(def ordered   hash-ordered-coll)
(def mix       mix-collection-hash)

#?(:clj
;; Macro for efficiency; we will demacro in the typed version
(defmacro unordered-args [& args]
  `(-> 0 ~@(->> args (map (fn [arg] `(unchecked-add-int (hash ~arg)))))
         (mix-collection-hash ~(count args))
         int)))

#?(:clj
(defmacro caching-set-unordered!
  "Tries to retrive an cached unordered hash value at the provided field. If not found, sets the
   field with an unordered hash value computed using the provided args.

   See also https://clojure.org/reference/data_structures."
  [field #_simple-symbol? & args]
  `(if (identical? ~field (unchecked-int default))
       (set! ~field (unordered-args ~@args))
       ~field)))

#?(:clj
;; Macro for efficiency; we will demacro in the typed version
(defmacro ordered-args [& args]
  `(-> 1 ~@(->> args (map (fn [arg]
                           `(-> (unchecked-multiply-int 31)
                                (unchecked-add-int (hash ~arg))))))
        (mix-collection-hash ~(count args))
        int)))

#?(:clj
(defmacro caching-set-ordered!
  "Tries to retrive an cached ordered hash value at the provided field. If not found, sets the
   field with an ordered hash value computed using the provided args.

   See also https://clojure.org/reference/data_structures."
  [field #_simple-symbol? & args]
  `(if (identical? ~field (unchecked-int default))
       (set! ~field (ordered-args ~@args))
       ~field)))

#?(:clj
;; Macro for efficiency; we will demacro in the typed version
(defmacro code-args [& args]
  `(-> 0 ~@(->> args (map (fn [arg] `(unchecked-add-int (code ~arg))))))))

#?(:clj
(defmacro caching-set-code!
  "Tries to retrive a cached hash-code value at the provided field. If not found, sets the field
   with a computed hash-code using the sum of the hash-codes of the provided args."
  [field #_simple-symbol? & args]
  `(if (identical? ~field (unchecked-int default))
       (set! ~field (code-args ~@args))
       ~field)))
