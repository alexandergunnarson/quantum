(ns quantum.compile.transpile.to.java
  (:require [quantum.compile.transpile.util :as util])
  #?(:clj (:import org.eclipse.jdt.core.formatter.CodeFormatter
                   org.eclipse.jdt.internal.formatter.DefaultCodeFormatter
                   org.eclipse.jface.text.Document)))

#?(:clj
(defn format-java
  "Formats Java code to be pretty."
  {:attribution "ztellman, http://blog.factual.com/using-clojure-to-generate-java-to-reimplement-clojure"
   :tests '{"private   void   sayHello(  ){ System.out.println(\"hello\"  ) ;  }"
            "private void sayHello() {
                  System.out.println(\"hello\");
              }"}}
  [s]
  (let [f  (DefaultCodeFormatter.)
        te (.format f CodeFormatter/K_UNKNOWN s 0 (count s) 0 nil)
        d  (Document. s)]
    (.apply te d)
    (.get d))))

(defn package [^String class-str ^String package-name]
  (let [^String package-str (str (util/semicoloned "package" package-name) "\n")]
    (str package-str
       class-str)))


; public static void main(String [] args) {
;   System.out.println(keyword_1);
; }

; (defn main [^Array<String> args]
;   (println "HELLO JAVA WORLD! :D"))

; ; Should probably be recursive
; (defn java-parse [args]
;   (if (nempty? args)
;       ))