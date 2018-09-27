https://github.com/clojure/clojure/blob/f572a60262852af68cdb561784a517143a5847cf/src/clj/clojure/core/specs.clj




(let [ignore? (fn-or fn? t/protocol? t/multimethod? t/unbound? var/namespace? dasync/thread?)]
  (->> (all-ns)
       (map+    ns-interns)
       (join    {})
       (map+    val)
       (remove+ #{#'clojure.core/*1
                  #'clojure.core/*2
                  #'clojure.core/*3
                  #'clojure.core/*data-readers*
                  #'clojure.core/default-data-readers
                  #'clojure.tools.reader/default-data-readers
                  #'clojure.core.async.impl.timers/timeouts-queue
                  #'clojure.tools.analyzer.jvm/default-passes
                  #'aleph.http/default-connection-pool
                  #'aleph.http/default-response-executor
                  #'clojure.core.async.impl.ioc-macros/passes
                  #'byte-streams/inverse-conversions
                  #'byte-streams/conversions})
       (remove+ (rcomp deref (fn-or ignore? (fn-and var? (fn-> deref ignore?)))))
       (map+    (juxt identity (fn-> deref quantum.core.meta.bench/shallow-byte-size)))
       (join    {})
       (map+    val)
       (reduce  +)
       (#(quantum.measure.convert/convert % :bytes :MB))
       double))

; -> 18.21 MB... there must be lots of data not referenced by vars

; TODO log these stats when every namespace is compiled
(->> (java.lang.management.ManagementFactory/getMemoryPoolMXBeans)
     (mapv (juxt #(.getName %) #(.getType %) #(.getUsage %))))

 {:code-cache 25.52099609375,
 :metaspace 203.1375122070312, (some of this is definitely garbage collected)
 :compressed-class-space 69.5936279296875}


TODO make sure to set a global onerror handler for PhantomJS (via a flag of course)

TODO decrease memory footprint? (it's huge!)
TODO allow return types for `defnt` like `sequential?`
TODO also allow return types like :whatever/validator

TODO longs vs. long-array

; TODO fn (params-match? ->double 1) -> true; (params-match? ->double "asd") -> false
; TODO do fast-as-possible ops given math expr
; (* a (- b c) v) -> (scale (* a (- b c)) v)

; TODO:

(fnt [^indexed? x a]
  (conj' x a)) ; and have it know what function needs to be looked up

(:abcde my-validated-map) ; and know what its type will be




(defn dropr-digit
  ([n] (quot n 10))
  ([digits n]
    (if (<= digits 0)
        n
        (recur (dec digits) (dropr-digit n)))))

(defn count-digits-integral
  "Counts the digits of a number `n` not having a decimal portion."
  ([n] (count-digits-integral (dropr-digit n) 1))
  ([n d] (if (zero? n)
             d
             (recur (dropr-digit n) (inc d)))))

(defn ->integral [n] (- n (->decimal n)))

(defn ->decimal [n] (rem n 1))

(defn pow-10 [n] (long (Math/pow 10 (int n))))

(defn decimal->integer-like-decimal
  "E.g. 0.003812317M -> 1003812317N
   `n` must be 0 ≤ n < 1
   Returns a `bigint`."
  [n]
  (if (zero? n)
      0
      (bigint (str "1" (subs (str (.stripTrailingZeros (bigdec n))) 2)))))

(defn integer-like-decimal->decimal
  "E.g. 1003812317N -> 0.003812317M
   `n` must be an integer.
   Returns a `bigdec`."
  [n]
  (bigdec (str "0." (subs (str n) 1))))

(->decimal 1/4)


(defn num-decimal-places [n]
  (-> n ->decimal bigdec (.stripTrailingZeros) (.scale) (max 0)))

(defn truncate-digits
  "Truncates an integer `n` the specified number of `digits`, replacing
   the truncated portion with 0s."
  [digits n]
  (if (<= digits 0)
      n
      (* (dropr-digit digits n)
         (pow-10 digits))))

(defn exact [n]
  (if (or (double?  n)
          (float?   n)
          (decimal? n))
      (rationalize n)
      n))



Take a look at Claypoole. Example code:

(require '[com.climate.claypoole :as cp])
;; Run with a thread pool of 100 threads, meaning up to 100 HTTP calls
;; will run simultaneously. with-shutdown! ensures the thread pool is
;; closed afterwards.
(cp/with-shutdown! [pool (cp/threadpool 100)
  (cp/pmap pool get-json [url url2]))
The reason you should prefer com.climate.claypoole/pmap over clojure.core/pmap in this case is that the latter sets the number of threads based on the number of CPUs, with no way of overriding. For networking and other I/O operations that aren't CPU bound, you typically want to set the number of threads based on the desired amount of I/O, not based on CPU capacity.

Or use a non-blocking client like http-kit that doesn't require one thread per connection, as suggested by mikera.

http://eng.climate.com/2014/02/25/claypoole-threadpool-tools-for-clojure/

; TODO found a bug when determining return type when reify is the return val; we don't want the absolute type



(defn ->vec
  "`vec` is an O(n) operation because it *always* creates an entirely new vector.
   By contrast, `->vec` ensures that vector coercion will always be an O(1) operation
   when calling `->vec` on (transient or persistent) vectors."
  [x]
  (cond (vector? x)           x
        (transient-vector? x) x
        :else                 (vec x)))

(defn view-rest
  "Like `rest`, but instead of a seq, creates a view in O(1) time of whatever
   collection is passed. Useful for e.g. calling `rest` on a vector but preserving
   all of the vector's properties."
  [xs]
  (if (vector? xs)
      (subvec xs 1)
      (throw (ex-info "`view-rest` not supported on type" {:type (type xs)}))))


(defn num-integral-digits [^double n]
  (-> n Math/abs Math/log10 inc int))

(is (= 2 (num-integral-digits -31.848399183)))
(is (= 1 (num-integral-digits -3.54983283781)))
(is (= 0 (num-integral-digits 0.98372135)))


; TODO incorporate
(defmacro accum-for
  "Like `for`, but exposes the reference to the accumulated (transient)
   output to the body.

   ```
   (accum-for [x xs accum] &body)
   ```
   is equivalent to
   ```
   (persistent!
      (reduce (fn [accum x] (conj! accum (do &body)))
        (transient [])
        xs))
   ```"
  [[x xs accum] & body]
  `(persistent!
     (reduce (fn [accum# ~x]
               (let [~accum accum#] (conj! accum# (do ~@body))))
       (transient [])
       ~xs)))




TODO
type preds are defined on particular classes
so for instance ReferenceOpenHashSet is a hash-set, random-access, mutable, etc.
generate based on this

We want to topo-sort the classes

(loom.graph/digraph (classes/class->ancestor-graph clojure.lang.IPersistentVector))

; We want the set of "lowest common denominators" as it were (not including `Object`)

(map/intersection-by-key
  (classes/class->ancestor-graph clojure.lang.IPersistentVector)
  (classes/class->ancestor-graph clojure.lang.IPersistentList))

(map/intersection-by-key
  (classes/class->ancestor-graph java.util.List)
  (classes/class->ancestor-graph clojure.lang.APersistentVector))

None, already existing features are sufficient
Zero runtime dependencies for projects using Koloboke Compile
Lists
Queues
Concurrent maps/sets
Concurrent Queues
Insertion-ordered hash maps/sets
LRU-ordered hash maps/sets
Sorted maps/sets
Array-based maps/sets (like Android's ArrayMap or fastutil's ArrayMaps)
Enum maps/sets
Maps with weak reference keys/values
Mulitmaps (key -> many values)
Real time hash maps/sets (low worst latency), a-la SmoothieMap
Support for optimized Streams and Streams of primitives

https://github.com/vigna/Sux4J — Succint data structures for Java




This is possible witha  transducer

(do (defn gen-comp-keys-into:xf
  ; TODO use `reduce-multi` or `multiplex`
  ([initf compf kf]
    (fn [rf] (prl! rf)
      (let [rfff (rfn [[ret best] x] (prl! "rfff" ret best x)
               (if (identical? best red/sentinel)
                   [(rf ret x) x]
                   (let [vret (kf best) vx (kf x)]
                     (cond (=     vret vx) [(rf ret x)     x]
                           (compf vret vx) [ret                best]
                           :else           [(rf (initf) x) x]))))]
        (aritoid (fn [] (prl! "init") [(?transient! (initf)) red/sentinel])
                 (fn [[ret _]] (prl! "finish") (?persistent! ret))
                 rfff
                 rfff)))))



(->> (quantum.core.reducers.reduce/reducer [9 3 2 1 2 395 2 3 1 29 1 2 38]
       (gen-comp-keys-into:xf vector core/> identity))
     (red/map+ (fn [x] (prl! x) (inc x)))
     (reduce conj! [(?transient! (vector)) red/sentinel])))



(defn gen-predicate ; TODO move to pcc.numeric?
  "Generates a predicate-like function which has pair and variable arities."
  [pred]
  (fn ([a b] (pred a b))
      ([a b & more] (and (pred a b) (every-pair? pred more)))))

(defn gen-operator ; TODO move to pcc.numeric?
  "Generates an operator-like function which has zero (base), one (identity),
   two (pair), and variable arities."
  [f0 f1 f2]
  (fn ([] (f0))
      ([a] (f1 a))
      ([a b] (f2 a b))
      ([a b & more] (reduce f2 (f2 a b) more))))

https://github.com/cognitect-labs/onto
The core premise is that entities can be found to belong to one or more classes. This is different than OO design, where you prescribe class membership at creation time. Here an entity may gain or lose classes by virtue of properties and values that are attached to it.

TODO look at AtomicLong.accumulateAndGet(long x, LongBinaryOperator accumulatorFunction)
TODO revisit whether transformers are necessary compared with transducers

http://www.kdgregory.com/?page=java.byteBuffer — useful reading about off-heap memory
https://www.akkadia.org/drepper/cpumemory.pdf — what every programmer should know about memory
https://mechanical-sympathy.blogspot.com/2012/10/compact-off-heap-structurestuples-in.html — compact off-heap structures and tuples
