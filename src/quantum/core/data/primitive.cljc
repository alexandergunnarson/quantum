(ns quantum.core.data.primitive
  (:refer-clojure :exclude
    [boolean? char? double? float? int?])
  (:require
    [quantum.core.type :as t
      :refer [defnt]]
    [quantum.core.vars
      :refer [def-]]))

;; TODO TYPED type coercion/casts should go in here

;; ===== Predicates ===== ;;

#?(:clj (def boolean? (t/isa? #?(:clj Boolean :cljs js/Boolean))))
#?(:clj (def byte?    (t/isa? Byte)))
#?(:clj (def short?   (t/isa? Short)))
#?(:clj (def char?    (t/isa? Character)))
#?(:clj (def int?     (t/isa? Integer)))
#?(:clj (def long?    (t/isa? Long)))
#?(:clj (def float?   (t/isa? Float)))
        (def double?  (t/isa? #?(:clj Double :cljs js/Number)))

        (def primitive? (t/or boolean? #?@(:clj [byte? short? char? int? long? float?]) double?))

#?(:clj (def comparable-primitive? (t/- primitive? boolean?)))

;; ===== Class relationships ===== ;;

#?(:clj
(def unboxed-class->boxed-class
  {Boolean/TYPE   Boolean
   Byte/TYPE      Byte
   Character/TYPE Character
   Long/TYPE      Long
   Double/TYPE    Double
   Short/TYPE     Short
   Integer/TYPE   Integer
   Float/TYPE     Float}))

#?(:clj
(def boxed-class->unboxed-class
  {Integer   Integer/TYPE
   Long      Long/TYPE
   Float     Float/TYPE
   Short     Short/TYPE
   Boolean   Boolean/TYPE
   Byte      Byte/TYPE
   Character Character/TYPE
   Double    Double/TYPE
   Void      Void/TYPE}))

;; ===== Extreme magnitudes and values ===== ;;

(defnt >min-magnitude
  #?(:clj ([x byte?   > byte?]            (byte  0)))
  #?(:clj ([x short?  > short?]           (short 0)))
  #?(:clj ([x char?   > char?]            (char  0)))
  #?(:clj ([x int?    > int?]             (int   0)))
  #?(:clj ([x long?   > long?]            (long  0)))
  #?(:clj ([x float?  > float?]           Float/MIN_VALUE))
          ([x double? > double?] #?(:clj  Double/MIN_VALUE
                                    :cljs js/Number.MIN_VALUE)))

#?(:clj (def- min-float  (- Float/MAX_VALUE)))
        (def- min-double (- #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

;; TODO TYPED for some reason it's not figuring out the type of `min-float` and `min-double`
#_(defnt >min-value
  #?(:clj ([x byte?   > byte?]   Byte/MIN_VALUE))
  #?(:clj ([x short?  > short?]  Short/MIN_VALUE))
  #?(:clj ([x char?   > char?]   Character/MIN_VALUE))
  #?(:clj ([x int?    > int?]    Integer/MIN_VALUE))
  #?(:clj ([x long?   > long?]   Long/MIN_VALUE))
  #?(:clj ([x float?  > float?]  min-float))
          ([x double? > double?] min-double))

(defnt >max-value
  #?(:clj ([x byte?   > byte?]            Byte/MAX_VALUE))
  #?(:clj ([x short?  > short?]           Short/MAX_VALUE))
  #?(:clj ([x char?   > char?]            Character/MAX_VALUE))
  #?(:clj ([x int?    > int?]             Integer/MAX_VALUE))
  #?(:clj ([x long?   > long?]            Long/MAX_VALUE))
  #?(:clj ([x float?  > float?]           Float/MAX_VALUE))
          ([x double? > double?] #?(:clj  Double/MAX_VALUE
                                    :cljs js/Number.MAX_VALUE)))
