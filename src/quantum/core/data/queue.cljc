(ns quantum.core.data.queue
  "Queues, particularly FIFO queues, as LIFO = stack = any vector.

   To investigate:
   - https://github.com/michalmarczyk/jumping-queues"
  (:require
    [clojure.core      :as core]
    [quantum.core.type :as t]))

(def   +queue? (t/isa? #?(:clj  clojure.lang.PersistentQueue
                          :cljs cljs.core/PersistentQueue)))
(def  !+queue? t/none?)
(def ?!+queue? (t/or +queue? !+queue?))

#?(:clj (def !!queue? (t/or (t/isa? java.util.concurrent.BlockingQueue)
                            (t/isa? java.util.concurrent.TransferQueue)
                            (t/isa? java.util.concurrent.ConcurrentLinkedQueue))))

(def   !queue? #?(:clj  ;; Considered single-threaded mutable unless otherwise noted
                        ;; TODO TYPED re-enable one `t/-` works properly
                        #_(t/- (t/isa? java.util.Queue) (t/or ?!+queue? !!queue?))
                        t/none?
                  :cljs (t/isa? goog.structs.Queue)))

(def    queue? (t/or ?!+queue? !queue? #?(:clj !!queue?)))

(t/defn >queue > +queue?
  ([] #?(:clj  clojure.lang.PersistentQueue/EMPTY
         :cljs (.-EMPTY cljs.core/PersistentQueue))))

#?(:clj
(defmethod print-method clojure.lang.PersistentQueue [q w]
  (print-method '<- w)
  (print-method (core/seq q) w)
  (print-method '-< w)))
