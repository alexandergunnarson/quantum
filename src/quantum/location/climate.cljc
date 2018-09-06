(ns quantum.climate
  (:refer-clojure
    :exclude [conj! assoc! transduce first second get contains? reduce count])
  (:require
    [clojure.core                :as core]
    [quantum.convert.core
      :refer [->hiccup]]
    [quantum.core.collections    :as coll
      :refer [transduce reduce join join!
              map+ map-indexed+ !map-indexed+ filter+ distinct-by-storing+
              range+ drop+ take+ mapcat+ cat+
              sort! sort-by! select! select-by!
              conj! assoc! assoc-in!* get-in*
              red-for ifori
              ->array subview-range
              get first second count contains?
              kw-map]]
    [quantum.core.collections.core
      :refer [arr<>]]
    [quantum.core.compare        :as comp]
    [quantum.core.convert        :as conv]
    [quantum.core.data.primitive :as prim]
    [quantum.core.data.set
      :refer [!hash-set|double]]
    [quantum.core.data.vector
      :refer [!vector]]
    [quantum.core.fn             :as fn
      :refer [fn-> rcomp]]
    [quantum.core.log            :as log]
    [quantum.core.macros
      :refer [defnt defnt']]
    [quantum.core.macros.deftype :as deftype]
    [quantum.core.numeric        :as num]
    [quantum.core.reducers       :as r]
    [quantum.core.reflect        :as refl]
    [quantum.core.type-old       :as t
      :refer [val?]]
    [quantum.measure.convert     :as unit])
  (:import
    org.geotools.gce.geotiff.GeoTiffReader
    org.geotools.coverage.grid.GridCoverage2D
    [java.util ArrayList]
    it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet
    quantum.core.data.Array))

; http://worldclim.org/version2

#_(defn sizeof:array-2d [dim0 dim1 size]
  (let [object-header-size 8
        length-field-size  4
        ref-size           8
        array-overhead
          (+ object-header-size
             length-field-size
             ref-size)]
    (+ array-overhead
       (* dim0
          (+ array-overhead
             (* dim1 size))))))

#_(defn bytes->gb [x] (double (/ x 1024 1024 1024)))
#_(let [resolution:minutes 35
        degrees->minutes #(* % 3600)
        ticks (/ (degrees->minutes 180) resolution:minutes)
        sizeof:data-point 40
        compression-ratio 1/2] ; because seas won't have data
    (bytes->gb (* (sizeof:array-2d ticks ticks sizeof:data-point)
                  compression-ratio))) ; 6.38GB

(def ^:const resolution:minutes 35) ; leads to ~6.5 GB of array storage space

(def ^:const resolution:degrees (double (/ resolution:minutes 60 60)))

(defnt minutes->degrees [^long x] (/ x 3600.0))

(defnt' value-at [^org.geotools.coverage.grid.GridCoverage2D grid ^double longitude ^double latitude]
  (.evaluate grid (org.geotools.geometry.DirectPosition2D. longitude latitude)))

(deftype/deftype ClimateDataPoint ; 8 (ref) + 8 (header) + 6*4 (fields) -> 40 bytes
  [^:get ^:set ^:unsynchronized-mutable ^float precipitation
   ^:get ^:set ^:unsynchronized-mutable ^float solar-radiation
   ^:get ^:set ^:unsynchronized-mutable ^float wind
   ^:get ^:set ^:unsynchronized-mutable ^float min-temperature
   ^:get ^:set ^:unsynchronized-mutable ^float avg-temperature
   ^:get ^:set ^:unsynchronized-mutable ^float max-temperature])

(def ^GeoTiffReader reader ; If you load this first (after importing the class), it's fine, but if you load it after the namespaces, certain classes aren't found... strange!
  (GeoTiffReader.
    (conv/->file
      "~/Downloads/climate/wc2.0_2.5m_tavg/wc2.0_2.5m_tavg_01.tif")))

(def ^GridCoverage2D grid (.read reader nil))

(def ^:const num-months 12)

