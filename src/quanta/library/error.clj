(ns quanta.library.error
  (:require
  	[slingshot.slingshot :as try-catch]
  	[quanta.library.ns :refer [defalias]])
  (:gen-class))

(defalias try+   try-catch/try+)
(defalias throw+ try-catch/throw+)

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

; PUT ERROR TYPES HERE
(defn unk-dispatch [dispatch]
  {:type :unk-dispatch
   :message (str "Unknown dispatch function '" (name dispatch) "' requested.")})
(defn unk-key [k]
  {:type :unk-key
   :message (str "Unknown dispatch key '"      (name k)        "' requested.")})
