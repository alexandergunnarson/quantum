(ns quantum.test.core.macros.optimization
  quantum.core.macros.optimization)

; ===== EXTERN =====

(defn extern? [x])

(defn test:extern* [ns- [spec-sym quoted-obj & extra-args]])

(defn test:extern- [obj])

; ===== MISCELLANEOUS =====

(defn test:identity* [obj])

(defn test:inline-replace [obj])