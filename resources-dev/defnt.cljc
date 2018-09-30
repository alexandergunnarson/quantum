Note that for anything built-in js/<whatever>, the `t/isa?` predicates might need some special help

;; TO MOVE

#?(:clj  (def thread?       (isa? java.lang.Thread)))

#?(:clj  (def class?           (isa? java.lang.Class)))

;; TODO for CLJS based on untyped impl
#?(:clj  (def protocol?        (>expr (ufn/fn-> :on-interface class?))))


;; ===== quantum.core.system

#?(:clj
(defnt pid [> (? t/string?)]
  (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
       (.getName))))

;; TODO TYPED
(defalias u/*registered-components)

;; ===== UNKNOWN ===== ;;

(defnt >sentinel [> t/object?] #?(:clj (Object.) :cljs #js {}))
(defalias >object >sentinel)

;; TODO TYPED
#?(:clj
(defmacro with
  "Evaluates @expr, then @body, then returns @expr.
   For (side) effects."
  [expr #_t/form?, & body #_(? (t/seq-of t/form?))]
  `(let [expr# ~expr] ~@body expr#)))






;; Truncation is different from safe coercion
`>integer` is for e.g.:
- truncation e.g. js/Math.trunc

>boolean is different than `truthy?`

Sometimes you want (byte <whatever-double>) to fail at runtime rather than fail at runtime when you can't know everything about the input's range

These two should be defined in the (whatever) data namespace:
- `>(whatever)`
- `(whatever)>`

TODO:
- `(or (and pred then) (and (not pred) else))` (which is not correct)
- needs to equal `(t/and (t/or (t/not a) b) (t/or a c))` (which is correct)
- `(- (or ?!+vector? !vector? #?(:clj !!vector?)) (isa? clojure.lang.Counted))` is not right

#_"
Note that `;; TODO TYPED` is the annotation we're using for this initiative

- TODO implement the following:
  ;; TODO test the new analyze-seq|new!!!
  [1 .] This is accepted by the type system without knowing the type:
      (java.math.BigInteger. 1 (-> (ByteBuffer/allocate (int 8)) (.putLong x) .array))

      So, constructors need the same kind of lookup that dot calls have
  [2 .] t/type
      - dependent types: `[x arr/array? > (t/type x)]`
  [3] t/value-of
    - `[x with-metable?, meta' meta? > (t/* with-metable?) #_(TODO TYPED (t/value-of x))]`
  [4] - t/input-type
      - `(t/input-type >namespace :?)` meaning the possible input types to the first input to `>namespace`
      - `(t/input-type reduce :_ :_ :?)`
      - Then if those fns ever get extended then it should trigger a chain-reaction of recompilations
  [5] - No trailing `>` means `> ?`
      - ? : type inference
        - use logic programming and variable unification e.g. `?1` `?2` ?
        - For this situation: `?` is `(t/- <whatever-deduced-type> dc/counted?)`
          ([n dnum/std-integer?, xs dc/counted?] (count xs))
          ([n dnum/std-integer?, xs ?] ...)
  - (comp/t== x)
     - dependent type such that the passed input must be identical to x
  - Analysis
    - Better analysis of compound literals
      - Literal vectors need to be analyzed — (t/finite-of t/built-in-vector? a-type b-type ...)
      - Literal sets need to be analyzed — (t/finite-of t/built-in-set? a-type b-type ...)
      - Literal maps need to be better analyzed — (t/finite-of t/built-in-map?  [ak-type av-type] ...)
      - Literal seqs need to be better analyzed — (t/finite-of t/built-in-list? [ak-type av-type] ...)
    - (if (dcoll/reduced? ret)
          ;; TODO TYPED `(ref/deref ret)` should realize it's dealing with a `reduced?`
          (ref/deref ret)
          ...)
    - (let [ct (count arr)]
        (loop [i 0 v init]
          (if (comp/< i ct)
              (let [ret (f v (get arr i))]
                (if (reduced? ret)
                    @ret
                    ;; TODO TYPED automatically figure out that `inc` will never go out of bounds here
                    (recur (inc* i) ret)))
              v)))
    - (let [xs' (seq xs)]
        (if (dcomp/== (class xs') (class xs))
            (reduce-seq rf ret xs')
            ;; TODO TYPED automatically figure out that:
            ;; - `(not (dcomp/== (class xs') (class xs)))`
            ;; - What the possible types of xs' are as a result
            (reduce rf init xs')))
    - ([rf rf?, init t/any?, xs #?(:clj  (t/isa? clojure.core.protocols/CollReduce)
                                   :cljs (t/isa?|direct cljs.core/IReduce))]
        ;; TODO add `^not-native` to `xs` for CLJS
        (#?(:clj  clojure.core.protocols/coll-reduce
            :cljs cljs.core/-reduce) xs rf init))
    - (if (A) ...) should be (if ^boolean (A) ...) if A returns a `p/boolean?`
  - t/- : fix
    - (t/- (t/isa? java.util.Queue) (t/or ?!+queue? !!queue?))
  - t/numerically : e.g. a double representing exactly what a float is able to represent
    - and variants thereof: `numerically-long?` etc.
    - t/numerically-integer?
  - We should not rely on the value of dynamic vars e.g. `*math-context*` unless specifically typed
    - We'll have to make a special class or *something* like that to ensure that typed bindings are only
      bound within typed contexts.
  - t/extend-defn!
  - dc/of
    - (dc/of number?) ; implicitly the container is a `reducible?`
    - (dc/of map/+map? symbol? dstr/string?)
    - (dc/of t/seq? namespace?)
    - dc/map-of
    - dc/seq-of
  - t/defrecord
  - t/def-concrete-type (i.e. `t/deftype`)
  - expressions (`quantum.untyped.core.analyze.expr`)
  - comparison of `t/fn`s is probably possible?
  - t/def
    - TODO what would this even look like?
  - t/fnt (t/fn; current t/fn might transition to t/fn-spec or whatever?)
  - t/ftype
    - conditionally optional arities etc.
  - t/declare
  - declare-fnt (a way to do protocols/interfaces)
    - extend-fnt!
  - defnt (t/defn)
    - Arity elision: if any type in an arity is `t/none?` then elide it and emit a warning
      - `([x bigint?] x)`
    - t/defn-
    - (t/and (t/or a b) c) should -> (t/or (t/and a c) (t/and b c)) for purposes of separating dispatches
    - t/extend-defn!
      - `(t/extend-defn! id/>name (^:inline [x namespace?] (-> x .getName id/>name)))`
    - ^:inline
      - if you do (Numeric/bitAnd a b) inline then bitAnd needs to know the primitive type so maybe
        we do the `let*`-binding approach to typing vars?
      - should be able to be per-arity like so:
        (^:inline [] ...)
      - ^:inline set on a function should propagate to all overloads, including ones added after the fact
      - A good example of inlining:
        (t/def empty?|rf
          (fn/aritoid
            (t/fn [] true)
            fn/identity
            (t/fn [ret _, x _]      (dc/reduced false))
            (t/fn [ret _, k _, v _] (dc/reduced false))))
        (t/defn empty? > p/boolean?
          ([x p/nil?] true)
          ([xs dc/counted?] (-> xs count num/zero?))
          ([xs (t/input-type educe :_ :_ :?)] (educe empty?|rf x)))
    - handle varargs
      - [& args _] shouldn't result in `t/any?` but rather like `t/reducible?` or whatever
    - do the defnt-equivalences
    - a linting warning that you can narrow the type to whatever the deduced type is from whatever
      wider declared type there is
    - the option of creating a `defnt` that isn't extensible? Or at least in which the input types are limited in the same way per-overload output types are limited by the per-fn output type?
    - dealing with `apply`...
  - t/defmacro
  - t/deftype
  - t/dotyped
  - lazy compilation especially around `t/input-type`
  - equivalence of typed predicates (i.e. that which is `t/<=` `(t/fn [x t/any? :> p/boolean?])`)
    to types:
    - [xs (t/fn [x (t/isa? clojure.lang.Range)] ...)]
  - No return value means that it should infer
- NOTE on namespace organization:
  - [quantum.untyped.core.ns :refer [namespace?]]
    instead of
    [quantum.untyped.core.type.predicates :refer [namespace?]]
    because not all predicates (type-related or otherwise) can be thought of ahead of time to be put
    in one giant namespace
  - Same with the `core.convert` namespace too
    - Conversion functions belong in the namespace that their destination types belong in
- TODO transition the quantum.core.* namespaces:
  ->>>>>> TODO need to add *all* quantum namespaces in here
  - Legend:
    [.] : in progress
    [-] : done as far as possible but not truly complete
    [x] : actually done
    [|] : not possible / N/A
    [!] : refused
  - List of semi-approximately topologically ordered namespaces to make typed:
    - [.] clojure.core / cljs.core (note that many things unexpectedly have associated macros)
          - [! !] ..
          - [. .] <
          - [. .] <=
          - [. .] = — look at coercive-=
          - [. .] ==
          - [. .] >
          - [. .] >=
          - [. .] +
          - [. .] +'
          - [. .] -
          - [. .] -'
          - [! !] ->
          - [! !] ->>
          - [. .] *
          - [. .] *'
          - [. .] /
          - [! |] accessor
          - [x  ] aclone
          - [   ] add-tap
          - [   ] add-watch
          - [  |] agent
          - [   ] agent-error
          - [   ] aget — TODO check out unchecked-aget, checked-aget, checked-aget' and CLJS macro
          - [x x] alength
          - [   ] alias
          - [   ] all-ns
          - [   ] alter
          - [   ] alter-meta!
          - [   ] alter-var-root
          - [   ] amap
          - [   ] ancestors
          - [   ] and — NOTE that CLJS macro has some secrets
          - [   ] any?
          - [   ] apply
          - [   ] areduce
          - [|  ] array
          - [| .] array? — TODO also look at goog/isArrayLike
          - [|  ] array-chunk
          - [|  ] array-copy
          - [|  ] array-copy-downward
          - [|  ] array-index-of
          - [|  ] array-iter
          - [| !] array-list
          - [   ] array-map
          - [|  ] array-seq
          - [! !] as->
          - [   ] aset — TODO check out unchecked-aset, checked-aset, checked-aset' and CLJS macro
          - [   ] aset-boolean
          - [   ] aset-byte
          - [   ] aset-char
          - [   ] aset-double
          - [   ] aset-float
          - [   ] aset-int
          - [   ] aset-long
          - [   ] aset-short
          - [   ] assert
          - [! |] assert-args
          - [   ] assoc
          - [   ] assoc!
          - [   ] assoc-in
          - [x x] associative?
          - [   ] atom
          - [   ] await
          - [   ] await1
          - [   ] await-for
          - [   ] bases
          - [   ] bigdec
          - [   ] bigint
          - [   ] biginteger
          - [   ] binding
          - [   ] binding-conveyor-fn
          - [. .] bit-and
          - [. .] bit-and-not
          - [x .] bit-clear
          - [|  ] bit-count
          - [x .] bit-flip
          - [x .] bit-not
          - [. .] bit-or
          - [x .] bit-set
          - [x .] bit-shift-left
          - [x .] bit-shift-right
          - [|  ] bit-shift-right-zero-fill
          - [x .] bit-test
          - [. .] bit-xor
          - [x .] boolean
          - [x x] boolean?
          - [   ] boolean-array
          - [   ] booleans
          - [   ] bound?
          - [   ] bound-fn
          - [   ] bound-fn*
          - [x x] bounded-count
          - [   ] butlast
          - [x .] byte
          - [x x] byte?
          - [   ] byte-array
          - [   ] bytes
          - [   ] bytes?
          - [   ] case
          - [  |] cast
          - [   ] cat
          - [x .] char — TODO (.fromCharCode js/String <number>) might be useful
          - [x x] char?
          - [   ] char-array
          - [   ] chars
          - [! |] check-valid-options
          - [x x] chunk
          - [x x] chunk-append
          - [x x] chunk-buffer
          - [x x] chunk-cons
          - [x x] chunk-first
          - [x x] chunk-next
          - [x x] chunk-rest
          - [x x] chunked-seq?
          - [|  ] chunkIteratorSeq
          - [  |] class
          - [x |] class?
          - [|  ] clj->js
          - [! |] clojure-version
          - [|  ] clone
          - [|  ] cloneable?
          - [|  ] coercive-=
          - [|  ] coercive-not
          - [|  ] coercive-not=
          - [   ] coll?
          - [   ] commute
          - [   ] comp
          - [. .] comparator
          - [x x] compare
          - [   ] compare-and-set!
          - [|  ] compare-indexed
          - [   ] compile
          - [   ] complement
          - [   ] completing
          - [   ] concat
          - [   ] cond
          - [! !] cond->
          - [! !] cond->>
          - [   ] condp
          - [   ] conj
          - [   ] conj!
          - [x  ] cons
          - [   ] constantly
          - [x  ] contains?
          - [|  ] copy-arguments
          - [x x] count
          - [x x] counted?
          - [   ] create-ns
          - [! !] create-struct
          - [   ] cycle
          - [x  ] dec
          - [x  ] dec'
          - [   ] declare
          - [x |] decimal?
          - [   ] dedupe
          - [|  ] default-dispatch-val
          - [! |] definline
          - [   ] defmacro
          - [! !] defmethod — rejected because t/defn supersedes
          - [! !] defmulti — rejected because t/defn supersedes
          - [. .] defn
          - [. .] defn-
          - [   ] defonce
          - [! !] defprotocol
          - [   ] defrecord
          - [! !] defstruct
          - [   ] deftype
          - [   ] delay
          - [x x] delay?
          - [   ] deliver
          - [|  ] demunge
          - [x |] denominator
          - [   ] deref
          - [   ] derive
          - [   ] descendants
          - [   ] destructure
          - [   ] disj
          - [   ] disj!
          - [|  ] dispatch-fn
          - [   ] dissoc
          - [   ] dissoc!
          - [   ] distinct
          - [   ] distinct?
          - [   ] doall
          - [   ] dorun
          - [   ] doseq
          - [   ] dosync
          - [   ] dotimes
          - [   ] doto
          - [x .] double
          - [x x] double?
          - [   ] double-array
          - [   ] doubles
          - [   ] drop
          - [   ] drop-last
          - [   ] drop-while
          - [   ] eduction
          - [  |] elide-top-frames
          - [   ] empty
          - [. .] empty?
          - [|  ] enable-console-print!
          - [   ] enumeration-seq
          - [   ] ensure
          - [   ] ensure-reduced
          - [|  ] equiv-map
          - [|  ] equiv-sequential
          - [   ] error-handler
          - [   ] error-mode
          - [|  ] es6-entries-iterator
          - [| !] es6-iterable
          - [|  ] es6-iterator
          - [|  ] es6-set-entries-iterator
          - [   ] eval
          - [   ] even?
          - [   ] every?
          - [   ] every-pred
          - [|  ] ex-cause
          - [   ] ex-data
          - [   ] ex-info
          - [|  ] ex-message
          - [|  ] exists?
          - [|  ] extend-object!
          - [! !] extend-protocol
          - [! !] extend-type
          - [x x] false?
          - [   ] file-seq
          - [   ] filter
          - [! |] filter-key
          - [   ] filterv
          - [x  ] find
          - [  |] find-keyword
          - [|  ] find-macros-ns
          - [x  ] find-ns
          - [|  ] find-ns-obj
          - [  |] find-var
          - [|  ] fix
          - [   ] ffirst
          - [x  ] first
          - [   ] flatten
          - [|  ] flatten1
          - [x .] float
          - [x x] float?
          - [   ] float-array
          - [   ] floats
          - [   ] flush
          - [. .] fn
          - [x x] fn?
          - [|  ] fn->comparator
          - [   ] fnext
          - [   ] fnil
          - [   ] for
          - [   ] force
          - [   ] format
          - [   ] frequencies
          - [   ] future
          - [   ] future?
          - [   ] future-call
          - [   ] future-cancel
          - [   ] future-cancelled?
          - [   ] future-done?
          - [|  ] gen-apply-to
          - [|  ] gen-apply-to-simple
          - [   ] gensym
          - [x  ] get
          - [   ] get-in
          - [   ] get-method
          - [   ] get-thread-bindings
          - [   ] get-validator
          - [   ] group-by
          - [   ] halt-when
          - [   ] hash
          - [|  ] hash-coll
          - [|  ] hash-combine
          - [|  ] hash-imap
          - [|  ] hash-iset
          - [|  ] hash-keyword
          - [   ] hash-map
          - [   ] hash-ordered-coll
          - [   ] hash-unordered-coll
          - [   ] hash-set
          - [|  ] hash-string*
          - [|  ] hash-string
          - [x x] ident?
          - [x .] identical? — NOTE CLJS has macro
          - [x x] identity
          - [   ] if-let
          - [   ] if-not (not as performant as we thought)
          - [   ] if-some
          - [|  ] ifind?
          - [x x] ifn?
          - [| !] implements?
          - [   ] import
          - [|  ] imul
          - [x  ] inc
          - [x  ] inc'
          - [x x] indexed?
          - [|  ] infinite?
          - [   ] inst?
          - [   ] inst-ms
          - [   ] instance? — NOTE CLJS has macro
          - [x .] int
          - [x x] int?
          - [   ] int-array
          - [|  ] int-rotate-left
          - [   ] intern
          - [   ] into
          - [   ] ints
          - [x x] integer?
          - [   ] interleave
          - [   ] interpose
          - [   ] into-array
          - [  |] is-annotation?
          - [  |] is-runtime-annotation?
          - [   ] isa?
          - [|  ] iter
          - [| x] iterable?
          - [   ] iterate
          - [   ] iterator-seq
          - [   ] io!
          - [|  ] js-arguments
          - [|  ] js-comment
          - [|  ] js-debugger
          - [|  ] js-delete
          - [|  ] js-in
          - [|  ] js-inline-comment
          - [|  ] js-invoke
          - [|  ] js-keys
          - [|  ] js-mod
          - [|  ] js-obj
          - [|  ] js-reserved-arr
          - [|  ] js-str
          - [|  ] js->clj
          - [   ] juxt
          - [   ] keep
          - [   ] keep-indexed
          - [   ] key
          - [|  ] key->js
          - [x  ] keys
          - [x x] keyword
          - [x x] keyword?
          - [|  ] keyword-identical?
          - [   ] last
          - [   ] lazy-cat
          - [   ] lazy-seq
          - [   ] let
          - [! !] letfn — we just don't use it very much
          - [   ] line-seq
          - [   ] list
          - [   ] list*
          - [x x] list?
          - [   ] load
          - [   ] load-reader
          - [   ] load-string
          - [   ] loaded-libs
          - [  |] locking
          - [x .] long
          - [x x] long?
          - [   ] long-array
          - [   ] longs
          - [   ] loop
          - [|  ] m3-seed
          - [|  ] m3-C1
          - [|  ] m3-C2
          - [|  ] m3-mix-K1
          - [|  ] m3-mix-H1
          - [|  ] m3-fmix
          - [|  ] m3-hash-int
          - [|  ] m3-hash-unencoded-chars
          - [   ] macroexpand
          - [   ] macroexpand-1
          - [   ] make-array
          - [   ] make-hierarchy
          - [   ] map
          - [x x] map?
          - [x x] map-entry?
          - [   ] map-indexed
          - [   ] mapcat
          - [   ] mapv
          - [   ] max
          - [   ] max-key
          - [   ] memfn
          - [   ] memoize
          - [   ] merge
          - [   ] merge-with
          - [x x] meta
          - [   ] methods
          - [   ] min
          - [   ] min-key
          - [   ] mix-collection-hash
          - [|  ] mk-bound-fn
          - [   ] mod
          - [|  ] munge
          - [x x] name
          - [x x] namespace
          - [x  ] namespace?
          - [! |] nary-inline
          - [   ] nat-int?
          - [   ] neg?
          - [   ] neg-int?
          - [   ] newline
          - [x  ] next
          - [   ] nfirst
          - [x .] nil? — NOTE `nil?` macro in CLJS has some secrets
          - [|  ] nil-iter
          - [   ] nnext
          - [   ] not — look at `coercive-not`
          - [   ] not-any?
          - [   ] not-empty
          - [   ] not-every?
          - [x .] not= — look at `coercive-not=`
          - [   ] ns
          - [x |] ns-aliases
          - [x |] ns-imports
          - [x  ] ns-interns
          - [x |] ns-map
          - [x  ] ns-name
          - [x |] ns-publics
          - [x |] ns-refers
          - [   ] ns-resolve
          - [  |] ns-unalias
          - [x |] ns-unmap
          - [x  ] nth
          - [   ] nthnext
          - [   ] nthrest
          - [   ] num
          - [x x] number?
          - [x |] numerator
          - [|  ] obj-map
          - [| x] object?
          - [x  ] object-array
          - [   ] odd?
          - [   ] or — NOTE that CLJS macro has some secrets
          - [   ] parents
          - [   ] partial
          - [   ] partition
          - [   ] partition-all
          - [   ] partition-by
          - [   ] pcalls
          - [x  ] peek
          - [   ] persistent!
          - [|  ] persistent-array-map-seq
          - [   ] pmap
          - [x  ] pop
          - [   ] pop!
          - [   ] pop-thread-bindings
          - [   ] pos?
          - [   ] pos-int?
          - [   ] pr
          - [   ] pr-on
          - [| !] pr-seq-writer
          - [| !] pr-sequential-writer
          - [   ] pr-str
          - [|  ] pr-str*
          - [|  ] pr-str-with-opts
          - [   ] prefer-method
          - [   ] prefers
          - [   ] preserving-reduced
          - [|  ] prn-str-with-opts
          - [|  ] prim-seq
          - [   ] print
          - [! |] print-dup
          - [| !] print-meta?
          - [! |] print-method
          - [| !] print-prefix-map
          - [   ] print-str
          - [   ] printf
          - [   ] println
          - [   ] println-str
          - [   ] prn
          - [   ] prn-str
          - [   ] promise
          - [   ] push-thread-bindings
          - [   ] pvalues
          - [x x] qualified-ident?
          - [x x] qualified-keyword?
          - [x x] qualified-symbol?
          - [   ] quot
          - [|  ] quote-string
          - [   ] rand
          - [   ] rand-int
          - [   ] rand-nth
          - [   ] random-sample
          - [|  ] random-uuid
          - [   ] range
          - [|  ] ranged-iterator
          - [x |] ratio?
          - [   ] rational?
          - [   ] rationalize
          - [   ] re-find
          - [   ] re-groups
          - [   ] re-matcher
          - [   ] re-matches
          - [   ] re-pattern
          - [   ] re-seq
          - [   ] read
          - [   ] read-line
          - [   ] read-string
          - [   ] read+string
          - [   ] reader-conditional
          - [   ] reader-conditional?
          - [   ] realized?
          - [x x] record?
          - [x x] reduce
          - [x x] reduce-kv
          - [|  ] reduceable?
          - [x x] reduced
          - [x x] reduced?
          - [! |] reduce1
          - [   ] reductions
          - [   ] ref
          - [   ] ref-history-count
          - [   ] ref-min-history
          - [   ] ref-max-history
          - [   ] ref-set
          - [   ] refer
          - [   ] refer-clojure
          - [| x] regexp?
          - [|  ] reify
          - [   ] release-pending-sends
          - [   ] rem
          - [   ] remove
          - [   ] remove-all-methods
          - [   ] remove-method
          - [  |] remove-ns
          - [   ] remove-tap
          - [   ] remove-watch
          - [   ] repeat
          - [   ] repeatedly
          - [   ] replace
          - [   ] require
          - [| !] require-macros
          - [   ] reset!
          - [   ] reset-meta!
          - [   ] reset-vals!
          - [   ] resolve
          - [x  ] rest
          - [   ] restart-agent
          - [   ] resultset-seq
          - [   ] reverse
          - [   ] reversible?
          - [   ] rseq
          - [   ] rsubseq
          - [   ] run!
          - [! !] satisfies?
          - [   ] second
          - [   ] select-keys
          - [   ] send
          - [   ] send-off
          - [   ] send-via
          - [x x] seq
          - [x x] seq?
          - [|  ] seq-iter
          - [   ] seqable?
          - [   ] seque
          - [   ] sequence
          - [   ] sequential?
          - [   ] set
          - [x x] set?
          - [   ] set-agent-send-executor!
          - [   ] set-agent-send-off-executor!
          - [   ] set-error-handler!
          - [   ] set-error-mode!
          - [|  ] set-from-indexed-seq
          - [|  ] set-print-err-fn!
          - [|  ] set-print-fn!
          - [   ] set-validator!
          - [   ] setup-reference
          - [x .] short
          - [x x] short?
          - [   ] short-array
          - [   ] shorts
          - [   ] shuffle
          - [   ] shutdown-agents
          - [| !] simple-benchmark
          - [x x] simple-ident?
          - [x x] simple-keyword?
          - [x x] simple-symbol?
          - [   ] slurp
          - [   ] some
          - [x x] some?
          - [   ] some->
          - [   ] some->>
          - [   ] some-fn
          - [   ] sort
          - [   ] sort-by
          - [x x] sorted?
          - [   ] sorted-map
          - [   ] sorted-map-by
          - [   ] sorted-set
          - [   ] sorted-set-by
          - [   ] special-symbol?
          - [|  ] specify
          - [|  ] specify!
          - [   ] spread
          - [   ] spit
          - [   ] split-at
          - [   ] split-with
          - [   ] spread
          - [. .] str
          - [x x] string?
          - [|  ] string-iter
          - [| !] string-print
          - [|  ] strip-ns
          - [! |] struct
          - [! |] struct-map
          - [   ] subs
          - [   ] subseq
          - [   ] subvec
          - [   ] supers
          - [   ] swap!
          - [   ] swap-vals!
          - [x x] symbol
          - [x x] symbol?
          - [|  ] symbol-identical?
          - [   ] sync
          - [|  ] system-time
          - [   ] tagged-literal
          - [   ] tagged-literal?
          - [   ] take
          - [   ] take-last
          - [   ] take-nth
          - [   ] take-while
          - [   ] tap>
          - [   ] test
          - [x |] the-ns
          - [| !] this-as
          - [   ] thread-bound?
          - [   ] throw-if
          - [   ] time
          - [x  ] to-array
          - [   ] to-array-2d
          - [   ] trampoline
          - [. .] transduce
          - [|  ] transformer-iterator
          - [   ] transient
          - [   ] tree-seq
          - [x x] true?
          - [   ] type
          - [|  ] type->str
          - [x  ] unchecked-add
          - [x  ] unchecked-add-int
          - [x .] unchecked-byte
          - [x .] unchecked-char
          - [x  ] unchecked-dec
          - [x  ] unchecked-dec-int
          - [x  ] unchecked-divide
          - [x  ] unchecked-divide-int
          - [x .] unchecked-double
          - [x .] unchecked-float
          - [x  ] unchecked-inc
          - [x  ] unchecked-inc-int
          - [x .] unchecked-int
          - [x .] unchecked-long
          - [x  ] unchecked-multiply
          - [x  ] unchecked-multiply-int
          - [x  ] unchecked-negate
          - [x  ] unchecked-negate-int
          - [x  ] unchecked-remainder-int
          - [x .] unchecked-short
          - [x  ] unchecked-subtract
          - [x  ] unchecked-subtract-int
          - [|  ] undefined? — NOTE has macro too
          - [   ] underive
          - [   ] unreduced
          - [| !] unsafe-cast
          - [x .] unsigned-bit-shift-right
          - [   ] update
          - [   ] update-in
          - [   ] uri?
          - [! !] use
          - [| !] use-macros
          - [|  ] uuid
          - [x x] uuid?
          - [   ] val
          - [x  ] vals
          - [x x] var?
          - [   ] var-get
          - [   ] var-set
          - [. .] vary-meta
          - [   ] vec
          - [   ] vector
          - [x x] vector?
          - [x x] volatile?
          - [   ] volatile!
          - [   ] vreset!
          - [   ] vswap!
          - [! !] when
          - [   ] when-first
          - [   ] when-let
          - [! !] when-not
          - [   ] when-some
          - [! !] while
          - [   ] with-bindings
          - [   ] with-bindings*
          - [   ] with-in-str
          - [   ] with-loading-context
          - [   ] with-local-vars
          - [x x] with-meta
          - [   ] with-open
          - [   ] with-out-str
          - [   ] with-precision
          - [   ] with-redefs
          - [   ] with-redefs-fn
          - [| !] write-all
          - [   ] xml-seq
          - [x  ] zero?
          - [   ] zipmap
    - [.] Intrinsics
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Intrinsics.java
          - [ ] Numbers.add(double,double)
          - [ ] Numbers.and(long,long)
          - [ ] Numbers.divide(double,double)
          - [ ] Numbers.equiv(double,double)
          - [ ] Numbers.equiv(long,long)
          - [ ] Numbers.gt(long,long)
          - [ ] Numbers.gt(double,double)
          - [ ] Numbers.gte(long,long)
          - [ ] Numbers.gte(double,double)
          - [ ] Numbers.isPos(long)
          - [ ] Numbers.isPos(double)
          - [ ] Numbers.isNeg(long)
          - [ ] Numbers.isNeg(double)
          - [ ] Numbers.isZero(double)
          - [ ] Numbers.isZero(long)
          - [ ] Numbers.lt(long,long)
          - [ ] Numbers.lt(double,double)
          - [ ] Numbers.lte(long,long)
          - [ ] Numbers.lte(double,double)
          - [ ] Numbers.multiply(double,double)
          - [ ] Numbers.or(long,long)
          - [ ] Numbers.xor(long,long)
          - [ ] Numbers.remainder(long,long)
          - [ ] Numbers.shiftLeft(long,long)
          - [ ] Numbers.shiftRight(long,long)
          - [ ] Numbers.unsignedShiftRight(long,long)
          - [ ] Numbers.minus(double)
          - [ ] Numbers.minus(double,double)
          - [ ] Numbers.inc(double)
          - [ ] Numbers.dec(double)
          - [ ] Numbers.quotient(long,long)
          - [ ] Numbers.shiftLeftInt(int,int)
          - [ ] Numbers.shiftRightInt(int,int)
          - [ ] Numbers.unsignedShiftRightInt(int,int)
          - [ ] Numbers.unchecked_int_add(int,int)
          - [ ] Numbers.unchecked_int_subtract(int,int)
          - [ ] Numbers.unchecked_int_negate(int)
          - [ ] Numbers.unchecked_int_inc(int)
          - [ ] Numbers.unchecked_int_dec(int)
          - [ ] Numbers.unchecked_int_multiply(int,int)
          - [ ] Numbers.unchecked_int_divide(int,int)
          - [ ] Numbers.unchecked_int_remainder(int,int)
          - [ ] Numbers.unchecked_add(long,long)
          - [ ] Numbers.unchecked_add(double,double)
          - [ ] Numbers.unchecked_minus(long)
          - [ ] Numbers.unchecked_minus(double)
          - [ ] Numbers.unchecked_minus(double,double)
          - [ ] Numbers.unchecked_minus(long,long)
          - [ ] Numbers.unchecked_multiply(long,long)
          - [ ] Numbers.unchecked_multiply(double,double)
          - [ ] Numbers.unchecked_inc(double)
          - [ ] Numbers.unchecked_inc(long)
          - [ ] Numbers.unchecked_dec(double)
          - [ ] Numbers.unchecked_dec(long)
          - [ ] RT.aget(short[],int)
          - [ ] RT.aget(float[],int)
          - [ ] RT.aget(double[],int)
          - [ ] RT.aget(int[],int)
          - [ ] RT.aget(long[],int)
          - [ ] RT.aget(char[],int)
          - [ ] RT.aget(byte[],int)
          - [ ] RT.aget(boolean[],int)
          - [ ] RT.aget(java.lang.Object[],int)
          - [ ] RT.alength(int[])
          - [ ] RT.alength(long[])
          - [ ] RT.alength(char[])
          - [ ] RT.alength(java.lang.Object[])
          - [ ] RT.alength(byte[])
          - [ ] RT.alength(float[])
          - [ ] RT.alength(short[])
          - [ ] RT.alength(boolean[])
          - [ ] RT.alength(double[])
          - [ ] RT.doubleCast(long)
          - [ ] RT.doubleCast(double)
          - [ ] RT.doubleCast(float)
          - [ ] RT.doubleCast(int)
          - [ ] RT.doubleCast(short)
          - [ ] RT.doubleCast(byte)
          - [ ] RT.uncheckedDoubleCast(double)
          - [ ] RT.uncheckedDoubleCast(float)
          - [ ] RT.uncheckedDoubleCast(long)
          - [ ] RT.uncheckedDoubleCast(int)
          - [ ] RT.uncheckedDoubleCast(short)
          - [ ] RT.uncheckedDoubleCast(byte)
          - [ ] RT.longCast(long)
          - [ ] RT.longCast(short)
          - [ ] RT.longCast(byte)
          - [ ] RT.longCast(int)
          - [ ] RT.uncheckedIntCast(long)
          - [ ] RT.uncheckedIntCast(double)
          - [ ] RT.uncheckedIntCast(byte)
          - [ ] RT.uncheckedIntCast(short)
          - [ ] RT.uncheckedIntCast(char)
          - [ ] RT.uncheckedIntCast(int)
          - [ ] RT.uncheckedIntCast(float)
          - [ ] RT.uncheckedLongCast(short)
          - [ ] RT.uncheckedLongCast(float)
          - [ ] RT.uncheckedLongCast(double)
          - [ ] RT.uncheckedLongCast(byte)
          - [ ] RT.uncheckedLongCast(long)
          - [ ] RT.uncheckedLongCast(int)
          - [ ] Util.equiv(long,long)
          - [ ] Util.equiv(boolean,boolean)
          - [ ] Util.equiv(double,double)
    - [ ] Java intrinsics
    - [.] clojure.lang.RT
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
          - [ ] aclone
          - [ ] addURL
          - [ ] aget
          - [.] alength
          - [ ] arrayToList
          - [ ] aset
          - [ ] assoc
          - [ ] assocN
          - [ ] baseLoader
          - [.] booleanCast
          - [x] boundedLength
          - [x] box
          - [.] byteCast
          - [ ] canSeq
          - [.] charCast
          - [!] chunkIteratorSeq
          - [ ] classForName
          - [ ] classForNameNonLoading
          - [!] compile
          - [ ] conj
          - [ ] cons
          - [ ] contains
          - [x] count
          - [x] countFrom
          - [ ] dissoc
          - [ ] doFormat
          - [.] doubleCast
          - [!] errPrintWriter
          - [ ] find
          - [ ] findKey
          - [ ] first
          - [.] floatCast
          - [ ] format
          - [ ] formatAesthetic
          - [ ] formatStandard
          - [!] fourth
          - [ ] get
          - [ ] getColumnNumber
          - [ ] getFrom
          - [ ] getLineNumber
          - [ ] getLineNumberingReader
          - [ ] getResource
          - [ ] hasTag
          - [!] init
          - [.] intCast
          - [ ] isLineNumberingReader
          - [x] isReduced
          - [ ] iter
          - [ ] keys
          - [x] keyword
          - [!] lastModified
          - [x] length
          - [ ] list
          - [ ] listStar
          - [!] load
          - [ ] loadClassForName
          - [!] loadLibrary
          - [!] loadResourceScript
          - [.] longCast
          - [ ] makeClassLoader
          - [ ] map
          - [ ] mapUniqueKeys
          - [!] maybeLoadResourceScript
          - [x] meta
          - [ ] more
          - [ ] nextID
          - [ ] nth
          - [ ] nthFrom
          - [ ] object_array
          - [ ] peek
          - [ ] peekChar
          - [ ] pop
          - [ ] print
          - [ ] printInnerSeq
          - [ ] printString
          - [!] processCommandLine
          - [ ] readChar
          - [ ] readString
          - [ ] resolveClassNameInContext
          - [ ] resourceAsStream
          - [ ] rest
          - [!] second
          - [x] seq
          - [x] seqFrom
          - [!] seqOrElse
          - [ ] seqToArray
          - [ ] seqToPassedArray
          - [ ] seqToTypedArray
          - [ ] set
          - [!] setValues
          - [.] shortCast
          - [ ] subvec
          - [ ] suppressRead
          - [!] third
          - [ ] toArray
          - [.] uncheckedByteCast
          - [.] uncheckedShortCast
          - [.] uncheckedCharCast
          - [.] uncheckedIntCast
          - [.] uncheckedLongCast
          - [.] uncheckedFloatCast
          - [.] uncheckedDoubleCast
          - [ ] vals
          - [!] var
          - [ ] vector
    - [.] clojure.lang.Numbers
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Numbers.java
          - [ ] add
          - [ ] addP
          - [ ] and
          - [ ] andNot
          - [ ] boolean_array
          - [ ] booleans
          - [ ] byte_array
          - [ ] bytes
          - [ ] char_array
          - [ ] chars
          - [ ] clearBit
          - [ ] compare
          - [ ] dec
          - [ ] decP
          - [ ] denominator
          - [ ] divide
          - [ ] double_array
          - [ ] doubles
          - [ ] equal
          - [ ] equiv
          - [ ] flipBit
          - [ ] float_array
          - [ ] floats
          - [ ] gt
          - [ ] gte
          - [ ] hasheq
          - [ ] hasheqFrom
          - [ ] inc
          - [ ] incP
          - [ ] int_array
          - [ ] ints
          - [ ] isNaN
          - [ ] isNeg
          - [ ] isPos
          - [ ] isZero
          - [ ] long_array
          - [ ] longs
          - [ ] lt
          - [ ] lte
          - [ ] max
          - [ ] min
          - [ ] minus
          - [ ] minusP
          - [ ] multiply
          - [ ] multiplyP
          - [ ] not
          - [ ] num
          - [x] numerator
          - [ ] or
          - [ ] quotient
          - [ ] rationalize
          - [ ] reduceBigInt
          - [ ] remainder
          - [ ] shiftLeft
          - [ ] shiftLeftInt
          - [ ] shiftRight
          - [ ] shiftRightInt
          - [ ] short_array
          - [ ] shorts
          - [ ] setBit
          - [ ] testBit
          - [ ] toBigDecimal
          - [ ] toBigInt
          - [ ] toBigInteger
          - [ ] toRatio
          - [ ] unchecked_add
          - [ ] unchecked_dec
          - [ ] unchecked_divide
          - [ ] unchecked_inc
          - [ ] unchecked_minus
          - [ ] unchecked_multiply
          - [ ] unchecked_negate
          - [ ] unchecked_remainder
          - [ ] unchecked_int_add
          - [ ] unchecked_int_dec
          - [ ] unchecked_int_divide
          - [ ] unchecked_int_inc
          - [ ] unchecked_int_multiply
          - [ ] unchecked_int_negate
          - [ ] unchecked_int_remainder
          - [ ] unchecked_int_subtract
          - [ ] unsignedShiftRight
          - [ ] unsignedShiftRightInt
          - [ ] xor
    - [.] clojure.lang.Util
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Util.java
         - [ ] classOf
         - [ ] clearCache
         - [x] compare
         - [x] equiv
         - [ ] hash
         - [ ] hashCombine
         - [ ] hasheq
         - [x] identical
         - [x] isInteger
         - [ ] isPrimitive
         - [ ] loadWithClass
         - [ ] pcequiv
         - [|] ret1
         - [|] runtimeException
         - [|] sneakyThrow
    - [.] (TEMPORARY) collections-typed
         - [ ] `get`
         - [ ] `merge`
         - [ ] `concat`
         - [ ] `repeat`
    - [ ] quantum.core.core -> TODO just need to delete this from all references
    - [ ] quantum.core.type.core
    - [x] quantum.core.data.async
    - [-] quantum.core.data.queue
    - [ ] quantum.core.type.defs
    - [.] quantum.core.refs -> quantum.core.data.refs ?
    - [ ] quantum.core.logic
          - (def nneg?    (l/fn-not neg?))
          - (def pos-int? (l/fn-and dnum/integer? pos?))
    - [.] quantum.core.fn
          - [ ] `apply`
                - especially with `t/defn` as the caller
    - [.] quantum.core.cache
    - [ ] quantum.core.type-old
    - [.] quantum.core.data.primitive
    - [.] quantum.core.data.string
          - [ ] `>str`
    - [.] quantum.core.data.map
    - [.] quantum.core.data.meta
    - [.] quantum.core.compare
          - [ ] `compare`
    - [x] quantum.core.ns ; TODO split up into data.ns?
    - [.] quantum.core.vars
    - [ ] quantum.core.print
    - [ ] quantum.core.log
    - [.] quantum.core.data.vector
    - [ ] quantum.core.spec
    - [.] quantum.core.error
    - [.] quantum.core.data.string
    - [.] quantum.core.data.array
    - [.] quantum.core.data.list
    - [.] quantum.core.data.collections
    - [.] quantum.core.data.tuple
    - [x] quantum.core.data.time
    - [.] quantum.core.compare.core
    - [.] quantum.core.data.numeric
    - [.] quantum.core.numeric.predicates
    - [.] quantum.core.numeric.convert
    - [.] quantum.core.numeric.exponents
    - [.] quantum.core.numeric.misc
    - [.] quantum.core.numeric.operators
    - [.] quantum.core.numeric.trig
    - [.] quantum.core.numeric.truncate
    - [x] quantum.core.numeric.types
    - [.] quantum.core.numeric
    - [.] quantum.core.data.set
    - [ ] quantum.core.string.regex
    - [ ] quantum.core.macros.type-hint
    - [ ] quantum.core.analyze.clojure.core
    - [ ] quantum.core.analyze.clojure.predicates
    - [ ] quantum.core.macros.optimization
    - [ ] quantum.core.macros.fn
    - [ ] quantum.core.macros.transform
    - [ ] quantum.core.macros.protocol
    - [ ] quantum.core.macros.reify
    - [ ] quantum.core.macros.defnt
    - [ ] quantum.core.macros
    - [.] quantum.core.reducers.reduce
    - [ ] quantum.core.collections.logic
    - [.] quantum.core.collections.core
    - [ ] quantum.core.form
          - [ ] `(t/def langs #{:clj :cljs :clr})`
          - [ ] `(t/def lang "The language this code is compiled under" #?(:clj :clj :cljs :cljs))`
    - [ ] quantum.core.form.generate
          - [ ] ```
                ;; TODO TYPED
                (defalias u/externs?)
                ```

    - Worked through all we can for now:
      - quantum.core.data.bits
      - quantum.core.convert.primitive
  - List of corresponding untyped namespaces to incorporate:
    - [ ] quantum.untyped.core.compare
    - [ ] quantum.untyped.core.core
    - [ ] quantum.untyped.core.ns
    - [ ] quantum.untyped.core.vars
    - [ ] quantum.untyped.core.data.map
    - [ ] quantum.untyped.core.type.defs
    - [ ] quantum.untyped.core.data
    - [ ] quantum.untyped.core.refs
    - [ ] quantum.untyped.core.data.bits
    - [x] quantum.untyped.core.identifiers
  - List of Array fns to implement:
    - [ ] count
    - [ ] get
    - [ ] set
    - [ ] new1dObjectArray
    - [ ] new1dArray
    - [ ] newUninitialized<n : 1-10>d<type>Array
    - [ ] newInitializedNd<type>Array
    - [ ] newUninitializedArrayOfType
    - [ ] newInitializedArrayOfType
  - List of Numeric fns to implement:
    - [ ] isTrue (?)
    - [ ] isFalse (?)
    - [ ] isNil (?)
    - [ ] (logical) and (?)
    - [ ] (logical) or (?)
    - [ ] (logical) not
    - [x] lt
    - [x] lte
    - [ ] gt
    - [ ] gte
    - [x] eq
    - [x] neq
    - [ ] inc
    - [ ] dec
    - [ ] isZero
    - [ ] isNeg
    - [ ] isPos
    - [x] add
    - [ ] subtract
    - [ ] negate
    - [ ] multiply
    - [ ] divide
    - [ ] max
    - [ ] min
    - [ ] rem
  - List of Primitive fns to implement:
    - uncheckedByteCast
    - uncheckedCharCast
    - uncheckedShortCast
    - uncheckedIntCast
    - uncheckedLongCast
    - uncheckedFloatCast
    - uncheckedDoubleCast
  - Standard metadata
    - e.g. `{:alternate-implementations #{'cljs.tools.reader/merge-meta}}`
    - :adapted-from <namespace-or-class-symbol>
    - :source <namespace-or-class-symbol>
    - :todo #{<todo-string>}
    - :attribution <github-username-symbol | string-description>
    - :doc <string-documentation>
    - :incorporated (t/or (t/set-of (t/or <namespace-or-class-symbol> <function-or-method-symbol>))
                          (t/map-of (t/or <namespace-or-class-symbol> <function-or-method-symbol>)
                                    date))
    - :equivalent `{(aritoid vector identity conj)
                   (fn ([]      (vector))
                       ([x0]    (identity x0))
                       ([x0 x1] (conj x0 x1)))}}
  - Instead of e.g. `ns-` or `var-` we can do `ns-val` and `var-val`
  - Should we type `when`, `let`?

- With `defnt`, protocols and interfaces aren't needed. You can just create `t/fn`s that you can
  then conform your fns to.
- `dotyped`, `defnt`, and `fnt` create typed contexts in which their internal forms are analyzed
  and overloads are resolved.
- `defnt` is intended to catch many runtime errors at compile time, but cannot catch all of them;
  types will very often have to be validated at runtime.

[ ] Compile-Time (Direct) Dispatch
    [x] Any argument, if it requires a non-nilable primitive-like value, will be marked as a
        primitive.
    [x] If nilable, there will be one overload for nil and one for primitive.
    [x] When a `fnt` with type overloads is referenced outside of a typed context, then the overload
        resolution will be done via Runtime Dispatch.
    - TODO Should we take into account 'actual' types (not just 'declared' types) when performing
      dispatch / overload resolution?
      - Let's take the example of `(defnt abcde [] (f (rand/int-between -10 -2)))`.
        - Let's say `rand/int-between`'s output is labeled `t/int?`. However, we know based on
          further static analysis of its implementation that the output is not only `t/int?` but
          also `t/neg?`, or perhaps even further, `(< -10 % -2)`.
        - In this case, should we take advantage of this knowledge?
          - Let's say we do. Then `(.invoke reify|we-know-specifics (rand/int-between -10 -2))`.
            Yay for efficiency! But let's then say we then change the implementation even if we
            don't change the 'interface'/typedefs. Now `rand/int-between` returns `(<= -10 % -2)` —
            that is, it's now numerically *inclusive* (for instance, maybe the implementation's
            previous behavior of generating numbers numerically *exclusive*ly was mistaken).
            `reify|we-know-specifics` would then still be invoked but incorrectly (and unsafely) so.
          - To be fair, we'll tend to change output specs/typedefs all the time as we do
            development. Do we need to keep track of every call site it affects and recompile
            accordingly? Perhaps. It seems like overkill though. It should be configurable in any
            case.
          - I think that because of this last point, we can and should rely on implementational
            specifics wherever available to boost performance (Maybe this should be configurable so
            it doesn't slow down development? The more we change the implementation, the more it has
            to recompile, ostensibly). We can take advantage of the output specs, certainly, if for
            nothing else than to ensure that our implementation (as characterized by its 'actual'
            output type) matches what we expect (as characterized by its 'expected'/'declared'
            output type).
          - One option (Option A) is to turn off compile-time overload resolution during
            development. This would mean it might get very slow during that time. But if it's in
            the same `defnt` (ignoring `extend-defnt!` for a minute) — like a recursive call — you
            could always leave on compile-time resolution for that.
          - Option B — probably better (though we'd still like to have all this configurable) —
            is to have each function know its dependencies (this would actually have the bonus
            property of enabling `clojure.tools.namespace.repl/refresh`-style function-level
            smart auto-recompilation which is nice). So let's go back to the previous example.
            `abcde` could keep track of (or the `defnt` ns could keep track of it, but you get the
            point) the fact that it depends on `rand/int-between` and `f`. It has a compile-time-
            resolvable call site that depends only on the output type of `rand/int-between` so if
            `rand/int-between`'s computed/actual output type (when given the inputs in question)
            ever changes, `abcde` needs to be recompiled and `abcde`'s output type recomputed. If,
            on the other hand, `f`'s output type (given the input) ever changes, `abcde` need not be
            recompiled, but rather, only its output type need be recomputed.
          - I think this reactive approach (do we need a library for that? probably not?) should
            solve our problems and let us code in a very flexible way. It'll just (currently) be a
            way that depends on a compiler in which the metalanguage and object language are
            identical.
[ ] Runtime (Dynamic) Dispatch
    [—] Protocol generation
        - For now we won't do it because we can very often find the correct overload at compile
          time. We will resort to using the `fn`.
        - It will be left as an optimization.
    [x] `fn` generation
        - Performs a worst-case linear check of the typedefs, `cond`-style.
[x] Interface generation
    [x] Even if the `defnt` is redefined, you won't have interface problems.
[ ] `reify` generation
    - Which `reify`s get generated is mainly up to the inputs but partially up to the fn body —
      If any typed fns are called in the fn body then this can change what gets generated.
      - TODO explain this more
    - Each of the `reify`s will keep their label (`__2__0` or whatever) as long as the original
      typedef of the `reify` is `t/=` to the new typedef of that reify
      - If a redefined `defnt` doesn't have that type overload then the previous reify is uninterned
        and thus made unavailable
      - That way, according to the dynamicity tests in `quantum.test.core.defnt`, we can redefine
        implementations at will as long as the specs don't change
      - To make this process faster we maintain a set of typedefs so at least cheap c/= checks can
        be performed
        - If c/= succeeds, great; the `reify` corresponding to the label (and reify-type) will be
          replaced; the typedef-set will remain unchanged
        - Else it must find a corresponding typedef by t/=
          - Then if it is found by t/= it will replace the `reify` and the typedef corresponding
            with that label and replace the typedef in the typedef-set
          - Else a new label will be given to the `reify`; the typedef will be added to the
            typedef-set
[ ] Types yielding generative specs
[—] Types using the clojure.spec interface
    - Not yet; wait for it to come out of alpha
[—] Support for compilers in which the metalanguage differs from the object language (i.e. 'normal'
    non-CLJS-in-CLJS CLJS)
    - This will have to be approached later. We'll figure it out; maybe just not yet.
"
