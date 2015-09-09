(ns ^{:doc "A parser for. Note that it is a compiler and not an interpreter."}
  quantum.core.eval
  (:refer-clojure :exclude [eval])
  (:require-quantum [ns macros type log err]))


(comment
  "3.3.2 An Application may not download or install executable code.
  Interpreted code may only be used in an Application if all scripts,
  code and interpreters are packaged in the Application and not
  downloaded. The only exception to the foregoing is scripts
  and code downloaded and run by Apple's built- in WebKit
  framework, provided that such scripts and code do not
  change the primary purpose of the Application by providing
  features or functionality that are inconsistent with the
  intended and advertised purpose of the Application as submitted
  to the App Store."
  )

; Forget about it. Just download the serialized code! :D

; Pre-evaled symbols table. This will be replaced by symbol-context.
(def symbols
  {'+   +
   'get get})

(comment
  (read-string "(+ 1 2)") ; I think read-string should be fine?
  '(+ 1 2)
  (let [f 	   (get symbols '+)
  		args   [1 2]
  		evaled (apply f args)]))

; Postwalk the forms, probably.

; Pre-compiled source is not possible on iOS.
; Interop isn't really recommended, because it would cause heavy reflection.
; For |(.getAttribute elem)| it would require reflecting to the method
; named "getAttribute" of @elem with x arguments of [a...b] types.

; '(fn [a b]
; 	(+ a b)) 
; (let [arglist '[a b]
; 	  body    '(do (+ a b))]
;   ...)
; It would recursively replace the symbols with what they are lexically bound to.
; This is certainly possible.
; Memoize it all, so you don't have to create the same objects every time.
(def lookup-lexical identity)
(defnt eval
  ([^list? x] (let [f    (get symbols (first x))
  	     args (rest x)]
       (fn [] (apply f args))))
  ([^fn?     x] (x)) ; This was a genned function
  ([^symbol? x] (lookup-lexical x)))

; => (def my-sym-table {'+ +}) ((get my-sym-table '+) 1 2)
; => RESULT ((fn [] (apply ((fn [] (get my-sym-table '+))) 1 2)))
; => (eval '(+ 1 2)) => #my_fn_genned => (eval ) => 3

#?(:clj
(defn resolve-ns
  "resolves the namespace or else returns nil if it does not exist
  (resolve-ns 'clojure.core) => 'clojure.core
  (resolve-ns 'clojure.core/some) => 'clojure.core
  (resolve-ns 'clojure.hello) => nil"
  [^clojure.lang.Symbol sym]
  (let [nsp  (.getNamespace sym)
        nsym (or  (and nsp
                       (symbol nsp))
                  sym)]
    (if nsym
      (err/suppress (do (require nsym) nsym))))))

