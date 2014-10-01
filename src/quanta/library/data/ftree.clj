(ns quanta.library.data.ftree
  (:require
    [quanta.library.ns        :as ns    :refer [defalias]]
    [clojure.data.finger-tree :as ftree :refer [double-list counted-double-list]])
  (:gen-class))

(defalias dlist double-list) ; a sequential collection that provides constant-time access to both the left and right ends.
(defalias c-dlist counted-double-list) ; all the features of double-list plus constant-time count and log-n nth

;(require '[clojure.data.finger-tree])