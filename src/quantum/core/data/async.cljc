(ns quantum.core.data.async
  (:require
    [quantum.core.type :as t]))

(def chan? (t/isa? #?(:clj  clojure.core.async.impl.protocols/Channel
                      :cljs cljs.core.async.impl.protocols/Channel)))

(def m2m-chan? (t/isa? #?(:clj  clojure.core.async.impl.channels.ManyToManyChannel
                          :cljs cljs.core.async.impl.channels/ManyToManyChannel)))
