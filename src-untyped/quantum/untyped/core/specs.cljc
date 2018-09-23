(ns quantum.untyped.core.specs
  "Adapted from clojure.core.specs version 1.9.0-alpha19
   and enabled in CLJS. Other specs added too.
   See https://github.com/clojure/core.specs.alpha/blob/5d85f93ab78386855374256934475af0afe7f380/src/main/clojure/clojure/core/specs/alpha.clj"
  (:require
    [clojure.core              :as core]
    [clojure.set               :as set]
    [quantum.untyped.core.data
      :refer [val?]]
    [quantum.untyped.core.fn
      :refer [fn1 fnl]]
    [quantum.untyped.core.spec :as s])
#?(:cljs
  (:require-macros
    [quantum.untyped.core.specs :as self
      :refer [quotable]])))

;;;; GENERAL

(s/def :quantum.core.specs/meta map?)

;;;; destructure

(s/def :quantum.core.specs/local-name (s/and simple-symbol? #(not= '& %)))

(s/def :quantum.core.specs/binding-form
  (s/or :sym :quantum.core.specs/local-name
        :seq :quantum.core.specs/seq-binding-form
        :map :quantum.core.specs/map-binding-form))

;; sequential destructuring

(s/def :quantum.core.specs/seq-binding-form
  (s/and vector?
         (s/cat :elems (s/* :quantum.core.specs/binding-form)
                :rest  (s/? (s/cat :amp #{'&} :form :quantum.core.specs/binding-form))
                :as    (s/? (s/cat :as #{:as} :sym :quantum.core.specs/local-name)))))

;; map destructuring

(s/def :quantum.core.specs/keys (s/coll-of ident?         :kind vector?))
(s/def :quantum.core.specs/syms (s/coll-of symbol?        :kind vector?))
(s/def :quantum.core.specs/strs (s/coll-of simple-symbol? :kind vector?))
(s/def :quantum.core.specs/or   (s/map-of simple-symbol? any?))
(s/def :quantum.core.specs/as   :quantum.core.specs/local-name)

(s/def :quantum.core.specs/map-special-binding
  (s/keys :opt-un [:quantum.core.specs/as :quantum.core.specs/or
                   :quantum.core.specs/keys :quantum.core.specs/syms :quantum.core.specs/strs]))

(s/def :quantum.core.specs/map-binding (s/tuple :quantum.core.specs/binding-form any?))

(s/def :quantum.core.specs/ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

(s/def :quantum.core.specs/map-bindings
  (s/every (s/or :mb  :quantum.core.specs/map-binding
                 :nsk :quantum.core.specs/ns-keys
                 :msb (s/tuple #{:as :or :keys :syms :strs} any?)) :into {}))

(s/def :quantum.core.specs/map-binding-form
  (s/merge :quantum.core.specs/map-bindings :quantum.core.specs/map-special-binding))

;; bindings

(s/def :quantum.core.specs/binding  (s/cat :binding :quantum.core.specs/binding-form :init-expr any?))
(s/def :quantum.core.specs/bindings (s/and vector? (s/* :quantum.core.specs/binding)))

;; let, if-let, when-let

(s/fdef core/let
  :args (s/cat :bindings :quantum.core.specs/bindings
               :body     (s/* any?)))

(s/fdef core/if-let
  :args (s/cat :bindings (s/and vector? :quantum.core.specs/binding)
               :then     any?
               :else     (s/? any?)))

(s/fdef core/when-let
  :args (s/cat :bindings (s/and vector? :quantum.core.specs/binding)
               :body     (s/* any?)))

;; defn, defn-, fn

(s/def :quantum.core.specs/fn|arglist
  (s/and
    vector?
    (s/cat :args    (s/* :quantum.core.specs/binding-form)
           :varargs (s/? (s/cat :amp #{'&} :form :quantum.core.specs/binding-form)))))

(s/def :quantum.core.specs/fn|prepost
  (s/and (s/keys :req-un [(or :quantum.core.specs.core/pre :quantum.core.specs.core/post)]) ; TODO we actually really only want to accept un-namespaced keys...
         (s/conformer #(set/rename-keys % {:quantum.core.specs.core/pre  :pre
                                           :quantum.core.specs.core/post :post}))))

(s/def :quantum.core.specs/fn|body
  (s/alt :prepost+body (s/cat :prepost :quantum.core.specs/fn|prepost
                              :body    (s/+ any?))
         :body         (s/* any?)))

(s/def :quantum.core.specs/fn|arglist+body
  (s/cat :quantum.core.specs/fn|arglist :quantum.core.specs/fn|arglist
         :body                          :quantum.core.specs/fn|body))

(s/def :quantum.core.specs/fn|name simple-symbol?)
(s/def ::fn|name :quantum.core.specs/fn|name)

(s/def :quantum.core.specs/docstring string?)

(s/def :quantum.core.specs/pre-meta  (s/? :quantum.core.specs/meta))
(s/def :quantum.core.specs/post-meta (s/? :quantum.core.specs/meta))

(s/def :quantum.core.specs/fn|unique-doc
  #(->> [(:quantum.core.specs/docstring %)
         (-> % :quantum.core.specs/fn|name meta :doc)
         (-> % :quantum.core.specs/pre-meta     :doc)
         (-> % :quantum.core.specs/post-meta    :doc)]
        (filter val?)
        count
        ((fn [x] (<= x 1)))))

(s/def :quantum.core.specs/fn|unique-meta
  #(empty? (set/intersection
             (-> % :quantum.core.specs/fn|name meta keys set)
             (-> % :quantum.core.specs/pre-meta     keys set)
             (-> % :quantum.core.specs/post-meta    keys set))))

(s/def :quantum.core.specs/fn|aggregate-meta
  (s/conformer
    (fn [{:keys [:quantum.core.specs/fn|name :quantum.core.specs/docstring
                 :quantum.core.specs/pre-meta :quantum.core.specs/post-meta] :as m}]
      (-> m
          (dissoc :quantum.core.specs/docstring
                  :quantum.core.specs/pre-meta
                  :quantum.core.specs/post-meta)
          (update :quantum.core.specs/fn|name #(some-> % (with-meta nil)))
          (assoc :quantum.core.specs/meta
            (-> ;; TODO use `merge-unique` instead of `:quantum.core.specs/fn|unique-meta`
                (merge (meta fn|name) pre-meta post-meta)
                (cond-> docstring (assoc :doc docstring))))))))

(defn fn-like|postchecks|gen [overloads-ident]
  (s/and (s/conformer
           (fn [v]
             (let [[overloads-k overloads-v] (get v overloads-ident)
                   overloads
                    (-> (case overloads-k
                          :overload-1 {:overloads [overloads-v]}
                          :overload-n overloads-v)
                        (update :overloads
                          (fnl mapv
                            (fn1 update :body
                              (fn [[k v]]
                                (case k
                                  :body         {:body v}
                                  :prepost+body v))))))]
               (assoc v :quantum.core.specs/post-meta (:quantum.core.specs/post-meta overloads)
                        overloads-ident               (get overloads :overloads)))))
         :quantum.core.specs/fn|unique-doc
         :quantum.core.specs/fn|unique-meta
         ;; TODO validate metadata like return value etc.
         :quantum.core.specs/fn|aggregate-meta))

(s/def :quantum.core.specs/fn|postchecks (fn-like|postchecks|gen :quantum.core.specs/fn|overloads))

(s/def :quantum.core.specs/fn
  (s/and
    (s/spec
      (s/cat
        :quantum.core.specs/fn|name (s/? :quantum.core.specs/fn|name)
        :quantum.core.specs/fn|overloads
          (s/alt
            :overload-1 :quantum.core.specs/fn|arglist+body
            :overload-n (s/cat :overloads (s/+ (s/spec :quantum.core.specs/fn|arglist+body))))))
    :quantum.core.specs/fn|postchecks))

(s/def :quantum.core.specs/defn
  (s/and
    (s/spec
      (s/cat
        :quantum.core.specs/fn|name   :quantum.core.specs/fn|name
        :quantum.core.specs/docstring (s/? :quantum.core.specs/docstring)
        :quantum.core.specs/pre-meta  :quantum.core.specs/pre-meta
        :quantum.core.specs/fn|overloads
          (s/alt
            :overload-1 :quantum.core.specs/fn|arglist+body
            :overload-n
              (s/cat
                :overloads                    (s/+ (s/spec :quantum.core.specs/fn|arglist+body))
                :quantum.core.specs/post-meta :quantum.core.specs/post-meta))))
    :quantum.core.specs/fn|postchecks))

(s/fdef core/defn  :args :quantum.core.specs/defn :ret any?)
(s/fdef core/defn- :args :quantum.core.specs/defn :ret any?)
(s/fdef core/fn    :args :quantum.core.specs/fn   :ret any?)

;;;; ns

(s/def :quantum.core.specs/exclude (s/coll-of simple-symbol?))
(s/def :quantum.core.specs/only    (s/coll-of simple-symbol?))
(s/def :quantum.core.specs/rename  (s/map-of simple-symbol? simple-symbol?))

(s/def :quantum.core.specs/filters
  (s/keys* :opt-un [:quantum.core.specs/exclude :quantum.core.specs/only :quantum.core.specs/rename]))

(s/def :quantum.core.specs/ns-refer-clojure
  (s/spec (s/cat :clause  #{:refer-clojure}
                 :filters :quantum.core.specs/filters)))

(s/def :quantum.core.specs/refer
  (s/or :all  #{:all}
        :syms (s/coll-of simple-symbol?)))

(s/def :quantum.core.specs/prefix-list
  (s/spec
    (s/cat :prefix   simple-symbol?
           :libspecs (s/+ :quantum.core.specs/libspec))))

(s/def :quantum.core.specs/libspec
  (s/alt :lib      simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [:quantum.core.specs/as :quantum.core.specs/refer])))))

(s/def :quantum.core.specs/ns-require
  (s/spec (s/cat :clause #{:require}
                 :body (s/+ (s/alt :libspec     :quantum.core.specs/libspec
                                   :prefix-list :quantum.core.specs/prefix-list
                                   :flag        #{:reload :reload-all :verbose})))))

(s/def :quantum.core.specs/package-list
  (s/spec
    (s/cat :package simple-symbol?
           :classes (s/* simple-symbol?))))

(s/def :quantum.core.specs/import-list
  (s/* (s/alt :class simple-symbol?
              :package-list :quantum.core.specs/package-list)))

(s/def :quantum.core.specs/ns-import
  (s/spec
    (s/cat :clause  #{:import}
           :classes :quantum.core.specs/import-list)))

(s/def :quantum.core.specs/ns-refer
  (s/spec (s/cat :clause  #{:refer}
                 :lib     simple-symbol?
                 :filters :quantum.core.specs/filters)))

;; same as :quantum.core.specs/prefix-list, but with :quantum.core.specs/use-libspec instead
(s/def :quantum.core.specs/use-prefix-list
  (s/spec
    (s/cat :prefix simple-symbol?
           :libspecs (s/+ :quantum.core.specs/use-libspec))))

;; same as :quantum.core.specs/libspec, but also supports the :quantum.core.specs/filters options in the libspec
(s/def :quantum.core.specs/use-libspec
  (s/alt :lib simple-symbol?
         :lib+opts
           (s/spec (s/cat :lib     simple-symbol?
                          :options (s/keys* :opt-un [:quantum.core.specs/as :quantum.core.specs/refer
                                                     :quantum.core.specs/exclude :quantum.core.specs/only
                                                     :quantum.core.specs/rename])))))

(s/def :quantum.core.specs/ns-use
  (s/spec (s/cat :clause #{:use}
                 :libs (s/+ (s/alt :libspecs    :quantum.core.specs/use-libspec
                                   :prefix-list :quantum.core.specs/use-prefix-list
                                   :flag        #{:reload :reload-all :verbose})))))

(s/def :quantum.core.specs/ns-load
  (s/spec (s/cat :clause #{:load}
                 :libs (s/* string?))))

(s/def :quantum.core.specs/name simple-symbol?)
(s/def :quantum.core.specs/extends simple-symbol?)
(s/def :quantum.core.specs/implements (s/coll-of simple-symbol? :kind vector?))
(s/def :quantum.core.specs/init symbol?)
(s/def :quantum.core.specs/class-ident (s/or :class simple-symbol? :class-name string?))
(s/def :quantum.core.specs/signature (s/coll-of :quantum.core.specs/class-ident :kind vector?))
(s/def :quantum.core.specs/constructors (s/map-of :quantum.core.specs/signature :quantum.core.specs/signature))
(s/def :quantum.core.specs/post-init symbol?)

(s/def :quantum.core.specs/method
  (s/and vector?
         (s/cat :name simple-symbol?
                :param-types :quantum.core.specs/signature
                :return-type simple-symbol?)))

(s/def :quantum.core.specs/methods (s/coll-of :quantum.core.specs/method :kind vector?))
(s/def :quantum.core.specs/main boolean?)
(s/def :quantum.core.specs/factory simple-symbol?)
(s/def :quantum.core.specs/state simple-symbol?)
(s/def :quantum.core.specs/get simple-symbol?)
(s/def :quantum.core.specs/set simple-symbol?)
(s/def :quantum.core.specs/expose (s/keys :opt-un [:quantum.core.specs/get :quantum.core.specs/set]))
(s/def :quantum.core.specs/exposes (s/map-of simple-symbol? :quantum.core.specs/expose))
(s/def :quantum.core.specs/prefix string?)
(s/def :quantum.core.specs/impl-ns simple-symbol?)
(s/def :quantum.core.specs/load-impl-ns boolean?)

(s/def :quantum.core.specs/ns-gen-class
  (s/spec (s/cat :clause #{:gen-class}
                 :options (s/keys* :opt-un [:quantum.core.specs/name :quantum.core.specs/extends :quantum.core.specs/implements
                                            :quantum.core.specs/init :quantum.core.specs/constructors :quantum.core.specs/post-init
                                            :quantum.core.specs/methods :quantum.core.specs/main :quantum.core.specs/factory :quantum.core.specs/state
                                            :quantum.core.specs/exposes :quantum.core.specs/prefix :quantum.core.specs/impl-ns :quantum.core.specs/load-impl-ns]))))

(s/def :quantum.core.specs/ns-clauses
  (s/* (s/alt :refer-clojure :quantum.core.specs/ns-refer-clojure
              :require       :quantum.core.specs/ns-require
              :import        :quantum.core.specs/ns-import
              :use           :quantum.core.specs/ns-use
              :refer         :quantum.core.specs/ns-refer
              :load          :quantum.core.specs/ns-load
              :gen-class     :quantum.core.specs/ns-gen-class)))

(s/def :quantum.core.specs/ns-form
  (s/cat :name      simple-symbol?
         :docstring (s/? :quantum.core.specs/docstring)
         :attr-map  (s/? :quantum.core.specs/meta)
         :clauses   :quantum.core.specs/ns-clauses))

(s/fdef core/ns
  :args :quantum.core.specs/ns-form)

#?(:clj
(defmacro ^:private quotable
  "Returns a spec that accepts both the spec and a (quote ...) form of the spec"
  [spec]
  `(s/or :spec ~spec :quoted-spec (s/cat :quote #{'quote} :spec ~spec))))

(s/def :quantum.core.specs/quotable-import-list
  (s/* (s/alt :class (quotable simple-symbol?)
              :package-list (quotable :quantum.core.specs/package-list))))

(s/fdef core/import
  :args :quantum.core.specs/quotable-import-list)

(s/fdef core/refer-clojure
  :args (s/* (s/alt
               :exclude (s/cat :op (quotable #{:exclude}) :arg (quotable :quantum.core.specs/exclude))
               :only    (s/cat :op (quotable #{:only})    :arg (quotable :quantum.core.specs/only))
               :rename  (s/cat :op (quotable #{:rename})  :arg (quotable :quantum.core.specs/rename)))))

;; ----- INTERFACE ----- ;;

(s/def :quantum.core.specs/code any?) ; TODO must be embeddable

(s/def :interface/name simple-symbol?)

;; ----- REIFY ----- ;;

(s/def :reify/method-name     simple-symbol?)

(s/def :reify|arglist/arg-sym simple-symbol?) ; technically, can be tagged with only particular tags
(s/def :reify/arglist         (s/and vector? (s/+ :reify|arglist/arg-sym)))

(s/def :reify|overload/ret-class-sym simple-symbol?) ; technically, one resolvable to a class

(s/def :reify|overload/body (s/* :quantum.core.specs/code))
