(ns quantum.untyped.core.loops
  (:refer-clojure :exclude
    [doseq])
  (:require
    [quantum.untyped.core.core :as ucore]))

(ucore/log-this-ns)

(defn default-on-different-count [xs0 xs1] nil)

(defn reduce-2
  "Reduces over two seqables at a time."
  ([f xs0 xs1] (reduce-2 f nil xs0 xs1))
  ([f init xs0 xs1] (reduce-2 f default-on-different-count init xs0 xs1))
  ([f on-different-count init xs0 xs1]
    (loop [ret init xs0' xs0 xs1' xs1]
      (cond (reduced? ret)
            @ret
            (or (empty? xs0') (empty? xs1'))
            (if (or (and (empty? xs0') (seq    xs1'))
                    (and (seq    xs0') (empty? xs1')))
                (unreduced (on-different-count xs0 xs1))
                ret)
            :else (recur (f ret (first xs0') (first xs1'))
                         (next xs0')
                         (next xs1'))))))

(defn reducei-2
  "Reduces over two seqables at a time, maintaining an index."
  ([f xs0 xs1] (reducei-2 f nil xs0 xs1))
  ([f init xs0 xs1] (reducei-2 f init xs0 xs1 default-on-different-count))
  ([f init xs0 xs1 on-different-count]
    (let [f' (let [*i (volatile! -1)]
                (fn [ret x0 x1] (f ret x0 x1 (vreset! *i (unchecked-inc (long @*i))))))]
      (reduce-2 f' on-different-count init xs0 xs1))))

;; TODO incorporate into `quantum.core.loops`
#?(:clj
(defmacro doseq
  "Like `doseq` but:
   - Indexed
   - Returns the result
   - Does not have `:let`, `:while` or `:when` semantics
   - Much smaller code footprint"
  [[x xs i] & body]
  `(loop [xs#  (seq ~xs)
          ret# nil]
     (if xs#
         (let [~x (first xs#)]
           (recur (next xs#) (do ~@body)))
         ret#))))

;; TODO incorporate into `quantum.core.loops`
#?(:clj
(defmacro doseqi
  "Like `doseq` but:
   - Indexed
   - Returns the result
   - Does not have `:let`, `:while` or `:when` semantics
   - Much smaller code footprint"
  [[x xs i] & body]
  `(loop [xs#  (seq ~xs)
          ~i   0
          ret# nil]
     (if xs#
         (let [~x (first xs#)]
           (recur (next xs#) (inc ~i) (do ~@body)))
         ret#))))
