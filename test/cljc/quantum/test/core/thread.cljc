(ns quantum.test.core.thread
  (:require [quantum.core.thread :as ns]))
 
#?(:clj
(defn test:add-child-proc! [parent id]))


#?(:clj
(defn test:register-thread! [{:keys [id thread handlers parent] :as opts}]))

#?(:clj
(defn test:deregister-thread! [id]))

#(:clj
  (defmacro test:thread+
    [{:keys [id handlers]} expr & exprs]))

#?(:clj (defn test:interrupt!* [x]))
; TODO for now
#?(:clj (defn test:close!*     [x]))

#?(:clj
(defn test:close!
  ([thread-id])
  ([thread-id {:keys [force?] :as opts}])))

#?(:clj
(defn test:close-all!
  ([])
  ([force?])))

#?(:clj
(defn test:close-all-alt!
  []))

#?(:clj
(defn test:close-all-forcibly!
  []))

; ASYNC

#?(:clj
(defn test:set-max-threads!
  [^java.util.concurrent.ThreadPoolExecutor threadpool-n n]))

#?(:clj
(defn test:gen-threadpool [type num-threads & [name-]]))

#?(:clj
(defn test:clear-work-queue! [^ThreadPoolExecutor threadpool-n]))

#?(:clj
(defn test:closeably-execute [threadpool-n ^Runnable r {:keys [id] :as opts}]))

(defn test:gen-proc-id [id parent name-])

#_(:clj
(defn test:f->chan [c f & args]))

#?(:clj
(defmacro test:async-fiber*
  [opts async-fn & args]))

#?(:clj
(defn tset:gen-async-fn ; defn+ ^:suspendable 
  [body-fn {:keys [type id] :as opts}]))

#?(:clj
(defmacro test:gen-async-opts
  [opts & body]))

#?(:clj
(defmacro test:async
  [opts & body]))
  
#?(:clj
(defmacro test:async-loop
  [opts bindings & body]))

#?(:clj
(defn test:proc-chain
  [universal-opts thread-chain-template]))

#?(:clj
(defn test:reap-threads! []))

#?(:clj
(defn test:thread-reaper []))

#?(:clj (defn test:pause-thread-reaper!  []))
#?(:clj (defn test:resume-thread-reaper! []))

; ===============================================================
; ============ TO BE INVESTIGATED AT SOME LATER DATE ============

#_(:clj
(defn test:promise-concur
  [method max-threads func list-0]))

#_(:clj
(defn test:promise-concur-go [method max-threads func list-0]))

#_(:clj
(defn test:concur-go
  [method max-threads func list-0]))

#_(:clj
(defn+ test:thread-or
  [& fs]))

#_(:clj
(defn+ test:thread-and
  [& fs]))

#_(:clj
(defn test:chunk-doseq
  [coll {:keys [total thread-count chunk-size threadpool thread-name chunk-fn] :as opts} f]))

; ===== DISTRIBUTOR =====

#?(:clj
(defnt test:shutdown!
  [^java.util.concurrent.ThreadPoolExecutor x])

#?(:clj
(defn test:->distributor
  [f {:keys [cache memoize-only-first-arg? threadpool max-threads
             max-work-queue-size name] :as opts}]))

#?(:clj
(defn test:distribute
  [distributor & inputs]))

#?(:clj
(defn test:distribute-all [distributor inputs-set & [apply?]]))