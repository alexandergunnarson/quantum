(ns quanta.library.print (:gen-class))
(set! *warn-on-reflection* true)
(require
  '[quanta.library.ns           :as ns   :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[quanta.library.collections           :refer :all]
  '[quanta.library.function              :refer :all]
  '[quanta.library.logic                 :refer :all]
  '[quanta.library.type                  :refer :all]
  '[quanta.library.numeric      :as num]
  '[quanta.library.string       :as str]
  '[quanta.library.data.xml     :as xml]
  '[clojure.pprint              :as pprint]
  '[fipp.edn                    :as pr])

; "At least 5 times faster than clojure.pprint/pprint
;  Prints no later than having consumed the bound amount of memory,
;  so you see your first few lines of output instantaneously.
;  :attribution: https://github.com/brandonbloom/fipp"
(defalias pprint pr/pprint)
(def ^:dynamic *max-length* 1000)
(def ^:dynamic *blacklist* (atom #{})) ; a list of classes not to print
(defn ! [obj]
  (let [ct (count+ obj)]
    (cond
      (> ct *max-length*)
        (println
          (str "Object is too long to print ("
               (str/sp ct "elements")
               ").")
          "*max-length* is set at" (str *max-length* "."))
      (contains? @*blacklist* (class obj))
        (println
          "Object's class"
          (str/paren (class obj))
          "is blacklisted for printing.")
      :else
        (pr/pprint obj))))

(defalias print-table pprint/print-table) 
(def ^:dynamic *print-right-margin* pprint/*print-right-margin*)
(def  suppress (partial (constantly nil)))
(defn pprint-xml
  {:todo ["Where does this function belong?"]}
  [xml-str]
  (let [print-tabbed 
          (fn [[tab xml-ln]]
            (->> " "
                 (repeat tab)
                 (apply str)
                 (<- str xml-ln)
                 println))
        print-body
          (fn [[elem-n :as elems-n] 
                elem-n-1
                ^long tabs-n
                elems-f]
            (if (empty? elems-n)
                (doseq [elem-n elems-f] (print-tabbed elem-n))
                (let [type-n   (xml/elem-type elem-n)
                      type-n-1 (xml/elem-type elem-n-1)
                      tabs-n+1
                        (cond (or (= :beg type-n)
                                  (= :body  type-n))
                              (if (= :beg type-n-1)
                                  (+ (long tabs-n)  2)
                                  (long tabs-n))
                              :end
                              (if (= :beg type-n-1)
                                  (long tabs-n)
                                  (- (long tabs-n) 2)))]
                  (recur (rest elems-n)
                         elem-n
                         (long tabs-n+1)
                         (conj elems-f [tabs-n+1 elem-n])))))]
    (print-body (whenf xml-str string? xml/split) nil (long 0) [])))
(defn pr-vec-table [vec-0]
  (let [vec-strs (->> vec-0 (map+ (fn->> (map+ str) fold+)) fold+)
        col-ct   (->> vec-0 (map+ count+) fold+ num/greatest)
        col-widths
          (->> vec-strs
               (map+ (fn->> (map+ count+) fold+))
               (#(for+ [n (range 0 col-ct)]
                   (->> %
                        (map+ (getf+ n))
                        fold+
                        num/greatest)))
               fold+)
        pad                " "
        col-separator      "|"
        header-underline   "_"
        extend-to
          (fn [^String str-0 to-ct char-0]
            (if (-> str-0 count+ (< to-ct))
                (apply str str-0 (repeat (- to-ct (count+ str-0)) char-0))
                str-0))
        indexed-table
         (->> vec-strs
              (map+ (compr #(for+ [n (range 0 col-ct)]
                {:width (get col-widths n) :data (get % n)}) fold+))
              fold+)
        pr-rows
          (fn [rows]
            (doseq [row rows]
              (->> row
                   (map+ (fn [{:as row :keys [width data]}]
                           (extend-to data width " ")))
                   fold+
                   (str/join (str pad col-separator pad))
                   println)))
        pr-headers
          #(do (-> indexed-table first vector pr-rows)
               (println 
                 (str/join (str header-underline col-separator header-underline)
                   (for [col-width col-widths]
                     (apply str (repeat col-width header-underline))))))]
    (pr-headers) 
    (pr-rows (rest+ indexed-table))))

(defn representative-coll
  "Gets the first element of every collection, until it returns empty.
  
   Useful for printing out representative samples of large collections
   which would be undesirable to print in whole."
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
                      record? (fn [source-n*]
                                (let [^Fn constructor-fn
                                        (get-map-constructor source-n*)]
                                  (->> source-n* first (apply hash-map)
                                       constructor-fn)))
                      map?    (fn->> first (apply hash-map))
                      vector? (fn->> first vector)
                      lseq?   (fn->> first vector)) ; is this a good decision?
                  assoc-key-n+1
                    (condf ret-n+1-0
                      map?    fkey+
                      vector? (constantly 0)
                      lseq?   (constantly 0))
                  assoc-keys-n+1
                    (conj assoc-keys assoc-key-n+1)
                  ret-n+1
                    (if (nil? ret)
                        ret-n+1-0
                        (assoc-in+ ret assoc-keys ret-n+1-0))
                  source-n+1
                    (condf ret-n+1-0
                      map?    fval+
                      vector? first
                      lseq?   first)]
              (recur source-n+1
                     assoc-keys-n+1
                     ret-n+1))))))  

(defn !* [obj] (-> obj representative-coll !))

(defmacro pr-attrs
  "Prints the attributes of a given object/expression.
   These attributes include the object/expression that was evaluated
   (the text itself), the class, its keys (if applicable), and the
   representative value of the object/expression.

   Representative value means the recursive first item in a collection.
   This eliminates, or at least greatly diminishes, massive and
   tiring pauses spent printing out the result of an object/expression."
  [obj]
  `(do (println "Expr:"     (quote ~obj))
       (println "Class:"    (class ~obj))
       (println "Keys:"     (ifn   ~obj coll? keys+ (constantly nil)))
       (println "Repr. object: ") (->    ~obj quanta.library.print/representative-coll !)
       (println)))

