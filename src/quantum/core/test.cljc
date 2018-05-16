(ns quantum.core.test
  (:require
    [clojure.test       :as test]
    [quantum.core.vars
      :refer [defaliases]]
    [quantum.untyped.core.test :as u]))

; TO EXPLORE
; - A/B testing
;   - https://github.com/ptaoussanis/touchstone
;   - https://github.com/facebook/planout
;   - https://xamarin.com/test-cloud
; - Mock data
;   - Ring requests
;     - https://github.com/ring-clojure/ring-mock
;     - myfreeweb/clj-http-fake
; ===========================

#?(:clj (defaliases u is is= deftest defspec-test testing test-syms! test-ns test-nss-where throws))
