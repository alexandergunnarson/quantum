(ns quantum.core.ns
        (:refer-clojure :exclude [Vec])
  (:require
           [clojure.repl :as repl]
           [clojure.core.rrb-vector]
                                       
           [flatland.ordered.map])
       
  (:import (clojure.lang Keyword Var Namespace))
       
  (:gen-class))

(defmacro reset-var!
  "Like |reset!| but for vars."
  {:attribution "Alex Gunnarson"}
  [var-0 val-f]
  `(alter-var-root (var ~var-0) (constantly ~val-f)))

(defmacro swap-var!
  "Like |swap!| but for vars."
  {:attribution "Alex Gunnarson"}
  ([var-0 f]
  `(alter-var-root (var ~var-0) ~f))
  ([var-0 f & args]
  `(alter-var-root (var ~var-0)
     (fn [var-n#]
       (~f var-n# ~@args)))))

       ; for now
 (defmacro local-context
   {:attribution "The Joy of Clojure, 2nd ed."
    :todo ["'IOException: Pushback buffer overflow' on certain
             very large data structures"
           "Use reducers"]}
   []
   (let [symbols (keys &env)]
     (zipmap
       (map (fn [sym] `(quote ~sym))
               symbols)
       symbols)))
 
       ; for now
 (defn contextual-eval
   "Restricts the use of specific bindings to |eval|."
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
         ~expr))))
 
       ; for now
 (defmacro let-eval [expr]
   `(contextual-eval local-context ~expr))
 
       ; /resolve/ not in cljs
 (defn resolve-key
   "Resolves the provided keyword as a symbol for a var
    in the current namespace.
    USAGE: (resolve-key :my-var)"
   ^{:attribution "Alex Gunnarson"}
   [^Keyword k]
   (-> k name symbol resolve))
 
       ; for now
 (defn eval-key
   "Evaluates the provided keyword as a symbol for a var
    in the current namespace.
    USAGE: (eval-key :my-var)"
   ^{:attribution "Alex Gunnarson"}
   [^Keyword k]
   (-> k name symbol eval))
 
      
 (defn var-name
   "Get the namespace-qualified name of a var."
   ^{:attribution "flatland.useful.ns"}
   [v]
   (apply symbol
     (map str
       ((juxt (comp ns-name :ns)
              :name)
              (meta v)))))
 
      
 (defn alias-var
   "Create a var with the supplied name in the current namespace, having the same
   metadata and root-binding as the supplied var."
   ^{:attribution "flatland.useful.ns"}
   [sym var-0]
   (apply intern *ns*
     (with-meta sym
       (merge
         {:dont-test
           (str "Alias of " (var-name var-0))}
         (meta var-0)
         (meta sym)))
     (when (.hasRoot var-0) [@var-0])))
 
 ; Trying to put the reader macros inside a macro didn't work...
      
 (defmacro defalias
   "Defines an alias for a var: a new var with the same root binding (if
   any) and similar metadata. The metadata of the alias is its initial
   metadata (as provided by def) merged into the metadata of the original."
   {:attribution "flatland.useful.ns"
    :contributors ["Alex Gunnarson"]}
   [dst src]
   `(alias-var (quote ~dst) (var ~src)))
 
      
 (defn alias-ns
   "Create vars in the current namespace to alias each of the public vars in
   the supplied namespace.
   Takes a symbol."
   ^{:attribution "flatland.useful.ns"}
   [ns-name]
   (require ns-name)
   (doseq [[name var] (ns-publics (the-ns ns-name))]
     (alias-var name var)))

     
