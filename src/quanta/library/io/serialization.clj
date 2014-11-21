(ns quanta.library.io.serialization (:gen-class))
(import
  (java.io File FileNotFoundException PushbackReader
    FileReader DataInputStream DataOutputStream IOException
    FileOutputStream BufferedOutputStream BufferedInputStream
    FileInputStream))
(require
  '[quanta.library.ns          :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(ns/nss *ns*)
(require
  '[clojure.java.io            :as clj-io                    ]
  '[quanta.library.log         :as log                       ]
  '[clojure.data.csv           :as csv                       ]
  '[quanta.library.data.array  :as arr   :refer :all         ]
  '[quanta.library.string      :as str                       ] 
  '[quanta.library.time.core   :as time                      ] 
  '[quanta.library.print       :as pr    :refer [! pr-attrs] ]
  '[quanta.library.collections :as coll  :refer :all         ]
  '[quanta.library.data.map    :as map   :refer [map-entry]  ]
  '[quanta.library.numeric     :as num   :refer [greatest-or]]
  '[quanta.library.logic                 :refer :all         ]
  '[quanta.library.type                  :refer :all         ]
  '[quanta.library.function              :refer :all         ]
  '[quanta.library.system      :as sys                       ] 
  '[quanta.library.error       :as err   :refer [try+ throw+]]
  '[taoensso.nippy             :as nippy :refer
     [read-bytes read-utf8 read-biginteger]]
  '[iota                       :as iota                      ])

(defmacro read-coll
  {:attribution "taoensso.encore"}
  [in coll]
  
  `(let [in# ~in]
    (repeatedly-into ~coll (.readInt in#)
      (trampoline thaw-from-in!* in#))))
    ; use trampoline for that
(defmacro read-kvs
  {:attribution "taoensso.encore"}
  [in coll]
  `(let [in# ~in]
    (repeatedly-into ~coll (/ (.readInt in#) 2)
    (map-entry
      (trampoline thaw-from-in!* in#)
      (trampoline thaw-from-in!* in#))))) ; use trampoline for that
(defmacro case-eval
  "Like `case` but evaluates test constants for their compile-time value."
  {:attribution "taoensso.encore"}
  [e & clauses]
  (let [;; Don't evaluate default expression!
        default (when (odd? (count clauses)) (last clauses))
        clauses (if default (butlast clauses) clauses)]
    `(case ~e
       ~@(map-indexed (fn [i# form#] (if (even? i#) (eval form#) form#))
                      clauses)
       ~(when default default))))
; custom readers are somewhat slow, comparably...
; could def constants with macros...?

(defn read-custom!
  {:trampoline true}
  [type-id in]
  (if-let [custom-reader (get @nippy/custom-readers type-id)]
    (try
      #(custom-reader in) ; this is very necessary
      (catch Exception e
        (throw
          (ex-info
            (format "Reader exception for custom type with internal id: %s"
              type-id) {:internal-type-id type-id} e))))
    (throw
      (ex-info
        (format "No reader provided for custom type with internal id: %s"
          type-id)
        {:internal-type-id type-id}))))

(defn thaw-from-in!*
  {:attribution "taoensso.nippy"
   :contributor "Alex Gunnarson"
   :trampoline true}
  [^java.io.DataInput in]
  (let [type-id (int (.readByte in))]
    (try+
      (case-eval type-id

        ; nippy/id-reader
        ; (let [edn (read-utf8 in)]
        ;   (try (edn/read-string {:readers *data-readers*} edn)
        ;        (catch Exception _ {:nippy/unthawable edn
        ;                            :type :reader})))

        nippy/id-serializable
          (let [class-name (read-utf8 in)]
            (try (let [;; .readObject _before_ Class/forName: it'll always read
                       ;; all data before throwing
                       object (.readObject (java.io.ObjectInputStream. in))
                       class ^Class (Class/forName class-name)]
                   (cast class object))
                 (catch Exception _ {:nippy/unthawable class-name
                                     :type :serializable})))

        nippy/id-bytes   (read-bytes in)
        nippy/id-nil     nil
        nippy/id-boolean (.readBoolean in)

        nippy/id-char    (.readChar in)
        nippy/id-string  (read-utf8 in)
        nippy/id-keyword (keyword (read-utf8 in))

        ;;; Optimized, common-case types (v2.6+)
        nippy/id-string-small           (String. (read-bytes in :small) "UTF-8")
        nippy/id-keyword-small (keyword (String. (read-bytes in :small) "UTF-8"))

        nippy/id-queue      #(read-coll in (PersistentQueue/EMPTY))
        nippy/id-sorted-set #(read-coll in (sorted-set))
        nippy/id-sorted-map #(read-kvs  in (sorted-map))

        nippy/id-list       (into '() (rseq (read-coll in [])))
        nippy/id-vector     #(read-coll in  [])
        nippy/id-set        #(read-coll in #{})
        nippy/id-map        #(read-kvs  in  {})
        nippy/id-seq        (or (seq (read-coll in []))
                                (lazy-seq nil)) ; Empty coll
                       
        nippy/id-meta
          (let [m (trampoline thaw-from-in!* in)]  ; trampolining
            (with-meta (trampoline thaw-from-in!* in) m)) ; trampolining

        nippy/id-byte       (.readByte  in)
        nippy/id-short      (.readShort in)
        nippy/id-integer    (.readInt   in)
        nippy/id-long       (.readLong  in)

        ;;; Optimized, common-case types (v2.6+)
        nippy/id-byte-as-long  (long (.readByte  in))
        nippy/id-short-as-long (long (.readShort in))
        nippy/id-int-as-long   (long (.readInt   in))
        ;; id-compact-long  (read-compact-long in)

        nippy/id-bigint     (bigint (read-biginteger in))
        nippy/id-biginteger (read-biginteger in)

        nippy/id-float  (.readFloat  in)
        nippy/id-double (.readDouble in)
        nippy/id-bigdec (BigDecimal. (read-biginteger in) (.readInt in))

        nippy/id-ratio (/ (bigint (read-biginteger in))
                          (bigint (read-biginteger in)))

        nippy/id-record ; 80
        (let [class    ^Class (Class/forName (read-utf8 in))
              meth-sig (into-array Class [clojure.lang.IPersistentMap])
              method   ^java.lang.reflect.Method
                       (.getMethod class "create" meth-sig)]
          (.invoke method class (into-array Object [(thaw-from-in! in)])))

        nippy/id-date  (java.util.Date. (.readLong in))
        nippy/id-uuid  (java.util.UUID. (.readLong in) (.readLong in))
        
        ; id-prefixed-custom ; Prefixed custom type
        ; (let [hash-id (.readShort in)]
        ;   (read-custom! hash-id in))

        ; Unprefixed custom type (catchall)
        #(read-custom! type-id in)
        )
      (catch Object _
        (log/error &throw-context)
        (throw+ 
          {:message (str/sp "Thaw failed against type-id:" type-id)})))))

(defn thaw-from-in! [in]
  (trampoline thaw-from-in!* in))



; RECORD SERIALIZATION DOES NOT ALLOW EXTRA KEYS. Sorry

(defn assoc-integral-values!
  "Read in integral values and assign to keys of record"
  [data-input integral-keys record-in]
  (reduce+
    (fn [record-n ^Keyword integral-k]
      (let [integral-v
             (nippy/thaw-from-in! data-input)]
        (assoc record-n integral-k integral-v)))
    record-in
    integral-keys))

(def max-extra-keys 50) ; stops parsing after this

(defn assoc-extra-kvs!
  "Read in extra keys and values, if any, and assign to record
   DEPRECATED. Does not work; blows the stack for some reason,
   possibly because the un-serializer recognizes it as simultaneously
   a hash-map and a record and gets stuck in an infinite loop."
  [data-input record-in]
  (reduce+
    (fn [record-n ^Int iteration]
      (try
        (let [extra-k
               (thaw-from-in! data-input)
              extra-v
               (thaw-from-in! data-input)
               ]
          (assoc record-n extra-k extra-v))
        (catch java.io.EOFException e
          (reduced record-n))))
    record-in
    (range+ 0 (-> max-extra-keys (* 2) inc))))

(defmacro extend-serialization-for-record!
  [^Class record-type & [type-id-0]]
  `(let [;type-id# (keyword (str *ns*) (str ~record-type))
         type-id# ~type-id-0
         map-to-record-fn#
           (->> ~record-type
                get-map-constructor)
         sample-record#
           (map-to-record-fn# {})
         integral-fields-ct#
           (count sample-record#)
         integral-keys#
           (keys+ sample-record#)]
     ; Extend freeze
     (nippy/extend-freeze ~record-type ~type-id-0 ; :my-type/foo ; A unique (namespaced) type identifier
       [x# ^java.io.DataOutputStream data-output#]
       (let [all-keys#   (keys+ x#)
             extra-keys# (-> (drop integral-fields-ct# all-keys#) doall)]
             ; TODO alert if extra keys are dropped
         ; imagine the freeze like so:
         ; [field1 field2 field3 :extra1 extra1 :extra2 extra2]

         ; Freeze integral values
         (doseq [integral-k# integral-keys#] ; doseq will freeze the fields in order
           (nippy/freeze-to-out! data-output#
             (get x# integral-k#)))

         ; EXTRA KEYS AND VALUES BREAK EVERYTHING!
         ; Freeze extra keys and values
         ; possibly easier for |thaw| if combined into hash-map?
         ; but more storage footprint
         ; (doseq [extra-k# extra-keys#]
         ;  ;(pr-attrs extra-k#)
         ;  ; Key
         ;   (nippy/freeze-to-out! data-output#
         ;     extra-k#)
         ;  ; Val
         ;   (nippy/freeze-to-out! data-output#
         ;     (get x# extra-k#)))
         ))

     ; Extend thaw
     (nippy/extend-thaw ~type-id-0 ; :my-type/foo ; Same type id
       [^java.io.DataInputStream data-input#]
       ; This function once called itself... that's what causes the StackOverflowError
       (let [record-0# (map-to-record-fn# {})]
         (->> record-0#
              (assoc-integral-values! data-input# integral-keys#)
              ;(assoc-extra-kvs! data-input#) ; this breaks EVERYTHING
              )
         ))
     nil))

; 1. read->(thaw-from-in! ...)->(trampoline thaw-from-in!* ...)
; 2. read-custom! (or end)
; 3. (custom-reader)->extended-thaw
; 4. assoc-integral-values! and assoc-extra-kvs! should be end anyway

(defn extend-serialization-for-records!
  {:todo ["Make this work later"]}
  [& records]
  (let [n (volatile! 1)]
    (doseq [record records]
      ;(extend-serialization-for-record! record (deref n))
      (vswap! n inc))))

(ns clj-qb.db (:gen-class))
(require '[quanta.library.io.serialization :as io-ser])
(io-ser/extend-serialization-for-record! QBDBEntry              1)
(io-ser/extend-serialization-for-record! QuickBooksDB           2)
(io-ser/extend-serialization-for-record! TransFirstDB           3)
(io-ser/extend-serialization-for-record! TFKitEntry             4)
(io-ser/extend-serialization-for-record! TransFirstCCEntry      5)
(io-ser/extend-serialization-for-record! TransFirstECheckEntry  6)
(io-ser/extend-serialization-for-record! ByDesignDB             7)
(io-ser/extend-serialization-for-record! BDCustomersEntry       8)
(io-ser/extend-serialization-for-record! BDInventoryEntry       9)
(io-ser/extend-serialization-for-record! BDOrderLinesEntry     10)
(io-ser/extend-serialization-for-record! BDOrdersEntry         11)
(io-ser/extend-serialization-for-record! BDPaymentsEntry       12)
(io-ser/extend-serialization-for-record! BDRepsEntry           13)
(io-ser/extend-serialization-for-record! SOCUpdatesMap         14)