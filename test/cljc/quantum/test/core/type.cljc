(ns quantum.test.core.type
  (:require [quantum.core.type :as ns]))

(defn test:instance+? [x])

        (defn test:byte-array? [x])
#?(:clj (defn test:bigint?     [x]))
#?(:clj (defn test:file?       [x]))
        (defn test:hash-map?   [x])
        (defn test:sorted-map? [x])
        (defn test:boolean?    [x])
        (defn test:listy?      [x])
        (defn test:vector?     [x])
        (defn test:set?        [x])
        (defn test:hash-set?   [x])
        (defn test:map?        [x])
        (defn test:array-list? [x])
        (defn test:queue?      [x])
        (defn test:lseq?       [x])
        (defn test:pattern?    [x])
        (defn test:regex?      [x])
        (defn test:editable?   [x])
        (defn test:transient?  [x])
        (defn test:array?      [x])
#?(:clj (defn test:prim-long?  [x]))
        (defn test:double?     [x])
#?(:clj (defn test:indexed?    [x]))
#?(:clj (defn test:throwable?  [x]))
        (defn test:error?      [x])
#?(:clj
(defn test:interface?
  [^java.lang.Class class-]))

#?(:clj
(defn test:abstract?
  [^java.lang.Class class-]))

#?(:clj (defn test:multimethod? [x]))

#?(:clj
(defn test:protocol?
  [obj]))

#?(:clj
(defn test:promise?
  [^Object obj]))
; ===== JAVA =====

#?(:clj
(defn test:enum?
  [type]))

; ; ======= TRANSIENTS =======

(defn test:should-transientize? [coll])

(defn test:identity [x])

(defn test:->pred [x])

(defn test:->literal [x])

(defn test:->base [x])

(defn test:transient!*  [x])
(defn test:persistent!*  [x])

(defn test:transient-fns [coll])

(defn test:recommended-transient-fns [coll])

(defn test:->joinable [x])