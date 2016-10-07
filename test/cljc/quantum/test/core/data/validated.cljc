(ns quantum.test.core.data.validated
  (:require
    [quantum.core.data.validated :as ns]
    [quantum.core.print          :as pr
      :refer        [!]]
    [quantum.core.logic
      :refer        [#?@(:clj [eq?])]
      :refer-macros [          eq?]]
    [quantum.core.error
      :refer        [#?@(:clj [catch-all])]
      :refer-macros [          catch-all]]
    [quantum.core.validate       :as v
      :refer        [#?@(:clj [validate defspec])]
      :refer-macros [          validate defspec]]))

; s/cat conforms to a map

(defspec ::odd?  (v/and integer? odd? ))
(defspec ::even? (v/and integer? even?))
(defspec ::a integer?)
(defspec ::b integer?)
(defspec ::c string?)
(defspec ::d (v/and integer? odd?))
(defspec ::e (v/and integer? even?))
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

#?(:clj
(try
  (eval
    `(do ; DEFINITION ;
         #_(pr/pprint-hints (macroexpand-1 `(ns/def-validated-map ~'MyTypeOfValidatedMap :req [::a ::b ::c ::d] :opt [::e])))
         (ns/def-validated-map ~'MyTypeOfValidatedMap :req [::a ::b ::c ::d] :opt [::e])
         (def ~'abc (->MyTypeOfValidatedMap {::a 1 ::b 1 ::c "2" ::d 3}))
         (! abc)
         ; ASSOC ;
         ; reassoc required
         (! (assoc abc ::a 5))
         ; assoc optional
         (! (assoc abc ::e 20))
         (assert-message
           v/spec-assertion-failed
           (! (assoc abc ::a "A")))
         ; DISSOC ;
         ; Dissoc required key
         (assert-message
           "Key is in ValidatedMap's required keys and cannot be dissoced"
           (! (dissoc abc ::a)))
         ; Dissoc optional key
         (! (-> abc (assoc ::e 20) (dissoc ::e)))
         ; Permissive about dissocing keys not in spec
         (dissoc abc ::f)
         ; CONJ ;
         (! (conj abc [::c "7"]))
         ))
  (catch Throwable e (! (or (.getCause e) e)))))
