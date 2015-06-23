(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.type.core
  (:require-quantum [ns log pr err map set logic fn])
  (:require [quantum.core.type.bootstrap :as boot])
  #?(:cljs
      (:require-macros [quantum.core.type.bootstrap :as boot])))

#?(:cljs (def class type))

(boot/def-types #?(:clj :clj :cljs :cljs))

(defn listy? [obj] (seq? obj)
  #_(->> obj class
         (contains? (get types 'listy?))))

(defn name-from-class
  [class-0]
#?(:clj
     (let [^String class-str (str class-0)]
       (-> class-str
           (subs (-> class-str (.indexOf " ") inc))
           symbol))
   :cljs
     (if (-> types (get 'primitive?) (contains? class-0))
         (or (get primitive-types class-0)
             (throw+ {:msg (str "Class " (type->str class-0) " not found in primitive types.")}))
         (-> class-0 type->str symbol))))

