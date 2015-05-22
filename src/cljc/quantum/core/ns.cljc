(ns
  ^{:doc "Useful namespace and var-related functions.
    
          Also provides convenience functions for importing |quantum| namespaces.
          These convenience functions are untested with ClojureScript."
    :attribution "Alex Gunnarson"}
  quantum.core.ns
  (:require
    [clojure.set :as set]
    #?(:clj  [clojure.repl :as repl])
             [clojure.core.rrb-vector]
    #?(:cljs [cljs.core :refer [Keyword]])
    #?(:clj  [flatland.ordered.map]))
  #?(:clj (:import (clojure.lang Keyword Var Namespace)))
  #?(:clj (:gen-class)))
; #?(:clj  [flatland.ordered.map]) - need the dependency in order to compile,
; even if it's not used by the target platform... 

#?@(:clj [(set! *unchecked-math*     :warn-on-boxed)
          (set! *warn-on-reflection* true)])

(def ns-debug? (atom false))

; ============ VAR MANIPULATION, ETC. ============
; CLJS compatible
#?(:clj
  (defmacro reset-var!
  "Like |reset!| but for vars."
  {:attribution "Alex Gunnarson"}
  [var-0 val-f]
  ;(.bindRoot #'clojure.core/ns ns+)
  ;(alter-meta! #'clojure.core/ns merge (meta #'ns+))

  `(alter-var-root (var ~var-0) (constantly ~val-f))))
; CLJS compatible
#?(:clj (defmacro swap-var!
  "Like |swap!| but for vars."
  {:attribution "Alex Gunnarson"}
  ([var-0 f]
  `(alter-var-root (var ~var-0) ~f))
  ([var-0 f & args]
  `(alter-var-root (var ~var-0)
     (fn [var-n#]
       (~f var-n# ~@args))))))


#?(:clj ; for now
  (defmacro context
    "Originally 'local-context'."
    {:attribution "The Joy of Clojure, 2nd ed."
     :todo ["'IOException: Pushback buffer overflow' on certain
              very large data structures"
            "Use reducers"]}
    []
    (let [symbols (keys &env)]
      (zipmap
        (map (fn [sym] `(quote ~sym))
                symbols)
        symbols))))

