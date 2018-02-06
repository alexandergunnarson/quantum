(ns
  ^{:doc "Simple logging capabilities. Keeps a global log, prints only if the
          level is enabled, etc.

          By no means a full-fledged logging system, but useful nonetheless."
    :attribution "alexandergunnarson"}
  quantum.core.log
  (:refer-clojure :exclude
    [pr])
  (:require
    [quantum.core.vars        :as var
      :refer [defalias defaliases]]
    [quantum.untyped.core.log :as u]))

;; ===== Data ===== ;;

(defaliases u *levels *log *outs)

;; ===== Log levels ===== ;;

(defaliases u enable! disable!)

;; ===== Actual printing and logging ===== ;;

(defalias u/pr*)

;; ===== Varieties of logging ===== ;;

#?(:clj
(defaliases u
  pr pr! ppr ppr!
  pr-no-trace pr-no-trace!
  pr-opts pr-opts! ppr-opts ppr-opts!
  ppr-meta ppr-meta! ppr-hints ppr-hints!
  prl prl! prlm prlm!))

;; ===== Level-specific macros ===== ;;

#?(:clj
(defaliases u
  always
  error error!
  warn warn!))

;; ===== `with` and wrap- macros ===== ;;

(defaliases u #?@(:clj [with-prl with-log-errors]) wrap-log-errors)

;; ===== Componentization ===== ;;

(defalias u/>log-initializer)

;; ===== Miscellaneous ===== ;;

#?(:clj (defaliases u this-ns))

