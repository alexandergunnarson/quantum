(ns
  ^{:doc "Definitions for types."
    :attribution "alexandergunnarson"}
  quantum.untyped.core.type.defs
  (:refer-clojure :exclude
    [boolean byte char short int long float double])
  (:require
    [clojure.core.async.impl.channels]
#?@(:clj
   [[clojure.core.async.impl.protocols]]
    :cljs
   [[com.gfredericks.goog.math.Integer]])
    [clojure.core                               :as core]
    [clojure.core.rrb-vector.rrbt]
    [clojure.string                             :as str]
    [quantum.untyped.core.data.map              :as umap
      :refer [om]]
    [quantum.untyped.core.data.set              :as uset]
    [quantum.untyped.core.data.tuple
      #?@(:cljs [:refer [Tuple]])]
    [quantum.untyped.core.defnt
      :refer [defns]]
    [quantum.untyped.core.fn
      :refer [<- fn-> fnl rcomp]]
    [quantum.untyped.core.form.evaluate
      :refer [env-lang]]
    [quantum.untyped.core.logic
      :refer [fn-and fn= condf1]]
    [quantum.untyped.core.numeric.combinatorics :as combo])
  (:import
    #?@(:clj  [#_clojure.core.async.impl.channels.ManyToManyChannel
               com.google.common.util.concurrent.AtomicDouble
               quantum.untyped.core.data.tuple.Tuple]
        :cljs [goog.string.StringBuffer
               goog.structs.Map
               goog.structs.Set
               goog.structs.AvlTree
               goog.structs.Queue])))

#?(:clj (def boolean Boolean/TYPE))
#?(:clj (def byte    Byte/TYPE))
#?(:clj (def char    Character/TYPE))
#?(:clj (def short   Short/TYPE))
#?(:clj (def int     Integer/TYPE))
#?(:clj (def long    Long/TYPE))
#?(:clj (def float   Float/TYPE))
#?(:clj (def double  Double/TYPE))

(def ^{:doc "Could do <Class>/MAX_VALUE for the maxes in Java but JS doesn't like it of course
             In JavaScript, all numbers are 64-bit floating point numbers.
             This means you can't represent in JavaScript all the Java longs
             Max 'safe' int: (dec (Math/pow 2 53))"}
  unboxed-symbol->type-meta
  {'boolean {:bits 1
             :min  0
             :max  1
   #?@(:clj [:array-ident  "Z"
             :outer-type  "[Z"
             :boxed       java.lang.Boolean
             :unboxed     Boolean/TYPE])}
   'byte    {:bits 8
             :min -128
             :max  127
   #?@(:clj [:array-ident  "B"
             :outer-type  "[B"
             :boxed       java.lang.Byte
             :unboxed     Byte/TYPE])}
   'short   {:bits 16
             :min -32768
             :max  32767
   #?@(:clj [:array-ident  "S"
             :outer-type  "[S"
             :boxed       java.lang.Short
             :unboxed     Short/TYPE])}
   'char    {:bits 16
             :min  0
             :max  65535
   #?@(:clj [:array-ident  "C"
             :outer-type  "[C"
             :boxed       java.lang.Character
             :unboxed     Character/TYPE])}
   'int     {:bits 32
             :min -2147483648
             :max  2147483647
   #?@(:clj [:array-ident  "I"
             :outer-type  "[I"
             :boxed       java.lang.Integer
             :unboxed     Integer/TYPE])}
   'long    {:bits 64
             :min -9223372036854775808
             :max  9223372036854775807
   #?@(:clj [:array-ident  "J"
             :outer-type  "[J"
             :boxed       java.lang.Long
             :unboxed     Long/TYPE])}
   ; Technically with floating-point nums, "min" isn't the most negative;
   ; it's the smallest absolute
   'float   {:bits         32
             :min-absolute 1.4E-45
             :min         -3.4028235E38
             :max          3.4028235E38
             :min-int     -16777216 ; -2^24
             :max-int      16777216 ;  2^24
   #?@(:clj [:array-ident  "F"
             :outer-type  "[F"
             :boxed       java.lang.Float
             :unboxed     Float/TYPE])}
   'double  {:bits        64
             ; Because:
             ; Double/MIN_VALUE        = 4.9E-324
             ; (.-MIN_VALUE js/Number) = 5e-324
             :min-absolute #?(:clj  Double/MIN_VALUE
                              :cljs (.-MIN_VALUE js/Number))
             :min         -1.7976931348623157E308
             :max          1.7976931348623157E308 ; Max number in JS
             :min-int     -9007199254740992 ; -2^53
             :max-int      9007199254740992 ;  2^53
   #?@(:clj [:array-ident  "D"
             :outer-type  "[D"
             :boxed       java.lang.Double
             :unboxed     Double/TYPE])}})

(def primitive-type-meta unboxed-symbol->type-meta)

(def array-ident->primitive-sym
  (->> unboxed-symbol->type-meta (map (juxt (rcomp val :array-ident) key)) (into {})))

(def elem-types-clj
  (->> unboxed-symbol->type-meta
       (map (fn [[k v]] [(:outer-type v) k]))
       (reduce
         (fn [m [k v]]
           (assoc m k v (symbol k) v))
         {})))

#?(:clj
(def boxed-types
  (->> unboxed-symbol->type-meta
       (map (fn [[k v]] [k (:boxed v)]))
       (into {}))))

#?(:clj
(def unboxed-types
  (zipmap (vals boxed-types) (keys boxed-types))))

#?(:clj
(def boxed->unboxed-types-evaled
  (->> unboxed-symbol->type-meta vals (map (juxt :boxed :unboxed)) (into {}) eval)))

(def max-values
  (->> unboxed-symbol->type-meta
       (map (fn [[k v]] [k (:max v)]))
       (into {})))

#?(:clj
(def promoted-types
  {'short  'int
   'byte   'short ; Because char is unsigned
   'char   'int
   'int    'long
   'float  'double}))

;; TODO move this
(defn max-type [types]
  (->> types
       (map (fn [type] [(get max-values type) type]))
       (remove (fn-> first nil?))
       (into (core/sorted-map-by >))
       first val))

#?(:clj (def class->str (fn-> str (.substring 6))))

