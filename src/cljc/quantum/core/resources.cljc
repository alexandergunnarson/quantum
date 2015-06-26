(ns quantum.core.resources
  (:require-quantum [ns num fn str err macros logic vec coll log async qasync])
  #?(:clj (:import (java.lang ProcessBuilder Process StringBuffer)
                   (java.io InputStream Reader Writer
                     IOException)
                   (java.util.concurrent TimeUnit)
                   quantum.core.data.queue.LinkedBlockingQueue
                   clojure.core.async.impl.channels.ManyToManyChannel)))

#?(:clj
(defnt open?
  [InputStream] ([stream]
                  (try (.available stream) true
                    (catch IOException _ false)))
  [LinkedBlockingQueue] ([obj] (qasync/closed? obj))))

#?(:clj (def closed? (fn-not open?)))

(defnt close!
  #?@(:clj
 [[Writer Reader]       ([obj] (.close obj))
  [ManyToManyChannel]   ([obj] (async/close! obj))
  [LinkedBlockingQueue] ([obj] (qasync/close! obj))])
  ;[clojure.lang.IAtom]  ([obj] (reset! obj nil)) ; make the queue disappear
  nil?                  ([obj] nil)
  :default              ([obj] (throw+ "Not yet implemented.")))

(defn with-cleanup [obj cleanup-seq]
  (conj! cleanup-seq #(close! obj))
  obj)
