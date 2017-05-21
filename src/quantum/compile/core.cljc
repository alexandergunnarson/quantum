(ns quantum.compile.core
           (:require
             #?(:clj [clojure.tools.analyzer.jvm     :as ana ])
             #?(:clj [clojure.tools.emitter.jvm.emit :as emit])
                     [quantum.core.vars              :as var
                       :refer [#?@(:clj [defalias])]         ])
  #?(:cljs (:require-macros
                     [quantum.core.vars              :as var
                       :refer [defalias]                     ])))

; TODO handle options-passing
#?(:clj
(defn ->bytecode
  "Emits a sequence of Java bytecodes for a given AST."
  [x]
  (cond
    ; Assumes map is AST
    (map? x)
    (emit/emit x)
    ; Assumes Clojure form
    :else
    (-> x ana/analyze ->bytecode))))

; TODO handle options-passing
#?(:clj
(defn ->classes
  "Compiles the AST into a sequence of one or more classes."
  [x]
  (cond
    ; Assumes map is AST
    (map? x)
    (emit/emit-classes x)
    ; Assumes Clojure form
    :else
    (-> x ana/analyze ->classes))))

#_(:clj
 ; (write-class! @class-name @bytecode)
(defalias write-class! emit/write-class))

#_(:clj
(defalias compile-and-load! emit/compile-and-load))