(ns quanta.library.print
  (:require
    [quanta.library.ns          :as ns   :refer [defalias]]
    [quanta.library.collections          :refer :all]
    [quanta.library.function             :refer :all]
    [quanta.library.logic                :refer :all]
    [quanta.library.numeric     :as num]
    [quanta.library.string      :as str]
    [quanta.library.data.xml    :as xml]
    [clojure.pprint             :as pprint]
    [fipp.edn                   :as pr])
  (:gen-class))

(def ^:dynamic *debug?* false)
(defn debug [& args]
  (when *debug?*
    (apply println args)))
(defalias
  ; "At least 5 times faster than clojure.pprint/pprint
  ;  Prints no later than having consumed the bound amount of memory,
  ;  so you see your first few lines of output instantaneously.
  ;  :attribution: https://github.com/brandonbloom/fipp"
  pprint pr/pprint)
(defalias ! pprint)
(defalias print-table pprint/print-table) 
(def ^:dynamic *print-right-margin* pprint/*print-right-margin*)
(def  suppress-pr (partial (constantly nil)))
(defn pprint-xml [xml-str]
  (defn print-tabbed [[tab xml-ln]]
    (->> " "
         (repeat tab)
         (apply str)
         (<- str xml-ln)
         println))
  (loop [[elem-n :as elems-n] (whenf xml-str string? xml/split-xml)
         elem-n-1 nil
         tabs-n 0
         elems-f  []]
    (if (empty? elems-n)
        (doseq [elem-n elems-f] (print-tabbed elem-n))
        (let [type-n   (xml/elem-type elem-n)
              type-n-1 (xml/elem-type elem-n-1)
              tabs-n+1
                (cond (or (= :beg type-n)
                          (= :body  type-n))
                      (if (= :beg type-n-1)
                          (+ tabs-n 2)
                          tabs-n)
                      :end
                      (if (= :beg type-n-1)
                          tabs-n
                          (- tabs-n 2)))]
          (recur (rest elems-n)
                 elem-n
                 tabs-n+1
                 (conj elems-f [tabs-n+1 elem-n]))))))
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