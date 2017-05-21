(ns quantum.apis.github.core
  (:require
    #?(:clj [clojure.java.shell :refer [sh]])
            [clojure.string :as str]))

; REMOTE

; LOCAL

#?(:clj
(defn git-hash
  "Returns current revision's git hash"
  {:source 'clojure.core.matrix.impl.common}
  []
  (-> (sh "git" "log" "--pretty=format:'%H'" "-n 1") ; TODO move to quantum `proc!`
      :out
      (str/replace #"'" ""))))
