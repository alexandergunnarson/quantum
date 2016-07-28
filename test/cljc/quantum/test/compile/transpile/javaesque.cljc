(ns quantum.test.compile.transpile.javaesque
  (:require [quantum.compile.transpile.javaesque :refer :all])
  (:require
    [quantum.compile.transpile.to.java    :as javac]
    [quantum.compile.transpile.to.c-sharp :as csc  ]
    [quantum.compile.transpile.util       :as util ]
    [quantum.compile.transpile.core       :as comp ]
    [quantum.core.string                  :as str  ]
    [quantum.core.io.core                 :as io   ]))

(defn ^String test:class-str [access ^String class-name & [^String contents]])

#_(:clj
(defn test:emit!
  [in-path-vec out-path-vec lang])

#_(:clj
(defn test:emit-std! [])
