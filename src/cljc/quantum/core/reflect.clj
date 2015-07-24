(ns quantum.core.reflect
  (:require-quantum [ns logic fn]))

#?(:clj
(defn var-args?
  "Checks whether a function contains variable arguments."
  {:adapted "https://github.com/zcaudate/hara/blob/master/src/hara/function/args.clj"}
  [^clojure.lang.Fn f]
  (->> f class (.getDeclaredMethods)
       (some (fn [^java.lang.reflect.Method mthd]
          (= "getRequiredArity" (.getName mthd)))))))

(defn var-arg-count
  "Counts the number of arguments types before variable arguments."
  {:adapted "https://github.com/zcaudate/hara/blob/master/src/hara/function/args.clj"}
  [f]
  (when (var-args? f)
    (.getRequiredArity ^clojure.lang.RestFn f)))

(defn arg-count
  "Counts the number of non-varidic argument types"
  {:tests '{(fn [x])            [1]
            (fn [x & xs])       []
            (fn ([x]) ([x y]))  [1 2]}}
  {:adapted "https://github.com/zcaudate/hara/blob/master/src/hara/function/args.clj"}
  [f]
  (let [ms (filter (fn [^java.lang.reflect.Method mthd]
                     (= "invoke" (.getName mthd)))
                   (.getDeclaredMethods (class f)))
        ps (map (fn [^java.lang.reflect.Method m]
                  (.getParameterTypes m)) ms)]
    (map count ps)))

; (defn op
;   "loose version of apply. Will adjust the arguments to put into a function
;   (op + 1 2 3 4 5 6) => 21
;   (op (fn [x] x) 1 2 3) => 1
;   (op (fn [_ y] y) 1 2 3) => 2
  
;   (op (fn [_] nil)) => (throws Exception)"
;   {:added "2.1"}
;   [f & args]
;   (let [nargs (count args)
;         vargs (varg-count f)]
;     (if (and vargs (>= nargs vargs))
;       (apply f args)
;       (let [fargs (arg-count f)
;             candidates (filter #(<= % nargs) fargs)]
;         (if (empty? candidates)
;           (throw (Exception. (str "arguments have to be of at least length " (apply min fargs))))
;           (let [cnt (apply max candidates)]
;             (apply f (take cnt args))))))))