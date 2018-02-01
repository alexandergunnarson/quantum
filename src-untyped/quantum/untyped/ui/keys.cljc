(ns quantum.untyped.ui.keys
  (:require
    [clojure.set :as set]))

(def code>label
  {9  :tab
   13 :return
   14 :enter
   27 :escape
   37 :left
   38 :up
   39 :right
   40 :down})

(def label>code (set/map-invert code>label))