(defn defs
  "Defines a provided list of symbol-value pairs as vars in the
   current namespace.
   USAGE: (defs 'a 1 'b 2 'c 3)"
  ^{:attribution "Alex Gunnarson"}
  [& {:as vars}]
  (doseq [var-n vars]
    (intern *ns* (-> var-n key name symbol) (val var-n))))

     
(defn defs-private
  "Like /defs/: defines a provided list of symbol-value pairs
   as private vars in the current namespace.
   USAGE: (defs-private 'a 1 'b 2 'c 3)"
  ^{:attribution "Alex Gunnarson"}
  [& {:as vars}]
  (doseq [var-n vars]
    (intern *ns* (-> var-n key name symbol) (val var-n))
    (alter-meta! (eval `(var ~(-> var-n key))) assoc :private true)))

     
(defn clear-vars
  ^{:attribution "Alex Gunnarson"}
  [& vars]
  (doseq [var-n vars]
    (alter-var-root (-> var-n name symbol resolve) (constantly nil)))
  (println "Vars cleared."))

     
(defn declare-ns
  ^{:attribution "Alex Gunnarson"}
  [curr-ns]
  (defs-private 'this-ns curr-ns))

      ; cljs doesn't have reflection
(defn defaults
  ^{:attribution "Alex Gunnarson"}
  []
  (set! *warn-on-reflection* true))

; Just to be able to synthesize class-name aliases...

;(def       Nil       nil)
;#+clj (def Fn        clojure.lang.Fn)
; (def       Key       #+clj clojure.lang.Keyword              #+cljs cljs.core.Keyword             )
; (def       Num       #+clj java.lang.Number                  #+cljs js/Number                     )
; (def       ExactNum  #+clj clojure.lang.Ratio                #+cljs js/Number                     )
; (def       Int       #+clj java.lang.Integer                 #+cljs js/Number                     )
; (def       Decimal   #+clj java.lang.Double                  #+cljs js/Number                     )
; (def       Set       #+clj clojure.lang.APersistentSet       #+cljs cljs.core.PersistentHashSet   )
; (def       Bool      #+clj Boolean                           #+cljs js/Boolean                    )
; (def       ArrList   #+clj java.util.ArrayList               #+cljs cljs.core.ArrayList           )
; (def       TreeMap   #+clj clojure.lang.PersistentTreeMap    #+cljs cljs.core.PersistentTreeMap   )
; (def       LSeq      #+clj clojure.lang.LazySeq              #+cljs cljs.core.LazySeq             )
; (def       Vec       #+clj clojure.lang.APersistentVector    #+cljs cljs.core.PersistentVector    ) ; Conflicts with clojure.core/->Vec
; (def       MEntry    #+clj clojure.lang.MapEntry             #+cljs Vec                           )
; (def       Regex     #+clj java.util.regex.Pattern           #+cljs js/RegExp                     )
; (def       Editable  #+clj clojure.lang.IEditableCollection  #+cljs cljs.core.IEditableCollection )
; (def       Transient #+clj clojure.lang.ITransientCollection #+cljs cljs.core.ITransientCollection)
; (def       Queue     #+clj clojure.lang.PersistentQueue      #+cljs cljs.core.PersistentQueue     )
; (def       Map       #+clj java.util.Map                     #+cljs cljs.core.IMap                )
; (def       Seq       #+clj clojure.lang.ISeq                 #+cljs cljs.core.ISeq)

(defrecord Nil       [])
(defrecord Key       [])
(defrecord Num       [])
(defrecord ExactNum  [])
(defrecord Int       [])
(defrecord Decimal   [])
(defrecord Set       [])
(defrecord Bool      [])
(defrecord ArrList   [])
(defrecord TreeMap   [])
(defrecord LSeq      [])
                         
(defrecord Regex     [])
(defrecord Editable  [])
(defrecord Transient [])
(defrecord Queue     [])
(defrecord Map       [])
(defrecord Seq       [])
(defrecord Record    [])
       (defrecord Fn                       [])
                                              
                                                         
                                                         

     
(defn ns-exclude [^Namespace curr-ns & syms]
  (binding [*ns* curr-ns]
    (doseq [sym syms]
      (ns-unmap (ns-name curr-ns) sym))))

     
(defn require-clj [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (do
            (set! *warn-on-reflection* true)
      (require
              '[clojure.core.rrb-vector]
              '[flatland.ordered.map])
           
      (import
        '(quantum.core.ns
           Map Set Queue 
           LSeq
           Key
           Fn
           Num Int Decimal ExactNum
           Bool
           Nil))
            
              
                                
                          
                  
               
              
                                    
                
                  
           
      (import
        '(clojure.lang
            Namespace
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
        'java.util.regex.Pattern
        '(java.util ArrayList)
        'org.joda.time.DateTime
        'clojure.core.Vec
        '(java.math BigDecimal)
        'clojure.core.rrb_vector.rrbt.Vector
        'flatland.ordered.map.OrderedMap))))

      ; for now
(defn require-lib [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require
      '[quantum.core.collections  :as coll        :refer :all                   ]
      '[quantum.core.cryptography :as crypto]
      '[quantum.core.function     :as fn          :refer :all                   ]
      '[quantum.core.io           :as io          :refer [path]                 ]
      '[quantum.core.java         :as java                                      ]
      '[quantum.core.log          :as log                                       ]
      '[quantum.core.logic        :as logic       :refer :all                   ]
      '[quantum.core.macros       :as macros      :refer :all                   ]
      '[quantum.core.ns           :as ns          :refer [defalias source defs] ]
      '[quantum.core.numeric      :as num                                       ]
      '[quantum.core.print        :as pr          :refer [! pprint pr-attrs !* ]]
      '[quantum.core.string       :as str         :refer [substring?]           ]
      '[quantum.core.system       :as sys                                       ]
      '[quantum.core.thread       :as thread                                    ]  
      '[quantum.core.type                         :refer :all                   ]
      '[quantum.core.data.array   :as arr         :refer :all                   ]
      '[quantum.core.data.binary  :as bin         :refer :all                   ]
      '[quantum.core.data.ftree   :as ftree                                     ]
      '[quantum.core.data.map     :as map         :refer :all                   :exclude [merge+ split-at]]
      '[quantum.core.data.queue   :as q                                         ]
      '[quantum.core.data.set     :as set         :refer [sorted-set+]          ]
      '[quantum.core.data.vector  :as vec         :refer [conjl catvec]         ]
      '[quantum.core.data.xml     :as xml                                       ]
      '[quantum.core.time.core    :as time                                      ]
      '[quantum.core.time.coerce  :as time-coerce                               ]
      '[quantum.core.time.format  :as time-form                                 ]
      '[quantum.core.time.local   :as time-loc                                  ]
      '[quantum.core.util.bench   :as bench       :refer [bench]                ]
      '[quantum.core.util.debug   :as debug       :refer [? break]              ]
      '[quantum.core.util.sh      :as sh                                        ]
      '[quantum.core.data.queue   :as q           :refer [queue]                ]
      '[quantum.core.thread                       :refer :all                   ]
      '[quantum.core.error        :as err         :refer :all                   ]
      '[clojure.core.async        :as async       :refer [go <! >! alts!]       ])))

     
(defn require-java-fx [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require '[quantum.core.ui.init])
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
          TableView TableView$TableViewSelectionModel))))

     
(defn require-fx-core [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require-java-fx curr-ns)
    (require
      '[quantum.core.ui.jfx :as fx :refer
         [fx do-fx
          set-listener! swap-content!
          fx-node? fx-obj?]])))

     
(defn require-fx [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require-fx-core curr-ns)
    (require
      '[quantum.core.ui.custom-objs :as objs :refer
         [jnew jdef* jdef jdef! jset! jget jget-prop jgets-map 
          jconj! jdissoc! jupdate!
          arrange-on! center-on! jconj-all!
          setx! sety!
          getx gety get-size get-pos
          place-at! nudge!]])))

      ; for now
(defn require-all
  "Loads/|import|s/|require|s all the namespaces and functions associated with a given
   library key @lib-key into the current namespace @curr-ns."
  {:attribution "Alex Gunnarson"
   :usage "(require-all *ns* :lib :grid :fx)"}
  ([curr-ns ^Keyword lib-key]
    (binding [*ns* curr-ns]
      (case lib-key
        :clj
          (require-clj curr-ns)
        :lib
          (require-lib curr-ns)
        :grid
        (require 
          '[quantum.datagrid.core       :as grid]
          '[quantum.datagrid.excel      :as xl  ])
        :java-fx
          (require-java-fx curr-ns)
        :fx-core
          (require-fx-core curr-ns)
        :fx
          (require-fx curr-ns)
        nil)))
  ([curr-ns lib-key-0 & lib-keys-0]
    ; Because :clj has to be required first out of them
    (let [lib-keys (into #{} (conj lib-keys-0 lib-key-0))]
      (require-all curr-ns :clj)
      (doseq [lib-key (disj lib-keys :clj)]
        (require-all curr-ns lib-key)))))

     
(defn nss
  "Defines, in the provided namespace, conveniently abbreviated symbols
   for other namespaces.
   Esp. for use with /in-ns/, where one can switch more
   quickly between namespaces for testing and coding purposes.
   USAGE: (nss *ns*)
   USAGE: (in-ns *exp) instead of (in-ns 'quantum.core.ui.experimental)"
  ^{:attribution "Alex Gunnarson"}
  [curr-ns]
  (binding [*ns* curr-ns]
    (defs-private
      'curr-ns      curr-ns
      '*exp         'quantum.core.ui.experimental
      '*ui          'clj-qb.ui.core
      '*coll        'quantum.core.collections    
      '*func        'quantum.core.function       
      '*io          'quantum.core.io             
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
      '*time-locala 'quantum.core.time.local     
      '*xl-io       'clj-qb.xl-io         
      '*sr          'clj-qb.sales-receipt 
      '*adj         'clj-qb.inv-adj       
      '*req-io      'clj-qb.req-io        
      '*parse       'clj-qb.resp-parse    
      '*db          'clj-qb.qb-database   
      '*req         'clj-qb.req-gen       
      '*qb          'clj-qb.dispatch          
      '*grid        'quantum.datagrid.core 
      '*xl          'quantum.datagrid.excel)))

; find-doc, doc, and source are incl. in /user/ ns but not in any others
      (defalias source   repl/source)   
      (defalias find-doc repl/find-doc)
      (defalias doc      repl/doc)


;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/ns.cljx
