(ns quantum.ai.ml.core
  (:refer-clojure :exclude
    [assoc!, count])
  (:require
    [quantum.core.collections      :as coll
      :refer [assoc!, count
              reducei-2]]
    [quantum.core.collections.core :as ccoll
      :refer [->objects-nd]]
    [quantum.core.data.validated   :as dv]
    [quantum.core.fn
      :refer [fn1]]
    [quantum.core.macros
      :refer [defnt]]
    [quantum.core.type             :as t]))

(dv/def instances (fn1 t/sequential?)) ; TODO better validation
(dv/def targets   (fn1 t/sequential?)) ; TODO better validation

; type may be #{:nominal :discrete :continuous}
; `discrete?` is true for discrete-continuous, and for nominal
(defrecord Attribute
  [name type ^boolean discrete?
   ^java.util.Map str->enum ^java.util.Map enum->str
   ^long i min max])

(def ->attribute map->Attribute)

(defnt discrete?   [^Attribute x] (:discrete? x))
(defnt continuous? [^Attribute x] (-> x :type (= :continuous)))
(defnt nominal?    [^Attribute x] (-> x :type (= :nominal)))

(defrecord Instance+Label [x• l])
(defn ->x•+l [x• l] (Instance+Label. x• l))

(defn -><x•+l>• [x•• l•]
  (reducei-2 (fn [ret a b i] (assoc! ret i (->x•+l (double-array a) b))) ; TODO need to ensure if it's already doubles, just return the original
             (->objects-nd (count x••))
             x•• l• true))

#_"

instances          labels
[a b c d e f g] -> [h i j]
[a b c d e f g] -> [h i j]

x••   : instances

l•◦   : all unique label values (a.k.a. all output classes)
l••   : labels (a.k.a targets or outputs)
l     : label value (a.k.a. output class)

a•:x  : all (implied to be unique) instance attributes (a.k.a. features), in order
a:x   : instance attribute

a•:l  : all (implied to be unique) label attributes (a.k.a. features), in order
a:l   : label attribute

c•:l  : all classes of the labels (same as `l◦`)
c:l   : a class of the labels (same as `l`)
c••:a : all classes of all attributes (a.k.a. features)
c•:a  : all classes of a particular attribute (a.k.a. feature)
"
