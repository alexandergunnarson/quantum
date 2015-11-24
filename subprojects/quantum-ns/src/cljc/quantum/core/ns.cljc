(ns
  ^{:doc "Useful namespace and var-related functions.
    
          Also provides convenience functions for importing |quantum| namespaces."
    :attribution "Alex Gunnarson"}
  quantum.core.ns
  (:require
    [clojure.set :as set]
    #?@(:clj  ([clojure.repl   :as repl  ]
               [clojure.java.javadoc]
               [clojure.pprint :as pprint]
               [clojure.stacktrace])
        :cljs ([cljs.core :as core :refer [Keyword]])))
  #?(:clj (:import (clojure.lang Keyword Var Namespace)))
  
  )
; #?(:clj (:gen-class))

#?(:clj (set! *unchecked-math*     :warn-on-boxed))
#?(:clj (set! *warn-on-reflection* true))

(def ns-debug? (atom false))
(def externs? (atom true))

(defn js-println [& args]
  (print "\n/* " )
  (apply println args)
  (println "*/"))

(defn this-fn-name
  ([] (this-fn-name :curr))
  ([k]
  #?(:clj  (-> (Thread/currentThread)
               .getStackTrace
               (#(condp = k
                   :curr (nth % 2)
                   :prev (nth % 3)
                   (throw (Exception. "Unrecognized key."))))
               .getClassName clojure.repl/demunge)
     :cljs "*")))

; ============ VAR MANIPULATION, ETC. ============
; CLJS compatible only if you port |alter-var-root| as in-ns, def, in-ns
#?(:clj
  (defn reset-var!
  "Like |reset!| but for vars."
  {:attribution "Alex Gunnarson"}
  [var-0 val-f]
  ;(.bindRoot #'clojure.core/ns ns+)
  ;(alter-meta! #'clojure.core/ns merge (meta #'ns+))

  (alter-var-root var-0 (constantly val-f))))
; CLJS compatible
#?(:clj (defn swap-var!
  "Like |swap!| but for vars."
  {:attribution "Alex Gunnarson"}
  ([var-0 f]
  (do (alter-var-root var-0 f)
       var-0))
  ([var-0 f & args]
  (do (alter-var-root var-0
         (fn [var-n]
           (apply f var-n args)))
       var-0))))


#?(:clj
(defmacro context
  {:contributors ["The Joy of Clojure, 2nd ed." "Alex Gunnarson"]
   :todo ["'IOException: Pushback buffer overflow' on certain
            very large data structures"
          "Use reducers"]}
  ([] `(context :clj))
  ([lang]
    (condp = lang
      :clj 
        (let [symbols (keys &env)]
          (zipmap
            (map (fn [sym] `(quote ~sym))
                    symbols)
            symbols))
      :cljs
        ; #{:ns :context :locals :fn-scope :js-globals :line :column}
        `(->> '~&env
              :locals
              (map (fn [[sym# meta#]]
                     [sym# (-> meta# :init :form)]))
              (into {}))))))

#?(:clj ; for now
  (defn c-eval
    "Contextual eval. Restricts the use of specific bindings to |eval|.

     Suffers from not being able to work on non-simples (e.g. atoms cannot be c-evaled)."
    {:attribution "The Joy of Clojure, 2nd ed."
     :todo ["'IOException: Pushback buffer overflow' on certain
              very large data structures"]}
    ([context expr]
      (eval
       `(let [~@(mapcat
                  (fn [[k v]]
                    (try [k `'~v]
                      (catch java.io.IOException _ [k "var too large to show"])))
                  context)]
          ~expr)))))
 
#?(:clj ; for now
  (defmacro let-eval [expr]
    `(c-eval context ~expr)))

#?(:clj ; /resolve/ not in cljs
  (defn resolve-key
    "Resolves the provided keyword as a symbol for a var
     in the current namespace.
     USAGE: (resolve-key :my-var)"
    ^{:attribution "Alex Gunnarson"}
    [^Keyword k]
    (-> k name symbol resolve)))

 #?(:clj ; for now
  (defn eval-key
    "Evaluates the provided keyword as a symbol for a var
     in the current namespace.
     USAGE: (eval-key :my-var)"
    ^{:attribution "Alex Gunnarson"}
    [^Keyword k]
    (-> k name symbol eval)))

 #?(:clj
  (defn var-name
    "Get the namespace-qualified name of a var."
    ^{:attribution "flatland.useful.ns"}
    [v]
    (apply symbol
      (map str
        ((juxt (comp ns-name :ns)
               :name)
               (meta v))))))

#?(:clj
(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  {:attribution "clojure.contrib.def/defalias"
   :contributors ["Alex Gunnarson"]}
  ([name orig]
     `(do
        (let [orig-var# (var ~orig)]
          (if true ; Can't have different clj-cljs things within macro...  ;#?(:clj (-> orig-var# .hasRoot) :cljs true)
              (do (def ~name (with-meta (-> ~orig var deref) (meta (var ~orig))))
                  ; for some reason, the :macro metadata doesn't really register unless you do it manually 
                  (when (-> orig-var# meta :macro true?)
                    (alter-meta! #'~name assoc :macro true)))
              (def ~name)))
        (var ~name)))
  ([name orig doc]
     (list `defalias (with-meta name (assoc (meta name) :doc doc)) orig))))

