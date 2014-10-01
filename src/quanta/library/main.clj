(ns quanta.library.main)
(require ; just for testing
  '[quanta.library.collections :as coll]
  '[quanta.library.reducers              :refer :all]
  '[quanta.library.function    :as fn    :refer :all]
  '[quanta.library.io          :as io]
  '[quanta.library.java        :as java]
  '[quanta.library.logic       :as log   :refer :all]
  '[quanta.library.ns          :as ns    :refer [defalias source]]
  '[quanta.library.numeric     :as num]
  '[quanta.library.print       :as pr    :refer [!]]
  '[quanta.library.string      :as str]
  '[quanta.library.thread      :as thread]
  '[quanta.library.type                  :refer :all]
  '[quanta.library.data.ftree  :as ftree]
  '[quanta.library.data.map    :as map   :refer [sorted-map+]]
  '[quanta.library.data.queue  :as q]
  '[quanta.library.data.set    :as set]
  '[quanta.library.data.vector :as vec   :refer [conjl]]
  '[quanta.library.data.xml    :as xml]
  '[quanta.library.time.core   :as time]
  '[quanta.library.time.coerce :as time-coerce]
  '[quanta.library.time.format :as time-format]
  '[quanta.library.time.local  :as time-local]
  '[quanta.library.util.bench  :as bench :refer [bench]]
  '[quanta.library.util.debug  :as debug :refer [?]])

(set! *warn-on-reflection* true)


; TESTING: https://github.com/jakemcc/lein-test-refresh
; https://github.com/mynomoto/repetition-hunter

; A BETTER MERGE!!

; clojure.zip traversal is interesting because it is iterative, not recursive,
; which is a key difference between zipper traversal and the prior clojure.walk
; implementation.

; https://github.com/ztellman/immutable-int-map/blob/master/README.md
; PROJECTS
; Root Latin:   latin-etym
; Google Drive:

; TODO: SYNTAX HIGHLIGHTING UPDATES
; ordered-set
; c-sorted-set
; alias-ns (?)
; some?

; TODO: NEW FEATURES
; go blocks with library.thread
; learn go blocks

; compression (zip, tar, gzip, etc.... what's the difference?)
; ;; leave /require/, etc. out of ns decl so we can load with classlojure.io/resource-forms
; time
; https://github.com/ztellman/automat
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
; https://github.com/ztellman/immutable-int-map
; Checking https://github.com/clojure/test.check
; https://github.com/clojure/test.generative
; https://github.com/clojure/data.generators
; https://github.com/clojure/jvm.tools.analyzer
; https://github.com/clojure/tools.analyzer
; https://github.com/Raynes/fs
; This library defines some utilities for working with the file system in Clojure.
; Mostly, it wants to fill the gap that clojure.java.io leaves and add on (and prettify)
; what java.io.File provides.
; https://github.com/clojure/data.finger-tree
; https://github.com/clojure/data.priority-map
; https://github.com/clojure/data.avl - really fast sorted sets and sorted maps
; double-list is a sequential collection that provides constant-time access to both the left and right ends.
; counted-double-list provides all the features of double-list plus constant-time count and log-n nth.
; counted-sorted-set is sorted set that also provides log-n nth
; I've been using Clojure for two years now and I've never needed monads for "real" code.
; https://github.com/clojure/core.typed/wiki
; http://adambard.com/blog/why-clojure-part-2-async-magic/
; http://adambard.com/blog/clojure-concurrency-smorgasbord/
; (require '[clojure.edn :as edn])




; TOP CONTRIBUTORS
; https://github.com/michalmarczyk
; https://github.com/taoensso
; https://github.com/technomancy ; Phil Hagelberg
; TOP PEOPLE (NON-CONTRIBUTING)
; https://github.com/Chouser     ; Chris Houser

; should have a transientize macro that speeds things up

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

; ; Write a macro to do arity à la SOCAccess
; ; Macro to print vars

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
; clojure.contrib.fcase           replced by condp
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
; clojure.contrib.monads          https://github.com/clojure/algo.monads/
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
; clojure.contrib.swing-utils
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


; mvn install:install-file -Dfile=lib/quanta.library-0.1.jar -DartifactId=library -Dversion=0.1.0 -DgroupId=quanta -Dpackaging=jar -DlocalRepositoryPath=lib/
