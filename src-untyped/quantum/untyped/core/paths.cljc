(ns quantum.untyped.core.paths
  (:require
    [clojure.string              :as str]
    [quantum.untyped.core.core   :as ucore]
    [quantum.untyped.core.string :as ustr]
    [quantum.untyped.core.system :as usys]
    [quantum.untyped.core.vars
      :refer [defalias]]))

(ucore/log-this-ns)

(defn path
  "Joins system-specific string paths (file paths, etc.)
   ensuring correct separator interposition."
  {:usage '(path "foo/" "/bar" "baz/" "/qux/")
   :todo ["Configuration for system separator vs. 'standard' separator etc."]}
  [& parts]
  (apply ustr/join-once usys/separator parts))

(defn url-path
  [& parts]
  (apply ustr/join-once "/" parts))
