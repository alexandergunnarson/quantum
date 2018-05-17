(ns quantum.core.print.prettier
  (:require
    [quantum.core.vars
      :refer [defalias]]
    [quantum.untyped.core.print.prettier :as u]))

(defalias u/extend-pretty-printing!)
