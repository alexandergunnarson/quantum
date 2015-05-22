(ns
  ^{:doc "Error handling. Improved try/catch, and built-in error types for convenience's sake."
    :attribution "Alex Gunnarson"}
  quantum.core.error
  (:require-quantum [ns])
  (:require
    #?(:clj [slingshot.slingshot :as try-catch])))

; #?@(:clj
;   []) ; splicing in definition doesn't work... ugh

(defrecord Err [type msg objs])

#?(:clj (defmalias try+   try-catch/try+))
#?(:clj (defmalias throw+ try-catch/throw+))

#?(:cljs (defn throw+ [err] (throw err)))

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

#?(:clj
(defmacro with-throw
  "Throws an exception with the given message @message if
   @expr evaluates to false.

   Specifically for use with :pre and :post conditions."
  {:attribution "Alex Gunnarson"}
  [expr throw-content]
  `(if ~expr ~expr (throw+ ~throw-content))))

#?(:clj
(defmacro with-catch
  {:usage '(->> 0 (/ 1) (with-catch (constantly -1)))}
  [^Fn handler try-val]
  `(try ~try-val
     (catch Error e# (~handler e#)))))

#?(:clj
  (defmacro try-or 
    "An exception-handling version of the 'or' macro.
     Trys expressions in sequence until one produces a result that is neither false nor an exception.
     Useful for providing a default value in the case of errors."
    {:attribution "mikera.cljutils.error"}
    ([exp & alternatives]
       (if-let [as (seq alternatives)] 
         `(or (try ~exp (catch Throwable t# (try-or ~@as))))
         exp))))
  
; ====== ERROR TYPES =======

; TODO modify these to be records in order to reduce map-creation overhead.

(defn unk-dispatch [dispatch]
  {:type :unk-dispatch
   :message (str "Unknown dispatch function '" (name dispatch) "' requested.")})
(defn unk-key [k]
  {:type :unk-key
   :message (str "Unknown dispatch key '"      (name k)        "' requested.")})
