(ns quanta.library.main)

; Mechanical Turk - people test your interface!! For a certain fee per person
; Arithmetic shifting is faster than logical shifting
; Arithmetic automatically does the one operation
; That's the difference between using ints vs. uints


(def interesting-people
  {"Rich Hickey" 
   "Zach Tellman"     "https://github.com/ztellman"
   "Mike Anderson"    "https://github.com/mikera"
   "Brandon Bloom"    "https://github.com/brandonbloom"
   "Alan Malloy"      "https://github.com/amalloy"
   "Michal Marczyk"   "https://github.com/michalmarczyk"
   "Peter Taoussanis" "https://github.com/ptaoussanis"})

(def interesting-groups
  #{"Prismatic" "https://github.com/Prismatic"
    "CircleCI"})

(def general-practices
  {:general
    {:page "https://github.com/Prismatic/eng-practices"
     :desc "Prismatic's Engineering Practices Sessions"}})

(def a
  {:quanta
    {:library
      {:general
        [{:page "ptaoussanis/encore"
          :desc ""}
         {:page "weavejester/medley"
          :desc ""}
         {:page "https://github.com/mikera/clojure-utils/"
          :desc "Clojure utils by mikera"}
         {:page "https://github.com/ztellman/potemkin"
          :desc "A collection of facades and workarounds for things
                 that are more difficult than they should be."}
         {:page "https://github.com/Prismatic/plumbing"
          :desc ""}]}
      {:logging
        [{:page "https://github.com/ptaoussanis/timbre"
          :desc ""}]}
      {:shell
        {:page "https://github.com/flatland/drip"
         :desc "Fast JVM launching without the hassle of persistent JVMs.
                USE IT."}}
      {:reducers
        {:page "https://github.com/aphyr/tesser"
         :desc "Clojure reducers, but for parallel execution on
                distributed systems.

                Barely started."}}
      {:thread
        {:page "https://github.com/ztellman/dirigiste"
         :desc "In the default JVM thread pools, once a thread is
                created it will only be retired when it hasn't
                performed a task in the last minute. In practice,
                this means that there are as many threads as the
                peak historical number of concurrent tasks handled
                by the pool, forever. These thread pools are also
                poorly instrumented, making it difficult to tune
                their latency or throughput.

                Dirigiste provides a fast, efficient, richly instrumented
                version of a java.util.concurrent.ExecutorService"}}
      {:macro-code
        [{:page "https://github.com/mynomoto/repetition-hunter"
          :desc "A tool to find repetitions in clojure code. Really
                 useful."}
         {:page "https://github.com/jonase/eastwood"
          :desc "eastwood - a Clojure lint tool. Alerts you to errors
                 you wouldn't otherwise have noticed."}
         {:page "https://github.com/ztellman/riddley"
          :desc "Code walking without caveats."}]}
      {:data
        {:unk
          {:page "https://github.com/ztellman/cambrian-collections/"
           :desc "A veritable explosion of data structures."}}
        {:array
          {:page "https://github.com/Prismatic/hiphip"
           :desc "hiphip (array)! simple, performant array manipulation in Clojure"}}
        {:mutable
          {:page "https://github.com/ztellman/proteus"
           :desc "let-mutable gives you variables that can be set
                  using set! within the scope. Since it's unsynchronized
                  and doesn't box numbers, it's faster (sometimes
                  significantly) than any state container in Clojure.
                  However, these variables cannot escape the local
                  scope; if passed into a function or closed over,
                  the current value of the variable will be captured.
                  This means that even though this is unsynchronized
                  mutable state, there's no potential for race conditions."}}
        {:tuple
          {:page "https://github.com/ztellman/clj-tuple"
           :desc "Often the lists we create have only a few elements
                  in them. This library provides a collection type,
                  tuple, which is optimized for these cases.

                  A tuple behaves exactly like a Clojure vector.
                  However, compared to lists and vectors, a two element
                  tuple is ~2-3x faster to create, destructure,
                  calculate a hash, check for equality, and look
                  up in a normal Java hash-map. Some of these gains
                  are amplified at larger sizes; a five element
                  tuple is ~30x faster to create than an equivalently-
                  sized vector. Tuples larger than six elements,
                  however, are auto-promoted to standard vectors."}}
        {:byte
          [{:page "https://github.com/ztellman/byte-streams"
            :desc "Java has a lot of different ways to represent a
                   stream of bytes.
 
                   This library is a Rosetta stone for all the byte
                   representations Java has to offer.
 
                   |byte-streams/convert|
 
                   When you need to write bytes to a file, network socket,
                   or other endpoints, you can use |byte-streams/transfer|."}
            {:page "ztellman/byte-transforms"
             :desc "Methods for hashing, compressing, and encoding bytes.
                    It contains the methods in the standard Java lib,
                    as well as a curated collection of the best available
                    methods."}]}
        {:struct
          {:page "https://github.com/ztellman/vertigo"
           :desc "Faster than Java arrays (!) because manipulating the
                  underlying bits.

                  There are a number of predefined primitive types,
                  including int8, int16, int32, int64, float32, and
                  float64.

                  We can either marshal an existing sequence onto a
                  byte-buffer, or wrap an existing byte source.

                  In our data structure we're guaranteed to have a
                  fixed layout, so we know exactly where the data is
                  already. We don't need to fetch all the intermediate
                  data structures, we simply need to calculate the
                  location and do a single read (for, e.g., |get-in|).

                  We get a read time that even for moderately nested
                  structures can be several orders of magnitude faster.

                  Any long-lived data which has a fixed layout can
                  benefit from Vertigo.

                  However, sequences which contain sub-sequences of
                  unpredictable length cannot be represented using
                  Vertigo's structs, and sequences which are discarded
                  after a single iteration are best left out of Vertigo."}}
        {:map
          {:page "https://github.com/achim/multiset"
           :desc "A simple multiset/bag implementation for Clojure."}
          {:page "https://github.com/aphyr/merkle"
           :desc "Clojure Merkle Trees
                  A Clojure library for computing and comparing hash
                  trees over sorted kv collections. Allows you to
                  efficiently find the differing pairs between two
                  such collections without exchanging the collections
                  themselves. Useful in the synchronization of
                  distributed systems."}
          {:page "https://github.com/ztellman/clj-radix"
           :desc "a persistent radix tree, for efficient nested maps
                  Not much description beyond that. Experiment!"}
          {:page "jordanlewis/data.union-find"
           :desc "The union-find data structure by Tarjan.
                  Important somehow. Not sure why yet."}
          {:page "https://github.com/clojure/data.int-map"
           :desc "A map optimized for integer keys.
                  Doubles as a set.
                  Faster in both updates and lookups than normal Clojure
                  data structures, and also more memory efficient,
                  sometimes significantly.
                  |dense-int-set| behaves the same as |int-set|;
                  the difference is only in their memory efficiency.
                  
                  (def s (range 1e6))

                  (into #{} s)              ; ~100mb
                  (into (int-set) s)        ; ~1mb
                  (into (dense-int-set) s)  ; ~150kb

                  The |dense-int-set| allocates larger contiguous chunks,
                  which is great if the numbers are densely clustered.
                  However, if the numbers are sparse, the memory is much worse
                  than normal Clojure structures.

                  Use |dense-int-set| where the elements are densely clustered
                  (each element has multiple elements within +/- 1000, and
                  |int-set| for everything else."}}}
      {:aux
        {:units-of-measurement
          [{:page "https://github.com/martintrojer/frinj"
            :desc "Units of measurement and conversion, etc."}
           {:page "https://github.com/fogus/minderbinder"
            :desc "Units of measurement and conversion, etc."}]}}
      {:pdf
        {:page "https://github.com/yogthos/clj-pdf"
         :desc "Library for generating PDFs"}}
      {:io
        {:page "https://github.com/Raynes/fs"
         :desc "File system utilities"}}
      {:system
        {:page "https://github.com/hugoduncan/criterium"
         :desc "Includes forced garbage collection, memory reporting,
                cache clearing, etc."}}}})

; TESTING: https://github.com/jakemcc/lein-test-refresh

; A BETTER MERGE!!

; clojure.zip traversal is interesting because it is iterative,
; not recursive, which is a key difference between zipper
; traversal and the prior clojure.walk implementation.

; PROJECTS
; Root Latin:   latin-etym
; Google Drive:

; TODO: SOCIAL/CREDENTIAL-BASED
; Publish library... this will help, it really will

; TODO: CLEANUP
; Fix aliases with foldp, foldv, etc...
; Rewrite core functions with reducers in mind -
; |some| |every?| etc. using |reduced|
; catvec + concat + cat, etc.

; TODO: LEARNING
; learn async

; TODO: OPTIMIZATION
; union vs. merge+ ? which one is faster?
; fix distinct+ to work with non-sorted colls
; enable hash-set folding

; TODO: NEW FEATURES
; Should have a transientize macro that speeds things up
; Should have a protocolize macro that speeds things up
; Should have an extern-fn that speeds things up and avoids creation of local fns
; There is something interesting lurking in conversion... something with
;   combinatorics

; compression (zip, tar, gzip, etc.... what's the difference?)
; ZIP (?)
; https://github.com/akhudek/fast-zip ; as a replacement for clojure.zip

; http://clojure.github.io/clojure/clojure.java.shell-api.html
; (require '[clojure.inspector :as ins :refer [inspect inspect-table inspect-tree]])
; Graphical object inspector for Clojure data structures.
; https://github.com/mikera/clojure-utils
;import mikera.cljutils.Clojure;
; public class Demo {
;     public static void main(String [] args) {
;         String s = "(+ 1 2)";
;         System.out.println("Evaluating Clojure code: "+s);

;         Object result = Clojure.eval(s);
;         System.out.println("=> "+ result);
;     }
; }
; Checking https://github.com/clojure/test.check
; https://github.com/clojure/test.generative
; https://github.com/clojure/data.generators
; https://github.com/clojure/jvm.tools.analyzer
; https://github.com/clojure/tools.analyzer
; "I've been using Clojure for two years now and I've never needed monads for "real" code."
; http://adambard.com/blog/why-clojure-part-2-async-magic/
; http://adambard.com/blog/clojure-concurrency-smorgasbord/



; diff
; function
; Usage: (diff a b)
; Recursively compares a and b, returning a tuple of
; [things-only-in-a things-only-in-b things-in-both].
; Comparison rules:

; * For equal a and b, return [nil nil a].
; * Maps are subdiffed where keys match and values differ.
; * Sets are never subdiffed.
; * All sequential things are treated as associative collections
;   by their indexes, with results returned as vectors.
; * Everything else (including strings!) is treated as
;   an atom and compared for equality.
; (defalias diff clojure.data/diff)
; (defn dotos [obj-0 funcs]
;   (doseq [func funcs] (doto obj-0 (func)))
;   obj-0)
; ; custom order -> so you don't need to do (#()) but only #()

; (defmacro letrec
;   "Like let, but the bindings may be mutually recursive, provided that
;    the heads of all values can be evaluated independently.
 
;    This means that functions, lazy sequences, delays and the like can
;    refer to other bindings regardless of the order in which they
;    appear in the letrec form."
;   ^{:attribution "Michal Marczyk - https://gist.github.com/michalmarczyk/3c6b34b8db36e64b85c0"}
;   [bindings & body]
;   (let [bindings (destructure bindings)
;         bcnt (quot (count bindings) 2)
;         arrs (gensym "letrec_bindings_array__")
;         arrv `(make-array Object ~bcnt)
;         bprs (partition 2 bindings)
;         bssl (map first bprs)
;         bsss (set bssl)
;         bexs (map second bprs)
;         arrm (zipmap bssl (range bcnt))]
;     `(let [~arrs ~arrv]
;        (symbol-macrolet [~@(mapcat (fn [s]
;                                      [s `(aget ~arrs ~(arrm s))])
;                                    bssl)]
;          ~@(map (fn [s e]
;                   `(aset ~arrs ~(arrm s) ~e))
;                 bssl
;                 bexs))
;        (let [~@(mapcat (fn [s]
;                          [s `(aget ~arrs ~(arrm s))])
;                        bssl)]
;          ~@body))))


; ; TODO: TO IMPLEMENT
; ;(defmacro with-printing-vars [function]
;   ; gets all the vars in a function and prints them out.
;   ; ' is printed as " "
;   ; - is printed as " "
;   ; ' is printed as ", "
; ;  )

; ; (defmacro incl-ref [object-ref]
; ;   `[~object-ref (eval ~object-ref)])

; ; (defn well-frick [object-ref]
; ;   (println "Object-ref: |" object-ref)
; ;   (println "Eval: |" (eval object-ref))) ; will evaluate the symbol "fake-resource" within this function's scope, separately.
; ; (defmacro incl-ref [object-ref]
; ;   ;`[~object-ref (eval ~object-ref)] ; strangely, it evaluates the symbol "resource" within the macro... uhh...
; ;   `[~object-ref (eval object-ref)]) ; strangely, it evaluates object-ref right away

; ; CREDIT + DEBIT CARD PAYMENTS
; ; http://stackoverflow.com/questions/9233657/api-for-receiving-payments-through-visa-mastercard-credit-debit-cards-paypal
; ; P*Works! from nSoftware has a good toolset: ICharge Payment Integrator
; ; However, unless you are fully prepared for the nightmare that is PCI compliance, just use Authorize.Net or PayPal's
; ; standard methods. What they charge you in fees you will be more than happy to pay compared to what happens if you lose
; ; credit card numbers to hacking.

; ; Connecting a payment application to the credit card processing networks is difficult, expensive and beyond the resources
; ; of most businesses. Instead, you can easily connect to the Authorize.Net Payment Gateway, which provides the complex
; ; infrastructure and security necessary to ensure secure, fast and reliable transactions.
; ; ; 1996 Authorize.Net Founded
; ; ; $100+ Billion Annual Transacting Volume
; ; ; 400,000+ Merchant Customers



; clojure.contrib.accumulators
; clojure.contrib.agent-utils
; clojure.contrib.combinatorics   https://github.com/clojure/math.combinatorics/
; clojure.contrib.command-line    https://github.com/clojure/tools.cli/ (based on the clargon library)
; clojure.contrib.complete
; clojure.contrib.complex-numbers
; clojure.contrib.cond
; clojure.contrib.condition       https://github.com/scgilardi/slingshot
; clojure.contrib.core            Partly migrated to https://github.com/clojure/core.incubator/
; clojure.contrib.dataflow
; clojure.contrib.datalog         Michael Fogus has a project https://github.com/tailrecursion/bacwn on Github based on this library.
; clojure.contrib.def             Partly migrated to https://github.com/clojure/core.incubator/
;                                 defvar: as of Clojure 1.3, you can specify a docstring in a def form: (def my-var "This is my docstring" some-value)
; clojure.contrib.error-kit       https://github.com/scgilardi/slingshot
; clojure.contrib.except
; clojure.contrib.find-namespaces https://github.com/clojure/tools.namespace/
; clojure.contrib.fnmap
; clojure.contrib.gen-html-docs
; clojure.contrib.generic         https://github.com/clojure/algo.generic/
; clojure.contrib.graph           https://github.com/clojure/algo.graph/
; clojure.contrib.greatest-least
; clojure.contrib.import-static
; clojure.contrib.jar
; clojure.contrib.java-utils
; clojure.contrib.jmx             https://github.com/clojure/java.jmx/
; clojure.contrib.json            https://github.com/clojure/data.json/
; clojure.contrib.lazy-seqs
; clojure.contrib.lazy-xml        https://github.com/clojure/data.xml/
; clojure.contrib.load-all
; clojure.contrib.logging         https://github.com/clojure/tools.logging/
; clojure.contrib.macro-utils     https://github.com/clojure/tools.macro/
; clojure.contrib.macros          https://github.com/clojure/tools.macro/
; clojure.contrib.map-utils
; clojure.contrib.math            https://github.com/clojure/math.numeric-tower/
; clojure.contrib.miglayout
; clojure.contrib.mmap
; clojure.contrib.mock
; clojure.contrib.monadic-io-streams
; clojure.contrib.ns-utils
; clojure.contrib.parent
; clojure.contrib.priority-map    https://github.com/clojure/data.priority-map/
; clojure.contrib.probabilities
; clojure.contrib.profile
; clojure.contrib.prxml
; clojure.contrib.reflect
; clojure.contrib.repl-ln
; clojure.contrib.repl-utils      Migrated to clojure.repl and clojure.java.javadoc.  show functionality similar to clojure.reflect/reflect.  Any equivalents for these? expression-info, run, run*
; clojure.contrib.seq
; clojure.contrib.server-socket
; clojure.contrib.set             Migrated to clojure.set, except proper-subset? and proper-superset?, which are easily implemented using subset? and superset?
; clojure.contrib.singleton
; clojure.contrib.sql             https://github.com/clojure/java.jdbc/
; clojure.contrib.standalone
; clojure.contrib.stream-utils
; clojure.contrib.strint          https://github.com/clojure/core.incubator/
; clojure.contrib.trace           https://github.com/clojure/tools.trace/
; clojure.contrib.types
; clojure.contrib.with-ns
; clojure.contrib.zip-filter      https://github.com/clojure/data.zip/ - https://github.com/akhudek/fast-zip is a drop-in replacement for it which uses records internally. 242.777689 µs vs. 960.326181 µs

; This section lists new contrib namespaces that do not correspond to old contrib namespaces.

; [x] clojure.core.async
; [ ] clojure.core.cache
; [ ] clojure.core.contracts
; [x] clojure.core.rrb-vector
; [ ] clojure.core.logic
; [ ] clojure.core.match
; [ ] clojure.core.memoize
; [ ] clojure.core.typed
; [ ] clojure.core.unify
; [ ] clojure.data.codec       | clojure.contrib.base64
; [x] clojure.data.csv
; [x] clojure.data.finger-tree
; [ ] clojure.data.fressian
; [ ] clojure.data.generators
; [ ] clojure.java.classpath   | clojure.contrib.classpath
; [ ] clojure.java.data
; [ ] clojure.test.benchmark
; [ ] clojure.test.generative
; [ ] clojure.tools.nrepl
; [ ] clojure.tools.reader
