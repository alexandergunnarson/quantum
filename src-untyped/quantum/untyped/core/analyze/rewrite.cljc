(ns quantum.untyped.core.analyze.rewrite)

(defn remove-do [code]
  (if (and (seq? code) (-> code first (= 'do)))
      (rest code)
      code))