; Takes <1 second
(def ^"[[Ljava.lang.Object;" !values
  (let [num-resolution-ticks (count (range -89.99999 90.0 resolution:degrees))]
    (Array/newInitializedNdObjectArray
      num-resolution-ticks
      num-resolution-ticks
      #_num-months)))

(defnt' missing? [^float x]
  (= x (float -3.4E38)))

(defnt' grid->values!
  "Outputs a 2D array of values for a given grid.
   The first index will be the nth resolution-tick of latitude.
   The second index will be the nth resolution-tick of longitude.
   The value with be `(value-at grid latitude longitude)`."
  [^GridCoverage2D grid ^objects-2d? !values:output]
  (ifori [long-i longitude -89.99999 (< longitude 90.0) (+ longitude resolution:degrees)]
    (ifori [lat-i latitude -89.99999 (< latitude 90.0) (+ latitude resolution:degrees)]
      (assoc-in!* !values:output
        (let [v (first (value-at grid longitude latitude))]
          (when-not (missing? v)
            (ClimateDataPoint.
              Float/NaN
              Float/NaN
              Float/NaN
              Float/NaN
              v
              Float/NaN)))
        long-i lat-i))))

(defnt c->f
  "Converts Celsius to Fahrenheit"
  [^float x] (-> x (* 9/5) (+ 32) double))

(defnt index->degrees [^long i]
  (+ -89.99999 (minutes->degrees (* i resolution:minutes))))

(defnt' avg-temperature-at [^long i:longitude ^long i:latitude]
  (when-let [^ClimateDataPoint cd (get-in* !values i:longitude i:latitude)]
    (let [latitude  (index->degrees i:latitude)
          longitude (index->degrees i:longitude)
          avg-temperature:c (.getAvgTemperature cd)]
      {:latitude          latitude
       :longitude         longitude
       :avg-temperature:c avg-temperature:c
       :avg-temperature:f (c->f avg-temperature:c)
       :lat-long-str      (str latitude "," longitude)})))

(defnt i:entry->i:longitude [^long i:entry]
  (long (num/div* i:entry (count !values)))) ; same as exact-div with floor

(defn entry->data [^doubles entry]
  (let [;; would destructure but it's quicker this way (for now)
        i:entry           (long (first entry))
        avg-temperature:c (second entry)
        i:longitude (i:entry->i:longitude i:entry)
        i:latitude  (- i:entry
                       (* i:longitude (count !values)))
        data (avg-temperature-at i:longitude i:latitude)]
    (assert (= (:avg-temperature:c data)
               avg-temperature:c))
    (merge data
      (kw-map i:latitude i:longitude))))

(defn avg-temperature:top-n:merge! [n compf !ret chunk]
  (->> chunk
       (join!      !ret)
       (select-by! n (fn [^doubles xs] (second xs)) compf) ; guarantees the kth will be correct, but does not guarantee i<k will be sorted, just < value of k
       (take+      n)
       (join!      (!vector))))

(defn avg-temperature:top-n:chunk+ [n compf ^long chunk-size ^long i:chunk]
  (let [prev-rows-ct (* chunk-size i:chunk)
        prev-data-points-ct (* prev-rows-ct (-> !values first count))]
    (->> (subview-range !values prev-rows-ct (inc chunk-size))
         cat+
         (!map-indexed+
           (fn [^long i ^ClimateDataPoint x]
             (when (val? x)
               (arr<> (double (+ i prev-data-points-ct)) (double (.getAvgTemperature x))))))
         (filter+ val?)
         #_(distinct-by-storing+ ; TODO really, we may want to exclude extremely similar points
           (fn [^objects xs] (second xs))
           (fn [] (!hash-set|double))
           (fn [^DoubleOpenHashSet seen ^double x] (contains? seen x))
           (fn [^DoubleOpenHashSet seen ^double x] (conj! seen x)))
         )))

; > means get the hottest
; < means get the coldest
(defn avg-temperature:top-n [n compf ^long chunk-size]
  (let [num-chunks (long (num/div* (count !values) chunk-size))] ; TODO use `num/floor`
    (->> (red-for [^long i:chunk (range num-chunks) ; TODO `range+` is giving trouble
                         !ret    (!vector)]
           (println "processing chunk" (inc i:chunk) "of" num-chunks)
           (->> (avg-temperature:top-n:chunk+ n compf chunk-size i:chunk)
                (avg-temperature:top-n:merge! n compf !ret)))
         (sort-by! (fn [^doubles xs] (second xs)) compf)
         (map+     entry->data)
         (join! (!vector)))))

(def top-10-hottest-avg-temp-in-january
  (avg-temperature:top-n 10 > 2000))

#_(avg-temperature-at 1000 0)

#_(future (time (grid->values! grid !values)) (println "Done!"))
