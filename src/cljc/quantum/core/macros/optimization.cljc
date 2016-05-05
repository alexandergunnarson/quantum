(ns ^{:doc "Helper functions for macros which provide optimization."}
  quantum.core.macros.optimization
           (:require [quantum.core.core        :as qcore]
                     [quantum.core.fn          :as fn
                       :refer [#?@(:clj [fn->])]        ]
                     [quantum.core.log         :as log  ]
                     [quantum.core.logic       :as logic
                       :refer [#?@(:clj [fn-and])]      ]
                     [quantum.core.vars        :as var  ])
  #?(:cljs (:require-macros
                     [quantum.core.fn          :as fn
                       :refer [fn->]                    ]
                     [quantum.core.log         :as log  ]
                     [quantum.core.logic       :as logic
                       :refer [fn-and]                  ])))

; ===== EXTERN =====d

(def extern? (fn-and seq? (fn-> first symbol?) (fn-> first name (= "extern"))))

#?(:clj
(defn extern* [ns- [spec-sym quoted-obj & extra-args]]
  (if @qcore/externs?
      (do (log/pr :macro-expand "EXTERNING" quoted-obj)
          (when-not (empty? extra-args)
            (throw (Exception. (str "|extern| takes only one argument. Received: " (-> extra-args count inc)))))
          (let [genned (gensym 'externed)
                obj-evaled
                  (try (eval quoted-obj) ; Possibly extern breaks because no runtime eval? 
                    (catch Throwable e#
                      (throw (Exception. (str "Can't extern object " quoted-obj
                                              " because of error: |" e# "|")))))]
            (if (symbol? quoted-obj)
                quoted-obj
                (do (intern ns- (var/unqualify genned) obj-evaled)
                    (log/pr :macro-expand quoted-obj "EXTERNED AS" (var/unqualify genned))
                    (var/unqualify genned)))))
      quoted-obj)))

#?(:clj
(defmacro extern-
  "Dashed so as to encourage only internal use within macros."
  [obj]
  `(extern* *ns* ['extern ~obj])))

; ===== MISCELLANEOUS =====

#?(:clj
(defmacro identity*
  "For use in macros where you don't want to have the extra fn call."
  [obj] obj))

#?(:clj
(defmacro inline-replace
  "TODO IMPLEMENT
   Can use it like so:
   (quantum.core.macros/inline-replace (~f ret# elem# @i#)).
   Must be given a function definition. Will replace the arguments
   accordingly.
   Currently just yields identity."
  [obj] obj))