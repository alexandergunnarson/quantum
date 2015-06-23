(ns quantum.core.resources
  (:require-quantum [ns num fn str err macros logic vec coll log async qasync])
  #?(:clj (:import quantum.core.thread.async.QueueCloseRequest
                   (java.lang ProcessBuilder Process StringBuffer)
                   (java.io InputStream InputStreamReader BufferedReader
                     OutputStreamWriter BufferedWriter
                     IOException)
                   (java.util.concurrent LinkedBlockingQueue TimeUnit)
                   clojure.core.async.impl.channels.ManyToManyChannel)))

#?(:clj
(defnt open?
  [InputStream] ([stream]
                  (try (.available stream) true
                    (catch IOException _ false)))
  [LinkedBlockingQueue] ([obj] (not (instance? QueueCloseRequest (.peek obj))))))

#?(:clj (def closed? (fn-not open?)))

(defnt close!
  #?@(:clj
 [[BufferedWriter InputStreamReader]
                        ([obj] (.close obj))
  [ManyToManyChannel]   ([obj] (async/close! obj))
  [LinkedBlockingQueue] ([obj] (put! obj (QueueCloseRequest.)))])
  ;[clojure.lang.IAtom]  ([obj] (reset! obj nil)) ; make the queue disappear
  nil?                  ([obj] nil)
  :default              ([obj] (throw+ "Not yet implemented.")))

(defn with-cleanup [obj cleanup-seq]
  (conj! cleanup-seq #(close! obj))
  obj)
