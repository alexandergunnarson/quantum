(ns quantum.core.data.ftree
  (:require
    [quantum.core.ns        :as ns    :refer [defalias]]
    [clojure.data.finger-tree :as ftree :refer [double-list counted-double-list]])
  (:gen-class))
; a sequential collection that provides constant-time access to both the left and right ends.
(defalias dlist   double-list)
; all the features of double-list plus constant-time count and log-n nth
(defalias c-dlist counted-double-list)