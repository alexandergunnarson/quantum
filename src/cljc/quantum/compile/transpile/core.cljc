(ns quantum.compile.transpile.core
  (:require [quantum.compile.transpile.to.core   :as to       ]
            [quantum.compile.transpile.from.java :as from-java]
            [quantum.core.io.core                :as io       ]
            [quantum.core.log                    :as log      ]
            [quantum.core.print                  :as pr       ]
            [quantum.core.convert                :as conv     ]
            [quantum.core.macros                 :as macros
              :refer [#?(:clj defnt)]                         ]
            [quantum.core.collections            :as core    
              :refer [#?(:clj kmap) in?]                      ]
            [quantum.core.error                  :as err    
              :refer [->ex #?(:clj throw-unless)]             ]))

#?(:clj
(defnt ^String transpile-from*
  ([^vec? from-src from to]
    (transpile-from* (conv/->file from-src) from to))
  ([^java.io.File from-src from to]
    (transpile-from*
      (io/get {:path (str from-src) :read-method :str})
      from to))
  ([^string? from-src from to]
    (cond
      (= from :clj)
        (-> (str "(do " from-src "\n)") ; Necessary for read-string input
            read-string
            (transpile-from* from to))
      (and (= from :java) (= to :clj))
        (-> from-src
            from-java/parse
            from-java/clean
            ((fn [x] (with-out-str (pr/! x)))) ; TODO is this necessary for formatter?
            ; TODO format
            )))
  ([^seq? from-src from to]
    (cond
      (= from :clj)
        (binding [to/*lang* to]
          (to/eval-form from-src))))))


#?(:clj
(defn transpile
  "Might alternatively be named 'compile', but this is not strictly correct.

   @literal? denotes whether the @from-src is a Clojure code literal.
   @wrapped? denotes whether the @from-src has already been wrapped in
             a |(do)|, as per |read-string|'s requirements. This saves
             time and memory."
  {:todo  ["Add optional compiler checks."
           "Correct formatting for Clojure"
           "Type inference for Java — can't all be object"]
   :usage '[(transpile :clj :java
             '(defn abcde [^long x]
                (let [a (+ x 4)]
                  (* b 2)))) 
            (transpile :java :clj
              [:projects "quantum" "test" "Temp.java"])]}
  [from to from-src & [to-src literal? wrapped?]]
  (throw-unless
    (in? [from to]
      #{[:clj  :java   ]
        [:java :clj    ]
        [:clj  :js     ]
        [:clj  :cs     ]
        [:clj  :c-sharp]
        ; [:clj :cpp]
        })
    (->ex :invalid "From-To language combination invalid" (kmap from to)))  

  (when (= [from to] [:clj :c-sharp])
    (log/pr :warn "Clojure -> C-Sharp transpiler is pre-alpha."))

  (let [transpiled-str (transpile-from* from-src from to)]
    transpiled-str)))

