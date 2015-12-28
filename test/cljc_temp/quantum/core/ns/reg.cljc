(ns quantum.core.ns.reg
  (:require [clojure.set               :as set                     ]
            [quantum.core.ns.reg-utils :as utils :refer [set-merge]]))

(def reg-raw
  '{ns          {:requires {:cljc #{clojure.core.rrb-vector}
                            :clj  #{flatland.ordered.map   }}
                 :aliases  {:cljc {ns      quantum.core.ns  
                                   ;test    quantum.core.test
                                 }
                            :clj  {core    clojure.core
                                   refresh clojure.tools.namespace.repl
                                   proteus proteus}
                            :cljs {core cljs.core}}
                 :refers   {:cljc
                             {ns   #{defalias defmalias source def- ns-exclude js-println
                                     ANil ABool ADouble ANum AExactNum AInt ADecimal AKey AVec ASet
                                     AArrList ATreeMap ALSeq ARegex AEditable ATransient AQueue AMap AError}
                              ;test #{qtest}
                            }
                            :clj {ns      #{alias-ns defs javadoc swap-var! reset-var!}
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
    core-async  {:aliases         {:clj  {core-async  clojure.core.async}
                                   :cljs {core-async  cljs.core.async}}
                 :macro-aliases   {:cljs {asyncm cljs.core.async.macros}}
                 :refers          {:cljc {core-async  #{<! >! alts!}}
                                   :clj  {core-async  #{go go-loop thread}}
                                   :cljs {asyncm      #{go go-loop}}}}
    async       {:aliases         {:cljc {async       quantum.core.thread.async}}
                 :refers          {:cljc {async       #{concur put!! >!! take!! <!! empty! peek!! alts!! chan wait-until}}}}
    res         {:aliases         {:cljc {res         quantum.core.resources}
                                   :clj  {component   com.stuartsierra.component}}
                 :refers          {:cljc {res         #{with-cleanup with-resources}}}}
    ; QUANTUM.CORE.COLLECTIONS
    coll        {:aliases         {:cljc {coll        quantum.core.collections}}
                 :core-exclusions #{contains? for doseq subseq
                                    reduce repeat repeatedly
                                    range merge count
                                    vec sorted-map sorted-map-by 
                                    into first second rest
                                    last butlast get pop peek empty
                                    take take-while
                                    key val conj! assoc! dissoc! disj!} 
                 :refers          {:cljc {coll #{for fori for-m until doseq doseqi
                                                 reduce reducei reducei-
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
                                                 ffilter filter+ filter-keys+ filter-vals+
                                                 remove+ remove-keys+ remove-vals+
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
                 :imports         (quantum.core.collections.core.MutableContainer)            }
    ccore       {:aliases         {:cljc {ccore       quantum.core.collections.core           }}
                 :imports         (quantum.core.collections.core.MutableContainer)            }
    cbase       {:aliases         {:cljc {cbase       quantum.core.collections.base
                                          zip         fast-zip.core                           }}
                 :imports         (fast_zip.core.ZipperLocation)                              }
    diff        {:aliases         {:cljc {diff        quantum.core.collections.diff           }}}
    ; QUANTUM.CORE.CRYPTOGRAPHY
    crypto      {:aliases         {:cljc {crypto      quantum.core.cryptography               }}}
    ; QUANTUM.CORE.ERROR
    err         {:core-exclusions #{assert}
                 :aliases         {:cljc {err         quantum.core.error                      }
                                   :cljs {err-cljs    quantum.core.cljs.error                 }}
                 :refers          {:cljc {err         #{throw+ with-assert assert
                                                        with-throw with-throws throw-when
                                                        throw-unless assertf-> assertf->>}    }
                                   :clj  {err         #{try+ try-times}                       }
                                   :cljs {err         #{Err}
                                          err-cljs    #{try+}                                 }}
                 :imports         (quantum.core.error.Err)                                    }
    ; QUANTUM.CORE.GRAPH
    graph       {:aliases         {:cljc {graph       quantum.core.graph                      }}}
    ; QUANTUM.CORE.IO.*
    io          {:aliases         {:cljc {io          quantum.core.io.core                    }}
                 :imports          ((java.io File
                                             FileNotFoundException IOException
                                             FileReader PushbackReader
                                             DataInputStream DataOutputStream 
                                             OutputStream FileOutputStream
                                             ByteArrayOutputStream
                                             BufferedOutputStream BufferedInputStream
                                             InputStream  FileInputStream
                                             PrintWriter))                                    }
    io-ser      {:aliases         {:clj  {io-ser      quantum.core.io.serialization           }}}
    fs          {:aliases         {:cljc {fs          quantum.core.io.filesystem              }}}
    ; QUANTUM.CORE.JAVA
    java        {:aliases         {:cljc {java        quantum.core.java                       }}}
    ; QUANTUM.CORE.LOG
    log         {:aliases         {:cljc {log         quantum.core.log                        }}}
    ; QUANTUM.CORE.NUMERIC
    num         {:aliases         {:cljc {num         quantum.core.numeric                    }}
                 :core-exclusions #{dec inc}
                 :refers          {:cljc {num         #{nneg? greatest least +* -* **
                                                        dec dec*
                                                        inc inc*
                                                        += -=
                                                        ++ --}                                }}}
    ; QUANTUM.CORE.REFLECT
    reflect     {:aliases         {:clj  {reflect     quantum.core.reflect                    }}
                 :refers          {:clj  {reflect     #{obj->map}                             }}}
    paths       {:aliases         {:clj  {paths       quantum.core.paths                      }}
                 :refers          {:clj  {paths       #{paths}                                }}}
    cache       {:core-exclusions #{memoize}
                 :aliases         {:cljc {cache       quantum.core.cache                      }}
                 :refers          {:cljc {cache       #{memoize}                              }}}
    ; QUANTUM.CORE.CONVERT.*
    convert     {:aliases         {:cljc {conv        quantum.core.convert                    }}
                 :refers          {:cljc {conv        #{->str ->bytes}                        }}}
    pconvert    {:aliases         {:cljc {pconv       quantum.core.convert.primitive          }}
                 :core-exclusions #{boolean byte char short int long float double}
                 :refers          {:cljc {pconv       #{boolean ->boolean
                                                        byte    ->byte   ->byte*
                                                        char    ->char   ->char*
                                                        short   ->short  ->short*
                                                        int     ->int    ->int*
                                                        long    ->long   ->long*
                                                        float   ->float  ->float*
                                                        double  ->double ->double*}           }}}
    ; QUANTUM.CORE.PRINT
    pr          {:aliases         {:cljc {pr          quantum.core.print                      }}
                 :refers          {:cljc {pr          #{! pprint pr-attrs !*}                 }}}
    ; QUANTUM.CORE.STRING.*
    str         {:core-exclusions #{re-find}
                 :aliases         {:cljc {str         quantum.core.string                     }}
                 :refers          {:cljc {str         #{re-find}                              }}}    
    strf        {:aliases         {:cljc {strf        quantum.core.string.format              }}}    
    ; QUANTUM.CORE.SYSTEM
    sys         {:aliases         {:cljc {sys         quantum.core.system                     }}}    
    ; QUANTUM.CORE.THREAD
    thread      {:aliases         {:cljc {thread      quantum.core.thread                     }}
                 :refers          {:clj  {thread      #{thread+ async async-loop}             }}}
    ; QUANTUM.CORE.DATA
    arr         {:aliases         {:cljc {arr         quantum.core.data.array                 }}
                 :refers          {:cljc {arr         #{aset!      }                          }
                                   :clj  {arr         #{byte-array+}                          }}}
    bin         {:core-exclusions #{bit-or bit-and bit-xor bit-not
                                    bit-shift-left bit-shift-right
                                    unsigned-bit-shift-right
                                    true? false? #_nil?}
                 :aliases         {:cljc {bin         quantum.core.data.binary                }}
                 :refers          {:cljc {bin         #{>>> >> <<
                                                        bit-or bit-and bit-xor bit-not
                                                        bit-shift-left bit-shift-right
                                                        unsigned-bit-shift-right}             }
                                   :clj  {bin         #{true? false? #_nil?}                  }}}
    bytes       {:aliases         {:cljc {bytes       quantum.core.data.bytes                 }}}
    csv         {:aliases         {:cljc {csv         quantum.core.data.complex.csv           }}}
    ftree       {:aliases         {:cljc {ftree       quantum.core.data.ftree                 }}
                 :refers          {:clj  {ftree       #{dlist}                                }}
                 :imports         (clojure.data.finger_tree.CountedDoubleList)                }
    hex         {:aliases         {:cljc {hex         quantum.core.data.hex                   }}}
    json        {:aliases         {:cljc {json        quantum.core.data.complex.json          }}}
    map         {:core-exclusions #{merge sorted-map sorted-map-by}
                 :aliases         {:cljc {map         quantum.core.data.map                   }}
                 :refers          {:cljc {map         #{map-entry ordered-map}                }
                                   :clj  {map         #{int-map imerge}                       }}}
    queue       {:aliases         {:cljc {q           quantum.core.data.queue                 }}
                 :refers          {:cljc {q           #{queue}                                }}}
    set         {:aliases         {:cljc {set         quantum.core.data.set                   }}
                 :refers          {:cljc {set         #{sorted-set+}                          }
                                   :clj  {set         #{ordered-set int-set dense-int-set}    }}}
    vec         {:aliases         {:cljc {vec         quantum.core.data.vector                }}
                 :refers          {:cljc {vec         #{catvec subvec+ vector+? vector+}      }}}
    xml         {:aliases         {:cljc {xml         quantum.core.data.complex.xml           }}}
    ; QUANTUM.CORE.TIME.*
    time        {:aliases         {:cljc {time        quantum.core.time.core                  }}}
    time-coerce {:aliases         {:cljc {time-coerce quantum.core.time.coerce                }}}
    time-format {:aliases         {:cljc {time-form   quantum.core.time.format                }}}
    time-local  {:aliases         {:cljc {time-loc    quantum.core.time.local                 }}}
    ; QUANTUM.CORE.UTIL.*
    bench       {:aliases         {:cljc {bench       quantum.core.util.bench                 }}
                 :refers          {:clj  {bench       #{bench}                                }}}
    debug       {:aliases         {:cljc {debug       quantum.core.util.debug                 }}
                 :refers          {:clj  {debug       #{? trace}                              }}}
    sh          {:aliases         {:cljc {sh          quantum.core.util.sh                    }}}
    fn          {:aliases         {:cljc {fn          quantum.core.function                   }}
                 :refers          {:cljc {fn          #{compr *fn f*n unary
                                                        zeroid monoid
                                                        firsta call juxtm juxt-kv
                                                        doto->> with->> withf withf->>
                                                        with-pr->> with-msg->> withfs
                                                        with-do rfn defcurried
                                                        fn->> fn-> <- fn-nil}                 }
                                   :clj  {fn          #{MWA jfn mfn}                          }}
                 :import          (quantum.core.function MultiRet)                            }
    logic       {:aliases         {:cljc {logic       quantum.core.logic                      }
                                   :cljs {logic-cljs  quantum.core.cljs.logic                 }}
                 :refers          {:cljc {logic       #{splice-or coll-or coll-and
                                                        nnil? nempty?
                                                        eq? fn= fn-eq? any?
                                                        ifn if*n ifp
                                                        whenf whenc whenp
                                                        whenf*n whencf*n
                                                        condf condfc condf*n condpc}          }
                                   :clj  {logic       #{fn-and fn-or fn-not}                  }
                                   :cljs {logic-cljs  #{fn-and fn-or fn-not}                  }}}
    loops       {:core-exclusions #{for doseq reduce}
                 :aliases         {:cljc {loops       quantum.core.loops                      }
                                   :cljs {loops-cljs  quantum.core.cljs.loops                 }}
                 :refers          {:cljc {loops       #{reduce- reducei-}                     }
                                   :clj  {loops       #{reduce reducei for fori
                                                        doseq doseqi ifor}                    }
                                   :cljs {loops-cljs  #{reduce reducei for fori
                                                        doseq doseqi ifor}                    }}}
    macros      {:requires        {:cljc #{quantum.core.log}} ; To get logging for macros
                 :aliases         {:cljc {macros      quantum.core.macros                     }
                                   :cljs {macros-cljs quantum.core.cljs.macros                }}
                 :refers          {:cljc {macros      #{quote+ fn+ defn+
                                                        defmethod+ defmethods+
                                                        let-alias assert-args
                                                        compile-if emit-comprehension
                                                        do-mod}                               }
                                   :clj  {macros      #{defnt defnt'}                         }
                                   :cljs {macros-cljs #{defnt}                                }}}
    rand        {:aliases         {:cljc {rand        quantum.core.nondeterministic           }}}
    red         {:aliases         {:cljc {red         quantum.core.reducers}}
                 :refers          {:cljc {red         #{map+ reduce+ filter+ remove+
                                                        take+ take-while+ drop+ fold+
                                                        range+ for+}}
                                   :clj  {red         #{taker+ dropr+ count*}}}}
    type        {;:exclusions
                 ; [seq? vector? set? map? string? associative? keyword? nil? list? coll? char?]
                 :aliases         {:cljc {type        quantum.core.type}}
                 :refers          {:cljc {type        #{instance+? array-list?
                                                        boolean? double? map-entry? listy?
                                                        sorted-map? queue? lseq?
                                                        pattern? regex? editable? transient?
                                                        should-transientize?}                 }
                                   :clj  {type        #{construct bigint? file?
                                                        byte-array? name-from-class}          }
                                   :cljs {type        #{class}                                }}}
    tcore       {:aliases         {:cljc {tcore       quantum.core.type.core                  }}}
    classes     {:aliases         {:cljc {classes     quantum.core.classes                    }}}
    ; EXT
    http        {:aliases         {:clj  {http        quantum.http.core                       }
                                   :cljs {http        quantum.cljs.http.core                  }}
                 :imports         (quantum.http.core.HTTPLogEntry)}
    ; Unit conversion
    uconv       {:aliases         {:cljc {uconv       quantum.measure.convert}}
                 :refers          {:cljc {uconv       #{convert}}}}
    web         {:aliases         {:clj  {web         quantum.web.core}}
                 :imports         ((org.openqa.selenium WebDriver WebElement TakesScreenshot
                                     StaleElementReferenceException NoSuchElementException
                                     OutputType Dimension)
                                   (org.openqa.selenium Keys By Capabilities
                                     By$ByClassName By$ByCssSelector By$ById By$ByLinkText
                                     By$ByName By$ByPartialLinkText By$ByTagName By$ByXPath)
                                   (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService PhantomJSDriverService$Builder )
                                   (org.openqa.selenium.remote RemoteWebDriver RemoteWebElement))}
    web-support {:aliases         {:clj  {compojure  compojure.core
                                          handler    compojure.handler   
                                          jetty ring.adapter.jetty
                                          ;friend     cemerick.friend            
                                          ;workflows  cemerick.friend.workflows  
                                          ;creds      cemerick.friend.credentials
                                          ;oauth2     friend-oauth2.workflow     
                                          ;oauth-util friend-oauth2.util         
                                          oauth      quantum.auth.oauth}}
                 :refers          {:clj  {compojure  #{defroutes GET ANY POST}                }}}
    auth        {:aliases         {:clj  {auth       quantum.auth.core                        }}}
    url         {:aliases         {:clj  {url        quantum.http.url                         }}}
    ui          {:aliases         {:cljc {ui         quantum.ui.core  
                                          rx         freactive.core                           }
                                   :clj  {fx         fx-clj.core
                                          fx.css     fx-clj.css                               }}
                 :refers          {:clj  {ui         #{fx}
                                          rx         #{rx}                                    }}
                 :imports         (quantum.ui.core.FXObservableAtom
                                   (javafx.stage              Modality Stage)
                                   (javafx.animation          Animation KeyValue KeyFrame Timeline AnimationTimer Interpolator
                                                              FadeTransition TranslateTransition RotateTransition ScaleTransition
                                                              PathTransition PathTransition$OrientationType)
                                   (javafx.collections        ObservableList FXCollections ListChangeListener
                                                              ListChangeListener$Change)
                                   (javafx.event              ActionEvent EventHandler EventType)
                                   (javafx.geometry           Insets Pos HPos)
                                   (javafx.scene              Group Scene Node Parent)
                                   (javafx.scene.effect       BoxBlur BlendMode Lighting Bloom)
                                   (javafx.scene.image        Image)
                                   (javafx.scene.input        DragEvent KeyEvent KeyCode MouseEvent)
                                   (javafx.scene.media        MediaPlayer Media MediaView)
                                   (javafx.scene.paint        Stop CycleMethod LinearGradient RadialGradient Color)
                                   (javafx.scene.text         Font FontPosture FontWeight Text TextBoundsType TextAlignment)
                                   (javafx.scene.layout       Region GridPane StackPane Pane Priority HBox VBox ColumnConstraints
                                                              Background BackgroundFill
                                                              Border BorderStroke BorderStrokeStyle BorderWidths)
                                   (javafx.scene.shape        Circle Rectangle StrokeType Path PathElement MoveTo CubicCurveTo)
                                   (javafx.util               Duration Callback)
                                   (javafx.beans              InvalidationListener)
                                   (javafx.beans.property     SimpleDoubleProperty SimpleStringProperty)
                                   (javafx.beans.value        ChangeListener ObservableValue)
                                   (javafx.scene.control      ComboBox ContentDisplay Labeled TableColumn TableRow
                                                              TableCell ListCell
                                                              ListView Label Tooltip
                                                              TextArea TextField ContentDisplay
                                                              TableView
                                                              TableView$TableViewSelectionModel
                                                              TableColumn$CellDataFeatures TableColumn$CellEditEvent)
                                   (javafx.scene.control.cell PropertyValueFactory TextFieldTableCell))}})

(defn get-ns-syms
  "Given namespace registrar @reg*, gets the ns symbols"
  [reg* k ns-syms]
  (->> ns-syms
       (map
         (fn [ns-sym]
           (or (get-in reg* [ns-sym k])

               #_(when (= k :refers)
                 (or (get reg* ns-sym)
                     (throw (Exception. (str "Quantum namespace alias does not exist: " ns-sym)))))
               )))
       (remove empty?)
       (#(cond (= k :core-exclusions)
                 (apply set-merge %)
               (= k :imports)
                 (apply set-merge %)
               (= k :requires)
                 (apply merge-with set-merge %)
               :else
                 (apply merge-with merge %)))))

(def reg
  (let [lib-exclusions (set/union '#{red loops ccore} '#{http web auth url ui}) ; Because contained in coll
        lib-keys (->> reg-raw keys (remove (partial contains? lib-exclusions)))
        lib
          {:core-exclusions (->> lib-keys (get-ns-syms reg-raw :core-exclusions) (into #{}))
           :requires        (->> lib-keys (get-ns-syms reg-raw :requires       ))
           :refers          (->> lib-keys (get-ns-syms reg-raw :refers         ))
           :aliases         (->> lib-keys (get-ns-syms reg-raw :aliases        ))
           :macro-aliases   (->> lib-keys (get-ns-syms reg-raw :macro-aliases  ))
           :imports         (->> lib-keys (get-ns-syms reg-raw :imports        ))}]
    (-> reg-raw (assoc :lib lib))))

(def ^{:doc "All macros in Quantum library. Primarily for ClojureScript's :refer-macros clause."}
  macros
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
   quantum.core.ns          #{def- defalias ns-exclude source},
   quantum.core.numeric     #{+= -=
                              ++ --}
   quantum.core.print       #{pr-attrs with-print-str*}
   quantum.core.reducers    #{for+ doseq+}
   quantum.core.test        #{qtest}
   quantum.core.thread      #{thread+ async async-loop}
   quantum.core.type        #{should-transientize?}
   quantum.measure.convert  #{convert}
   cljs.core.async.macros   #{go go-loop}})