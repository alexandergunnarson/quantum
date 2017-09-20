(ns quantum.core.untyped.convert
  (:require
    [quantum.core.core :as qcore
      :refer [namespace?]]
    [quantum.core.fn   :as fn
      :refer [fn']]
    [quantum.core.logic
      :refer [condf1 fn-not]]
    [quantum.core.vars :as var
      :refer [defalias]]))

(defn ->name [x]
  (cond   (nil? x)       ""
#?@(:clj [(namespace? x) (-> x ns-name name)
          (var? x)       (apply symbol
                           (map str
                             ((juxt (comp ns-name :ns)
                                    :name)
                                    (meta x))))
          (fn? x)        #?(:clj  (-> x class .getName clojure.main/demunge)
                            :cljs (if (-> x .-name str/blank?)
                                      "<anonymous>"
                                      (-> x .-name demunge-str)))])
          :else          (name x)))

(defalias ->keyword qcore/->keyword)

(defn ->symbol [x]
  (cond   (symbol? x)    x
          (string? x)    (symbol x)
#?@(:clj [(namespace? x) (ns-name x)])
          (fn? x)        (-> x ->name symbol)
          :else          (-> x str symbol)))

(def ensure-set
  (condf1
    nil?          (fn' #{})
    (fn-not set?) hash-set
    identity))
