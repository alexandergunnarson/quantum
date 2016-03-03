(ns
  ^{:doc "Incorporates the semi-obscure clojure.lang.PersistentQueue into the
          quantum library."
    :attribution "Alex Gunnarson"}
  quantum.core.data.queue
  (:require-quantum [:core]))

; QUEUES
; https://github.com/michalmarczyk/jumping-queues

(defn queue
  "Creates an empty persistent queue, or one populated with a collection."
  {:attribution "weavejester.medley"}
  ([] #?(:clj  (clojure.lang.PersistentQueue/EMPTY)
         :cljs (.-EMPTY cljs.core/PersistentQueue)))
  ([coll] (into (queue) coll)))

#?(:clj
  (defmethod print-method clojure.lang.PersistentQueue
    [q w]
    (print-method '<- w)
    (print-method (seq q) w)
    (print-method '-< w)))