(ns quantum.core.refs
  (:refer-clojure :exclude
    [deref
     atom add-watch
     agent agent-error await await-for
     alter io! sync dosync ensure ref-set
     var-set])
  (:require
    [clojure.core      :as core]
    [quantum.core.vars
      :refer [defalias]]))

(defalias deref     core/deref)

; ===== ATOMS ===== ;

(defalias atom      core/atom)
(defalias add-watch core/add-watch)

; ===== AGENTS ===== ;

#?(:clj (defalias agent       core/agent))
#?(:clj (defalias agent-error core/agent-error))
#?(:clj (defalias await       core/await))
#?(:clj (defalias await-for   core/await-for))
#?(:clj (defalias commute     core/commute))

; ===== REFS ===== ;

#?(:clj (defalias alter       core/alter  ))
#?(:clj (defalias io!         core/io!    ))
#?(:clj (defalias sync        core/sync   ))
#?(:clj (defalias dosync      core/dosync ))
#?(:clj (defalias ensure      core/ensure ))
#?(:clj (defalias ref-set     core/ref-set))

; ===== VARS ===== ;

#?(:clj (defalias var-set     core/var-set))
