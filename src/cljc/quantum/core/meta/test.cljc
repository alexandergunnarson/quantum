(ns quantum.core.meta.test)

#?(:clj
(defmacro qtest
  {:attribution "Alex Gunnarson"}
  [f-sym]
  `(let [var-f#    (if (var? ~f-sym)
                       ~f-sym
                       (ns-resolve *ns* ~f-sym))
         var-meta# (meta var-f#)
         in#       (:in  var-meta#)
         out#      (:out var-meta#)]
     (if (or (nil? in#) (nil? out#))
         (println (str "Test not defined for |" var-f# "|."))
         (let [ret#        (apply (deref var-f#) (eval in#))
               out-evaled# (eval out#)]
           (if (= ret# out-evaled#)
               (println (str "Test passed for |" var-f# "|."))
               (throw
                 (Exception.
                   (str "Test failed for |" var-f# "|. "
                        "Expected " out-evaled# "; got " ret# ".")))))))))

#?(:clj
(defn qtests [sym]
  (for [test result (->  sym resolve meta :tests)]
    [test
     (try (apply (eval sym) (eval test))
        (catch Throwable e e))
      (eval result)])))

#?(:clj
(defn test-ns
  "Tests all public vars of a namespace."
  [ns-sym]
  (->> ns-sym
       ns-publics
       vals
       (map (fn [var-] (qtest var-)))
       doall)))