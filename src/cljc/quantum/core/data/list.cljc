(ns quantum.core.data.list
  (:require-quantum [:core])
  (:require [quantum.core.data.finger-tree :as ftree]))

; brandonbloom: Benchmarks suggest that the rrb-vector
; deque is twice as fast as finger trees version for
;realistic EDN data.
(defalias dlist ftree/counted-double-list)
(defalias conjl ftree/conjl)