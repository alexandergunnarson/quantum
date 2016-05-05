(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.classes
  (:refer-clojure :exclude [name])
  (:require [quantum.core.collections.base :as cbase
              :refer [name default-zipper camelcase ns-qualify zip-reduce
                      comparators ensure-set]]
            [quantum.core.classes.reg      :as class-reg]
            [quantum.core.data.map         :as map      ]
            [quantum.core.data.set         :as set      ]))

; ; PACKAGE RESOLUTION
; ; clojure (class @clojure.lang.Compiler/LOADER)
; ; java (ClassLoader/getSystemClassLoader)

; ; (ClassLoader/getSystemClassLoader)
; #?(:clj (defalias clojure-classes-unevaled class-reg/clojure-classes-unevaled))
; #?(:clj (defalias java-classes-unevaled class-reg/java-classes-unevaled))

; #?(:clj (defn class->symbol [^Class c] (-> c .getName symbol)))

; #?(:clj
; (defn supers-symbols [class-sym]
;   (->> class-sym eval supers (map class->symbol) (into #{}))))

; #?(:clj
; (defn classes->children [classes]
;   (->> (reduce (fn [ret [child supers-n]]
;          (reduce
;             (fn [ret-n s]
;               (update ret-n s (fn-> ensure-set (conj child))))
;             ret supers-n))
;          {}
;          (->> classes
;               (map (juxt identity
;                          supers-symbols))
;               (into {})))
;        (map (fn [[k v]] [k (disj v nil)]))
;        (into (map/sorted-map)))))

; #?(:clj
; (defn package-resolve [class-name]
;   (reduce 
;     (fn [ret ^Package p]
;       (let [pack    (.getName p)
;           tentative (str pack "." class-name)]
;         (try (reduced (conj ret (Class/forName tentative)))
;           (catch ClassNotFoundException e ret))))
;     #{}
;     (Package/getPackages))))

; #?(:clj
; (def class-children-unevaled
;   (classes->children (set/union java-classes-unevaled clojure-classes-unevaled))))

; #?(:clj
; (defn common-limiting-superclass
;   "Akin to greatest common factor (GCF) for classes.
;    Used in reducing code size for |defnt|."
;   {:todo "Implement highest limiting superclass."}
;   [classes]
;   (let [common-direct-superclasses
;          (->> classes (map supers-symbols)
;               (apply set/intersection))]
;     (reduce
;       (fn [ret superclass]
;         (let [unaccounted-for-classes
;                (set/difference (get class-children-unevaled superclass) classes)]
;           (when (empty? unaccounted-for-classes)
;             (reduced superclass))))
;       nil
;       common-direct-superclasses))))

; #?(:clj
; (defn all-implementing-classes* [subs visited leaves]
;   (if (empty? subs)
;       [visited leaves]
;       (let [sub (first subs)]
;         (let [children (get class-children-unevaled sub)
;               [visited-n+1 leaves-n+1]
;                  (cond
;                    (contains? visited sub)
;                      [nil nil]
;                    (empty? children)
;                      [#{sub} #{sub}]
;                    :else (all-implementing-classes* children visited leaves))]
;            (recur (rest subs)
;                   (set/union visited (conj visited-n+1 sub))
;                   (set/union leaves  leaves-n+1)))))))

; #?(:clj
; (def- all-implementing-leaf-classes-entry
;   (memoize
;     (fn [class-sym]
;       (all-implementing-classes*
;         (get class-children-unevaled class-sym)
;         #{} #{})))))

; #?(:clj
; (defn all-implementing-leaf-classes
;   "Subclass leaf nodes for class sym."
;   [class-sym]
;   (-> class-sym all-implementing-leaf-classes-entry second)))

; #?(:clj
; (defn all-implementing-descendant-classes
;   "Subclass descendant nodes for class sym."
;   [class-sym]
;   (-> class-sym all-implementing-leaf-classes-entry first)))

#?(:clj
(defn ancestor-list
  "Lists the direct ancestors of a class
  (ancestor-list clojure.lang.PersistentHashMap)
  => [clojure.lang.PersistentHashMap
      clojure.lang.APersistentMap
      clojure.lang.AFn
      java.lang.Object]"
  {:source "zcaudate/hara.class.inheritance"}
  ([cls] (ancestor-list cls []))
  ([^java.lang.Class cls output]
     (if (nil? cls)
       output
       (recur (.getSuperclass cls) (conj output cls))))))

; #?(:clj
; (defn ancestor-tree
;   "Lists the hierarchy of bases and interfaces of a class.
;   (ancestor-tree Class)
;   => [[java.lang.Object #{java.io.Serializable
;                           java.lang.reflect.Type
;                           java.lang.reflect.AnnotatedElement
;                           java.lang.reflect.GenericDeclaration}]]
;   "
;   {:source "zcaudate/hara.class.inheritance"}
;   ([cls] (ancestor-tree cls []))
;   ([^Class cls output]
;      (let [base (.getSuperclass cls)]
;        (if-not base output
;                (recur base
;                       (conj output [base (-> (.getInterfaces cls) seq set)])))))))


; #?(:clj
; (defn best-match
;   "finds the best matching interface or class from a list of candidates
;   (best-match #{Object} Long) => Object
;   (best-match #{String} Long) => nil
;   (best-match #{Object Number} Long) => Number"
;   {:source "zcaudate/hara.class.inheritance"}
;   [candidates ^Class cls]
;   (or (get candidates cls)
;       (->> (apply concat (ancestor-tree cls))
;            (map (fn [v]
;                   (if (set? v)
;                     (first (set/intersection v candidates))
;                     (get candidates v))))
;            (filter identity)
;            first))))

