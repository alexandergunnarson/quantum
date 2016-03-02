(ns
  ^{:doc "Useful Java-specific functions. Invoking private or protected methods,
          getting the methods associated with a particular class, etc."
    :todo ["Rename ns and move functions"]
    :attribution "Alex Gunnarson"}
  quantum.core.java
  (:require-quantum [ns str])
 #?@(:clj [(:require
             [clojure.data       :as cljdata]
             [clojure.reflect    :refer :all]
             #_[alembic.still]) ; TODO fix class conflict between Pulsar and alembic
           (:import java.lang.reflect.Method)]))

#?(:clj
  (defn get-by-key [object info-type & args]
    (let [method-str (str "get" (str/capitalize (name info-type)))]
      (clojure.lang.Reflector/invokeInstanceMethod object method-str (to-array args)))))
  
#?(:clj
  (defn methods-names [object]
    (sort ; alphabetizes
      (map :name (:members (reflect object))))))

#?(:clj
  (defmacro load-deps [deps]
    `(alembic.still/distill ~deps)))

#_#?(:clj (defalias lein alembic.still/lein))

; (def my-method-fetch
;   (memoize
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

#?(:clj
  (defn invoke*
    "Invoke a private or protected Java method."
    {:attribution "flatland.useful.java"}
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
            result))))))

#?(:clj
(defmacro invoke
  {:usage '(-> (WebClient.) .getTopLevelWindows first (invoke isJavaScriptInitializationNeeded))}
  [instance method & params]
  `(invoke* ~method ~instance ~@params)))

#?(:clj
(defmacro field [instance field]
  `(-> (doto (.getDeclaredField (class ~instance) ~field)
             (.setAccessible true))
       (.get  ~instance))))