#?(:clj ; for now
  (defn c-eval
    "Contextual eval. Restricts the use of specific bindings to |eval|.

     Suffers from not being able to work on non-simples (e.g. atoms cannot be c-evaled)."
    {:attribution "The Joy of Clojure, 2nd ed."
     :todo ["'IOException: Pushback buffer overflow' on certain
              very large data structures"
            "Use reducers"]}
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
    `(contextual-eval local-context ~expr)))

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

; #?(:clj
; (defmacro defalias
;   "Defines an alias for a var: a new var with the same root binding (if
;   any) and similar metadata. The metadata of the alias is its initial
;   metadata (as provided by def) merged into the metadata of the original."
;   {:attribution "flatland.useful.ns"
;    :contributors ["Alex Gunnarson"]}
;   [a b]
;   ;`(alias-var (quote ~dst) (var ~src))
;   ;`(~clojure.core/intern ~clojure.core/*ns* '~dst (var ~src)) ; using a var says "undeclared var, eval"
;   `(def ~a (var ~b))
;   ))

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
(defmacro ns-find
  "Finds a var in the available namespaces."
  {:usage '(ns-find abc)
   :todo ["Make it better and filter out unnecessary results"]}
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
  [macro]
  `(fn [& args#]
     (clojure.core/eval (cons '~macro args#)))))

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
(defn require-java-fx [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require '[quantum.ui.init])
    (import
       '(javafx.animation      Animation KeyValue KeyFrame Timeline AnimationTimer Interpolator
                               FadeTransition TranslateTransition RotateTransition ScaleTransition
                               PathTransition PathTransition$OrientationType)
       '(javafx.collections    ObservableList FXCollections)
       '(javafx.event          ActionEvent EventHandler EventType)
       '(javafx.geometry       Insets Pos)
       '(javafx.scene          Group Scene Node)
       '(javafx.scene.effect   BoxBlur BlendMode Lighting Bloom)
       '(javafx.scene.image    Image)
       '(javafx.scene.input    DragEvent KeyEvent KeyCode MouseEvent)
       '(javafx.scene.paint    Stop CycleMethod LinearGradient RadialGradient Color)
       '(javafx.scene.text     Font FontPosture FontWeight Text TextBoundsType TextAlignment)
       '(javafx.scene.layout   GridPane StackPane Pane Priority HBox VBox)
       '(javafx.scene.shape    Circle Rectangle StrokeType Path PathElement MoveTo CubicCurveTo)
       '(java.util             ArrayList List)
       '(javafx.util           Duration Callback)
       '(javafx.beans.property SimpleDoubleProperty)
       '(javafx.beans.value    ChangeListener ObservableValue)
       '(javafx.scene.control
          ComboBox ContentDisplay Labeled TableColumn TableRow
          TableCell ListCell TextArea TextField ContentDisplay
          TableView TableView$TableViewSelectionModel)))))

#?(:clj
(defn require-fx-core [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require-java-fx curr-ns)
    (require
      '[quantum.ui.jfx :as fx :refer
         [fx do-fx
          set-listener! swap-content!
          fx-node? fx-obj?]]))))

#?(:clj
(defn require-fx
  {:todo ["This is very outdated. Consider deprecating"]}
  [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require-fx-core curr-ns)
    (require
      '[quantum.ui.custom-objs :as objs :refer
         [jnew jdef* jdef jdef! jset! jget jget-prop jgets-map 
          jconj! jdissoc! jupdate!
          arrange-on! center-on! jconj-all!
          setx! sety!
          getx gety get-size get-pos
          place-at! nudge!]]))))

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
#?@(:clj
  [(defalias source   repl/source)   
   (defalias find-doc repl/find-doc)
   (defalias doc      repl/doc)])

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
          async 
            {:aliases       {:clj  {async  clojure.core.async}
                             :cljs {async  cljs.core.async}}
             :macro-aliases {:cljs {asyncm cljs.core.async.macros}}
             :refers        {:cljc {async  #{<! >! alts! close! chan}}
                             :clj  {async  #{go go-loop >!! <!! thread}}
                             :cljs {asyncm #{go go-loop}}}}
          coll
           {:aliases
               {:cljc {coll quantum.core.collections}}
             :core-exclusions
              #{contains? for doseq subseq reduce repeat repeatedly
                range merge count vec into first second rest
                last butlast get pop peek empty take take-while} 
             :refers
               {:cljc
                 {coll #{for doseq reduce reduce- reducei reducei-
                         count lasti
                         subseq getr gets
                         repeat repeatedly
                         range range+
                         merge merge-keep-left
                         vec
                         get first second rest
                         last butlast
                         pop popl popr peek
                         empty
                         conjl conjr
                         index-of last-index-of
                         take ltake take-while ltake-while
                         take-until take-until-inc
                         take-from take-after
                         taker-untili taker-until
                         dropl dropr dropr-until dropr-after
                         into redv redm fold foldm
                         map+
                         ffilter filter+ remove+
                         dissoc-in+ assocs-in+ update-in+
                         split-remove
                         kmap
                         contains? in?}}}}
          ccore  {:aliases {:cljc {ccore  quantum.core.collections.core}}}
          crypto {:aliases {:cljc {crypto quantum.core.cryptography    }}}
          err
            {:aliases   {:cljc {err quantum.core.error}}
             :refers    {:cljc {err #{throw+ try+ with-throw}}
                         :cljs {err #{Err}}}
             :imports   (quantum.core.error.Err)}
          io 
            {:aliases {:cljc {io quantum.core.io.core}}
             :imports 
              ((java.io File
                        FileNotFoundException IOException
                        FileReader PushbackReader
                        DataInputStream DataOutputStream 
                        OutputStream FileOutputStream
                        BufferedOutputStream BufferedInputStream
                        InputStream  FileInputStream
                        PrintWriter))}
          java    {:aliases {:cljc {java quantum.core.java}}}
          num     {:aliases {:cljc {num  quantum.core.numeric}}
                   :refers  {:cljc {num #{nneg? int+ long+ greatest least}}}}
          ns
            {:requires {:cljc #{clojure.core.rrb-vector}
                        :clj  #{flatland.ordered.map   }}
             :aliases  {:cljc {ns   quantum.core.ns  
                               test quantum.core.test}
                        :clj  {core clojure.core}
                        :cljs {core cljs.core}}
             :refers   {:cljc
                         {ns   #{defalias defmalias source def- swap-var! reset-var! ns-exclude
                                 ANil ABool ADouble ANum AExactNum AInt ADecimal AKey AVec ASet
                                 AArrList ATreeMap ALSeq ARegex AEditable ATransient AQueue AMap AError}
                          test #{qtest}}
                        :clj {ns #{alias-ns defs}}
                        :cljs
                         {ns #{Exception IllegalArgumentException
                               Nil Bool Num ExactNum Int Decimal Key Vec Set
                               ArrList TreeMap LSeq Regex Editable Transient Queue Map}}}
            :injection
              {:clj #(do (set! *warn-on-reflection* true)
                         (set! *unchecked-math* :warn-on-boxed)
                         (gen-class)
                         nil)}
            :imports
              ((quantum.core.ns
                  Nil Bool Num ExactNum Int Decimal Key Map Set Queue Fn
                  ArrList TreeMap LSeq Regex Editable Transient)
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
          pr      {:aliases {:cljc {pr     quantum.core.print      }} :refers {:clj {pr     #{! pprint pr-attrs !*}}}}
          str     {:aliases {:cljc {str    quantum.core.string     }}}    
          sys     {:aliases {:cljc {sys    quantum.core.system     }}}    
          thread  {:aliases {:cljc {thread quantum.core.thread     }} :refers {:clj {thread #{thread+             }}}}
          ; DATA
          arr     {:aliases {:cljc {arr    quantum.core.data.array }} :refers {:clj  {arr    #{byte-array+ aset!   }}}}
          bin     {:aliases {:cljc {bin    quantum.core.data.binary}} :refers {:cljc {bin    #{>>>                 }}}}
          bytes   {:aliases {:cljc {bytes  quantum.core.data.bytes }}}
          ftree   {:aliases {:cljc {ftree  quantum.core.data.ftree }}}
          hex     {:aliases {:cljc {hex    quantum.core.data.hex   }}}
          json    {:aliases {:cljc {json   quantum.core.data.json  }}} 
          map
            {:aliases {:cljc {map quantum.core.data.map}}
             :refers  {:cljc {map #{map-entry merge+ sorted-map+}}
                       :clj  {map #{ordered-map}}}}
          queue 
            {:aliases {:cljc {q   quantum.core.data.queue}}
             :refers  {:cljc {q   #{queue}}}}
          set
            {:aliases {:cljc {set quantum.core.data.set}}
             :refers  {:cljc {set #{sorted-set+}}
                       :clj  {set #{ordered-set}}}}
          vec
            {:aliases {:cljc {vec quantum.core.data.vector}}
             :refers  {:cljc {vec #{catvec subvec+ vector+? vector+}}}}
          xml         {:aliases {:cljc {xml         quantum.core.data.xml   }}}
          ; TIME
          time        {:aliases {:cljc {time        quantum.core.time.core  }}}
          time-coerce {:aliases {:cljc {time-coerce quantum.core.time.coerce}}}
          time-format {:aliases {:cljc {time-form   quantum.core.time.format}}}
          time-local  {:aliases {:cljc {time-loc    quantum.core.time.local }}}
          ; UTIL
          bench       {:aliases {:cljc {bench       quantum.core.util.bench }} :refers {:clj {bench #{bench}}}}
          debug       {:aliases {:cljc {debug       quantum.core.util.debug }} :refers {:clj {debug #{? break trace}}}}
          sh          {:aliases {:cljc {sh          quantum.core.util.sh    }}}
          ; EXT
          http        {:aliases {:cljc {http        quantum.http.core       }}}
          
         fn
           {:aliases         {:cljc {fn    quantum.core.function}}
            :refers          {:cljc {fn    #{compr *fn f*n unary zeroid monoid firsta call
                                             with->> withf withf->> with-pr->> with-msg->> withfs
                                             with-do rfn defcurried fn->> fn-> <- }}
                              :clj  {fn    #{jfn}}}}
         logic
           {:aliases         {:cljc {logic quantum.core.logic}}
            :refers          {:cljc {logic #{splice-or coll-or fn-and fn-or fn-not nnil? nempty? eq? fn= fn-eq? any?
                                             ifn if*n whenf whenc whenf*n whencf*n condf condfc condf*n condpc}}}}
         log {:aliases {:cljc {log quantum.core.log}}}
         loops
           {:core-exclusions #{for doseq reduce}
            :aliases         {:cljc {loops quantum.core.loops}}
            :refers          {:cljc {loops #{for doseq doseqi reduce reduce- reducei reducei-}}}}
         macros
           {:requires        {:cljc #{quantum.core.log}} ; To get logging for macros
            :aliases         {:cljc {macros quantum.core.macros}}
            :refers          {:cljc {macros #{defn+ assert-args compile-if emit-comprehension do-mod}}}}
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
                                            defnt should-transientize?}}
                              :clj  {type #{bigint? file? byte-array? name-from-class}}
                              :cljs {type #{class}}}}}
       lib-exclusions (set/union '#{red loops ccore} '#{http}) ; Because contained in coll
       lib-keys (->> ns-defs-0 keys (remove (partial contains? lib-exclusions)))
       lib
         {:core-exclusions (->> lib-keys (get-ns-syms ns-defs-0 :core-exclusions) (into #{}))
          :requires        (->> lib-keys (get-ns-syms ns-defs-0 :requires       ))
          :refers          (->> lib-keys (get-ns-syms ns-defs-0 :refers         ))
          :aliases         (->> lib-keys (get-ns-syms ns-defs-0 :aliases        ))
          :imports         (->> lib-keys (get-ns-syms ns-defs-0 :imports        ))}]
    (-> ns-defs-0 (assoc :lib lib))))

(defn js-println [& args]
  (print "\n/* " )
  (apply println args)
  (println "*/"))

(def all-macros-in-lib
 '{quantum.core.log #{pr ppr}
   quantum.core.logic #{whenf*n condfc ifn whencf*n whenc if*n condf*n condpc whenf condf},
   quantum.core.test #{qtest},
   quantum.core.function #{defcurried with-do <- fn-> rfn fn->>},
   quantum.core.macros #{defn+ compile-if assert-args},
   quantum.core.ns #{def- defalias reset-var! ns-exclude swap-var! source defmalias},
   quantum.core.error #{try+ with-throw throw+},
   quantum.core.collections #{reduce doseq repeatedly kmap reducei for reduce- reducei-},
   quantum.core.type #{defnt should-transientize?}
   cljs.core.async.macros #{go go-loop}})

#?(:clj
(defn require-quantum* [lang ns-syms & [debug? determine-macros?]]
  (let [_ (when debug? (js-println "Parsing ns-syms" ns-syms))
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
        aliases       (->> (get-initial :aliases      )   (apply merge-with merge))
        alias-require-statements       (->> aliases       (map (fn [[k v]] [v :as k])))
        macro-aliases (->> (get-initial :macro-aliases)   (apply merge-with merge))
        macro-alias-require-statements (->> macro-aliases (map (fn [[k v]] [v :as k])))
        complex-require-statements
          (->> (get-initial :refers)
               (#(do (when debug? (js-println "INITIAL REFERS" %)) %))
               (apply merge-with set-merge)
               (map
                 (fn [[alias-n refers]]
                   (let [ns-sym (or (get aliases       alias-n)
                                    (get macro-aliases alias-n)
                                    (throw (Exception. (str "Namespace alias not found: " alias-n))))
                         refer-template [ns-sym :as alias-n :refer]]
                     (condp = lang
                       :clj  {:refer (conj refer-template (into [] refers))}
                       :cljs
                         (let [macro?
                                 (fn [sym]
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
                                      (get-in all-macros-in-lib [ns-sym sym])))
                               macros (->> refers
                                           (filter macro?)
                                           (into #{}))
                               non-macros (set/difference refers macros)]
                           {:refer        (when-not (empty? non-macros)
                                            (conj refer-template (into [] non-macros)))
                            :refer-macros (when-not (empty? macros)
                                            (conj refer-template (into [] macros)))})))))
               (apply merge-with
                 (fn [a b] (->> (if (vector? a) (list a) a) (cons b)))))
        require-statements-f
          (into (:refer complex-require-statements)
            (into alias-require-statements
               simple-require-statements))
        final-statements
          {:require        (->> require-statements-f (remove empty?))
           :require-macros (->> complex-require-statements
                                :refer-macros
                                (remove empty?))
           :refer-clojure  core-exclusions}
        _ (when debug?
            (println "/*")
            (clojure.pprint/pprint final-statements)
            (println "*/"))] ; do it no matter what
  
  ; Injections
  (->> (for [ns-sym ns-syms]
         (get-in ns-defs [ns-sym :injection lang]))
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
  (->> (require-quantum* :cljs ns-syms true true)
       (map
         (fn [[s & r]]
           [s (->> r (apply hash-map) :refer-macros (into #{}))]))
       (remove (fn [[s macros]] (empty? macros)))
       (apply merge {}))))

#?(:clj
(defn require-quantum-cljs [ns-syms]
  ; Double backslash because it's printing to each .js file too 
  (js-println "TRANSFORMING CLJS REQUIRE-QUANTUM STATEMENT:" ns-syms)
  (when (empty? ns-syms)
    (throw (Exception. "|require-quantum| body cannot be empty.")))
  (require-quantum* :cljs ns-syms @quantum.core.ns/ns-debug?)))

; EXTEND CLOJURE.CORE'S |NS|

#?(:clj
(do
  (in-ns 'clojure.core)
  (def require-quantum #(quantum.core.ns/require-quantum* :clj % @quantum.core.ns/ns-debug?))
  (in-ns 'quantum.core.ns)))

