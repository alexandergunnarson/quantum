(ns
  quantum.core.numeric.strict-args
  "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc.
   All vars in this namespace are strict-arg vars (i.e., are guaranteed
   not to rely on protocol dispatch.)"
  {:attribution "alexandergunnarson"}
  (:refer-clojure :exclude
    [* *' + +' - -' / < > <= >= == rem inc dec zero? neg? pos? pos-int?
     min max quot mod format
     #?@(:clj  [bigint biginteger bigdec numerator denominator inc' dec'])])
  (:require
    [clojure.core                      :as c]
    [quantum.core.data.numeric         :as dn]
    [quantum.core.vars                 :as var
      :refer [defalias defaliases]]
    [quantum.core.numeric.convert   ]
    [quantum.core.numeric.misc      ]
    [quantum.core.numeric.operators    :as op]
    [quantum.core.numeric.predicates]
    [quantum.core.numeric.trig      ]
    [quantum.core.numeric.truncate     :as trunc])
#?(:cljs
  (:require-macros
    [quantum.core.numeric.strict-args  :as self]))
#?(:clj
  (:import
    [java.nio ByteBuffer]
    [quantum.core Numeric] ; loops?
    [net.jafama FastMath]
    clojure.lang.BigInt
    java.math.BigDecimal)))
;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj (defalias +*   op/+*&))
#?(:clj (defalias +'   op/+'&))
#?(:clj (defalias +    op/+& ))

#?(:clj (defalias -*   op/-*&))
#?(:clj (defalias -'   op/-'&))
#?(:clj (defalias -    op/-& ))

#?(:clj (defalias **   op/**&))
#?(:clj (defalias *'   op/*'&))
#?(:clj (defalias *    op/*& ))

#?(:clj (defalias div* op/div*&))
#?(:clj (defalias div' op/div'&))
#?(:clj (defalias /    op/div& ))

#_(defaliases quantum.core.numeric.operators
  #?@(:clj [;inc*$ #_inc' inc$
            ;dec*$ #_dec' dec$
            ; abs'$ abs$
                 ])
            ;inc'$ dec'$
            )
