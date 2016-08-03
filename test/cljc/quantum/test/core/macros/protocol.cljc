(ns quantum.test.core.macros.protocol
  (:require
    [quantum.core.macros.protocol :as ns]
    [#?(:clj clojure.test
        :cljs cljs.test)
      :refer        [#?@(:clj [deftest is testing])]
      :refer-macros [deftest is testing]]))

(defn test:ensure-protocol-appropriate-type-hint [arg lang i])

(defn test:ensure-protocol-appropriate-arglist [lang arglist-0])

(def env
  '{:genned-protocol-method-name-qualified
    quantum.test.core.macros.defnt/test:defnt-def-protocol,
    :gen-interface-def
    (gen-interface
     :name
     quantum.test.core.macros.defnt.Test_COLON_defntDefInterface
     :methods
     [[Test_COLON_defntDef
       [java.util.concurrent.atomic.AtomicInteger]
       Object]
      [Test_COLON_defntDef [java.lang.Short] Object]
      [Test_COLON_defntDef [java.lang.Integer] Object]
      [Test_COLON_defntDef [java.math.BigInteger] Object]
      [Test_COLON_defntDef [long] Object]
      [Test_COLON_defntDef [short] Object]
      [Test_COLON_defntDef [int] Object]
      [Test_COLON_defntDef [java.lang.Long] Object]
      [Test_COLON_defntDef [clojure.lang.BigInt] Object]
      [Test_COLON_defntDef [java.lang.StringBuilder boolean] Object]
      [Test_COLON_defntDef [java.lang.StringBuilder char] Object]
      [Test_COLON_defntDef [java.lang.String boolean] Object]
      [Test_COLON_defntDef [java.lang.String char] Object]
      [Test_COLON_defntDef [short long double] Object]
      [Test_COLON_defntDef [short long float] Object]
      [Test_COLON_defntDef [short int double] Object]
      [Test_COLON_defntDef [short int float] Object]
      [Test_COLON_defntDef [byte long double] Object]
      [Test_COLON_defntDef [byte long float] Object]
      [Test_COLON_defntDef [byte int double] Object]
      [Test_COLON_defntDef [byte int float] Object]]),
    :gen-interface-code-body-unexpanded
    {[Test_COLON_defntDef
      [#{java.util.concurrent.atomic.AtomicInteger}]
      Object]
     [[^AtomicInteger a] (.get a)],
     [Test_COLON_defntDef
      [#{java.lang.Short java.lang.Integer java.math.BigInteger long short
         int java.lang.Long clojure.lang.BigInt}]
      Object]
     [[^integer? a] (inc a)],
     [Test_COLON_defntDef
      [#{java.lang.StringBuilder java.lang.String} #{boolean char}]
      Object]
     [[a b] [a b]],
     [Test_COLON_defntDef
      [#{short byte} #{long int} #{double float}]
      Object]
     [[a b c] [a b c]]},
    :strict? nil,
    :externs nil,
    :reified-sym test:defnt-def-reified,
    :gen-interface-code-header
    (gen-interface
     :name
     quantum.test.core.macros.defnt.Test_COLON_defntDefInterface
     :methods),
    :relaxed? nil,
    :genned-protocol-name Test_COLON_defntDefProtocol,
    :gen-interface-code-body-expanded
    [[[Test_COLON_defntDef
       [java.util.concurrent.atomic.AtomicInteger]
       Object]
      ([^java.util.concurrent.atomic.AtomicInteger a] (.get a))]
     [[Test_COLON_defntDef [java.lang.Short] Object]
      ([^java.lang.Short a] (inc a))]
     [[Test_COLON_defntDef [java.lang.Integer] Object]
      ([^java.lang.Integer a] (inc a))]
     [[Test_COLON_defntDef [java.math.BigInteger] Object]
      ([^java.math.BigInteger a] (inc a))]
     [[Test_COLON_defntDef [long] Object] ([^long a] (inc a))]
     [[Test_COLON_defntDef [short] Object] ([^short a] (inc a))]
     [[Test_COLON_defntDef [int] Object] ([^int a] (inc a))]
     [[Test_COLON_defntDef [java.lang.Long] Object]
      ([^java.lang.Long a] (inc a))]
     [[Test_COLON_defntDef [clojure.lang.BigInt] Object]
      ([^clojure.lang.BigInt a] (inc a))]
     [[Test_COLON_defntDef [java.lang.StringBuilder boolean] Object]
      ([^java.lang.StringBuilder a ^boolean b] [a b])]
     [[Test_COLON_defntDef [java.lang.StringBuilder char] Object]
      ([^java.lang.StringBuilder a ^char b] [a b])]
     [[Test_COLON_defntDef [java.lang.String boolean] Object]
      ([^java.lang.String a ^boolean b] [a b])]
     [[Test_COLON_defntDef [java.lang.String char] Object]
      ([^java.lang.String a ^char b] [a b])]
     [[Test_COLON_defntDef [short long double] Object]
      ([^short a ^long b ^double c] [a b c])]
     [[Test_COLON_defntDef [short long float] Object]
      ([^short a ^long b ^float c] [a b c])]
     [[Test_COLON_defntDef [short int double] Object]
      ([^short a ^int b ^double c] [a b c])]
     [[Test_COLON_defntDef [short int float] Object]
      ([^short a ^int b ^float c] [a b c])]
     [[Test_COLON_defntDef [byte long double] Object]
      ([^byte a ^long b ^double c] [a b c])]
     [[Test_COLON_defntDef [byte long float] Object]
      ([^byte a ^long b ^float c] [a b c])]
     [[Test_COLON_defntDef [byte int double] Object]
      ([^byte a ^int b ^double c] [a b c])]
     [[Test_COLON_defntDef [byte int float] Object]
      ([^byte a ^int b ^float c] [a b c])]],
    :genned-protocol-method-name test:defnt-def-protocol,
    :sym test:defnt-def,
    :lang :clj,
    :reify-def
    (def
     test:defnt-def-reified
     (reify
      quantum.test.core.macros.defnt.Test_COLON_defntDefInterface
      (^Object Test_COLON_defntDef
       [this ^java.util.concurrent.atomic.AtomicInteger a]
       (.get ^java.util.concurrent.atomic.AtomicInteger a))
      (^Object Test_COLON_defntDef
       [this ^java.lang.Short a]
       (inc ^java.lang.Short a))
      (^Object Test_COLON_defntDef
       [this ^java.lang.Integer a]
       (inc ^java.lang.Integer a))
      (^Object Test_COLON_defntDef
       [this ^java.math.BigInteger a]
       (inc ^java.math.BigInteger a))
      (^Object Test_COLON_defntDef [this ^long a] (inc a))
      (^Object Test_COLON_defntDef [this ^short a] (inc a))
      (^Object Test_COLON_defntDef [this ^int a] (inc a))
      (^Object Test_COLON_defntDef
       [this ^java.lang.Long a]
       (inc ^java.lang.Long a))
      (^Object Test_COLON_defntDef
       [this ^clojure.lang.BigInt a]
       (inc ^clojure.lang.BigInt a))
      (^Object Test_COLON_defntDef
       [this ^java.lang.StringBuilder a ^boolean b]
       [^java.lang.StringBuilder a b])
      (^Object Test_COLON_defntDef
       [this ^java.lang.StringBuilder a ^char b]
       [^java.lang.StringBuilder a b])
      (^Object Test_COLON_defntDef
       [this ^java.lang.String a ^boolean b]
       [^java.lang.String a b])
      (^Object Test_COLON_defntDef
       [this ^java.lang.String a ^char b]
       [^java.lang.String a b])
      (^Object Test_COLON_defntDef
       [this ^short a ^long b ^double c]
       [a b c])
      (^Object Test_COLON_defntDef
       [this ^short a ^long b ^float c]
       [a b c])
      (^Object Test_COLON_defntDef
       [this ^short a ^int b ^double c]
       [a b c])
      (^Object Test_COLON_defntDef
       [this ^short a ^int b ^float c]
       [a b c])
      (^Object Test_COLON_defntDef
       [this ^byte a ^long b ^double c]
       [a b c])
      (^Object Test_COLON_defntDef
       [this ^byte a ^long b ^float c]
       [a b c])
      (^Object Test_COLON_defntDef
       [this ^byte a ^int b ^double c]
       [a b c])
      (^Object Test_COLON_defntDef
       [this ^byte a ^int b ^float c]
       [a b c]))),
    :genned-method-name Test_COLON_defntDef,
    :reify-body
    (reify
     quantum.test.core.macros.defnt.Test_COLON_defntDefInterface
     (^Object Test_COLON_defntDef
      [this ^java.util.concurrent.atomic.AtomicInteger a]
      (.get ^java.util.concurrent.atomic.AtomicInteger a))
     (^Object Test_COLON_defntDef
      [this ^java.lang.Short a]
      (inc ^java.lang.Short a))
     (^Object Test_COLON_defntDef
      [this ^java.lang.Integer a]
      (inc ^java.lang.Integer a))
     (^Object Test_COLON_defntDef
      [this ^java.math.BigInteger a]
      (inc ^java.math.BigInteger a))
     (^Object Test_COLON_defntDef [this ^long a] (inc a))
     (^Object Test_COLON_defntDef [this ^short a] (inc a))
     (^Object Test_COLON_defntDef [this ^int a] (inc a))
     (^Object Test_COLON_defntDef
      [this ^java.lang.Long a]
      (inc ^java.lang.Long a))
     (^Object Test_COLON_defntDef
      [this ^clojure.lang.BigInt a]
      (inc ^clojure.lang.BigInt a))
     (^Object Test_COLON_defntDef
      [this ^java.lang.StringBuilder a ^boolean b]
      [^java.lang.StringBuilder a b])
     (^Object Test_COLON_defntDef
      [this ^java.lang.StringBuilder a ^char b]
      [^java.lang.StringBuilder a b])
     (^Object Test_COLON_defntDef
      [this ^java.lang.String a ^boolean b]
      [^java.lang.String a b])
     (^Object Test_COLON_defntDef
      [this ^java.lang.String a ^char b]
      [^java.lang.String a b])
     (^Object Test_COLON_defntDef
      [this ^short a ^long b ^double c]
      [a b c])
     (^Object Test_COLON_defntDef
      [this ^short a ^long b ^float c]
      [a b c])
     (^Object Test_COLON_defntDef
      [this ^short a ^int b ^double c]
      [a b c])
     (^Object Test_COLON_defntDef [this ^short a ^int b ^float c] [a b c])
     (^Object Test_COLON_defntDef
      [this ^byte a ^long b ^double c]
      [a b c])
     (^Object Test_COLON_defntDef [this ^byte a ^long b ^float c] [a b c])
     (^Object Test_COLON_defntDef [this ^byte a ^int b ^double c] [a b c])
     (^Object Test_COLON_defntDef
      [this ^byte a ^int b ^float c]
      [a b c])),
    :ns-qualified-interface-name
    quantum.test.core.macros.defnt.Test_COLON_defntDefInterface,
    :genned-interface-name Test_COLON_defntDefInterface,
    :sym-with-meta test:defnt-def,
    :arglists
    [[^AtomicInteger a]
     [^integer? a]
     [#{String StringBuilder} a #{boolean char} b]
     [#{short byte} a #{long int} b #{double float} c]],
    :ns- nil,
    :arities
    [[[^AtomicInteger a] (.get a)]
     [[^integer? a] (inc a)]
     [[a b] [a b]]
     [[a b c] [a b c]]],
    :arglists-types
    ([[AtomicInteger] Object]
     [[integer?] Object]
     [[#{String StringBuilder} #{boolean char}] Object]
     [[#{short byte} #{long int} #{double float}] Object]),
    :reified-sym-qualified
    ^quantum.test.core.macros.defnt.Test_COLON_defntDefInterface quantum.test.core.macros.defnt/test:defnt-def-reified,
    :available-default-types
    {0 #{Object boolean char double float},
     1 #{Object double short float byte},
     2 #{Object boolean char long short int byte}}}
)

(deftest test:gen-protocols-from-interface
  (is (= (ns/gen-protocols-from-interface env)
         '[(defprotocol Test_COLON_defntDefProtocol__3
             (test:defnt-def-protocol [a2 a0 a1]))
           (defprotocol Test_COLON_defntDefProtocol__2
             (test:defnt-def-protocol [a1 a2 a0]))
           (defprotocol Test_COLON_defntDefProtocol   
             (test:defnt-def-protocol [a0] [a0 a1 a2]))])))

(defn test:gen-extend-protocol-from-interface
  [{:keys [genned-protocol-name genned-protocol-method-name
           reify-body lang first-types]}])