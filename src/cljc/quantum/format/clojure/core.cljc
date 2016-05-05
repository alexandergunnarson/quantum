(ns quantum.format.clojure.core
  (:refer-clojure :exclude [format])
  #_(:require [quantum.core.analyze.clojure.predicates :refer :all]
            [quantum.core.analyze.clojure.core       :refer :all])
  #?(:clj (:import com.carrotsearch.hppc.CharArrayDeque)))

#_(def file-loc "/Users/alexandergunnarson/Development/Source Code Projects/quanta-test/test/temp.java")
#_(def parsed (-> (java.io.File. ^String file-loc) quantum.compile.java/parse quantum.compile.java/clean))

#_(defn format
  ([x] (String. (.toArray ^CharArrayDeque (format x "" 0 false))))
  ([x s i split?]
    (concat! s ; otherwise you end up appending in weird places...
      (if (sequential? x)
          (condf x
            do-statement?
              (paren+
                (sp+ (-> x first str) (rest x)))
            defn-statement?
              (constantly nil)
            :else (constantly nil))
          x)))) ; will get converted to a string anyway





