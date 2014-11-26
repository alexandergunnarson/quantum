(ns quantum.core.io.serialization (:gen-class))
(import
  (java.io File FileNotFoundException PushbackReader
    FileReader DataInputStream DataOutputStream IOException
    FileOutputStream BufferedOutputStream BufferedInputStream
    FileInputStream))
(require
  '[quantum.core.ns          :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(ns/nss *ns*)
(require
  '[clojure.java.io            :as clj-io                    ]
  '[quantum.core.log         :as log                       ]
  '[clojure.data.csv           :as csv                       ]
  '[quantum.core.data.array  :as arr   :refer :all         ]
  '[quantum.core.string      :as str                       ] 
  '[quantum.core.time.core   :as time                      ] 
  '[quantum.core.print       :as pr    :refer [! pr-attrs] ]
  '[quantum.core.collections :as coll  :refer :all         ]
  '[quantum.core.data.map    :as map   :refer [map-entry]  ]
  '[quantum.core.numeric     :as num   :refer [greatest-or]]
  '[quantum.core.logic                 :refer :all         ]
  '[quantum.core.type                  :refer :all         ]
  '[quantum.core.function              :refer :all         ]
  '[quantum.core.system      :as sys                       ] 
  '[quantum.core.error       :as err   :refer [try+ throw+]]
  '[taoensso.nippy             :as nippy :refer
     [read-bytes read-utf8 read-biginteger]]
  '[iota                       :as iota                      ])

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
               (nippy/thaw-from-in! data-input)
              extra-v
               (nippy/thaw-from-in! data-input)
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
         ; [field1 field2 field3]

         ; Freeze integral values
         (doseq [integral-k# integral-keys#] ; doseq will freeze the fields in order
           (nippy/freeze-to-out! data-output#
             (get x# integral-k#)))))

     ; Extend thaw
     (nippy/extend-thaw ~type-id-0 ; :my-type/foo ; Same type id
       [^java.io.DataInputStream data-input#]
       ; This function once called itself... that's what causes the StackOverflowError
       (let [record-0# (map-to-record-fn# {})]
         (->> record-0#
              (assoc-integral-values! data-input# integral-keys#))))
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