(ns quantum.core.data.ftree
  (:require
    #+clj [quantum.core.ns        :as ns    :refer [defalias]]
    #+clj
    [clojure.data.finger-tree :as ftree :refer [double-list counted-double-list]])
  #+clj (:gen-class))
; a sequential collection that provides constant-time access to both the left and right ends.
#+clj
(defalias dlist   double-list)
; all the features of double-list plus constant-time count and log-n nth
#+clj
(defalias c-dlist counted-double-list)