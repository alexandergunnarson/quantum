(ns quantum.test.core.macros.defnt
  (:require [quantum.core.macros.defnt :as ns]
            [quantum.core.log   :as log
              :include-macros true]
            [quantum.core.error :as err
              :include-macros true]
            [#?(:clj clojure.test
                :cljs cljs.test)
              :refer        [#?@(:clj [deftest is testing])]
              :refer-macros [deftest is testing]])
  #?(:clj (:import java.util.concurrent.atomic.AtomicInteger)))

(log/enable! :debug)

(def data
  {:ns- *ns*
   :lang :clj
   :class-sym 'AtomicInteger
   :expanded 'java.util.concurrent.atomic.AtomicInteger})

(deftest test:get-qualified-class-name
  (let [{:keys [ns- lang class-sym]} data]
    (is (= (ns/get-qualified-class-name
             lang ns- class-sym)
           (:expanded data)))))

(defn test:classes-for-type-predicate
  ([pred lang])
  ([pred lang type-arglist]))

(deftest test:expand-classes-for-type-hint
  (let [{:keys [ns- lang class-sym]} data]
    (is (= (ns/expand-classes-for-type-hint
             class-sym lang ns- [class-sym])
           #{(:expanded data)}))))

(defn test:hint-arglist-with [arglist hints])

(defn test:defnt-remove-hints [x])

(defn test:defnt-arities
  [body])

(defn test:defnt-arglists
  [body])

(defn test:defnt-gen-protocol-names [{:keys [sym strict? lang]}])

(defn test:defnt-gen-interface-unexpanded
  [{:keys [sym arities arglists-types lang]}])

(defn test:defnt-replace-kw
  [kw {:keys [type-hints type-arglist available-default-types hint inner-type-n]}])

(defn test:defnt-gen-interface-expanded
  [{:keys [lang
           gen-interface-code-body-unexpanded
           available-default-types]
    :as env}])

(defn test:defnt-gen-interface-def
  [{:keys [gen-interface-code-header gen-interface-code-body-expanded]}])

(defn test:defnt-positioned-types-for-arglist
  [arglist types])

(defn test:defnt-types-for-arg-positions
  [{:keys [lang arglists-types]}])

(defn test:protocol-verify-arglists
  [arglists lang])

(deftest
  ^{:todo ["Add failure tests"]}
  test:protocol-verify-unique-first-hint
  (is (= nil
         (ns/protocol-verify-unique-first-hint
          '([Test_COLON_defnt_COLON_nsEval
             [#{java.util.concurrent.atomic.AtomicInteger}]
             Object]
            [Test_COLON_defnt_COLON_nsEval
             [#{java.lang.Short java.lang.Integer java.math.BigInteger long short
                int java.lang.Long clojure.lang.BigInt}]
             Object])))))

(defn test:defnt-gen-helper-macro
  [{:keys [genned-method-name
           genned-protocol-method-name-qualified
           reified-sym-qualified
           strict?
           relaxed?
           sym-with-meta
           args-sym
           args-hinted-sym
           lang]}])

(defn test:defnt-gen-final-defnt-def
  [{:keys [lang sym strict? externs genned-protocol-method-name
           gen-interface-def helper-macro-interface-def
           reify-def reified-sym
           helper-macro-def
           protocol-def extend-protocol-def]}])

(defn test:defnt*-helper
  ([opts lang ns- sym doc- meta- body [unk & rest-unk]])
  ([opts lang ns- sym doc- meta- body]))

(defn test:defnt
  [sym & body])

(defn test:defnt'
  [sym & body])

(defn test:defntp
  [sym & body])

; Non |deftest| tests

(log/enable! :macro-expand)
(log/enable! :macro-expand-protocol)

(def test-boxed-long (Long.    1))
(def test-boxed-int  (Integer. 1))
(def test-string     "abcde")

#_(ns/defnt test:defnt-def**
  ([^integer? a b] (+ a b)))

(ns/defnt test:defnt-def
  #?(:clj ([^AtomicInteger a] (.get a))) ; namespace-resolved class
          ([^integer?      a] (inc a))   ; predicate class
          #_([^integer? a ^integer? b] [a b])
          ([#{String StringBuilder} a #{boolean char} b] [a b])
          ([#{byte short} a #{int long} b #{float double} c] [a b c]))

(ns/defnt test:defnt-def-generic
  ([^string? x] (first x)) ; predicate class
  ([x] x)) ; Generic

(deftest test:defnt
  #?(:clj (is (= 300 (test:defnt-def (AtomicInteger. 300))))) ; reify
          (is (= 2   (test:defnt-def test-boxed-long))) ; protocol
          (is (= 2   (test:defnt-def test-boxed-int )))
          (is (= 2   (test:defnt-def 1))) ; reify
  #?(:clj (is (instance? IllegalArgumentException
                (err/suppress (test:defnt-def 1.0))))) ; reify, 'No matching method found'
          (is (= \a (test:defnt-def-generic test-string)))
          (is (= 1  (test:defnt-def-generic 1)))
          (is (= [1 2] (test:defnt-def 1 2))) ; reify
          (is (= [1 "abcde"] (test:defnt-def test-boxed-int test-string))) ; reify
          )

(log/disable! :macro-expand)


#_(:clj
  (defnt' div*-bin-
  "Lax |/|; continues on overflow/underflow.
   TODO Doesn't preserve ratios."
  (^double ; is it actually always double?
    [#{byte char short int long float double} n
     #{byte char short int long float double} d]
    (quantum.core.Numeric/divide n d))
  (^Number [^java.math.BigInteger n ^java.math.BigInteger d]
    (when (.equals d BigInteger/ZERO)
      (throw (ArithmeticException. "Divide by zero")))
    (let [^BigInteger gcd (.gcd n d)]
      (if (.equals gcd BigInteger/ZERO)
          BigInt/ZERO
          (let [n-f (.divide n gcd)
                d-f (.divide d gcd)]
            (cond
              (.equals d BigInteger/ONE)
                (BigInt/fromBigInteger n-f)
              (.equals d (.negate BigInteger/ONE))
                (BigInt/fromBigInteger (.negate n-f))
              :else (clojure.lang.Ratio. (if (neg? d-f) (.negate n-f) n-f)
                                         (if (neg? d-f) (.negate d-f) d-f)))))))
  ([^clojure.lang.BigInt n ^clojure.lang.BigInt d]
    (div*-bin- (->big-integer n) (->big-integer d)))
  ([^java.math.BigDecimal n ^java.math.BigDecimal d]
    (if (nil? *math-context*)
        (.divide n d)
        (.divide n d *math-context*)))))

; TODO you lose the |defnt'| power with this

; (:clj (defnt div*-bin-denom-byte
; ([#{byte char short int long float double} d ^byte   n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-char
; ([#{byte char short int long float double} d ^char   n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-short
; ([#{byte char short int long float double} d ^short  n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-int
; ([#{byte char short int long float double} d ^int    n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-long
; ([#{byte char short int long float double} d ^long   n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-float
; ([#{byte char short int long float double} d ^float  n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-double
; ([#{byte char short int long float double} d ^double n] (div*-bin- n d))))
