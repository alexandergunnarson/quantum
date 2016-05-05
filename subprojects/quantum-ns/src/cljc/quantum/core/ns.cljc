(ns
  ^{:doc "Useful namespace and var-related functions.
    
          Also provides convenience functions for importing |quantum| namespaces."
    :attribution "Alex Gunnarson"}
  quantum.core.ns
  (:require    [quantum.core.ns.reg-utils :as utils :refer [set-merge ex]]
               [quantum.core.ns.reg       :as reg                        ]
      #?(:clj  [clojure.core              :as core]
         :cljs [cljs.core                 :as core  :refer [Keyword]     ])
               [clojure.string            :as str                        ]
    #?@(:clj  ([clojure.set               :as set                        ]
               [clojure.pprint            :as pprint                     ]
               [clojure.stacktrace                                       ]
               [cljs.analyzer             :as ana                        ]
               [cljs.util                 :as cljs-util                  ]
               [cljs.env                  :as cljs-env                   ]
               )))
  #?(:clj (:import (clojure.lang Keyword Var Namespace))))

#?(:clj
(defmacro search-var
  "Searches for a var @var0 in the available namespaces."
  {:usage '(ns-find abc)
   :todo ["Make it better and filter out unnecessary results"]
   :attribution "Alex Gunnarson"}
  [var0]
 `(->> (all-ns)
       (map ns-publics)
       (filter
         (fn [obj#]
           (->> obj#
                keys
                (map name)
                (apply str)
                (re-find (re-pattern (str '~var0)))))))))

#?(:clj
(defmacro ns-exclude [& syms]
  `(doseq [sym# '~syms]
     (ns-unmap *ns* sym#))))

#?(:clj
(defmacro with-ns
  "Perform an operation in another ns."
  {:todo ["Dubious as to whether this actually works."]}
  [ns- & body]
  (let [ns-0 (ns-name *ns*)]
    `(do (in-ns ~ns-) ~@body (in-ns ~ns-0)))))

#?(:clj
(defmacro with-temp-ns
  "Evaluates @exprs in a temporarily-created namespace.
  All created vars will be destroyed after evaluation."
  {:source "zcaudate/hara.namespace.eval"}
  [& exprs]
  `(try
     (create-ns 'sym#)
     (let [res# (with-ns 'sym#
                  (clojure.core/refer-clojure)
                  ~@exprs)]
       res#)
     (finally (remove-ns 'sym#)))))


#?(:clj
(defmacro import-static
  "Imports the named static fields and/or static methods of the class
  as (private) symbols in the current namespace.
  Example: 
      user=> (import-static java.lang.Math PI sqrt)
      nil
      user=> PI
      3.141592653589793
      user=> (sqrt 16)
      4.0
  Note: The class name must be fully qualified, even if it has already
  been imported.  Static methods are defined as MACROS, not
  first-class fns."
  {:source "Stuart Sierra, via clojure.clojure-contrib/import-static"}
  [class & fields-and-methods]
  (let [only (set (map str fields-and-methods))
        ^Class the-class (Class/forName (str class))
        static?       (fn [^java.lang.reflect.Member x]
                        (-> x .getModifiers java.lang.reflect.Modifier/isStatic))
        statics       (fn [array]
                        (set (map (fn [^java.lang.reflect.Member x] (.getName x))
                                  (filter static? array))))
        all-fields    (-> the-class .getFields  statics)
        all-methods   (-> the-class .getMethods statics)
        fields-to-do  (set/intersection all-fields  only)
        methods-to-do (set/intersection all-methods only)
        make-sym (fn [string]
                     (with-meta (symbol string) {:private true}))
        import-field (fn [name]
                         (list 'def (make-sym name)
                               (list '. class (symbol name))))
        import-method (fn [name]
                          (list 'defmacro (make-sym name)
                                '[& args]
                                (list 'list ''. (list 'quote class)
                                      (list 'apply 'list
                                            (list 'quote (symbol name))
                                            'args))))]
    `(do ~@(map import-field fields-to-do)
         ~@(map import-method methods-to-do)))))

#?(:clj (declare require-quantum*))
#?(:clj
(defn find-macros
  "Returns a map of the namespaces with their macros."
  {:usage '(find-macros '[:lib])}
  [ns-syms]
  (->> (require-quantum* :cljs nil ns-syms true)
       (map
         (fn [[s & r]]
           [s (->> r (apply hash-map) :refer-macros (into #{}))]))
       (remove (fn [[s macros]] (empty? macros)))
       (apply merge {}))))

; ===== MISCELLANEOUS CONVENIENCE FUNCTIONS ====

#?(:clj (def trace #(clojure.stacktrace/print-cause-trace *e)))

; ============ CLASS ALIASES ============

; Just to be able to synthesize class-name aliases...
; TODO these aren't quite right
         (def ANil       nil)
;#?(:clj (def Fn         clojure.lang.IFn))
         (def AKey       #?(:clj clojure.lang.Keyword              :cljs cljs.core.Keyword             ))
         (def ANum       #?(:clj java.lang.Number                  :cljs js/Number                     ))
         (def AExactNum  #?(:clj clojure.lang.Ratio                :cljs js/Number                     ))
         (def AInt       #?(:clj java.lang.Integer                 :cljs js/Number                     ))
         (def ADouble    #?(:clj java.lang.Double                  :cljs js/Number                     ))
         (def ADecimal   #?(:clj java.lang.Double                  :cljs js/Number                     ))
         (def ASet       #?(:clj clojure.lang.APersistentSet       :cljs cljs.core.PersistentHashSet   ))
         (def ABool      #?(:clj Boolean                           :cljs js/Boolean                    ))
         (def AArrList   #?(:clj java.util.ArrayList               :cljs cljs.core.ArrayList           ))
         (def ATreeMap   #?(:clj clojure.lang.PersistentTreeMap    :cljs cljs.core.PersistentTreeMap   ))
         (def ALSeq      #?(:clj clojure.lang.LazySeq              :cljs cljs.core.LazySeq             ))
         (def AVec       #?(:clj clojure.lang.APersistentVector    :cljs cljs.core.PersistentVector    )) ; Conflicts with clojure.core/->Vec
         (def AMEntry    #?(:clj clojure.lang.MapEntry             :cljs cljs.core.Vec                 ))
         (def ARegex     #?(:clj java.util.regex.Pattern           :cljs js/RegExp                     ))
         (def AEditable  #?(:clj clojure.lang.IEditableCollection  :cljs cljs.core.IEditableCollection ))
         (def ATransient #?(:clj clojure.lang.ITransientCollection :cljs cljs.core.ITransientCollection))
         (def AQueue     #?(:clj clojure.lang.PersistentQueue      :cljs cljs.core.PersistentQueue     ))
         (def AMap       #?(:clj java.util.Map                     :cljs cljs.core.IMap                ))
         (def AError     #?(:clj java.lang.Throwable               :cljs js/Error                      ))
#?(:clj  (def ASeq       clojure.lang.ISeq                                                     ))
; Otherwise "Use of undeclared Var"
;#?(:cljs (defrecord Exception                [e]))
;#?(:cljs (defrecord IllegalArgumentException [e]))

; ============ |REQUIRE-QUANTUM| CONVENIENCE FUNCTIONS (EXTEND |NS|) ============

#?(:clj
(defn macro-sym? [ns-sym determine-macros? sym]
  (if determine-macros?
      (let [macro-var
              (or (try (ns-resolve (the-ns ns-sym) sym)
                    (catch Exception _ nil)) ; No namespace found
                  (when @debug?
                    (js-println (str "Tried to determine whether symbol " sym
                                  " was a macro but could not find in ns " ns-sym "."
                                  " Assuming not a macro."))))
            macro-based-on-meta?
              (when macro-var (-> macro-var meta :macro))]
        macro-based-on-meta?)
     (get-in reg/macros [ns-sym sym]))))

#?(:clj
(defn require-quantum* [lang- ns-sym ns-syms & [determine-macros?]]
  (let [_ (when @debug? (js-println "Parsing ns-syms in" ns-sym ns-syms))
        allowed? (fn [[k v]] (contains? #{:cljc lang-} k))
        get-initial
          (fn [k]
            (->> ns-syms
                 (reg/get-ns-syms reg/reg k)
                 (filter allowed?)
                 vals))
        simple-require-statements
          (->> (get-initial :requires)
               (apply set-merge)
               (map vector))
        _ (when @debug? (js-println "simple-require-statements" simple-require-statements))
        core-exclusions (->> :core-exclusions get-initial (apply set-merge)
                             (into [])
                             (conj [:exclude]))
        _ (when @debug? (js-println "core-exclusions" core-exclusions))
        aliases         (->> :aliases         get-initial (apply merge-with merge))
        alias-require-statements       (->> aliases       (map (fn [[k v]] [v :as k])))
        _ (when @debug? (js-println "alias-require-statements" alias-require-statements))
        macro-aliases   (->> :macro-aliases   get-initial (apply merge-with merge))
        macro-alias-require-statements (->> macro-aliases (map (fn [[k v]] [v :as k])))
        _ (when @debug? (js-println "macro-alias-require-statements" macro-alias-require-statements))
        complex-require-statements
          (->> (get-initial :refers)
               (#(do (when @debug? (js-println "INITIAL REFERS" %)) %))
               (apply merge-with set-merge)
               (map
                 (fn [[alias-n refers]]
                   (let [ns-sym-n (or (get aliases       alias-n)
                                    (get macro-aliases alias-n)
                                    (throw
                                      (Exception.
                                        (str "Namespace alias " alias-n " not found in "
                                          {:aliases aliases :macro-aliases macro-aliases}))))
                         refer-template [ns-sym-n :as alias-n :refer]]
                     (condp = lang-
                       :clj  {:refer (conj refer-template (into [] refers))}
                       :cljs
                         (let [macros (->> refers
                                           (filter #(macro-sym? ns-sym-n determine-macros? %))
                                           doall
                                           (into #{}))
                               non-macros (set/difference refers macros)]
                           {:refer        (when-not (empty? non-macros)
                                            (conj refer-template (into [] non-macros)))
                            :refer-macros (when (or ((complement empty?) macros)
                                                    (contains? reg/macros ns-sym-n))
                                            (conj refer-template (into [] macros)))})))))
               (apply merge-with
                 (fn [a b] (->> (if (vector? a) (list a) a) (cons b))))
               (map (fn [[k v]] [k (if (vector? v) (list v) v)]))
               (apply merge {}))
        _ (when @debug? (js-println "complex-require-statements" complex-require-statements))
        require-statements-f
          (->> complex-require-statements 
               :refer
               (into simple-require-statements)
               (into alias-require-statements)
               (remove empty?))
        macro-require-statements-0
          (->> complex-require-statements
               :refer-macros
               (into macro-alias-require-statements)
               (remove empty?))
        macro-require-statements-f
          (->> require-statements-f
               (filter #(->> % first (contains? reg/macros)))
               (map (fn [[sym-n _ alias-n]] [sym-n :as alias-n]))
               (into macro-require-statements-0))
        final-statements
          {:require        require-statements-f
           :require-macros macro-require-statements-f
           :refer-clojure  core-exclusions}
        _ (when @debug?
            (println "/*")
            (clojure.pprint/pprint final-statements)
            (println "*/"))] ; do it no matter what
  
  ; Injections
  (->> (for [ns-sym-n ns-syms]
         (get-in reg/reg [ns-sym-n :injection lang-]))
       (remove nil?)
       (map (fn [f] ((eval f))))
       doall)

  (condp = lang-
    :clj
      (do ; Can't use a macro in the same namespace... hmm  
          (apply (mfn quantum.core.ns/ns-exclude)
            (second core-exclusions))
          (apply require      require-statements-f)
          (apply (mfn import) (reg/get-ns-syms reg/reg :imports ns-syms)))
    :cljs final-statements))))

#?(:clj
(defn require-quantum-cljs [ns-sym ns-syms]
  ; Double backslash because it's printing to each .js file too 
  (js-println "REQUIRE-QUANTUM FOR" ns-sym ns-syms)
  (when (empty? ns-syms)
    (throw (Exception. "|require-quantum| body cannot be empty.")))
  (require-quantum* :cljs ns-sym ns-syms)))

#_(:clj
(do (in-ns 'clojure.core)
    (require 'quantum.core.ns)
    (def require-quantum #(quantum.core.ns/require-quantum* :clj nil %))
    (in-ns 'quantum.core.ns)))

; ===== CLOJURESCRIPT REQUIRE-QUANTUM PATCH ===== 

; Technically for CLJS, but uses |require-quantum-cljs| 
#?(:clj
(defn desugar-ns-specs+
  "Given an original set of ns specs desugar :include-macros and :refer-macros
   usage into only primitive spec forms - :use, :require, :use-macros,
   :require-macros. If a library includes a macro file of with the same name
   as the namespace will also be desugared."
  ([args] (desugar-ns-specs+ nil args))
  ([ns-sym args]
    (let [{:keys [require-quantum] :as args-mod}
          (->> args
               (map (fn [[k & specs]] [k (into [] specs)]))
               (into {}))
          {qrequire        :require
           qrequire-macros :require-macros
           qrefer-clojure  :refer-clojure
           :or {qrequire [] qrequire-macros [] qrefer-clojure []}
           :as quantum-specs}
          (if require-quantum
              (->> require-quantum first (require-quantum-cljs ns-sym))
              {})
          {:keys [require] :as indexed}
          (-> args-mod
              ((fn [args-mod-n]
                 (if require-quantum
                     (-> args-mod-n
                         (update :require        #(into % qrequire       ))
                         (update :require-macros #(into % qrequire-macros))
                         (update :refer-clojure  #(into (into [] %) qrefer-clojure )))
                     args-mod-n)))
              (dissoc :require-quantum))
          ; To rebind them so the compiler doesn't freak out when it require-quantum
          args (->> indexed (map (fn [[k v]] (->> v (into [k]) seq))))
          sugar-keys #{:include-macros :refer-macros}
          remove-from-spec
          (fn [pred spec]
            (if-not (and (sequential? spec) (some pred spec))
              spec
              (let [[l r] (split-with (complement pred) spec)]
                (recur pred (concat l (drop 2 r))))))
          replace-refer-macros
          (fn [spec]
            (if-not (sequential? spec)
              spec
              (map (fn [x] (if (= x :refer-macros) :refer x)) spec)))
          reload-spec? #(#{:reload :reload-all} %)
          to-macro-specs
          (fn [specs]
            (->> specs
              (filter
                (fn [x]
                  (or (and (sequential? x)
                           (some sugar-keys x))
                      (reload-spec? x)
                      (ana/macro-autoload-ns? x))))
              (map (fn [x]
                     (if-not (reload-spec? x)
                       (->> x (remove-from-spec #{:include-macros})
                              (remove-from-spec #{:refer})
                              (replace-refer-macros))
                       x)))))
          remove-sugar (partial remove-from-spec sugar-keys)]
      (if-let [require-specs (seq (to-macro-specs require))]
        (map (fn [x]
               (if-not (reload-spec? x)
                 (let [[k v] x]
                   (cons k (map remove-sugar v)))
                 x))
          (update-in indexed [:require-macros] (fnil into []) require-specs))
        args)))))

; |in-ns| is necessary; can't do it inline
#?(:clj (in-ns 'cljs.analyzer))

#?(:clj
(.addMethod ^clojure.lang.MultiFn parse 'ns
  (fn [_ env [_ name & args :as form] _ opts]
    (when-not (symbol? name)
      (throw (error env "Namespaces must be named by a symbol.")))
    (let [name (cond-> name (:macros-ns opts) macro-ns-name)]
      (let [segments (string/split (clojure.core/name name) #"\.")]
        (when (= 1 (count segments))
          (warning :single-segment-namespace env {:name name}))
        (when (some js-reserved segments)
          (warning :munged-namespace env {:name name}))
        (find-def-clash env name segments)
        #?(:clj
           (when (some (complement util/valid-js-id-start?) segments)
             (throw
               (AssertionError.
                 (str "Namespace " name " has a segment starting with an invaild "
                      "JavaScript identifier"))))))
      (let [docstring    (if (string? (first args)) (first args))
            mdocstr      (-> name meta :doc)
            args         (if docstring (next args) args)
            metadata     (if (map? (first args)) (first args))
            form-meta    (meta form)
            args         (quantum.core.ns/desugar-ns-specs+ (if metadata (next args) args))
            name         (vary-meta name merge metadata)
            excludes     (parse-ns-excludes env args)
            deps         (atom #{})
            aliases      (atom {:fns {} :macros {}})
            spec-parsers {:require        (partial parse-require-spec env false deps aliases)
                          :require-macros (partial parse-require-spec env true deps aliases)
                          :use            (comp (partial parse-require-spec env false deps aliases)
                                            (partial use->require env))
                          :use-macros     (comp (partial parse-require-spec env true deps aliases)
                                            (partial use->require env))
                          :import         (partial parse-import-spec env deps)}
            valid-forms  (atom #{:use :use-macros :require :require-macros :import})
            reload       (atom {:use nil :require nil :use-macros nil :require-macros nil})
            reloads      (atom {})
            {uses :use requires :require use-macros :use-macros require-macros :require-macros imports :import :as params}
            (reduce
              (fn [m [k & libs]]
                (when-not (#{:use :use-macros :require :require-macros :import} k)
                  (throw (error env "Only :refer-clojure, :require, :require-macros, :use, :use-macros, and :import libspecs supported")))
                (when-not (@valid-forms k)
                  (throw (error env (str "Only one " k " form is allowed per namespace definition"))))
                (swap! valid-forms disj k)
                ;; check for spec type reloads
                (when-not (= :import k)
                  (when (some #{:reload} libs)
                    (swap! reload assoc k :reload))
                  (when (some #{:reload-all} libs)
                    (swap! reload assoc k :reload-all)))
                ;; check for individual ns reloads from REPL interactions
                (when-let [xs (seq (filter #(-> % meta :reload) libs))]
                  (swap! reloads assoc k
                    (zipmap (map first xs) (map #(-> % meta :reload) xs))))
                (apply merge-with merge m
                  (map (spec-parsers k)
                    (remove #{:reload :reload-all} libs))))
              {} (remove (fn [[r]] (= r :refer-clojure)) args))]
        (set! *cljs-ns* name)
        (let [ns-info
              {:name           name
               :doc            (or docstring mdocstr)
               :excludes       excludes
               :use-macros     use-macros
               :require-macros require-macros
               :uses           uses
               :requires       requires
               :imports        imports}
              ns-info
              (if (:merge form-meta)
                ;; for merging information in via require usage in REPLs
                (let [ns-info' (get-in @env/*compiler* [::namespaces name])]
                  (if (pos? (count ns-info'))
                    (let [merge-keys
                          [:use-macros :require-macros :uses :requires :imports]]
                      (merge
                        ns-info'
                        (merge-with merge
                          (select-keys ns-info' merge-keys)
                          (select-keys ns-info merge-keys))))
                    ns-info))
                ns-info)]
          (swap! env/*compiler* update-in [::namespaces name] merge ns-info)
          (merge {:op      :ns
                  :env     env
                  :form    form
                  :deps    @deps
                  :reload  @reload
                  :reloads @reloads}
            (cond-> ns-info
              (@reload :use)
              (update-in [:uses]
                (fn [m] (with-meta m {(@reload :use) true})))
              (@reload :require)
              (update-in [:requires]
                (fn [m] (with-meta m {(@reload :require) true})))))))))))

#?(:clj (in-ns 'quantum.core.ns))