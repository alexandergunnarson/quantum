(ns quantum.test.core.data.validated
  (:require
    [#?(:clj clojure.test
        :cljs cljs.test)
      :refer        [#?@(:clj [deftest is testing])]
      :refer-macros [deftest is testing]]
    [quantum.core.data.validated :as ns]
    [quantum.core.print          :as pr
      :refer        [!]]
    [quantum.core.log            :as log
      :include-macros true]
    [quantum.core.logic
      :refer        [#?@(:clj [eq?])]
      :refer-macros [          eq?]]
    [quantum.core.fn
      :refer        [#?@(:clj [fn->])]
      :refer-macros [          fn->]]
    [quantum.core.error
      :refer        [#?@(:clj [catch-all])]
      :refer-macros [          catch-all]]
    [quantum.core.validate       :as v
      :refer        [#?@(:clj [validate defspec])]
      :refer-macros [          validate defspec]]
    [quantum.core.macros
      :refer        [#?@(:clj [defnt])]
      :refer-macros [          defnt]]))

; s/cat conforms to a map

(defspec ::odd?  (v/and integer? odd? ))
(defspec ::even? (v/and integer? even?))
(defspec ::a integer?)
(defspec ::b integer?)
(defspec ::c string?)
(defspec ::d (v/and integer? odd?))
(defspec ::e (v/and integer? even?))
(defspec ::f (v/and string? (fn-> count (= 5))))
#_(def shape1 (v/cat :forty-two #{42}
              :odds (v/+ ::odd?)
              :m (v/keys :req #_:req-un [::a ::b ::c])
              :oes (v/* (v/cat :o ::odd? :e ::even?))
              :ex (v/alt :odd ::odd? :even ::even?)))
; (validate s
;   [42 11 13 15 {::a 1 ::b 2 ::c 3} 1 2 3 42 43 44 11])
; {:forty-two 42,
;  :odds [11 13 15],
;  :m {:a 1, :b 2, :c 3},
;  :oes [{:o 1, :e 2} {:o 3, :e 42} {:o 43, :e 44}],
;  :ex {:odd 11}}

#?(:clj
(defmacro assert-message [msg & body]
  `(catch-all
     (do ~@body
         (assert false {:error "Was supposed to throw message" :message ~msg}))
     e#
     (assert (= (.getMessage e#) ~msg) {:e e#}))))

; TODO Now what about unions, differences, etc? Do we just do new defrecords for each?
(ns/def-validated-map MyTypeOfValidatedMap :req [::a ::b ::c ::d] :opt [::e])

(defnt trythis
          ([^MyTypeOfValidatedMap x] (assoc x ::e 41))
  #?(:clj ([^java.util.Map        x] (assoc x ::e 41))))

#?(:clj
(deftest validated-map
  (let [^MyTypeOfValidatedMap abc (->MyTypeOfValidatedMap {::a 1 ::b 1 ::c "2" ::d 3})]
    (testing "assoc"
      (testing "reassoc required"
        (! (assoc abc ::a 5)))
      (testing "optional"
        (! (assoc abc ::e 20)))
      (assert-message
        v/spec-assertion-failed
        (! (assoc abc ::a "A"))))
    (testing "dissoc"
      (testing "required key"
        (assert-message
          "Key is in ValidatedMap's required keys and cannot be dissoced"
          (! (dissoc abc ::a))))
      (testing "optional key"
        (! (-> abc (assoc ::e 20) (dissoc ::e))))
      (testing "Permissive about dissocing keys not in spec"
        (dissoc abc ::f)))
    (testing "conj"
      (! (conj abc [::c "7"])))
    (testing "defnt"
      ; Here the schema is enforced *much* more cheaply than if the
      ; entire thing were revalidated at every single call
      (testing "Invalid state is possible when not validated"
        (! (trythis ^java.util.Map (.-v abc))))
      (testing "Invalid state is impossible when validated"
        (assert-message
          v/spec-assertion-failed
          (! (trythis abc)))))
    (testing "equality"
      (is (= abc (->MyTypeOfValidatedMap {::a 1 ::b 1 ::c "2" ::d 3})))
      (is (not= abc {::a 1 ::b 1 ::c "2" ::d 3}))))))

(ns/def-validated MyTypeOfValidatedValue ::f)

(defnt trythis2 [^MyTypeOfValidatedValue x]
  (quantum.core.core/set x "abcdf"))

#?(:clj
(deftest validated-value
  (let [abcde (->MyTypeOfValidatedValue "abcde")]
    (! abcde)
    (testing "equality"
      (is (= abcde (->MyTypeOfValidatedValue "abcde")))
      (is (not= abcde "abcde")))
    (testing "defnt"
      (is (= @(trythis2 abcde) "abcdf")))
    (testing "set"
      (assert-message
        v/spec-assertion-failed
        (! (quantum.core.core/set abcde "abcdef"))))
    (testing "get"
      (= (quantum.core.core/get abcde) "abcde")))))
