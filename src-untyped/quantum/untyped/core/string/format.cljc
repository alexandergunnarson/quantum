(ns quantum.untyped.core.string.format
  (:require
    [quantum.untyped.core.error :as uerr]
    [quantum.untyped.core.vars
      :refer [defalias]]))

(defn >lower-case
  "Converts string to all lower-case.
   Works in strictly locale independent way.
   If you want a localized version, use `>locale-lower`."
  [x]
  (if (string? x)
      (.toLowerCase ^String x)
      (uerr/not-supported! `>lower-case x)))

(defalias >lower >lower-case)

(defn >upper-case
  "Converts string to all upper-case.
   Works in strictly locale independent way.
   If you want a localized version, use `>locale-upper`."
  [x]
  (if (string? x)
      (.toUpperCase ^String x)
      (uerr/not-supported! `>upper-case x)))

(defalias >upper >upper-case)
