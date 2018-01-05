(ns quantum.core.untyped.analyze.rewrite)

(defn remove-do [code]
  (if (and (seq? code) (-> code first (= 'do)))
      (rest code)
      code))
