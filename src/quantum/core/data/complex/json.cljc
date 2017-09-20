(ns
  ^{:doc "A simple JSON library which aliases cheshire.core."
    :attribution "alexandergunnarson"}
  quantum.core.data.complex.json
  (:require
#?@(:clj
   [[cheshire.core            :as json]
    [cheshire.parse           :as json:parse]])
    [cognitect.transit        :as t]
    [quantum.core.collections :as coll]
    [quantum.core.fn          :as fn
      :refer [<- fn1]]
    [quantum.core.logic
      :refer [whenp]]
    [quantum.core.type        :as t
      :refer [val?]]))

; 2.888831 ms for Cheshire (on what?) vs. clojure.data.json : 7.036831 ms

(defn json->
  "JSON decode an object from @s."
  {:performance "Source: http://swannodette.github.io/2014/07/26/transit--clojurescript/
                 The performance [with transit-cljs] is 20-30X faster than
                 combining JSON.parse with cljs.core/js->clj."}
  ([x] (json-> x nil))
  ([x key-fn]
  #?(:clj  (json/parse-string x (or key-fn keyword))
     :cljs (when-not (empty? x)
             (->> x
                  (t/read (t/reader :json))
                  (<- whenp (val? key-fn)
                      (fn1 coll/apply-to-keys key-fn)))))))

(defn json->-with-start
  "Decodes a JSON-encoded string `s` starting at index `start-i`, inclusive."
  [^String s ^long start-i]
  (when s
    (json:parse/parse
      (.createParser ^com.fasterxml.jackson.core.JsonFactory
                     (or cheshire.factory/*json-factory*
                         cheshire.factory/json-factory)
                     (doto (java.io.StringReader. s) (.skip start-i)))
      nil nil nil)))

(defn ->json
  "JSON encode @x into a String."
  {:performance "Source: http://swannodette.github.io/2014/07/26/transit--clojurescript/
                 The performance [with transit-cljs] is 20-30X faster than
                 combining JSON.parse with cljs.core/js->clj."}
  ([x] (->json x nil))
  ([x opts]
    #?(:clj  (json/generate-string x opts)
       :cljs (t/write (t/writer :json-verbose) x))))
