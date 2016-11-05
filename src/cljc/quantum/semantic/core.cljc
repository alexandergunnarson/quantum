(ns quantum.semantic.core
           (:refer-clojure :exclude [assert get])
           (:require
             [quantum.core.logic
               :refer        [#?@(:clj [fn-or])]
               :refer-macros [          fn-or]]
             [quantum.core.fn
               :refer        [#?@(:clj [<- fn1])]
               :refer-macros [          <- fn1]]
             [quantum.core.collections :as coll
               :refer [map+ remove+
                       mutable! eq! aset-in!
                       #?@(:clj [kmap aget-in aget-in*
                                 ifor get reducei])]
               :refer-macros [   kmap aget-in aget-in*
                                 ifor get reducei]]
             [quantum.core.error
               :refer        [->ex]]
             [quantum.core.numeric :as num
               :refer        [#?@(:clj [+* inc*])]
               :refer-macros [inc* +*]]
             [quantum.core.string   :as str]
             [quantum.core.validate :as v
               :refer        [#?(:clj validate)]
               :refer-macros [        validate]])
  #?(:cljs (:import goog.string.StringBuffer)))

; TO EXPLORE
; - Mathematica
;   - Tools for text mining including semantic analysis
; ====================================

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
  (validate w (v/or* keyword? string?))
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
       (remove+ (fn1 = \-))
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

