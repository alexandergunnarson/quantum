(ns quantum.core.specs
  "Adapted from clojure.core.specs version 1.9.0-alpha19
   and enabled in CLJS. Other specs added too.
   See https://github.com/clojure/core.specs.alpha/blob/5d85f93ab78386855374256934475af0afe7f380/src/main/clojure/clojure/core/specs/alpha.clj"
  (:require
    [clojure.core      :as core]
    [clojure.set       :as set]
    [quantum.core.core
      :refer [val?]]
    [quantum.core.fn   :as fn
      :refer [fn1 fnl]]
    [quantum.core.spec :as s]))

;;;; GENERAL

(s/def ::meta map?)

;;;; destructure

(s/def ::local-name (s/and simple-symbol? #(not= '& %)))

(s/def ::binding-form
  (s/or :sym ::local-name
        :seq ::seq-binding-form
        :map ::map-binding-form))

;; sequential destructuring

(s/def ::seq-binding-form
  (s/and vector?
         (s/cat :elems (s/* ::binding-form)
                :rest  (s/? (s/cat :amp #{'&} :form ::binding-form))
                :as    (s/? (s/cat :as #{:as} :sym ::local-name)))))

;; map destructuring

(s/def ::keys (s/coll-of ident?         :kind vector?))
(s/def ::syms (s/coll-of symbol?        :kind vector?))
(s/def ::strs (s/coll-of simple-symbol? :kind vector?))
(s/def ::or   (s/map-of simple-symbol? any?))
(s/def ::as ::local-name)

(s/def ::map-special-binding
  (s/keys :opt-un [::as ::or ::keys ::syms ::strs]))

(s/def ::map-binding (s/tuple ::binding-form any?))

(s/def ::ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

(s/def ::map-bindings
  (s/every (s/or :mb ::map-binding
                 :nsk ::ns-keys
                 :msb (s/tuple #{:as :or :keys :syms :strs} any?)) :into {}))

(s/def ::map-binding-form (s/merge ::map-bindings ::map-special-binding))

;; bindings

(s/def ::binding (s/cat :binding ::binding-form :init-expr any?))
(s/def ::bindings (s/and vector? (s/* ::binding)))

;; let, if-let, when-let

(s/fdef core/let
  :args (s/cat :bindings ::bindings
               :body (s/* any?)))

(s/fdef core/if-let
  :args (s/cat :bindings (s/and vector? ::binding)
               :then any?
               :else (s/? any?)))

(s/fdef core/when-let
  :args (s/cat :bindings (s/and vector? ::binding)
               :body (s/* any?)))

;; defn, defn-, fn

(s/def ::fn|arglist
  (s/and
    vector?
    (s/cat :args    (s/* ::binding-form)
           :varargs (s/? (s/cat :amp #{'&} :form ::binding-form)))))

(s/def ::fn|prepost
  (s/and (s/keys :req-un [(or ::core/pre ::core/post)]) ; TODO we actually really only want to accept un-namespaced keys...
         (s/conformer #(set/rename-keys % {::core/pre :pre ::core/post :post}))))

(s/def ::fn|body
  (s/alt :prepost+body (s/cat :prepost ::fn|prepost
                              :body    (s/+ any?))
         :body         (s/* any?)))

(s/def ::fn|arglist+body
  (s/cat ::fn|arglist ::fn|arglist :body ::fn|body))

(s/def ::fn|name simple-symbol?)

(s/def ::docstring string?)

(s/def ::fn|unique-doc
  #(->> [(::docstring %)
         (-> % ::fn|name meta :doc)
         (-> % :pre-meta      :doc)
         (-> % :post-meta     :doc)]
        (filter val?)
        count
        ((fn [x] (<= x 1)))))

(s/def ::fn|unique-meta
  #(empty? (set/intersection
             (-> % ::fn|name meta keys set)
             (-> % :pre-meta  keys set)
             (-> % :post-meta keys set))))

(s/def ::fn|aggregate-meta
  (s/conformer
    (fn [{:keys [::fn|name ::docstring pre-meta post-meta] :as m}]
      (-> m
          (dissoc ::docstring :pre-meta :post-meta)
          (cond-> fn|name
            (update ::fn|name with-meta
              (-> (merge (meta fn|name) pre-meta post-meta) ; TODO use `merge-unique` instead of `::defn|unique-meta`
                  (cond-> docstring (assoc :doc docstring)))))))))

(s/def ::fn|postchecks
  (s/and (s/conformer
           (fn [v]
             (let [[arities-k arities-v] (get v :arities)
                   arities
                    (-> (case arities-k
                          :arity-1 {:arities [arities-v]}
                          :arity-n arities-v)
                        (update :arities
                          (fnl mapv
                            (fn1 update :body
                              (fn [[k v]]
                                (case k
                                  :body         {:body v}
                                  :prepost+body v))))))]
               (assoc v :post-meta (:post-meta arities)
                        :arities   (:arities   arities)))))
         ::fn|unique-doc
         ::fn|unique-meta
         ;; TODO validate metadata like return value etc.
         ::fn|aggregate-meta))

(s/def ::fn
  (s/and (s/spec
           (s/cat ::fn|name (s/? ::fn|name)
                  :arities  (s/alt :arity-1 ::fn|arglist+body
                                   :arity-n (s/cat :arities (s/+ (s/spec ::fn|arglist+body))))))
         ::fn|postchecks))

(s/def ::defn
  (s/and
    (s/spec
      (s/cat ::fn|name   ::fn|name
             ::docstring (s/? ::docstring)
             :pre-meta   (s/? ::meta)
             :arities    (s/alt :arity-1 ::fn|arglist+body
                                :arity-n (s/cat :arities   (s/+ (s/spec ::fn|arglist+body))
                                                :post-meta (s/? ::meta)))))
    ::fn|postchecks))

(s/fdef core/defn  :args ::defn :ret any?)
(s/fdef core/defn- :args ::defn :ret any?)
(s/fdef core/fn    :args ::fn   :ret any?)

;;;; ns

(s/def ::exclude (s/coll-of simple-symbol?))
(s/def ::only    (s/coll-of simple-symbol?))
(s/def ::rename  (s/map-of simple-symbol? simple-symbol?))
(s/def ::filters (s/keys* :opt-un [::exclude ::only ::rename]))

(s/def ::ns-refer-clojure
  (s/spec (s/cat :clause #{:refer-clojure}
                 :filters ::filters)))

(s/def ::refer (s/or :all #{:all}
                     :syms (s/coll-of simple-symbol?)))

(s/def ::prefix-list
  (s/spec
    (s/cat :prefix simple-symbol?
           :libspecs (s/+ ::libspec))))

(s/def ::libspec
  (s/alt :lib      simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer])))))

(s/def ::ns-require
  (s/spec (s/cat :clause #{:require}
                 :body (s/+ (s/alt :libspec ::libspec
                                   :prefix-list ::prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(s/def ::package-list
  (s/spec
    (s/cat :package simple-symbol?
           :classes (s/* simple-symbol?))))

(s/def ::import-list
  (s/* (s/alt :class simple-symbol?
              :package-list ::package-list)))

(s/def ::ns-import
  (s/spec
    (s/cat :clause #{:import}
           :classes ::import-list)))

(s/def ::ns-refer
  (s/spec (s/cat :clause #{:refer}
                 :lib simple-symbol?
                 :filters ::filters)))

;; same as ::prefix-list, but with ::use-libspec instead
(s/def ::use-prefix-list
  (s/spec
    (s/cat :prefix simple-symbol?
           :libspecs (s/+ ::use-libspec))))

;; same as ::libspec, but also supports the ::filters options in the libspec
(s/def ::use-libspec
  (s/alt :lib simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer ::exclude ::only ::rename])))))

(s/def ::ns-use
  (s/spec (s/cat :clause #{:use}
                 :libs (s/+ (s/alt :libspec ::use-libspec
                                   :prefix-list ::use-prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(s/def ::ns-load
  (s/spec (s/cat :clause #{:load}
                 :libs (s/* string?))))

(s/def ::name simple-symbol?)
(s/def ::extends simple-symbol?)
(s/def ::implements (s/coll-of simple-symbol? :kind vector?))
(s/def ::init symbol?)
(s/def ::class-ident (s/or :class simple-symbol? :class-name string?))
(s/def ::signature (s/coll-of ::class-ident :kind vector?))
(s/def ::constructors (s/map-of ::signature ::signature))
(s/def ::post-init symbol?)
(s/def ::method (s/and vector?
                  (s/cat :name simple-symbol?
                         :param-types ::signature
                         :return-type simple-symbol?)))
(s/def ::methods (s/coll-of ::method :kind vector?))
(s/def ::main boolean?)
(s/def ::factory simple-symbol?)
(s/def ::state simple-symbol?)
(s/def ::get simple-symbol?)
(s/def ::set simple-symbol?)
(s/def ::expose (s/keys :opt-un [::get ::set]))
(s/def ::exposes (s/map-of simple-symbol? ::expose))
(s/def ::prefix string?)
(s/def ::impl-ns simple-symbol?)
(s/def ::load-impl-ns boolean?)

(s/def ::ns-gen-class
  (s/spec (s/cat :clause #{:gen-class}
                 :options (s/keys* :opt-un [::name ::extends ::implements
                                            ::init ::constructors ::post-init
                                            ::methods ::main ::factory ::state
                                            ::exposes ::prefix ::impl-ns ::load-impl-ns]))))

(s/def ::ns-clauses
  (s/* (s/alt :refer-clojure ::ns-refer-clojure
              :require       ::ns-require
              :import        ::ns-import
              :use           ::ns-use
              :refer         ::ns-refer
              :load          ::ns-load
              :gen-class     ::ns-gen-class)))

(s/def ::ns-form
  (s/cat :name      simple-symbol?
         :docstring (s/? ::docstring)
         :attr-map  (s/? ::meta)
         :clauses   ::ns-clauses))

(s/fdef core/ns
  :args ::ns-form)

#?(:clj
(defmacro ^:private quotable
  "Returns a spec that accepts both the spec and a (quote ...) form of the spec"
  [spec]
  `(s/or :spec ~spec :quoted-spec (s/cat :quote #{'quote} :spec ~spec))))

(s/def ::quotable-import-list
  (s/* (s/alt :class (quotable simple-symbol?)
              :package-list (quotable ::package-list))))

(s/fdef core/import
  :args ::quotable-import-list)

(s/fdef core/refer-clojure
  :args (s/* (s/alt
               :exclude (s/cat :op (quotable #{:exclude}) :arg (quotable ::exclude))
               :only    (s/cat :op (quotable #{:only})    :arg (quotable ::only))
               :rename  (s/cat :op (quotable #{:rename})  :arg (quotable ::rename)))))
