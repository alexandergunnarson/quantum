(ns quantum.interop.python
  (:require
    [clojure.string    :as str]
    [quantum.core.vars :as var])
  (:import #?(:clj [org.python.util PythonInterpreter])
           #?(:clj [org.python.core PyObject Py PyModule])))

(declare ^:dynamic *interp*)

#?(:clj
(defn append-paths
  "Appends a vector of paths to the python system path."
  {:from "rplevy/clojure-python"}
  [libpaths]
  (.exec ^PythonInterpreter *interp* "import sys")
  (doseq [p libpaths]
    (.exec ^PythonInterpreter *interp* (str "sys.path.append('" p "')")))
  *interp*))

#?(:clj
(defn init-interpreter!
  "Establish a global python interpreter. The init function is only usefully
   called once. Alternatively, only use with-interpreter."
  {:from "rplevy/clojure-python"}
  [& [{:keys [libpaths] :as options}]]
  (when (instance? clojure.lang.Var$Unbound *interp*)
    (var/reset-var! #'*interp* (PythonInterpreter.))
    (append-paths libpaths))))

#?(:clj
(defmacro with-interpreter
  "Dynamically bind a new python interpreter for the calling context."
 {:from "rplevy/clojure-python"}
  [{:keys [libpaths] :as options} & body]
  `(binding [*interp* (PythonInterpreter.)]
     (append-paths ~libpaths)
     ~@body)))

#?(:clj
(defn py-eval [s]
  (.exec ^PythonInterpreter *interp* s)))

#?(:clj
(defmacro py-import-lib
  "Import lib. Defaults to use same name it has in python. If it is something
   like foo.bar, the name is bar."
  {:from "rplevy/clojure-python"}
  [lib & libs]
  (let [lib-sym (or (last libs) lib)
        lib-strs (map name (cons lib libs))
        py-name (str/join "." lib-strs)]
    `(do (py-eval (str "import " ~py-name))
         (def ~lib-sym
              (-> ^PythonInterpreter *interp*
                  ^PyObject .getLocals
                  (.__getitem__ ^PyObject ~(first lib-strs))
                  ^PyModule ~@(map (fn [lib#] `(.__getattr__ ^PyObject ~lib#))
                         (next lib-strs))
                  .__dict__))))))

#?(:clj
(defmacro py-import-obj
  "Import objects from lib."
  {:from "rplevy/clojure-python"}
  [lib obj & objs]
  (cons 'do
        (map
         (fn [o#]
           `(def ~o# (.__finditem__ ^PyObject ~lib ~(name o#)))))
        (cons obj objs))))

(defmacro py-fn
  "Create a native clojure function applying the python wrapper calls on a python
   function at the top level of the library use this where lambda is preferred
   over named function."
  {:from "rplevy/clojure-python"}
  [lib fun]
  `(let [f# (.__finditem__
             ^PyObject ~lib
             ~(name fun))]
     (fn [& args#]
       (call f# args#))))

(defmacro import-fn
  "This is like import but it defines the imported item as a native function that
   applies the python wrapper calls."
  {:from "rplevy/clojure-python"}
  [lib fun & funs]
  (cons 'do
        (map
         (fn [fun]
           `(def ~fun (py-fn ~lib ~fun)))
         (cons fun funs))))

(defmacro __
  "Access attribute of class or attribute of attribute of (and so on) class."
  {:from "rplevy/clojure-python"}
  ([class attr]
     `(.__findattr__ ^PyObject ~class ~(name attr)))
  ([class attr & attrs]
     `(__ (__ ~class ~attr) ~@attrs)))

(defmacro _>
  "Call attribute as a method.
   Basic usage: (_> [class attrs ...] args ...)
   Usage with keyword args: (_> [class attrs ...] args ... :key arg :key arg)
   Keyword args must come after any non-keyword args"
  {:from "rplevy/clojure-python"}
  ([[class & attrs] & args]
     (let [keywords (map name (filter keyword? args))
           non-keywords (filter (fn [a] (not (keyword? a))) args)]
       `(call (__ ~class ~@attrs) [~@non-keywords] ~@keywords))))

(defn dir
  "It's slightly nicer to call the dir method in this way."
  {:from "rplevy/clojure-python"}
  [x] (seq (.__dir__ ^PyObject x)))

(defn pyobj-nth
  "Nth item in a 'PyObjectDerived'."
  {:from "rplevy/clojure-python"}
  [o i] (.__getitem__ ^PyObject o (int i)))

(defn pyobj-range
  "Access 'PyObjectDerived' items as non-lazy range."
  {:from "rplevy/clojure-python"}
  [o start end] (for [i (range start end)] (pyobj-nth o i)))

(defn pyobj-iterate
  "Access 'PyObjectDerived' items as Lazy Seq."
  {:from "rplevy/clojure-python"}
  [pyobj] (lazy-seq (.__iter__ ^PyObject pyobj)))

(defn java2py
  "To wrap java objects for input as jython, and unwrap Jython output as java."
  {:from "rplevy/clojure-python"}
  [args]
  (into-array
   PyObject
   (map #(Py/java2py %) args)))

(defn call
  "The first len(args)-len(keywords) members of args[] are plain arguments. The
   last len(keywords) arguments are the values of the keyword arguments."
  {:from "rplevy/clojure-python"}
  [fun args & key-args]
  (.__tojava__
    ^PyObject
    (if key-args
        (.__call__ ^PyObject fun ^"[Lorg.python.core.PyObject;" (java2py args) ^"[Ljava.lang.String;" (into-array String key-args))
        (.__call__ ^PyObject fun ^"[Lorg.python.core.PyObject;" (java2py args)))
    Object))

