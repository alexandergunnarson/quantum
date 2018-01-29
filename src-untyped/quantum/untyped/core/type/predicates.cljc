(ns quantum.untyped.core.type.predicates
  "For type predicates that are not yet turned into specs.
   TODO excise and place in `quantum.untyped.core.type`."
  (:refer-clojure :exclude [boolean? seqable?])
  (:require
    [clojure.core :as core]
    #_[quantum.untyped.core.core :as ucore]))

#_(ucore/log-this-ns)

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

(def val? some?)

(defn boolean? [x] #?(:clj  (instance? Boolean x)
                      :cljs (or (true? x) (false? x))))

(defn lookup? [x]
  #?(:clj  (instance? clojure.lang.ILookup x)
     :cljs (satisfies? ILookup x)))

#?(:clj
(defn protocol? [x]
  (and (lookup? x) (-> x (get :on-interface) class?))))

(defn regex? [x] (instance? #?(:clj java.util.regex.Pattern :cljs js/RegExp) x))

#?(:clj  (defn seqable?
           "Returns true if (seq x) will succeed, false otherwise."
           {:from "clojure.contrib.core"}
           [x]
           (or (seq? x)
               (instance? clojure.lang.Seqable x)
               (nil? x)
               (instance? Iterable x)
               (-> x class .isArray)
               (string? x)
               (instance? java.util.Map x)))
   :cljs (def seqable? core/seqable?))

(defn editable? [coll]
  #?(:clj  (instance?  clojure.lang.IEditableCollection coll)
     :cljs (satisfies? cljs.core.IEditableCollection    coll)))

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

(defn metable? [x]
  #?(:clj  (instance?  clojure.lang.IMeta x)
     :cljs (satisfies? cljs.core/IMeta    x)))

(defn with-metable? [x]
  #?(:clj  (instance?  clojure.lang.IObj   x)
     :cljs (satisfies? cljs.core/IWithMeta x)))

(defn derefable? [x]
  #?(:clj  (instance?  clojure.lang.IDeref x)
     :cljs (satisfies? cljs.core/IDeref    x)))
