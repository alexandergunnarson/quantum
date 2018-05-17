(ns quantum.untyped.core.data.array
          (:refer-clojure :exclude
            [array])
          (:require
            [clojure.core :as core])
  #?(:clj (:import
            [quantum.core.data Array])))

(defn ^"[Ljava.lang.Object;" *<>
  ([]                                      #?(:clj  (Array/newUninitialized1dObjectArray 0)
                                              :cljs #js []))
  ([a0]                                    #?(:clj  (Array/new1dObjectArray a0)
                                              :cljs #js                    [a0]))
  ([a0 a1]                                 #?(:clj  (Array/new1dObjectArray a0 a1)
                                              :cljs #js                    [a0 a1]))
  ([a0 a1 a2]                              #?(:clj  (Array/new1dObjectArray a0 a1 a2)
                                              :cljs #js                    [a0 a1 a2]))
  ([a0 a1 a2 a3]                           #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3)
                                              :cljs #js                    [a0 a1 a2 a3]))
  ([a0 a1 a2 a3 a4]                        #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3 a4)
                                              :cljs #js                    [a0 a1 a2 a3 a4]))
  ([a0 a1 a2 a3 a4 a5]                     #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3 a4 a5)
                                              :cljs #js                    [a0 a1 a2 a3 a4 a5]))
  ([a0 a1 a2 a3 a4 a5 a6]                  #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3 a4 a5 a6)
                                              :cljs #js                    [a0 a1 a2 a3 a4 a5 a6]))
  ([a0 a1 a2 a3 a4 a5 a6 a7]               #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3 a4 a5 a6 a7)
                                              :cljs #js                    [a0 a1 a2 a3 a4 a5 a6 a7]))
  ([a0 a1 a2 a3 a4 a5 a6 a7 a8]            #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3 a4 a5 a6 a7 a8)
                                              :cljs #js                    [a0 a1 a2 a3 a4 a5 a6 a7 a8]))
  ([a0 a1 a2 a3 a4 a5 a6 a7 a8 a9]         #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3 a4 a5 a6 a7 a8 a9)
                                              :cljs #js                    [a0 a1 a2 a3 a4 a5 a6 a7 a8 a9]))
  ([a0 a1 a2 a3 a4 a5 a6 a7 a8 a9 a10]     #?(:clj  (Array/new1dObjectArray a0 a1 a2 a3 a4 a5 a6 a7 a8 a9 a10)
                                              :cljs #js                    [a0 a1 a2 a3 a4 a5 a6 a7 a8 a9 a10])))
