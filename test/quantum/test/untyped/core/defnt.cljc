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
  :args
    (s/or
      :arity-1 (s/cat :a (s/with (fn [a] a) number?))
      :arity-2 (s/and (s/cat :a (s/with (fn [a] a) number?)
                             :b (s/with (fn [b] b) number?))
                      (fn [{a :a b :b}] (> a b)))
      :arity-varargs
        (s/and
          (s/cat
            :a      (s/with (fn [a] a) string?)
            :b      (s/with (fn [b] b) boolean?)
            :c      (s/and
                      (s/with (fn [c]                                             c)        #(-> % count (= 3)))
                      (s/with (fn [{:keys [ca]}]                                  ca)       keyword?)
                      (s/with (fn [{:keys [cb]}]                                  cb)       string?)
                      (s/with (fn [{cc :cc}]                                      cc)       map?)
                      (s/with (fn [{{cca :cca} :cc}]                              cca)      map?)
                      (s/with (fn [{{{:keys [ccaa]} :cca} :cc}]                   ccaa)     keyword?)
                      (s/with (fn [{{{ccab :ccab} :cca} :cc}]                     ccab)     seq?)
                      (s/with (fn [{{{[as#] :ccab} :cca} :cc}]                    as#)      some?)
                      (s/with (fn [{{{[[ccabaa]] :ccab} :cca} :cc}]               ccabaa)   some?)
                      (s/with (fn [{{{[[_# ccabab]] :ccab} :cca} :cc}]            ccabab)   some?)
                      (s/with (fn [{{{[[_# {:keys [ccababa]}]] :ccab} :cca} :cc}] ccababa)) some?
                      (s/with (fn [{{{[_# ccabb] :ccab} :cca} :cc}]               ccabb)    some?))
            :d      (s/and
                      (s/with (fn [d]         d)   sequential?)
                      (s/with (fn [[da]]      da)  double?)
                      (s/with (fn [[_# & db]] db)) seq?)
            :arg-4# (s/and
                      (s/with (fn [as#]  as#) vector?)
                      (s/with (fn [[ea]] ea)) symbol?)
            :f      (s/and
                      (s/with (fn [f]    f)  seq?)
                      (s/with (fn [[fa]] fa) string?)))
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
   :fn
     (s/with-gen-spec (fn [{:keys [ret]}] ret)
       (fn [{[arity-kind# args#] :args}]
         (case arity-kind#
           :arity-1
             (let [{a :a} args#] (s/spec number?))
           :arity-2
             (let [{a :a b :b} args#] (s/spec (s/and number? #(> % a) #(> % b))))
           :arity-3
             (let [{a :a
                    b :b
                    {:as c
                     :keys [ca cb]
                     {:as cc
                      {:as cca
                       :keys [ccaa]
                       [[ccabaa {:as ccabab :keys [ccababa]}] ccabb :as ccab] :ccab} :cca} :cc} :c
                    [da & db :as d] :d
                    [ea] :arg-4#
                    [fa :as f] :f} args#] (s/spec number?))))))

(fghij "zx" true {:ca :x :cb "y" :cc {:cca {:ccaa :z :ccab (list [1 {:ccababa 2}] 3)}}} [1.0 4] ['a])
