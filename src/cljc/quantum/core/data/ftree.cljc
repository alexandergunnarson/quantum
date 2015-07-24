(ns
  ^{:doc "Finger tree data structures, like double lists.

          With the widespread use of vectors, this namespace may not be especially useful."
    :attribution "Alex Gunnarson"}
  quantum.core.data.ftree
  (:require-quantum [ns])
  (:require
    #?(:clj [clojure.data.finger-tree :as ftree])))
; a sequential collection that provides constant-time access to both the left and right ends.
#?(:clj (defalias u-dlist ftree/double-list))
; all the features of double-list plus constant-time count and log-n nth
#?(:clj (defalias dlist ftree/counted-double-list))