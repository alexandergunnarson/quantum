(ns quantum.core.io
  (:refer-clojure :exclude [get assoc! dissoc! contains?])
  (:require [quantum.core.io.core :as io.core]
            [quantum.core.vars
              :refer [#?(:clj defalias)]]))

#?(:clj (defalias contains?       io.core/contains?       ))
#?(:clj (defalias assoc!          io.core/assoc!         ))
#_(:clj (defalias dissoc!         io.core/dissoc!        ))
#?(:clj (defalias create-file!    io.core/create-file!   ))
#?(:clj (defalias ->input-stream  io.core/->input-stream ))
#?(:clj (defalias resource        io.core/resource       ))
#?(:clj (defalias ->output-stream io.core/->output-stream))
#?(:clj (defalias copy!           io.core/copy!          ))
#?(:clj (defalias mkdir!          io.core/mkdir!         ))