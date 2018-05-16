(ns
  ^{:doc "Useful namespace and var-related functions."
    :attribution "alexandergunnarson"}
  quantum.core.ns
  (:refer-clojure :exclude
    [ns in-ns all-ns create-ns the-ns find-ns ns-name ns-map
     alias ns-aliases require import ns-imports use
     ns-interns ns-publics refer ns-refers refer-clojure ns-unalias ns-unmap loaded-libs
     remove-ns])
  (:require
    [quantum.untyped.core.ns   :as uns]
    [quantum.untyped.core.vars :as uvar
      :refer [defaliases]]))

(defaliases uns
  ns the-ns find-ns ns-name ns-map
  ns-map ns-unmap ns-unmap!
  in-ns all-ns
  create-ns create-ns!
  alias alias!
  ns-unalias ns-unalias!
  ns-aliases
  require require!
  import import!
  ns-imports
  use use!
  ns-interns
  ns-publics
  refer refer!
  refer-clojure refer-clojure!
  ns-refers
  remove-ns remove-ns!
  ;;
  the-alias ns>alias ns-name>alias
  clear-ns-interns! search-var ns-exclude
  with-ns with-temp-ns
  import-static load-ns load-nss
  loaded-libs load-lib! load-package! load-dep!
  assert-ns-aliased)

;; TODO type and enable
#_(:clj
(defn alias-ns
  "Create vars in the current namespace to alias each of the public vars in
  the supplied namespace.
  Takes a symbol."
  {:attribution "flatland.useful.ns"}
  [ns-name-]
  (require ns-name-)
  (doseq [[name var] (ns-publics (the-ns ns-name-))]
    (uvar/alias-var name var))))
