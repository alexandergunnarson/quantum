(ns
  ^{:doc "Printing functions such as fipp.edn (a fast pretty printer),
          `pr-attrs` (which prints the key attributes of a given object
          or expression, blacklisted printable objects (so you don't
          have to wait while you accidentally print out an entire database),
          and so on."
    :attribution "alexandergunnarson"}
  quantum.core.print
  (:require
    [quantum.untyped.core.print :as u]
    [quantum.untyped.core.vars  :as uvar
      :refer [defalias defaliases]]))


;; ===== Data and dynamic bindings ===== ;;

(defaliases u *blacklist *collapse-symbols?* *print-as-code?*)

;; ===== `println` varieties ===== ;;

(defalias u/js-println)

;; ===== `ppr` ===== ;;

(defalias u/ppr)

;; ===== `ppr` varieties ===== ;;

(defaliases u ppr-meta ppr-hints ppr-error ppr-str)

;; ===== Print groups ===== ;;

(defaliases u >group group?)
