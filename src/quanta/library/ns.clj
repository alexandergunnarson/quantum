(ns quanta.library.ns
  (:require [clojure.repl :as repl]
            [clojure.core.rrb-vector]
            [flatland.ordered.map])
  (:gen-class))

(import '(clojure.lang Keyword Var Namespace))

(defmacro local-context
  {:attribution "The Joy of Clojure, 2nd ed."
   :todo ["'IOException: Pushback buffer overflow' on certain
            very large data structures"]}
  []
  (let [symbols (keys &env)]
    (zipmap
      (map (fn [sym] `(quote ~sym))
              symbols)
      symbols)))

(defn contextual-eval
  "Restricts the use of specific bindings to |eval|."
  {:attribution "The Joy of Clojure, 2nd ed."
   :todo ["'IOException: Pushback buffer overflow' on certain
            very large data structures"]}
  [context expr]
  (eval
   `(let [~@(mapcat
              (fn [[k v]]
                (try [k `'~v]
                  (catch java.io.IOException _ [k "var too large to show"])))
              context)]
      ~expr)))
(defn resolve-key
  "Resolves the provided keyword as a symbol for a var
   in the current namespace.
   USAGE: (resolve-key :my-var)"
  ^{:attribution "Alex Gunnarson"}
  [^Keyword k]
  (-> k name symbol resolve))
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
  [name ^Var var]
  (apply intern *ns*
    (with-meta name
      (merge
        {:dont-test
          (str "Alias of " (var-name var))}
        (meta var)
        (meta name)))
    (when (.hasRoot var) [@var])))
(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  ^{:attribution "flatland.useful.ns"}
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

(defn defaults
  ^{:attribution "Alex Gunnarson"}
  []
  (set! *warn-on-reflection* true))

; Just to be able to synthesize class-name aliases...
; (defrecord Vec     []) ; Conflicts with clojure.core/->Vec
(defrecord Map     [])
; (defrecord List    []);  Conflicts with java.util.List
(defrecord Set     [])
(defrecord Queue   [])
(defrecord LSeq    [])
(defrecord Fn      [])
(defrecord Key     [])
(defrecord Num     [])
(defrecord Int     [])
(defrecord Decimal [])
(defrecord Bool    [])

(defn ns-exclude! [^Namespace curr-ns & syms]
  (binding [*ns* curr-ns]
    (doseq [sym syms]
      (ns-unmap (ns-name curr-ns) sym))))


(defn require-clj [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (do
      (ns-unmap (ns-name curr-ns) 'some?)
      (set! *warn-on-reflection* true)
      (require
        '[clojure.core.rrb-vector]
        '[flatland.ordered.map])
      (import
        '(quanta.library.ns
            Map Set Queue 
            LSeq  
            Key
            Fn
            Num Int Decimal
            Bool))
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
        '(clojure.core Vec)
        'java.util.regex.Pattern
        '(java.util ArrayList)
        'org.joda.time.DateTime
        '(java.math BigDecimal)
        'clojure.core.rrb_vector.rrbt.Vector
        'flatland.ordered.map.OrderedMap))))
(defn require-lib [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (ns-unmap (ns-name curr-ns) 'some?)
    (require
      '[quanta.library.collections :as coll        :refer :all                   ]
      '[quanta.library.function    :as fn          :refer :all                   ]
      '[quanta.library.io          :as io          :refer [path]                 ]
      '[quanta.library.java        :as java                                      ]
      '[quanta.library.log         :as log                                       ]
      '[quanta.library.logic       :as logic       :refer :all                   ]
      '[quanta.library.macros      :as macros      :refer :all                   ]
      '[quanta.library.ns          :as ns          :refer [defalias source defs] ]
      '[quanta.library.numeric     :as num                                       ]
      '[quanta.library.print       :as pr          :refer [! pprint pr-attrs !* ]]
      '[quanta.library.string      :as str         :refer [substring?]           ]
      '[quanta.library.system      :as sys                                       ]
      '[quanta.library.thread      :as thread                                    ]  
      '[quanta.library.type                        :refer :all                   ]
      '[quanta.library.data.array  :as arr         :refer :all                   ]
      '[quanta.library.data.ftree  :as ftree                                     ]
      '[quanta.library.data.map    :as map         :refer :all                   :exclude [merge+ split-at]]
      '[quanta.library.data.queue  :as q                                         ]
      '[quanta.library.data.set    :as set         :refer [sorted-set+]          ]
      '[quanta.library.data.vector :as vec         :refer [conjl catvec]         ]
      '[quanta.library.data.xml    :as xml                                       ]
      '[quanta.library.time.core   :as time                                      ]
      '[quanta.library.time.coerce :as time-coerce                               ]
      '[quanta.library.time.format :as time-form                                 ]
      '[quanta.library.time.local  :as time-loc                                  ]
      '[quanta.library.util.bench  :as bench       :refer [bench]                ]
      '[quanta.library.util.debug  :as debug       :refer [? break]              ]
      '[quanta.library.util.sh     :as sh                                        ]
      '[quanta.library.data.queue  :as q           :refer [queue]                ]
      '[quanta.library.thread                      :refer :all                   ]
      '[quanta.library.error       :as err         :refer :all                   ]
      '[clojure.core.async         :as async       :refer [go <! >! alts!]       ])))
(defn require-java-fx [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require '[quanta.library.ui.init])
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
      '[quanta.library.ui.jfx :as fx :refer
         [fx do-fx
          set-listener! swap-content!
          fx-node? fx-obj?]])))
(defn require-fx [^Namespace curr-ns]
  (binding [*ns* curr-ns]
    (require-fx-core curr-ns)
    (require
      '[quanta.library.ui.custom-objs :as objs :refer
         [jnew jdef* jdef jdef! jset! jget jget-prop jgets-map 
          jconj! jdissoc! jupdate!
          arrange-on! center-on! jconj-all!
          setx! sety!
          getx gety get-size get-pos
          place-at! nudge!]])))
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
          '[quanta.datagrid.core       :as grid]
          '[quanta.datagrid.excel      :as xl  ])
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
   USAGE: (in-ns *exp) instead of (in-ns 'quanta.library.ui.experimental)"
  ^{:attribution "Alex Gunnarson"}
  [curr-ns]
  (binding [*ns* curr-ns]
    (defs-private
      'curr-ns      curr-ns
      '*exp         'quanta.library.ui.experimental
      '*ui          'clj-qb.ui.core
      '*coll        'quanta.library.collections    
      '*func        'quanta.library.function       
      '*io          'quanta.library.io             
      '*java        'quanta.library.java           
      '*log         'quanta.library.logic          
      '*ns          'quanta.library.ns             
      '*num         'quanta.library.numeric        
      '*pr          'quanta.library.print          
      '*str         'quanta.library.string   
      '*sys         'quanta.library.system      
      '*thread      'quanta.library.thread         
      '*type        'quanta.library.type           
      '*arr         'quanta.library.data.array     
      '*ftree       'quanta.library.data.ftree     
      '*map         'quanta.library.data.map       
      '*q           'quanta.library.data.queue     
      '*set         'quanta.library.data.set       
      '*vec         'quanta.library.data.vector    
      '*xml         'quanta.library.data.xml       
      '*bench       'quanta.library.util.bench     
      '*debug       'quanta.library.util.debug     
      '*q           'quanta.library.data.queue     
      '*thread      'quanta.library.thread         
      '*err         'quanta.library.error          
      '*time        'quanta.library.time.core      
      '*time-coerce 'quanta.library.time.coerce    
      '*time-form   'quanta.library.time.format    
      '*time-locala 'quanta.library.time.local     
      '*xl-io       'clj-qb.xl-io         
      '*sr          'clj-qb.sales-receipt 
      '*adj         'clj-qb.inv-adj       
      '*req-io      'clj-qb.req-io        
      '*parse       'clj-qb.resp-parse    
      '*db          'clj-qb.qb-database   
      '*req         'clj-qb.req-gen       
      '*qb          'clj-qb.dispatch          
      '*grid        'quanta.datagrid.core 
      '*xl          'quanta.datagrid.excel)))

; find-doc, doc, and source are incl. in /user/ ns but not in any others
(defalias source   repl/source)   
(defalias find-doc repl/find-doc)
(defalias doc      repl/doc)

