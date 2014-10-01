(ns quanta.library.java
	(:gen-class)
	(:require [clojure.string :as str]
            [clojure.data :as cljdata]
            [clojure.walk :refer :all]
            [clojure.reflect :refer :all]
            [quanta.library.print :refer [print-table !]])
	(:import java.lang.reflect.Method))

(defn get-by-key [object info-type & args]
  (let [method-str (str "get" (str/capitalize (name info-type)))]
    (clojure.lang.Reflector/invokeInstanceMethod object method-str (to-array args))))

(defn methods-names [object]
  (sort ; alphabetizes
      (map :name (:members (reflect object)))))
(defn pr-methods [object]
  (! (methods-names object)))

; (def my-method-fetch
;   (memoize ; oh, so it's basically a function, but it just memoizes within
;     (fn [class-]
;       (let [class-map (reflect class-)
;             instance-methods
;               (filter (fn [element] (contains? (:flags element) :public)) ; only public
;                 (filter :return-type (:members class-map))) ; those that have a return type, presumably
;             static-methods
;               (filter (fn [element] (contains? (:flags element) :static))
;                 current-methods)
;             methods
;               {:instance (map :name instance-methods)
;                :static (map :name static-methods)}]
;         (reduce
;           (fn [a b] ; applies the function to the vector
;             (let [b (my-method-fetch (resolve b))] ; recursive call... thus the memoization, probably
;               (-> a
;                   (update-in [:instance] #(flatten (conj % (:instance b))))
;                   (update-in [:static] #(flatten (conj % (:static b)))))))
;           methods (:bases class-map)))))) ; #{com.google.api.client.json.GenericJson}
(defn invoke*
  "Invoke a private or protected Java method."
  ^{:attribution "flatland.useful.java"}
  [^String method instance & params]
  (let [signature (into-array Class (map class params))
        c (class instance)]
    (when-let [^Method method
    	        (some
    	          #(try
                     (.getDeclaredMethod ^Class % method signature)
                     (catch NoSuchMethodException e))
                   (conj (ancestors c) c))]
      (let [accessible (.isAccessible method)]
        (.setAccessible method true)
        (let [result (.invoke method instance (into-array params))]
          (.setAccessible method accessible)
          result)))))
(defmacro invoke [instance method & params]
  "(-> (WebClient.) .getTopLevelWindows first (invoke isJavaScriptInitializationNeeded))"
  `(invoke* ~(-> method name str) ~instance ~@params))



