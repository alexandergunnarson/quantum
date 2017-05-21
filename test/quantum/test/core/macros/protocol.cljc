(ns quantum.test.core.macros.protocol
  (:require
    [quantum.core.macros.protocol :as ns]
    [quantum.core.log :as log
      :include-macros true]
    [#?(:clj clojure.test
        :cljs cljs.test)
      :refer        [#?@(:clj [deftest is testing])]
      :refer-macros [deftest is testing]]))

(defn test:ensure-protocol-appropriate-type-hint [arg lang i])

(defn test:ensure-protocol-appropriate-arglist [lang arglist-0])

(deftest test:append-variant-identifier
  (is (= (ns/append-variant-identifier 'test-protocol 3)
         'test-protocol__3)))
  
(defn test:gen-extend-protocol-from-interface
  [{:keys [genned-protocol-name genned-protocol-method-name
           reify-body lang first-types]}])