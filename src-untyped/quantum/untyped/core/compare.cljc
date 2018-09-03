(ns quantum.untyped.core.compare
  "General comparison operators and constants"
  (:refer-clojure :exclude [==])
  (:require
    [quantum.untyped.core.core :as ucore
      :refer [defaliases]]
    [quantum.untyped.core.fn
      :refer [fn']]))

(ucore/log-this-ns)

(def == identical?)
(def not== (comp not identical?))

(def ^:const <ident -1)
(def ^:const =ident  0)
(def ^:const >ident  1)
(def ^:const ><ident 2)
(def ^:const <>ident 3)

(def comparisons #{<ident =ident >ident ><ident <>ident})
(def comparison? comparisons)

(defn invert [c #_comparison? #_> #_comparison?]
  (case c
    -1      >ident
     1      <ident
    (0 2 3) c))

(defn comparison<     [c] (= c <ident))
(defn comparison<=    [c] (or (= c <ident) (= c =ident)))
(defn comparison=     [c] (= c =ident))
(defn comparison-not= [c] (not= c =ident))
(defn comparison>=    [c] (or (= c >ident) (= c =ident)))
(defn comparison>     [c] (= c >ident))
(defn comparison><    [c] (= c ><ident))
(defn comparison<>    [c] (= c <>ident))

(defn compf<     [compf x0 x1] (comparison<     (compf x0 x1)))
(defn compf<=    [compf x0 x1] (comparison<=    (compf x0 x1)))
(defn compf=     [compf x0 x1] (comparison=     (compf x0 x1)))
(defn compf-not= [compf x0 x1] (comparison-not= (compf x0 x1)))
(defn compf>=    [compf x0 x1] (comparison>=    (compf x0 x1)))
(defn compf>     [compf x0 x1] (comparison>     (compf x0 x1)))
(defn compf><    [compf x0 x1] (comparison><    (compf x0 x1)))
(defn compf<>    [compf x0 x1] (comparison<>    (compf x0 x1)))

(defn comp<     [x0 x1] (compf<     compare x0 x1))
(defn comp<=    [x0 x1] (compf<=    compare x0 x1))
(defn comp=     [x0 x1] (compf=     compare x0 x1))
(defn comp-not= [x0 x1] (compf-not= compare x0 x1))
(defn comp>=    [x0 x1] (compf>=    compare x0 x1))
(defn comp>     [x0 x1] (compf>     compare x0 x1))
(defn comp><    [x0 x1] (compf><    compare x0 x1))
(defn comp<>    [x0 x1] (compf<>    compare x0 x1))

(def class->comparator
  {#?@(:clj
        [Class (fn [^Class a ^Class b]
                 (.compareTo (.getName a) (.getName b)))])})
