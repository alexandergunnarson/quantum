(ns quantum.test.core.resources
  (:require [quantum.core.resources :as ns]))

(defn test:open? [x])

(defn test:close! [x])

(defn test:closeable? [x])

(defn test:cleanup! [x])

(defn test:with-cleanup [obj cleanup-seq])

(defn test:with-resources
  [bindings & body])

; ======= SYSTEM ========

(defn test:->system
  [config make-system])

(defn test:register-system!
  [ident config make-system])

(defn test:reload-namespaces
  [namespaces])