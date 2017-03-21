(ns quantum.core.refs
  (:refer-clojure :exclude
    [deref
     atom add-watch set-validator!
     agent agent-error await await-for
     alter io! sync dosync ensure ref-set error-handler error-mode set-error-mode!
       set-error-mode! set-agent-send-executor! set-agent-send-off-executor!
     var-set])
  (:require
    [clojure.core      :as core]
    [quantum.core.vars
      :refer [defalias]]))

(defalias deref     core/deref)

; ===== ATOMS ===== ;

(defalias atom           core/atom)
(defalias add-watch      core/add-watch)
(defalias set-validator! core/set-validator!)

; ===== AGENTS ===== ;

#?(:clj (defalias agent       core/agent))
#?(:clj (defalias agent-error core/agent-error))
#?(:clj (defalias await       core/await))
#?(:clj (defalias await-for   core/await-for))
#?(:clj (defalias commute     core/commute))

; ===== REFS ===== ;

#?(:clj (defalias alter                        core/alter  ))
#?(:clj (defalias io!                          core/io!    ))
#?(:clj (defalias sync                         core/sync   ))
#?(:clj (defalias dosync                       core/dosync ))
#?(:clj (defalias ensure                       core/ensure ))
#?(:clj (defalias ref-set                      core/ref-set))
#?(:clj (defalias error-handler                core/error-handler))
#?(:clj (defalias set-error-handler!           core/set-error-handler!))
#?(:clj (defalias error-mode                   core/error-mode))
#?(:clj (defalias set-error-mode!              core/set-error-mode!))
#?(:clj (defalias set-agent-send-executor!     core/set-agent-send-executor!))
#?(:clj (defalias set-agent-send-off-executor! core/set-agent-send-off-executor!))

; ===== VARS ===== ;

#?(:clj (defalias var-set            core/var-set))



