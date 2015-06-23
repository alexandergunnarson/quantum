(ns
  ^{:doc "Useful looping constructs. Most of these, like |doseq| and |for|,
          are faster than their lazy clojure.core counterparts."
    :attribution "Alex Gunnarson"}
  quantum.core.cljs.loops
  (:refer-clojure :exclude [doseq for reduce])
  (:require-quantum [ns fn logic log map macros type red])
  (:require [quantum.core.loops]))

#?(:clj
(defmacro reduce [& args]
  `(quantum.core.loops/reduce* :cljs ~@args)))

#?(:clj (defalias reducei quantum.core.loops/reducei))
#?(:clj (defalias for     quantum.core.loops/for    ))
#?(:clj (defalias doseq   quantum.core.loops/doseq  ))
#?(:clj (defalias doseqi  quantum.core.loops/doseqi ))

