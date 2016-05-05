(ns quantum.compile.transpile.javaesque
  (:require
    [quantum.compile.transpile.to.java    :as javac]
    [quantum.compile.transpile.to.c-sharp :as csc  ]
    [quantum.compile.transpile.util       :as util ]
    [quantum.compile.transpile.core       :as comp ]
    [quantum.core.string                  :as str  ]
    [quantum.core.io.core                 :as io   ]))

(defn ^String class-str [access ^String class-name & [^String contents]]
  (util/bracket (str/sp (name access) "class" class-name) contents))

(def ks
  {:java   
    {:file-ext "java"
     :ns-fn    javac/package}
   :c-sharp
    {:file-ext "cs"
     :ns-fn    csc/nspace}})

#_(:clj
(defn emit!
  {:todo ["Put the package + imports + etc. in the java-parse thing"]
   :in '[[:test "in.clj"] [:resources "out"]]}
  [in-path-vec out-path-vec lang]

  (let [^String in-str         (io/get {:path in-path-vec :read-method :str})
        ^String in-str-wrapped (str "(do \n" in-str "\n)") ; Wrapped because otherwise |read-str| fails
                in-code        (read-string in-str-wrapped)

                ns-declaration (-> in-code first rest)
                ; qjava.Main
                lang-attrs     (-> ks (get lang))
        ^String ns-name-str    (-> ns-declaration first rest name)
                ns-name-vec    (str/split ns-name-str #"\.")
        ^String class-name     (last ns-name-vec)
        ^String out-file-name  (str class-name "." (-> lang-attrs :file-ext))
        ^String package-name   (str/join "." (butlast ns-name-vec))
        ^String class-contents (->> in-code rest rest (comp/eval-form lang))
        ^String class-str-f    (class-str :public class-name class-contents)
        ^String out-str        ((-> lang-attrs :ns-fn) class-str-f package-name)]
    (println "FINAL STRING:")
    (println out-str)
    ; TODO fix path here... just a problem with io package
    (spit (io/path ""
            "qcompile" "resources" "out" out-file-name)
      out-str)
  )))

#_(:clj
(defn emit-std! []
  (emit! [:test "in.clj"] [:resources "out"] :c-sharp)))

;(emit-std!)