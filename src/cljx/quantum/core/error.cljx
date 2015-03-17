(ns quantum.core.error
  (:require
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]  
    #+clj [slingshot.slingshot :as try-catch])
  #+clj
  (:import
    clojure.core.Vec
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

#+clj (defalias try+   try-catch/try+)
#+clj (defalias throw+ try-catch/throw+)

#+cljs (defn throw+ [err] (throw err))

; NEED MORE MACRO EXPERIENCE TO DO THIS
; (defmacro catch-or
;   "Like /catch/, but catches multiple given exceptions in the same way."
;   {:in "[[[:status 401] {:keys [status]}]
;          [[:status 403] {:keys [status]}]
;          [[:status 500] {:keys [status]}]]
;         (handle-http-error status)"}
;   [exception-keys-pairs func]
;   (for [[exception-n# keys-n#] exception-keys-pairs]
;      `(catch exception-n# keys-n# func)))

(defn with-throw
  "Throws an exception with the given message @message if
   @expr evaluates to false.

   Specifically for use with :pre and :post conditions."
  [expr throw-content]
  `(if ~expr ~expr (throw+ ~throw-content)))

; ====== ERROR TYPES =======

(defn unk-dispatch [dispatch]
  {:type :unk-dispatch
   :message (str "Unknown dispatch function '" (name dispatch) "' requested.")})
(defn unk-key [k]
  {:type :unk-key
   :message (str "Unknown dispatch key '"      (name k)        "' requested.")})
