(ns quantum.test.untyped.core.defnt
  (:require
    [clojure.spec.alpha         :as s]
    [quantum.untyped.core.defnt :as this]))

#_(defns abcde "Documentation" {:metadata "abc"}
  ([a #(instance? Long %)] (+ a 1))
  ([b ?, c _ > integer?] {:pre 1} 1 2)
  ([d string?, e #(instance? StringBuilder %) & f _ > number?]
    (.substring ^String d 0 1)
    (.append ^StringBuilder e 1)
    3 4))

;; TODO assert that the below 2 things are equivalent

(macroexpand
'(this/defns fghij "Documentation" {:metadata "abc"}
  ([a number? > number?] (inc a))
  ([a number?, b number?
    | (> a b)
    > (s/and number? #(> % a) #(> % b))] (+ a b))
  ([a string?
    b boolean?
    {:as   c
     :keys [ca keyword? cb string?]
     {:as cc
      {:as   cca
       :keys [ccaa keyword?]
       [[ccabaa some? {:as ccabab :keys [ccababa some?]} some?] some? ccabb some? :as ccab]
       [:ccab seq?]}
      [:cca map?]}
     [:cc map?]}
    #(-> % count (= 3))
    [da double? & db seq? :as d] sequential?
    [ea symbol?] vector?
    & [fa string? :as f] seq?
    | (and (> a b) (contains? c a)
           a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb d da db ea f fa)
    > number?] 0)))

(s/fdef fghijk
  :args (s/or :arity-1 (s/cat :a (let [spec# number?] (fn [a] (spec# a))))
              :arity-2 (s/and (s/cat :a (let [spec# number?] (fn [a] (spec# a)))
                                     :b (let [spec# number?] (fn [b] (spec# b))))
                              (fn [{a :a b :b}] (> a b)))
              :arity-varargs
                (s/and (s/cat :a      (let [spec# string?]  (fn [a] (spec# a)))
                              :b      (let [spec# boolean?] (fn [b] (spec# b)))
                              :c      (s/and (let [spec# #(-> % count (= 3))] (fn [c]                                             (spec# c)))
                                             (let [spec# keyword?]            (fn [{:keys [ca]}]                                  (spec# ca)))
                                             (let [spec# string?]             (fn [{:keys [cb]}]                                  (spec# cb)))
                                             (let [spec# map?]                (fn [{cc :cc}]                                      (spec# cc)))
                                             (let [spec# map?]                (fn [{{cca :cca} :cc}]                              (spec# cca)))
                                             (let [spec# keyword?]            (fn [{{{:keys [ccaa]} :cca} :cc}]                   (spec# ccaa)))
                                             (let [spec# seq?]                (fn [{{{ccab :ccab} :cca} :cc}]                     (spec# ccab)))
                                             (let [spec# some?]               (fn [{{{[as#] :ccab} :cca} :cc}]                    (spec# as#)))
                                             (let [spec# some?]               (fn [{{{[[ccabaa]] :ccab} :cca} :cc}]               (spec# ccabaa)))
                                             (let [spec# some?]               (fn [{{{[[_# ccabab]] :ccab} :cca} :cc}]            (spec# ccabab)))
                                             (let [spec# some?]               (fn [{{{[[_# {:keys [ccababa]}]] :ccab} :cca} :cc}] (spec# ccababa)))
                                             (let [spec# some?]               (fn [{{{[_# ccabb] :ccab} :cca} :cc}]               (spec# ccabb))))
                              :d      (s/and (let [spec# sequential?] (fn [d]         (spec# d)))
                                             (let [spec# double?]     (fn [[da]]      (spec# da)))
                                             (let [spec# seq?]        (fn [[_# & db]] (spec# db))))
                              :arg-4# (s/and (let [spec# vector?] (fn [as#]  (spec# as#)))
                                             (let [spec# symbol?] (fn [[ea]] (spec# ea))))
                              :f      (s/and (let [spec# seq?]    (fn [f] (spec# f)))
                                             (let [spec# string?] (fn [[fa]] (spec# fa)))))
                       (fn [{a :a
                             b :b
                             {:as c
                              :keys [ca cb]
                              {:as cc
                               {:as cca
                                :keys [ccaa]
                                [[ccabaa {:as ccabab :keys [ccababa]}] ccabb :as ccab] :ccab} :cca} :cc} :c
                             [da & db :as d] :d
                             [ea] :arg-4#
                             [fa :as f] :f}]
                         (and (> a b) (contains? c a)
                              a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb d da db ea f fa))))
   :fn   (fn [{ret :ret [arity-kind args] :args}]
           (case arity-kind
             :arity-1 (let [{a :a} args]
                        (number? ret))
             :arity-2 (let [{a :a b :b} args]
                        ((s/and number? #(> % a) #(> % b)) ret))
             :arity-3 (let [{a :a
                             b :b
                             {:as c
                              :keys [ca cb]
                              {:as cc
                               {:as cca
                                :keys [ccaa]
                                [[ccabaa {:as ccabab :keys [ccababa]}] ccabb :as ccab] :ccab} :cca} :cc} :c
                             [da & db :as d] :d
                             [ea] :arg-4#
                             [fa :as f] :f} args]
                        (number? ret)))))

(fghij "zx" true {:ca :x :cb "y" :cc {:cca {:ccaa :z :ccab (list [1 {:ccababa 2}] 3)}}} [1.0 4] ['a])
