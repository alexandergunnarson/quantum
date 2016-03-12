(ns
  ^{:doc
      "Serialization for any virtually data structure using taoensso.nippy.
      
       Specifically provides support for custom record types, which nippy does
       not support very well."
    :attribution "Alex Gunnarson"}
  quantum.core.io.serialization
  (:refer-clojure :exclude [read])
  (:require-quantum [ns coll])
  #?(:clj
    (:require 
      [taoensso.nippy             :as nippy :refer
        [read-bytes read-utf8 read-biginteger]]
      [iota                       :as iota]
      [cognitect.transit :as t]))
  #?(:clj
     (:import 
        (java.io File FileNotFoundException PushbackReader
          FileReader DataInputStream DataOutputStream IOException
          FileOutputStream BufferedOutputStream BufferedInputStream
          FileInputStream
          ByteArrayOutputStream))))

; This |do| covers the entire file. For purposes of reader macro
#?(:clj
(do

; RECORD SERIALIZATION DOES NOT ALLOW EXTRA KEYS. Sorry

; https://developers.google.com/protocol-buffers/docs/overview
; Compare protocol buffers to Nippy freezing.
; Protocol buffers are are 3-10 times smaller than XML and are 20-100 times faster.

(defn assoc-integral-values!
  "Read in integral values and assign to keys of record"
  {:attribution "Alex Gunnarson"}
  [data-input integral-keys record-in]
  (reduce
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
  {:attribution "Alex Gunnarson"}
  [data-input record-in]
  (reduce
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
  {:attribution "Alex Gunnarson"
   :todo ["Likely inefficient"]
   :examples '[(extend-serialization-for-record! QBDBEntry    1)
               (extend-serialization-for-record! QuickBooksDB 2)]}
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
  {:attribution "Alex Gunnarson"
   :todo ["Make this work later"]}
  [& records]
  (dotimes [n (lasti records)]
      ;(extend-serialization-for-record! record (deref (inc n)))
    ))

(defn ^String ->transit [^Map m]
  (let [baos (ByteArrayOutputStream.)]
    (->> m (t/write (t/writer baos :json)))
    (.close baos)
    (.toString baos)))


))