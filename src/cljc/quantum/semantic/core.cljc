(ns quantum.semantic.core
           (:refer-clojure :exclude [assert get])
           (:require
             [quantum.core.logic
               :refer        [#?@(:clj [fn-or])]
               :refer-macros [          fn-or]]
             [quantum.core.fn
               :refer        [#?@(:clj [<- f*n])]
               :refer-macros [          <- f*n]]
             [quantum.core.collections :as coll
               :refer [map+ remove+
                       mutable! eq! aset-in!
                       #?@(:clj [kmap aget-in aget-in*
                                 ifor get reducei])]
               :refer-macros [   kmap aget-in aget-in*
                                 ifor get reducei]]
             [quantum.core.error
               :refer        [#?(:clj assert) ->ex]
               :refer-macros [        assert]]
             [quantum.core.numeric :as num
               :refer        [#?@(:clj [+*]) inc* ]
               :refer-macros [          +*]]
             [quantum.core.string :as str])
  #?(:cljs (:import goog.string.StringBuffer)))

(defn ->soundex
  {:tests `{[:extenssions] :E235
            [:extensions]  :E235
            [:marshmellow] :M625
            [:marshmallow] :M625
            [:brimingham]  :B655
            [:birmingham]  :B655
            [:poiner]      :P560
            [:pointer]     :P536}}
  [w]
  (assert ((fn-or keyword? string?) w) #{w})
  (->> w name
       (coll/ldropl 1)
       (map+ (fn [c] (condp contains? c
                       #{\a \e \i \o \u \y \h \w} \-
                       #{\b \f \p \v}             \1
                       #{\c \g \j \k \q \s \x \z} \2
                       #{\d \t}                   \3
                       #{\l}                      \4
                       #{\m \n}                   \5
                       #{\r}                      \6
                       (throw (->ex nil
                                    "Not a soundex-able word"
                                    (kmap w))))))
       (coll/distinct-by+ identity (fn [x y] (and (= x y) (str/numeric? x))))
       (remove+ (f*n = \-))
       (reducei (fn [^StringBuilder s c i]
                  (when (= i 0)
                    (.append s (-> w name first str/->upper)))
                  (cond (> i 2)
                        (reduced s)
                        :else (.append s c)))
                #?(:clj  (StringBuilder.)
                   :cljs (StringBuffer.)))
       (<- coll/padr 3 \0)
       str
       keyword))

(defn levenshtein-matrix
  {:tests '{["kitten" "sitting"]
            [[0 1 2 3 4 5 6 7]
             [1 1 2 3 4 5 6 7]
             [2 2 1 2 3 4 5 6]
             [3 3 2 1 2 3 4 5]
             [4 4 3 2 1 2 3 4]
             [5 5 4 3 2 2 3 4]
             [6 6 5 4 3 3 2 3]]}
   :performance
      ["Boxed math doesn't seem to
        make a difference in performance"
       "|ifor| shaves about 20-40% off the time compared to
        |doseq| with |range|! pretty amazing"]
    :todo ["Move from |aset-in!| to |aset-in!*|"
           "Eliminate boxed math"
           "Improve |coll/->multi-array|"]} 
  [s1 s2]
  (let [s1-ct+1 (-> s1 count int inc*)
        s2-ct+1 (-> s2 count int inc*)
        ^"[[I" m    (coll/->multi-array (int 0)
                      [(-> s1 count inc)
                       (-> s2 count inc)])
        cost (mutable! 0)]
    (ifor [i 0 (< i s1-ct+1) (inc* i)]
      (aset-in! m [i 0] i))
    (ifor [j 0 (< j s2-ct+1) (inc* j)]
      (aset-in! m [0 j] j))
    (ifor [i 1 (< i s1-ct+1) (inc* i)]
      (ifor [j 1 (< j s2-ct+1) (inc* j)]
        (if (= (get s1 (dec i))
               (get s2 (dec j)))
            (eq! cost 0)
            (eq! cost 1))
        (aset-in! m [i j]
          (min (inc     (aget-in* m (dec i) j      ))     ; deletion
               (inc     (aget-in* m i       (dec j)))     ; insertion
               (+ @cost (aget-in* m (dec i) (dec j))))))) ; substitution
     m))

(defn levenshtein-distance [str1 str2]
  {:modified-by {"Alex Gunnarson"
                 ["removed boxed math"
                  "|nth| -> |get|"
                  "removed unnecessary |persistent!| call"]}
   :original-source "https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Clojure"}
  (let [str1 (name str1)
        str2 (name str2)
        n (-> str1 count int)
        m (-> str2 count int)]
    (cond 
     (= 0 n) m
     (= 0 m) n
     :else
     (let [prev-col (transient (vec (range (inc* m))))
           col      (transient [])] ; initialization for the first column.
       (dotimes [i n]
         (let [i (int i)]
           (assoc! col 0 (inc* i)) ; update col[0]
           (dotimes [j m]
             (let [j (int j)]
               (assoc! col (inc* j)  ; update col[1..m] 
               (min (inc* (int (get col      j       )))
                    (inc* (int (get prev-col (inc* j))))
                    (+*   (int (get prev-col j))
                          (if (= (get str1 i)
                                 (get str2 j))
                              0 1))))))
           (dotimes [i (count prev-col)] 
             (assoc! prev-col i (get col i))))) ; 
       (last col))))) ; last element of last column