#?(:clj
(defmacro defmalias
  "Defines a macro alias, whether or not the original symbol is a macro.

   May be unused."
  [name orig]
  `(do (defalias ~name ~orig)
       (alter-meta! (var ~name) assoc :macro true)
       (var ~name))))

#?(:clj
(defn alias-var
  "Create a var with the supplied name in the current namespace, having the same
  metadata and root-binding as the supplied var."
  {:attribution "flatland.useful.ns"}
  [sym var-0]
  (apply intern *ns*
    (with-meta sym
      (merge
        {:dont-test
          (str "Alias of " (var-name var-0))}
        (meta var-0)
        (meta sym)))
    (when (.hasRoot var-0) [@var-0]))))

#?(:clj
(defn alias-ns
  "Create vars in the current namespace to alias each of the public vars in
  the supplied namespace.
  Takes a symbol."
  {:attribution "flatland.useful.ns"}
  [ns-name]
  (require ns-name)
  (doseq [[name var] (ns-publics (the-ns ns-name))]
    (alias-var name var))))

#?(:clj
(defn defs
  "Defines a provided list of symbol-value pairs as vars in the
   current namespace."
  {:attribution "Alex Gunnarson"
   :usage '(defs 'a 1 'b 2 'c 3)}
  [& {:as vars}]
  (doseq [var-n vars]
    (intern *ns* (-> var-n key name symbol) (val var-n)))))

#?(:clj
(defmacro def-
  "Like |defn-| but without the function definition implicit."
  {:attribution "Alex Gunnarson"}
  [sym v] `(doto (def ~sym ~v) (alter-meta! merge {:private true}))))

