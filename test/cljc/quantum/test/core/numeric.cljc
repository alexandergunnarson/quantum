(ns quantum.test.core.numeric
  (:require [quantum.core.numeric :as ns]))

(defn test:num-literals
  ([form])
  ([form1 form2 & more]))
;_____________________________________________________________________
;================={   MORE COMPLEX OPERATIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defn test:int-nil [n])

(defn test:evenly-divisible-by? [a b])
(defn test:exactly [x])
;_____________________________________________________________________
;==============={        OTHER OPERATIONS          }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defn test:safe+
  ([a    ])
  ([a b  ])
  ([a b c])
  ([a b c & args]))

(defn test:safe*
  ([a    ])
  ([a b  ])
  ([a b c])
  ([a b c & args]))

(defn test:safe-
  ([a])
  ([a b])
  ([a b c])
  ([a b c & args]))

(defn test:safediv
  ([a b  ])
  ([a b c])
  ([a b c & args]))

(defn test:whole-number? [n])

(defn test:divisible? [num div])
(defn test:indivisible? [num div])

(defn test:native-integer? [n])

(defn test:inverse [f])

(defn test:base [f])

;___________________________________________________________________________________________________________________________________
;=================================================={         MUTATION         }=====================================================
;=================================================={                          }=====================================================
(defn test:+= [x a])
(defn test:-= [x a])

(defn test:++ [x])
(defn test:-- [x])

;___________________________________________________________________________________________________________________________________
;=================================================={         DISPLAY          }=====================================================
;=================================================={                          }=====================================================
(defn test:display-num [x])

(defn test:format [n type])

(defn test:percentage-of [of total-n])

(defn test:percent? [n])
