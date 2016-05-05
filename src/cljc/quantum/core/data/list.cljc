(ns quantum.core.data.list
           (:require [quantum.core.data.finger-tree :as ftree]
                     [quantum.core.vars             :as var
                       :refer [#?(:clj defalias)]            ]))
  #?(:cljs (:require-macros
                     [quantum.core.vars             :as var
                       :refer [defalias]                     ]))

; brandonbloom: Benchmarks suggest that the rrb-vector
; deque is twice as fast as finger trees version for
; realistic EDN data.
(defalias dlist ftree/counted-double-list)
(defalias conjl ftree/conjl)