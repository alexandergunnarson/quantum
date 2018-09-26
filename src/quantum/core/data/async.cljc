(ns quantum.core.data.async
  (:require
    [quantum.core.type :as t]))

(def closeable-chan? (t/isa? #?(:clj  clojure.core.async.impl.protocols/Channel
                                :cljs cljs.core.async.impl.protocols/Channel)))

(def readable-chan? (t/isa? #?(:clj  clojure.core.async.impl.protocols/ReadPort
                               :cljs cljs.core.async.impl.protocols/ReadPort)))

(def writable-chan? (t/isa? #?(:clj  clojure.core.async.impl.protocols/WritePort
                               :cljs cljs.core.async.impl.protocols/WritePort)))

(def m2m-chan? (t/isa? #?(:clj  clojure.core.async.impl.channels.ManyToManyChannel
                          :cljs cljs.core.async.impl.channels/ManyToManyChannel)))
