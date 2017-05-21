(ns quantum.core.data.primitive
  (:require
    [quantum.core.macros :refer [defnt]]))

(defnt ->min-magnitude
  #?(:clj ([^byte   x]          (byte  0)))
  #?(:clj ([^char   x]          (char  0)))
  #?(:clj ([^short  x]          (short 0)))
  #?(:clj ([^int    x]          (int   0)))
  #?(:clj ([^long   x]          (long  0)))
  #?(:clj ([^float  x]          Float/MIN_VALUE    ))
          ([^double x] #?(:clj  Double/MIN_VALUE
                          :cljs js/Number.MIN_VALUE)))

#?(:clj (def ^:const min-float  (- Float/MAX_VALUE)))
        (def ^:const min-double (- #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

(defnt ->min-value
  #?(:clj ([^byte   x] Byte/MIN_VALUE     ))
  #?(:clj ([^char   x] Character/MIN_VALUE))
  #?(:clj ([^short  x] Short/MIN_VALUE    ))
  #?(:clj ([^int    x] Integer/MIN_VALUE  ))
  #?(:clj ([^long   x] Long/MIN_VALUE     ))
  #?(:clj ([^float  x] min-float          ))
          ([^double x] min-double         ))

(defnt ->max-value
  #?(:clj ([^byte   x]          Byte/MAX_VALUE     ))
  #?(:clj ([^char   x]          Character/MAX_VALUE))
  #?(:clj ([^short  x]          Short/MAX_VALUE    ))
  #?(:clj ([^int    x]          Integer/MAX_VALUE  ))
  #?(:clj ([^long   x]          Long/MAX_VALUE     ))
  #?(:clj ([^float  x]          Float/MAX_VALUE    ))
          ([^double x] #?(:clj  Double/MAX_VALUE
                          :cljs js/Number.MAX_VALUE)))