#?(:clj
  (defn defs-private
    "Like |defs|: defines a provided list of symbol-value pairs
     as private vars in the current namespace."
    {:attribution "Alex Gunnarson"
     :todo       ["Needs maintenance"]
     :usage '(defs-private 'a 1 'b 2 'c 3)}
    [& {:as vars}]
    (doseq [var-n vars]
      (intern *ns* (-> var-n key name symbol) (val var-n))
      (alter-meta! (eval `(var ~(-> var-n key))) assoc :private true))))

#?(:clj
  (defn clear-vars
    {:attribution "Alex Gunnarson"}
    [& vars]
    (doseq [var-n vars]
      (alter-var-root (-> var-n name symbol resolve) (constantly nil)))
    (println "Vars cleared.")))

#?(:clj
  (defn declare-ns
    {:attribution "Alex Gunnarson"}
    [curr-ns]
    (defs-private 'this-ns curr-ns)))

#?(:clj  ; cljs doesn't have reflection
  (defn defaults
    {:attribution "Alex Gunnarson"}
    []
    (set! *warn-on-reflection* true)))

#?(:clj
(defmacro search-var
  "Searches for a var in the available namespaces."
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
(defmacro mfn
  "|mfn| is short for 'macro-fn', just as 'jfn' is short for 'java-fn'.
   Originally named |functionize| by mikera."
  {:attribution "mikera, http://stackoverflow.com/questions/9273333/in-clojure-how-to-apply-a-macro-to-a-list/9273560#9273560"}
  ([macro-sym]
   `(fn [& args#]
      (println "WARNING: Runtime eval with |mfn| via" '~macro-sym)
      (clojure.core/eval (cons '~macro-sym args#))))
  ([n macro-sym]
    (let [genned-arglist (->> (repeatedly gensym) (take n) (into []))]
      `(fn ~genned-arglist
         (~macro-sym ~@genned-arglist))))))


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
        the-class (. Class forName (str class))
        static? (fn [x]
                    (. java.lang.reflect.Modifier
                       (isStatic (. x (getModifiers)))))
        statics (fn [array]
                    (set (map (memfn getName)
                              (filter static? array))))
        all-fields (statics (. the-class (getFields)))
        all-methods (statics (. the-class (getMethods)))
        fields-to-do (set/intersection all-fields only)
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

; ============ CLASS ALIASES ============

; Just to be able to synthesize class-name aliases...

(def       ANil       nil)
;#?(:clj (def Fn        clojure.lang.Fn))
(def       AKey       #?(:clj clojure.lang.Keyword              :cljs cljs.core.Keyword             ))
(def       ANum       #?(:clj java.lang.Number                  :cljs js/Number                     ))
(def       AExactNum  #?(:clj clojure.lang.Ratio                :cljs js/Number                     ))
(def       AInt       #?(:clj java.lang.Integer                 :cljs js/Number                     ))
(def       ADouble    #?(:clj java.lang.Double                  :cljs js/Number                     ))
(def       ADecimal   #?(:clj java.lang.Double                  :cljs js/Number                     ))
(def       ASet       #?(:clj clojure.lang.APersistentSet       :cljs cljs.core.PersistentHashSet   ))
(def       ABool      #?(:clj Boolean                           :cljs js/Boolean                    ))
(def       AArrList   #?(:clj java.util.ArrayList               :cljs cljs.core.ArrayList           ))
(def       ATreeMap   #?(:clj clojure.lang.PersistentTreeMap    :cljs cljs.core.PersistentTreeMap   ))
(def       ALSeq      #?(:clj clojure.lang.LazySeq              :cljs cljs.core.LazySeq             ))
(def       AVec       #?(:clj clojure.lang.APersistentVector    :cljs cljs.core.PersistentVector    )) ; Conflicts with clojure.core/->Vec
(def       AMEntry    #?(:clj clojure.lang.MapEntry             :cljs cljs.core.Vec                 ))
(def       ARegex     #?(:clj java.util.regex.Pattern           :cljs js/RegExp                     ))
(def       AEditable  #?(:clj clojure.lang.IEditableCollection  :cljs cljs.core.IEditableCollection ))
(def       ATransient #?(:clj clojure.lang.ITransientCollection :cljs cljs.core.ITransientCollection))
(def       AQueue     #?(:clj clojure.lang.PersistentQueue      :cljs cljs.core.PersistentQueue     ))
(def       AMap       #?(:clj java.util.Map                     :cljs cljs.core.IMap                ))
#?(:clj (def ASeq             clojure.lang.ISeq                                                     ))
(def       AError     #?(:clj java.lang.Throwable               :cljs js/Error                      ))
; #?@(:clj [def ASeq clojure.lang.ISeq]) ; DOESN'T WORK
#?(:clj (do (def ASeq clojure.lang.ISeq)))

(defrecord Nil       [])
(defrecord Key       [])
(defrecord Num       [])
(defrecord ExactNum  [])
(defrecord Int       [])
(defrecord Decimal   [])
(defrecord Set       [])
#?(:clj (defrecord Delay []))
(defrecord Bool      [])
(defrecord ArrList   [])
(defrecord TreeMap   [])
(defrecord LSeq      [])
#?(:cljs (defrecord Vec []))
(defrecord Regex     [])
(defrecord Editable  [])
(defrecord Transient [])
(defrecord Queue     [])
(defrecord Map       [])
(defrecord Seq       [])
(defrecord Record    [])
#?(:clj  (defrecord Fn                       []))
#?(:cljs (defrecord JSObj                    []))
#?(:cljs (defrecord Exception                [^String msg]))
#?(:cljs (defrecord IllegalArgumentException [^String msg]))

; ============ NAMESPACE-REQUIRE CONVENIENCE FUNCTIONS ============

#?(:clj
(defmacro ns-exclude [& syms]
  `(doseq [sym# '~syms]
     (ns-unmap *ns* sym#))))

#?(:clj
  (defn nss
    "Defines, in the provided namespace, conveniently abbreviated symbols
     for other namespaces.
     Esp. for use with /in-ns/, where one can switch more
     quickly between namespaces for testing and coding purposes."
    {:attribution "Alex Gunnarson"
     :usage ['(do (nss *ns*)
                  (in-ns *exp))
             "instead of"
             '(in-ns 'quantum.ui.experimental)]}
    [curr-ns]
    (binding [*ns* curr-ns]
      (defs-private
        'curr-ns      curr-ns
        '*exp         'quantum.ui.experimental
        '*coll        'quantum.core.collections    
        '*func        'quantum.core.function       
        '*io          'quantum.core.io.core        
        '*java        'quantum.core.java           
        '*log         'quantum.core.logic          
        '*ns          'quantum.core.ns             
        '*num         'quantum.core.numeric        
        '*pr          'quantum.core.print          
        '*str         'quantum.core.string   
        '*sys         'quantum.core.system      
        '*thread      'quantum.core.thread         
        '*type        'quantum.core.type           
        '*arr         'quantum.core.data.array     
        '*ftree       'quantum.core.data.ftree     
        '*map         'quantum.core.data.map       
        '*q           'quantum.core.data.queue     
        '*set         'quantum.core.data.set       
        '*vec         'quantum.core.data.vector    
        '*xml         'quantum.core.data.xml       
        '*bench       'quantum.core.util.bench     
        '*debug       'quantum.core.util.debug     
        '*q           'quantum.core.data.queue     
        '*thread      'quantum.core.thread         
        '*err         'quantum.core.error          
        '*time        'quantum.core.time.core      
        '*time-coerce 'quantum.core.time.coerce    
        '*time-form   'quantum.core.time.format    
        '*time-local  'quantum.core.time.local           
        '*grid        'quantum.datagrid.core 
        '*xl          'quantum.datagrid.excel))))

; ; find-doc, doc, and source are incl. in /user/ ns but apparently not in any others
; ; TODO: Possibly find a way to do this in ClojureScript?
#?(:clj (defalias source   repl/source  ))
#?(:clj (defalias find-doc repl/find-doc)) ; searches in clojure function names and docstrings!
#?(:clj (defalias doc      repl/doc     ))
#?(:clj (defalias javadoc  clojure.java.javadoc/javadoc))

#?(:clj (def trace #(clojure.stacktrace/print-cause-trace *e)))

(defn set-merge [& colls]
  (->> colls (apply concat) (into #{})))

(defn get-ns-syms [ns-defs-f k ns-syms]
  (->> ns-syms
       (map
         (fn [ns-sym]
           (or (get-in ns-defs-f [ns-sym k])

               #_(when (= k :refers)
                 (or (get ns-defs-f ns-sym)
                     (throw (Exception. (str "Quantum namespace alias does not exist: " ns-sym)))))
               )))
       (remove empty?)
       (#(cond
           (= k :core-exclusions)
             (apply set-merge %)
           (= k :imports)
             (apply set-merge %)
           (= k :requires)
             (apply merge-with set-merge %)
           :else
             (apply merge-with merge %)))))

(def ns-defs
  (let [ns-defs-0
        '{; LIB
          core-async 
            {:aliases       {:clj  {core-async  clojure.core.async}
                             :cljs {core-async  cljs.core.async}}
             :macro-aliases {:cljs {asyncm cljs.core.async.macros}}
             :refers        {:cljc {core-async  #{<! >! alts!}}
                             :clj  {core-async  #{go go-loop thread}}
                             :cljs {asyncm #{go go-loop}}}}
          async
            {:aliases {:cljc {async quantum.core.thread.async}}
             :refers  {:cljc {async #{concur put!! >!! take!! <!! empty! peek!! alts!! chan wait-until}}}}
          res
            {:aliases {:cljc {res       quantum.core.resources}
                       :clj  {component com.stuartsierra.component}}
             :refers  {:cljc {res #{with-cleanup with-resources}}}}
          coll
            {:aliases
               {:cljc {coll quantum.core.collections}}
             :core-exclusions
              #{contains? for doseq subseq reduce repeat repeatedly
                range merge count
                vec sorted-map sorted-map-by 
                into first second rest
                last butlast get pop peek empty take take-while
                key val conj! assoc! dissoc! disj!} 
             :refers
               {:cljc
                 {coll #{for fori for-m until doseq doseqi reduce reducei reducei-
                         count lasti
                         subseq getr gets
                         repeat repeatedly
                         range range+
                         merge merge-keep-left mergel merger
                         vec
                         array
                         sorted-map
                         sorted-map-by
                         get first second rest
                         last butlast
                         pop popl popr peek
                         empty
                         conjl conjr
                         index-of last-index-of
                         take ltake take-while ltake-while
                         take-until take-until-inc
                         take-from take-after
                         takel takel-from takel-after
                         taker-untili taker-until
                         dropl dropr dropr-until dropr-after
                         into redv redm fold foldm
                         map+ map-keys+ map-vals+
                         ffilter filter+ remove+
                         flatten+
                         dissoc-in+ assocs-in+ update-in+
                         split-remove
                         kmap
                         key val
                         contains? in? in-v? in-k? containsk? containsv?
                         postwalk prewalk walk
                         conj! disj! assoc! dissoc! update!
                         update-nth update-first update-last
                         genkeyword
                         break
                         deficlass
                         seq-loop loopr}}}
             :imports (quantum.core.collections.core.MutableContainer)}
          diff   {:aliases {:cljc {diff   quantum.core.collections.diff}}}
          ccore  {:aliases {:cljc {ccore  quantum.core.collections.core}}
                  :imports (quantum.core.collections.core.MutableContainer)}
          cbase  {:aliases {:cljc {cbase  quantum.core.collections.base
                                   zip    fast-zip.core}}
                  :imports (fast_zip.core.ZipperLocation)}
          crypto {:aliases {:cljc {crypto quantum.core.cryptography    }}}
          err
            {:core-exclusions #{assert}
             :aliases   {:cljc {err      quantum.core.error}
                         :cljs {err-cljs quantum.core.cljs.error}}
             :refers    {:cljc {err      #{throw+ with-assert assert with-throw with-throws throw-when throw-unless assertf-> assertf->>}}
                         :clj  {err      #{try+ try-times}}
                         :cljs {err      #{Err}
                                err-cljs #{try+}}}
             :imports   (quantum.core.error.Err)}
          graph
            {:aliases {:cljc {graph quantum.core.graph}}}
          io 
            {:aliases {:cljc {io quantum.core.io.core}}
             :imports 
              ((java.io File
                        FileNotFoundException IOException
                        FileReader PushbackReader
                        DataInputStream DataOutputStream 
                        OutputStream FileOutputStream
                        ByteArrayOutputStream
                        BufferedOutputStream BufferedInputStream
                        InputStream  FileInputStream
                        PrintWriter))}
          io-ser  {:aliases       {:clj  {io-ser quantum.core.io.serialization}}}
          fs      {:aliases       {:cljc {fs     quantum.core.io.filesystem   }}}

          java    {:aliases       {:cljc {java quantum.core.java   }}}
          log     {:aliases       {:cljc {log  quantum.core.log    }}}
          num     {:aliases       {:cljc {num  quantum.core.numeric}}
                   :core-exclusions #{dec inc}
                   :refers        {:cljc {num #{nneg? greatest least +* -* **
                                                dec dec*
                                                inc inc*
                                                += -=
                                                ++ --}}}}
          ns
            {:requires {:cljc #{clojure.core.rrb-vector}
                        :clj  #{flatland.ordered.map   }}
             :aliases  {:cljc {ns      quantum.core.ns  
                               ;test    quantum.core.test
                             }
                        :clj  {core    clojure.core
                               refresh clojure.tools.namespace.repl
                               proteus proteus}
                        :cljs {core cljs.core}}
             :refers   {:cljc
                         {ns   #{defalias defmalias source def- swap-var! reset-var! ns-exclude js-println
                                 ANil ABool ADouble ANum AExactNum AInt ADecimal AKey AVec ASet
                                 AArrList ATreeMap ALSeq ARegex AEditable ATransient AQueue AMap AError}
                          ;test #{qtest}
                        }
                        :clj {ns      #{alias-ns defs javadoc}
                              refresh #{refresh refresh-all}
                              proteus #{let-mutable}}
                        :cljs
                         {ns #{Exception IllegalArgumentException
                               Nil Bool Num ExactNum Int Decimal Key Vec Set
                               ArrList TreeMap LSeq Regex Editable Transient Queue Map}}}
            :injection
              {:clj #(do (set! clojure.core/*warn-on-reflection* true)
                         (set! clojure.core/*unchecked-math* :warn-on-boxed)
                         #_(gen-class) ; not needed unless for interop, apparently
                         nil)}
            :imports
              ((quantum.core.ns
                  Nil Bool Num ExactNum Int Decimal Key Map Set Queue Fn
                  ArrList TreeMap LSeq Regex #_Editable Transient)
               clojure.lang.Compiler$CompilerException
               (clojure.lang
                 Namespace Symbol
                 Keyword
                 Delay
                 Atom Var
                 AFunction
                 PersistentList
                 APersistentVector PersistentVector
                 MapEntry
                 APersistentMap    PersistentArrayMap PersistentHashMap
                 APersistentSet
                 PersistentQueue
                 LazySeq
                 Ratio)
               java.util.regex.Pattern
               (java.util ArrayList)
               org.joda.time.DateTime
               clojure.core.Vec
               (java.math BigDecimal)
               clojure.core.rrb_vector.rrbt.Vector
               flatland.ordered.map.OrderedMap)}
          paths    {:aliases {:clj  {paths    quantum.core.paths            }} :refers {:clj  {paths  #{paths}}}}
          cache    {:core-exclusions #{memoize}
                    :aliases {:cljc {cache    quantum.core.cache}}
                    :refers  {:cljc {cache #{memoize}}}}
          convert  {:aliases {:cljc {conv quantum.core.convert}}
                    :refers  {:cljc {conv #{->str ->bytes}}}}
          pconvert {:aliases {:cljc {pconv quantum.core.convert.primitive}}
                    :core-exclusions #{boolean byte char short int long float double}
                    :refers  {:cljc {pconv #{boolean ->boolean
                                             byte    ->byte   ->byte*
                                             char    ->char   ->char*
                                             short   ->short  ->short*
                                             int     ->int    ->int*
                                             long    ->long   ->long*
                                             float   ->float  ->float*
                                             double  ->double ->double*}}}}
          pr       {:aliases {:cljc {pr       quantum.core.print            }}
                    :refers  {:cljc {pr     #{! pprint pr-attrs !*}}}}
          str      {:core-exclusions #{re-find}
                    :aliases {:cljc {str      quantum.core.string           }}
                    :refers  {:cljc {str      #{re-find}}}}    
          strf     {:aliases {:cljc {strf     quantum.core.string.format    }}}    
          sys      {:aliases {:cljc {sys      quantum.core.system           }}}    
          thread   {:aliases {:cljc {thread   quantum.core.thread           }}
                    :refers  {:clj  {thread #{thread+ async async-loop}}}}
          ; DATA
          arr     {:aliases {:cljc {arr    quantum.core.data.array }} :refers {:cljc {arr    #{aset!               }}
                                                                               :clj  {arr    #{byte-array+         }}}}
          bin     {:core-exclusions #{bit-or bit-and bit-xor bit-not
                                      bit-shift-left bit-shift-right
                                      unsigned-bit-shift-right
                                      true? false? #_nil?}
                   :aliases {:cljc    {bin    quantum.core.data.binary}}
                   :refers  {:cljc {bin #{>>> >> << bit-or bit-and bit-xor bit-not
                                          bit-shift-left bit-shift-right
                                          unsigned-bit-shift-right}}
                             :clj  {bin #{true? false? #_nil?}}}}
          bytes   {:aliases {:cljc {bytes  quantum.core.data.bytes }}}
          csv     {:aliases {:cljc {csv    quantum.core.data.complex.csv   }}}
          ftree   {:aliases {:cljc {ftree  quantum.core.data.ftree }}
                   :refers  {:clj  {ftree  #{dlist}}}
                   :imports (clojure.data.finger_tree.CountedDoubleList)}
          hex     {:aliases {:cljc {hex    quantum.core.data.hex   }}}
          json    {:aliases {:cljc {json   quantum.core.data.complex.json  }}} 
          map
            {:core-exclusions #{merge sorted-map sorted-map-by}
             :aliases {:cljc {map quantum.core.data.map}}
             :refers  {:cljc {map #{map-entry ordered-map}}
                       :clj  {map #{int-map imerge}}}}
          queue 
            {:aliases {:cljc {q   quantum.core.data.queue}}
             :refers  {:cljc {q   #{queue}}}}
          set
            {:aliases {:cljc {set quantum.core.data.set}}
             :refers  {:cljc {set #{sorted-set+}}
                       :clj  {set #{ordered-set int-set dense-int-set}}}}
          vec
            {:aliases {:cljc {vec quantum.core.data.vector}}
             :refers  {:cljc {vec #{catvec subvec+ vector+? vector+}}}}
          xml         {:aliases {:cljc {xml         quantum.core.data.complex.xml   }}}
          ; TIME
          time        {:aliases {:cljc {time        quantum.core.time.core  }}}
          time-coerce {:aliases {:cljc {time-coerce quantum.core.time.coerce}}}
          time-format {:aliases {:cljc {time-form   quantum.core.time.format}}}
          time-local  {:aliases {:cljc {time-loc    quantum.core.time.local }}}
          ; UTIL
          bench       {:aliases {:cljc {bench       quantum.core.util.bench }} :refers {:clj {bench #{bench}}}}
          debug       {:aliases {:cljc {debug       quantum.core.util.debug }} :refers {:clj {debug #{? trace}}}}
          sh          {:aliases {:cljc {sh          quantum.core.util.sh    }}}
          fn
            {:aliases         {:cljc {fn    quantum.core.function}}
             :refers          {:cljc {fn    #{compr *fn f*n unary zeroid monoid firsta call juxtm juxt-kv
                                              doto->> with->> withf withf->> with-pr->> with-msg->> withfs
                                              with-do rfn defcurried fn->> fn-> <- fn-nil}}
                               :clj  {fn    #{MWA jfn mfn}}}
             :import (quantum.core.function MultiRet)}
          logic
            {:aliases         {:cljc {logic      quantum.core.logic}
                               :cljs {logic-cljs quantum.core.cljs.logic}}
             :refers          {:cljc {logic      #{splice-or coll-or coll-and nnil? nempty?
                                                   eq? fn= fn-eq? any?
                                                   ifn if*n ifp
                                                   whenf whenc whenp whenf*n whencf*n
                                                   condf condfc condf*n condpc}}
                               :clj  {logic      #{fn-and fn-or fn-not}}
                               :cljs {logic-cljs #{fn-and fn-or fn-not}}}}
          loops
            {:core-exclusions #{for doseq reduce}
             :aliases         {:cljc {loops      quantum.core.loops}
                               :cljs {loops-cljs quantum.core.cljs.loops}}
             :refers          {:cljc {loops      #{reduce- reducei-}}
                               :clj  {loops      #{reduce reducei for fori doseq doseqi ifor}}
                               :cljs {loops-cljs #{reduce reducei for fori doseq doseqi ifor}}}}
          macros
            {:requires        {:cljc #{quantum.core.log}} ; To get logging for macros
             :aliases         {:cljc {macros      quantum.core.macros     }
                               :cljs {macros-cljs quantum.core.cljs.macros}}
             :refers          {:cljc {macros      #{quote+ fn+ defn+ defmethod+ defmethods+ let-alias assert-args compile-if emit-comprehension do-mod}}
                               :clj  {macros      #{defnt defnt'}}
                               :cljs {macros-cljs #{defnt}}}}
          rand
            {:aliases         {:cljc {rand quantum.core.nondeterministic}}}
          red 
            {:aliases         {:cljc {red quantum.core.reducers}}
             :refers          {:cljc {red #{map+ reduce+ filter+ remove+ take+ take-while+ drop+ fold+ range+ for+}}
                               :clj  {red #{taker+ dropr+ count*}}}}
          type
            {;:exclusions
             ; [seq? vector? set? map? string? associative? keyword? nil? list? coll? char?]
             :aliases         {:cljc {type quantum.core.type}}
             :refers          {:cljc {type #{instance+? array-list? boolean? double? map-entry? listy?
                                             sorted-map? queue? lseq? pattern? regex? editable? transient?
                                             should-transientize?}}
                               :clj  {type #{construct bigint? file? byte-array? name-from-class}}
                               :cljs {type #{class}}}}
          tcore
            {:aliases         {:cljc {tcore quantum.core.type.core}}}
          classes {:aliases {:cljc {classes quantum.core.classes}}}
         ; EXT
          http {:aliases {:clj  {http        quantum.http.core       }
                          :cljs {http        quantum.cljs.http.core  }}
                :imports (quantum.http.core.HTTPLogEntry)}
          ; Unit conversion
          uconv {:aliases {:cljc {uconv        quantum.measure.convert}}
                 :refers  {:cljc {uconv       #{convert}}}}
          web   {:aliases {:clj  {web         quantum.web.core}}
                 :imports ((org.openqa.selenium WebDriver WebElement TakesScreenshot
                             StaleElementReferenceException NoSuchElementException
                             OutputType Dimension)
                           (org.openqa.selenium Keys By Capabilities
                             By$ByClassName By$ByCssSelector By$ById By$ByLinkText
                             By$ByName By$ByPartialLinkText By$ByTagName By$ByXPath)
                           (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService PhantomJSDriverService$Builder )
                           (org.openqa.selenium.remote RemoteWebDriver RemoteWebElement ))}
          web-support
                {:aliases {:clj {compojure compojure.core
                                 handler   compojure.handler   
                                 jetty ring.adapter.jetty
                                 ;friend     cemerick.friend            
                                 ;workflows  cemerick.friend.workflows  
                                 ;creds      cemerick.friend.credentials
                                 ;oauth2     friend-oauth2.workflow     
                                 ;oauth-util friend-oauth2.util         
                                 oauth      quantum.auth.oauth}}
                 :refers {compojure #{defroutes GET ANY POST}}}
          auth  {:aliases {:clj {auth quantum.auth.core}}}
          url   {:aliases {:clj {url  quantum.http.url }}}
          ui    {:aliases {:cljc {ui     quantum.ui.core  
                                  rx     freactive.core}
                           :clj  {fx     fx-clj.core
                                  fx.css fx-clj.css }}
                 :refers  {:clj {ui #{fx}
                                 rx #{rx}}}
                 :imports (quantum.ui.core.FXObservableAtom
                           (javafx.stage Modality Stage)
                           (javafx.animation      Animation KeyValue KeyFrame Timeline AnimationTimer Interpolator
                                                 FadeTransition TranslateTransition RotateTransition ScaleTransition
                                                 PathTransition PathTransition$OrientationType)
                          (javafx.collections    ObservableList FXCollections ListChangeListener
                                                 ListChangeListener$Change)
                          (javafx.event          ActionEvent EventHandler EventType)
                          (javafx.geometry       Insets Pos HPos)
                          (javafx.scene          Group Scene Node Parent)
                          (javafx.scene.effect   BoxBlur BlendMode Lighting Bloom)
                          (javafx.scene.image    Image)
                          (javafx.scene.input    DragEvent KeyEvent KeyCode MouseEvent)
                          (javafx.scene.media    MediaPlayer Media MediaView)
                          (javafx.scene.paint    Stop CycleMethod LinearGradient RadialGradient Color)
                          (javafx.scene.text     Font FontPosture FontWeight Text TextBoundsType TextAlignment)
                          (javafx.scene.layout   Region GridPane StackPane Pane Priority HBox VBox ColumnConstraints
                                                 Background BackgroundFill
                                                 Border BorderStroke BorderStrokeStyle BorderWidths)
                          (javafx.scene.shape    Circle Rectangle StrokeType Path PathElement MoveTo CubicCurveTo)
                          (javafx.util           Duration Callback)
                          (javafx.beans          InvalidationListener)
                          (javafx.beans.property SimpleDoubleProperty SimpleStringProperty)
                          (javafx.beans.value    ChangeListener ObservableValue)
                          (javafx.scene.control
                            ComboBox ContentDisplay Labeled TableColumn TableRow
                            TableCell ListCell ListView Label Tooltip TextArea TextField ContentDisplay
                            TableView
                            TableView$TableViewSelectionModel TableColumn$CellDataFeatures TableColumn$CellEditEvent)
                          (javafx.scene.control.cell PropertyValueFactory TextFieldTableCell))}}
       lib-exclusions (set/union '#{red loops ccore} '#{http web auth url ui}) ; Because contained in coll
       lib-keys (->> ns-defs-0 keys (remove (partial contains? lib-exclusions)))
       lib
         {:core-exclusions (->> lib-keys (get-ns-syms ns-defs-0 :core-exclusions) (into #{}))
          :requires        (->> lib-keys (get-ns-syms ns-defs-0 :requires       ))
          :refers          (->> lib-keys (get-ns-syms ns-defs-0 :refers         ))
          :aliases         (->> lib-keys (get-ns-syms ns-defs-0 :aliases        ))
          :macro-aliases   (->> lib-keys (get-ns-syms ns-defs-0 :macro-aliases  ))
          :imports         (->> lib-keys (get-ns-syms ns-defs-0 :imports        ))}]
    (-> ns-defs-0 (assoc :lib lib))))

(def all-macros-in-lib
 '{quantum.core.cljs.error  #{try+}
   quantum.core.cljs.macros #{defnt}
   quantum.core.cljs.loops  #{reduce reducei for doseq doseqi}
   quantum.core.cljs.logic  #{fn-or fn-and fn-not}
   quantum.core.collections #{reduce reduce- reducei reducei- doseq doseqi for fori repeatedly kmap map->record}
   quantum.core.error       #{try+ try-times throw+
                              with-throw with-throws
                              throw-unless throw-when
                              with-catch with-assert assert
                              assertf-> assertf->>},
   quantum.core.function    #{defcurried with-do f*n <- fn-> rfn fn->> mfn MWA doto->>}
   quantum.core.cljs.deps.function #{f*n}
   quantum.core.log         #{pr ppr}
   quantum.core.logic       #{whenf whenc  whenf*n whencf*n whenp
                              ifn          if*n             ifp
                              condf condfc condf*n   condpc
                              if-let
                              eq?  fn-eq? fn=
                              neq? fn-neq?
                              fn-or   fn-and fn-not
                              coll-or coll-and}
   quantum.core.loops       #{unchecked-inc-long until reduce- reduce reducei- reducei
                              dos lfor doseq- doseq doseqi- doseqi for}
   quantum.core.macros      #{quote+ fn+ defn+ defmethod+ defmethods+ defnt compile-if assert-args let-alias}
   quantum.core.ns          #{def- defalias ns-exclude source defmalias},
   quantum.core.numeric     #{+= -=
                              ++ --}
   quantum.core.print       #{pr-attrs with-print-str*}
   quantum.core.reducers    #{for+ doseq+}
   quantum.core.test        #{qtest}
   quantum.core.thread      #{thread+ async async-loop}
   quantum.core.type        #{should-transientize?}
   quantum.measure.convert  #{convert}
   cljs.core.async.macros   #{go go-loop}})

#?(:clj
(defn macro-sym? [ns-sym debug? determine-macros? sym]
  (when debug? (js-println "MACRO-SYM?" sym (get-in all-macros-in-lib [ns-sym sym])))
  (if determine-macros?
      (let [macro-var
              (or (try (ns-resolve (the-ns ns-sym) sym)
                    (catch Exception _ nil)) ; No namespace found
                  (when debug?
                    (js-println (str "Tried to determine whether symbol " sym
                                  " was a macro but could not find in ns " ns-sym "."
                                  " Assuming not a macro."))))
            macro-based-on-meta?
              (when macro-var (-> macro-var meta :macro))]
        (when debug? (println sym "is a macro?" macro-based-on-meta?))
        macro-based-on-meta?)
     (get-in all-macros-in-lib [ns-sym sym]))))

#?(:clj
(defn require-quantum* [lang ns-sym ns-syms & [debug? determine-macros?]]
  (let [debug? (or (-> *ns* str (.indexOf "quanta") (not= -1)) debug?)
        _ (when debug? (js-println "Parsing ns-syms in" ns-sym ns-syms))
        allowed? (fn [[k v]] (contains? #{:cljc lang} k))
        core-exclusions
          (->> ns-syms (get-ns-syms ns-defs :core-exclusions)
               (into [])
               (conj [:exclude]))
        core-require-statement
          (->> core-exclusions
               (#(if (empty? %)
                     []
                     (->> % (into []) (conj [:exclude])))))
        get-initial
          (fn [k]
            (->> ns-syms
                 (get-ns-syms ns-defs k)
                 (filter allowed?)
                 vals))
        simple-require-statements
          (->> (get-initial :requires)
               (apply set-merge)
               (map vector))
        _ (when debug? (js-println "simple-require-statements" simple-require-statements))
        aliases       (->> (get-initial :aliases      ) (apply merge-with merge))
        alias-require-statements       (->> aliases       (map (fn [[k v]] [v :as k])))
        _ (when debug? (js-println "alias-require-statements" alias-require-statements))
        macro-aliases (->> (get-initial :macro-aliases) (apply merge-with merge))
        macro-alias-require-statements (->> macro-aliases (map (fn [[k v]] [v :as k])))
        _ (when debug? (js-println "macro-alias-require-statements" macro-alias-require-statements))
        complex-require-statements
          (->> (get-initial :refers)
               (#(do (when debug? (js-println "INITIAL REFERS" %)) %))
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
                     (condp = lang
                       :clj  {:refer (conj refer-template (into [] refers))}
                       :cljs
                         (let [macros (->> refers
                                           (filter #(macro-sym? ns-sym-n debug? determine-macros? %))
                                           doall
                                           (into #{}))
                               non-macros (set/difference refers macros)]
                           {:refer        (when-not (empty? non-macros)
                                            (conj refer-template (into [] non-macros)))
                            :refer-macros (when (or ((complement empty?) macros)
                                                    (contains? all-macros-in-lib ns-sym-n))
                                            (conj refer-template (into [] macros)))})))))
               (apply merge-with
                 (fn [a b] (->> (if (vector? a) (list a) a) (cons b))))
               (map (fn [[k v]] [k (if (vector? v) (list v) v)]))
               (apply merge {}))
        _ (when debug? (js-println "complex-require-statements" complex-require-statements))
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
               (filter #(->> % first (contains? all-macros-in-lib)))
               (map (fn [[sym-n _ alias-n]] [sym-n :as alias-n]))
               (into macro-require-statements-0))
        final-statements
          {:require        require-statements-f
           :require-macros macro-require-statements-f
           :refer-clojure  core-exclusions}
        _ (when debug?
            (println "/*")
            (clojure.pprint/pprint final-statements)
            (println "*/"))] ; do it no matter what
  
  ; Injections
  (->> (for [ns-sym-n ns-syms]
         (get-in ns-defs [ns-sym-n :injection lang]))
       (remove nil?)
       (map (fn [f] ((eval f))))
       doall)

  (condp = lang
    :clj
      (do ; Can't use a macro in the same namespace... hmm  
        (apply (mfn quantum.core.ns/ns-exclude)
            (second core-exclusions))
          (apply require          require-statements-f)
          (apply (mfn import)     (get-ns-syms ns-defs :imports ns-syms)))
    :cljs final-statements))))

#?(:clj
(defn find-macros
  "Returns a map of the namespaces with their macros."
  {:usage '(find-macros '[:lib])}
  [ns-syms]
  (->> (require-quantum* :cljs nil ns-syms true true)
       (map
         (fn [[s & r]]
           [s (->> r (apply hash-map) :refer-macros (into #{}))]))
       (remove (fn [[s macros]] (empty? macros)))
       (apply merge {}))))

#?(:clj
(defn require-quantum-cljs [ns-sym ns-syms]
  ; Double backslash because it's printing to each .js file too 
  (js-println "TRANSFORMING CLJS REQUIRE-QUANTUM STATEMENT:" ns-syms)
  (when (empty? ns-syms)
    (throw (Exception. "|require-quantum| body cannot be empty.")))
  (require-quantum* :cljs ns-sym ns-syms @quantum.core.ns/ns-debug?
    )))

; EXTEND CLOJURE.CORE'S |NS|

#?(:clj
(do
  (in-ns 'clojure.core)
  (def require-quantum #(quantum.core.ns/require-quantum* :clj nil % @quantum.core.ns/ns-debug?))
  (in-ns 'quantum.core.ns)))

#?(:clj
(defmacro with-ns
  "Perform an operation in another ns."
  [ns- & body]
  (let [ns-0 (ns-name *ns*)]
    `(do (in-ns '~ns-) ~@body (in-ns '~ns-0)))))

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

