(ns
  ^{:doc "Printing functions such as fipp.edn (a fast pretty printer),
          |pr-attrs| (which prints the key attributes of a given object
          or expression, blacklisted printable objects (so you don't
          have to wait while you accidentally print out an entire database), 
          and so on."
    :attribution "Alex Gunnarson"}
  quantum.core.print
  (:require-quantum [ns fn logic])
  (:require [fipp.edn :as pr]))

; "At least 5 times faster than clojure.pprint/pprint
;  Prints no later than having consumed the bound amount of memory,
;  so you see your first few lines of output instantaneously.
;  :attribution: https://github.com/brandonbloom/fipp"
(defalias pprint pr/pprint)
(def ^:dynamic *max-length* 1000)
(def ^:dynamic *blacklist* (atom #{})) ; a list of classes not to print

(defn !
  ([obj]
    (let [ct (long
               (try
                 (count obj)
                 (catch quantum.core.ns/AError _ 1)))] ; if you don't qualify it in JS it won't like it...
      (cond
        (> ct (long *max-length*))
          (println
            (str "Object is too long to print ("
                 (str ct " elements")
                 ").")
            "*max-length* is set at" (str *max-length* "."))
        (contains? @*blacklist* (type obj))
          (println
            "Object's class"
            (str (type obj) "(" ")")
            "is blacklisted for printing.")
        :else
          (pr/pprint obj))))
  ([obj & objs]
    (doseq [obj-n (cons obj objs)]
      (! obj-n))))
;(clojure.main/repl :print !) ; This causes a lot of strange problems... sorry...

(def  suppress (partial (constantly nil)))

; (defn pprint-xml
;   {:attribution "Alex Gunnarson"
;    :todo ["A rather large function. Did this a long time ago" "Where does this function belong?"]}
;   [xml-str]
;   (let [print-tabbed 
;           (fn [[tab xml-ln]]
;             (->> " "
;                  (repeat tab)
;                  (apply str)
;                  (<- str xml-ln)
;                  println))
;         print-body
;           (fn [[elem-n :as elems-n] 
;                 elem-n-1
;                 ^long tabs-n
;                 elems-f]
;             (if (empty? elems-n)
;                 (doseq [elem-n elems-f] (print-tabbed elem-n))
;                 (let [type-n   (xml/elem-type elem-n)
;                       type-n-1 (xml/elem-type elem-n-1)
;                       tabs-n+1
;                         (cond (or (= :beg type-n)
;                                   (= :body  type-n))
;                               (if (= :beg type-n-1)
;                                   (+ (long tabs-n)  2)
;                                   (long tabs-n))
;                               :end
;                               (if (= :beg type-n-1)
;                                   (long tabs-n)
;                                   (- (long tabs-n) 2)))]
;                   (recur (rest elems-n)
;                          elem-n
;                          (long tabs-n+1)
;                          (conj elems-f [tabs-n+1 elem-n])))))]
;     (print-body (whenf xml-str string? xml/split) nil (long 0) [])))

(defn representative-coll
  "Gets the first element of every collection, until it returns empty.

   Useful for printing out representative samples of large collections
   which would be undesirable to print in whole."
  {:attribution "Alex Gunnarson"}
  [source-0]
  (if ((fn-or (fn-not coll?) empty?) source-0)
      source-0
      (loop [source-n   source-0
             assoc-keys []
             ret        nil]
        (if ((fn-or (fn-not coll?) empty?) source-n)
            ret
            (let [ret-n+1-0
                    (condf source-n
                      ; TODO |get-map-constructor| is in collections
                      ; record? (fn [source-n*]
                      ;           (let [^Fn constructor-fn
                      ;                   (get-map-constructor source-n*)]
                      ;             (->> source-n* first (apply hash-map)
                      ;                  constructor-fn)))
                      map?    (fn->> first (apply hash-map))
                      vector? (fn->> first vector)) 
                  assoc-key-n+1
                    (condf ret-n+1-0
                      map?    (fn-> keys first)
                      vector? (constantly 0))
                  assoc-keys-n+1
                    (conj assoc-keys assoc-key-n+1)
                  ret-n+1
                    (if (nil? ret)
                        ret-n+1-0
                        (assoc-in ret assoc-keys ret-n+1-0))
                  source-n+1
                    (condf ret-n+1-0
                      map?    (-> vals first)
                      vector? first)]
              (recur source-n+1
                     assoc-keys-n+1
                     ret-n+1))))))  

(defn !* [obj] (-> obj representative-coll !))

#?(:clj
(defmacro pr-attrs
  "Prints the attributes of a given object/expression.
   These attributes include the object/expression that was evaluated
   (the text itself), the class, its keys (if applicable), and the
   representative value of the object/expression.

   Representative value means the recursive first item in a collection.
   This eliminates, or at least greatly diminishes, massive and
   tiring pauses spent printing out the result of an object/expression."
  {:attribution "Alex Gunnarson"}
  [obj]
  `(do (println "Expr:"     (quote ~obj))
       (println "Class:"    (class ~obj))
       (println "Keys:"     (ifn   ~obj coll? keys+ (constantly nil)))
       (println "Repr. object: ") (-> ~obj quantum.core.print/representative-coll !)
       (println))))