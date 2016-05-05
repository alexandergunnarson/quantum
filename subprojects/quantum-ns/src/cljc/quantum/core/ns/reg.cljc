(ns quantum.core.ns.reg
  (:require [clojure.set               :as set                     ]
            [quantum.core.ns.reg-utils :as utils :refer [set-merge]]))

(def reg-raw
  '{reg        {:aliases         {:cljc {reg     quantum.core.ns  } ; Temporarily call it |quantum.core.ns| for backwards compatibility
                                  :clj  {refresh clojure.tools.namespace.repl}}
                :refers          {:cljc {reg     #{ns-exclude js-println
                                                   ANil ABool ADouble ANum AExactNum AInt ADecimal
                                                   AKey AVec ASet AArrList ATreeMap ALSeq
                                                   ARegex AEditable ATransient AQueue AMap AError}}
                                  :clj  {refresh #{refresh refresh-all}}
                                  ; Otherwise "Use of undeclared Var"
                                  ;:cljs {reg     #{Exception IllegalArgumentException}}
                                  }}
    qcore       {:requires        {:cljc #{clojure.core.rrb-vector}
                                   :clj  #{flatland.ordered.map   }}
                 :aliases         {:cljc {qcore       quantum.core.core                 }
                                   :clj  {core        clojure.core
                                          proteus     proteus                           }
                                   :cljs {core        cljs.core                         }}
                 :refers          {:cljc {qcore       #{with lens cursor}               }
                                   :clj  {proteus     #{let-mutable}                    }}
                 :imports
                   (clojure.core.rrb_vector.rrbt.Vector
                    flatland.ordered.map.OrderedMap
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
                    clojure.core.Vec
                    (java.math BigDecimal))}
    var         {:core-exclusions {:clj  #{defonce}                                     }
                 :aliases         {:cljc {var         quantum.core.vars                 }}
                 :refers          {:cljc {var         #{defalias defmalias def-}}
                                   ; If |defonce| is used in CLJS, even if you :require-macros it, it says cljs.core/defonce 
                                   :clj  {var         #{defonce reset-var! swap-var! defs defs-}}}}
    repl        {:aliases         {:clj  {repl        quantum.core.meta.repl            }}
                 :refers          {:clj  {repl        #{source find-doc doc javadoc}}}}
    core-async  {:aliases         {:clj  {core-async  clojure.core.async}
                                   :cljs {core-async  cljs.core.async}}
                 :macro-aliases   {:cljs {asyncm cljs.core.async.macros}}
                 :refers          {:cljc {core-async  #{<! >! alts!}}
                                   :clj  {core-async  #{go go-loop thread}}
                                   :cljs {asyncm      #{go go-loop}}}}
    async       {:aliases         {:cljc {async       quantum.core.thread.async}}
                 :refers          {:cljc {async       #{concur put!! >!! take!! <!!
                                                        empty! peek!! alts!! chan wait-until}}}}
    res         {:aliases         {:cljc {res         quantum.core.resources}
                                   :clj  {component   com.stuartsierra.component}}
                 :refers          {:cljc {res         #{with-cleanup with-resources}}}}
    ; QUANTUM.CORE.COLLECTIONS
    coll        {:aliases         {:cljc {coll        quantum.core.collections}}
                 :core-exclusions {:cljc #{contains? for doseq subseq
                                           reduce repeat repeatedly
                                           range merge count
                                           vec sorted-map sorted-map-by 
                                           into first second rest
                                           last butlast get pop peek empty
                                           take take-while
                                           key val conj! assoc! dissoc! disj!} }
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
                                                 conj conjl conjr
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
                 :refers          {:cljc {cbase       #{kmap}}}
                 :imports         (fast_zip.core.ZipperLocation)                              }
    diff        {:aliases         {:cljc {diff        quantum.core.collections.diff           }}}
    ; QUANTUM.CORE.CRYPTOGRAPHY
    crypto      {:aliases         {:cljc {crypto      quantum.core.cryptography               }}}
    ; QUANTUM.CORE.ERROR
    err         {:core-exclusions {:cljc #{assert}}
                 :aliases         {:cljc {err         quantum.core.error                      }}
                 :refers          {:cljc {err         #{throw+ with-assert assert
                                                        throw-when throw-unless
                                                        assertf-> assertf->>
                                                        try+ try-times
                                                        ->ex}                                 }
                                   :cljs {err         #{Err}                                  }}
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
                 :core-exclusions {:cljc #{dec inc}}
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
    cache       {:core-exclusions {:cljc #{memoize}}
                 :aliases         {:cljc {cache       quantum.core.cache                      }}
                 :refers          {:cljc {cache       #{memoize}                              }}}
    ; QUANTUM.CORE.CONVERT.*
    convert     {:aliases         {:cljc {conv        quantum.core.convert                    }}
                 :refers          {:cljc {conv        #{->str ->bytes}                        }}}
    pconvert    {:aliases         {:cljc {pconv       quantum.core.convert.primitive          }}
                 :core-exclusions {:clj  #{boolean byte char short int long float double}}
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
                 :refers          {:cljc {pr          #{! pr-attrs !*}                        }}}
    ; QUANTUM.CORE.STRING.*
    str         {:core-exclusions {:cljc #{re-find}}
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
    bin         {:core-exclusions {:cljc #{bit-or bit-and bit-xor bit-not
                                           bit-shift-left bit-shift-right
                                           unsigned-bit-shift-right
                                           true? false? #_nil?}}
                 :aliases         {:cljc {bin         quantum.core.data.binary                }}
                 :refers          {:cljc {bin         #{>>> >> <<
                                                        bit-or bit-and bit-xor bit-not
                                                        bit-shift-left bit-shift-right
                                                        unsigned-bit-shift-right}             }
                                   :clj  {bin         #{true? false? #_nil?}                  }}}
    bytes       {:aliases         {:cljc {bytes       quantum.core.data.bytes                 }}}
    csv         {:aliases         {:cljc {csv         quantum.core.data.complex.csv           }}}
    list        {:aliases         {:cljc {list        quantum.core.data.list                  }}
                 :refers          {:clj  {list        #{dlist}                                }}
                 :imports         (quantum.core.data.finger_tree.CountedDoubleList)           
               }
    hex         {:aliases         {:cljc {hex         quantum.core.data.hex                   }}
                 :macro-aliases   {:cljs {hex         quantum.core.data.hex                   }}}
    json        {:aliases         {:cljc {json        quantum.core.data.complex.json          }}}
    map         {:core-exclusions {:cljc #{merge sorted-map sorted-map-by}}
                 :aliases         {:cljc {map         quantum.core.data.map                   }}
                 :refers          {:cljc {map         #{map-entry ordered-map}                }
                                   :clj  {map         #{int-map}                              }}}
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
    ; QUANTUM.CORE.META.*
    bench       {:aliases         {:cljc {bench       quantum.core.meta.bench                 }}
                 :refers          {:clj  {bench       #{bench}                                }}}
    debug       {:aliases         {:cljc {debug       quantum.core.meta.debug                 }}
                 :refers          {:clj  {debug       #{trace}                                }}}
    sh          {:aliases         {:cljc {sh          quantum.core.thread.sh                    }}}
    fn          {:aliases         {:cljc {fn          quantum.core.fn                         }}
                 :refers          {:cljc {fn          #{compr *fn f*n unary
                                                        zeroid monoid
                                                        firsta call juxtm juxt-kv
                                                        doto->> with->> withf withf->>
                                                        with-pr->> with-msg->> withfs
                                                        with-do rfn defcurried
                                                        fn->> fn-> <- fn-nil MWA}                 }
                                   :clj  {fn          #{jfn mfn}                          }}
                 :import          (quantum.core.fn MultiRet)                                  }
    logic       {:core-exclusions {:cljc #{when-let if-let}}
                 :aliases         {:cljc {logic       quantum.core.logic                      }}
                 :refers          {:cljc {logic       #{splice-or coll-or coll-and
                                                        nnil? nempty?
                                                        eq? fn= fn-eq? any?
                                                        ifn if*n ifp
                                                        whenf whenc whenp
                                                        whenf*n whencf*n
                                                        condf condfc condf*n condpc
                                                        fn-and fn-or fn-not
                                                        when-let if-let}}}}
    loops       {:core-exclusions {:cljc #{for doseq reduce}}
                 :aliases         {:cljc {loops       quantum.core.loops                      }}
                 :refers          {:cljc {loops       #{reduce- reducei-
                                                        reduce reducei for fori
                                                        doseq doseqi ifor}                   }}}
    macros      {:requires        {:cljc #{quantum.core.log}} ; To get logging for macros
                 :aliases         {:cljc {macros      quantum.core.macros                     }}
                 :refers          {:cljc {macros      #{quote+ fn+ defn+
                                                        defmethod+ defmethods+
                                                        let-alias assert-args
                                                        compile-if emit-comprehension
                                                        do-mod}                               }
                                   :clj  {macros      #{defnt defnt' deftransmacro}                         }
                                   :cljs {macros      #{defnt}                                }}}
    cmacros     {:aliases         {:cljc {cmacros     quantum.core.macros.core}}
                 :refers          {:cljc {cmacros     #{if-cljs when-cljs resolve-local}}}}
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
    tpred       {:core-exclusions {:cljs #{seqable?}}
                 :aliases         {:cljc {tpred       quantum.core.type.predicates}}
                 :refers          {:cljc {tpred       #{boolean? atom? seqable? derefable?}   }}}       
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



(def ^{:doc "All macros in Quantum library. Primarily for ClojureScript's :refer-macros clause."}
  macros
 '{quantum.core.core         #{with}
   quantum.core.collections  #{for for-m fori lfor
                               doseq doseqi
                               reduce reduce- reducei reducei-
                               seq-loop loopr
                               until
                               repeatedly
                               count lasti
                               getr subseq
                               index-of last-index-of
                               get first second peek rest butlast last
                               conj conjl conjr
                               pop popl popr
                               assoc! dissoc! conj! disj! update!
                               contains? containsk? containsv?
                               array
                               taker-until
                               map-entry map->record
                               deficlass
                               kmap}
   quantum.core.collections.base #{kmap}
   quantum.core.error        #{try+ try-times throw+
                               throw-unless throw-when
                               with-catch with-assert assert
                               assertf-> assertf->>},
   quantum.core.fn           #{defcurried with-do f*n <- fn-> rfn fn->> mfn MWA doto->>}
   quantum.core.log          #{pr ppr}
   quantum.core.logic        #{whenf whenc  whenf*n whencf*n whenp
                               ifn          if*n             ifp
                               condf condfc condf*n condpc
                               eq?  fn-eq? fn=
                               neq? fn-neq?
                               fn-or   fn-and fn-not
                               coll-or coll-and
                               when-let if-let}
   quantum.core.loops        #{unchecked-inc-long until
                               dos
                               reduce  reduce-
                               reducei reducei-
                               doseq   doseq- 
                               doseqi  doseqi-
                               for fori ifor lfor}
   quantum.core.macros       #{quote+ fn+ defn+ defmethod+ defmethods+ defnt compile-if assert-args let-alias}
   quantum.core.ns           #{ns-exclude},
   quantum.core.meta.repl    #{source find-doc doc javadoc}
   quantum.core.vars         #{defalias defonce defmalias def-
                               reset-var! swap-var! defs defs-}
   quantum.core.numeric      #{+= -=
                               ++ --}
   quantum.core.print        #{pr-attrs with-print-str*}
   quantum.core.reducers     #{for+ doseq+}
   quantum.core.resources    #{with-resources}
   quantum.core.test         #{qtest}
   quantum.core.thread       #{thread+ async async-loop}
   quantum.core.thread.async #{wait-until}
   quantum.core.type         #{should-transientize?}
   quantum.measure.convert   #{convert}
   cljs.core.async.macros    #{go go-loop}
   quantum.core.macros.core  #{resolve-local}})

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
                 (apply merge-with set-merge %)
               (= k :imports)
                 (apply set-merge %)
               (= k :requires)
                 (apply merge-with set-merge %)
               :else
                 (apply merge-with merge %)))))

(def reg
  (let [possible-keys [:injection :core-exclusions :requires :refers :aliases :macro-aliases :imports]
        lib-exclusions (set/union '#{red loops ccore} '#{http web auth url ui}) ; Because contained in coll
        lib-keys (->> reg-raw keys (remove (partial contains? lib-exclusions)))
        ; quantum->bundle
        bundle (fn [bundle-keys]
                 (zipmap possible-keys
                   (->> possible-keys
                        (map (fn [k] (->> bundle-keys (get-ns-syms reg-raw k)))))))]
    (assoc reg-raw
      :lib  (bundle lib-keys)
      :core (bundle '#{reg qcore var repl cmacros}))))

