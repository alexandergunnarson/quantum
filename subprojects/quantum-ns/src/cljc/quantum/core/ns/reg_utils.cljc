(ns ^{:doc "Required utility functions for quantum.ns"
      :todo ["It would be nice to be able to put this somewhere else"]}
  quantum.core.ns.reg-utils)

(defn set-merge
  "Merges the content of @colls into a set."
  [& colls]
  (->> colls (apply concat) (into #{})))

(defn ex [arg]
  #?(:clj  (Exception. ^String arg)
     :cljs (js/Error   arg)))