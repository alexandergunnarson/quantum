(ns quantum.analyze.clojure.predicates
  (:require-quantum [:lib]))

(defn form-and-begins-with? [sym]
  (fn-and listy? (fn-> first (= sym))))
(defn form-and-begins-with-any? [set-n]
  (fn-and listy? (fn [x] (apply splice-or (first x) = set-n))))

(def else-pred?             (fn-or (eq? :else) (eq? true)))

(def str-expression?    (fn-and listy? (fn-> first (= 'str))))
(def string-concatable? (fn-or string? str-expression?))
; STATEMENTS
(def return-statement?      (form-and-begins-with? 'return))
(def defn-statement?        (form-and-begins-with? 'defn  ))
(def fn-statement?          (form-and-begins-with? 'fn  ))
(def function-statement?    (fn-or defn-statement? fn-statement?))
(def scope?                 (form-and-begins-with-any? '#{defn fn while when doseq for do}))
(def let-statement?         (form-and-begins-with? 'let   ))
(def do-statement?          (form-and-begins-with? 'do   ))
(def if-statement?          (form-and-begins-with? 'if    ))
(def cond-statement?        (form-and-begins-with? 'cond  ))
(def when-statement?        (form-and-begins-with? 'when  ))
; CONDITIONAL BRANCHES
(def one-branched?          (fn-or when-statement?
                                   (fn-and if-statement?   (fn-> count (= 3)))
                                   (fn-and cond-statement? (fn-> count (= 3)))))
(def two-branched?          (fn-or (fn-and if-statement?   (fn-> count (= 4)))
                                   (fn-and cond-statement? (fn-> count (= 5))
                                                           (fn-> (nth 3) else-pred?))))
(def many-branched?         (fn-and cond-statement?
                              (fn-or (fn-and (fn-> count (= 5))
                                             (fn-> (nth 3) (fn-not else-pred?)))
                                     (fn-> count (> 5)))))
(def conditional-statement? (fn-or cond-statement? if-statement? when-statement?))
(def cond-foldable?         (fn-and two-branched?
                              (fn-or (fn-and if-statement?   (fn-> (nth 3) conditional-statement?))
                                     (fn-and cond-statement? (fn-> (nth 4) conditional-statement?)))))