(ns
  ^{:doc "A simple JSON library which aliases cheshire.core."
    :attribution "Alex Gunnarson"}
  quantum.core.data.complex.json
  (:require-quantum [:core err fn logic])
  (:require [clojure.string           :as str ]
            [cognitect.transit        :as t   ]
            [quantum.core.collections :as coll]
    #?(:clj [cheshire.core            :as json])))

; 2.888831 ms for Cheshire (on what?) vs. clojure.data.json : 7.036831 ms

(defn json->
  "JSON decode an object from @s."
  {:performance "Source: http://swannodette.github.io/2014/07/26/transit--clojurescript/
                 The performance [with transit-cljs] is 20-30X faster than
                 combining JSON.parse with cljs.core/js->clj."}
  ([x] (json-> x nil))
  ([x key-fn]
  #?(:clj  (json/parse-string x (or key-fn keyword))
     :cljs (when-not (str/blank? x)
             (->> x
                  (t/read (t/reader :json))
                  (<- whenp (nnil? key-fn)
                      (f*n coll/apply-to-keys key-fn)))))))

(defn ->json
  "JSON encode @x into a String."
  {:performance "Source: http://swannodette.github.io/2014/07/26/transit--clojurescript/
                 The performance [with transit-cljs] is 20-30X faster than
                 combining JSON.parse with cljs.core/js->clj."}
  ([x] (->json x nil))
  ([x opts]
    #?(:clj  (json/generate-string x opts)
       :cljs (t/write (t/writer :json-verbose) x))))
