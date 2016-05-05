; Or, "nested"
(ns quantum.core.collections.inner
  (:require [quantum.core.data.map :as map]
            [quantum.core.data.set :as set]))

(defn nest-keys
  "Returns a map that takes `m` and extends all keys with the
  `nskv` vector. `ex` is the list of keys that are not extended.
  (nest-keys {:a 1 :b 2} [:hello :there])
   => {:hello {:there {:a 1 :b 2}}}
   (nest-keys {:there 1 :b 2} [:hello] [:there])
   => {:hello {:b 2} :there 1}"
  {:source "zcaudate/hara.data.path"}
  ([m nskv] (nest-keys m nskv []))
  ([m nskv ex]
    (let [e-map (select-keys m ex)
          x-map (apply dissoc m ex)]
      (map/merge e-map (if (empty? nskv)
                     x-map
                     (assoc-in {} nskv x-map))))))

; TODO requires treeify-keys-nested
#_(defn unnest-keys
  "The reverse of `nest-keys`. Takes `m` and returns a map
  with all keys with a `keyword-nsvec` of `nskv` being 'unnested'
  (unnest-keys {:hello/a 1
                :hello/b 2
                :there/a 3
                :there/b 4} [:hello])
  => {:a 1 :b 2
      :there {:a 3 :b 4}}
  (unnest-keys {:hello {:there {:a 1 :b 2}}
                :again {:c 3 :d 4}} [:hello :there] [:+] )
  => {:a 1 :b 2
      :+ {:again {:c 3 :d 4}}}"
  {:source "zcaudate/hara.data.path"}
  ([m nskv] (unnest-keys m nskv []))
  ([m nskv ex]
   (let [tm     (treeify-keys-nested m)
         c-map  (get-in tm nskv)
         x-map  (dissoc-in tm nskv)] ; was map/dissoc-in
    (map/merge c-map (if (empty? ex)
                   x-map
                   (assoc-in {} ex x-map))))))

(defn key-paths
  "The set of all paths in a map, governed by a max level of nesting
  (key-paths {:a {:b 1} :c {:d 1}})
  => [[:c :d] [:a :b]]
  (key-paths {:a {:b 1} :c {:d 1}} 1)
  => [[:c] [:a]]"
  {:source "zcaudate/hara.data.nested"
   :todo ["Combine with graph"]}
  ([m] (key-paths m -1 []))
  ([m max] (key-paths m max []))
  ([m max arr]
   (reduce-kv (fn [out k v]
                (cond (and (not= max 1)
                           (map? v))
                      (vec (concat out (key-paths v (dec max) (conj arr k))))

                      :else (conj out (conj arr k))))
              []
              m)))

(defn keys-nested
  "The set of all nested keys in a map
  (keys-nested {:a {:b 1 :c {:d 1}}})
  => #{:a :b :c :d}"
  {:source "zcaudate/hara.data.nested"
   :todo ["I don't think this actually works"]}
  ([m] (reduce-kv (fn [s k v]
                    (if (map? v)
                      (set/union (conj s k) (keys-nested v))
                      (conj s k)))
                  #{}
                  m)))

(defn merge-nested
  "Merges nested values from left to right.
  (merge-nested {:a {:b {:c 3}}} {:a {:b 3}})
  => {:a {:b 3}}
  (merge-nested {:a {:b {:c 1 :d 2}}}
                {:a {:b {:c 3}}})
  => {:a {:b {:c 3 :d 2}}}"
  {:source "zcaudate/hara.data.nested"}
  ([] nil)
  ([m] m)
  ([m1 m2]
   (reduce-kv (fn [out k v]
                (let [v1 (get out k)]
                  (cond (nil? v1)
                        (assoc out k v)

                        (and (map? v) (map? v1))
                        (assoc out k (merge-nested v1 v))

                        (= v v1)
                        out

                        :else
                        (assoc out k v))))
              m1
              m2))
  ([m1 m2 & ms]
     (apply merge-nested (merge-nested m1 m2) ms)))
