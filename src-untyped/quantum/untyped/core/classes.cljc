(ns quantum.untyped.core.classes
#?(:clj
  (:import
    java.lang.reflect.Modifier)))

#?(:clj (defn final?     [x] (and (class? x) (Modifier/isFinal (.getModifiers ^Class x)))))
#?(:clj (defn interface? [x] (and (class? x) (.isInterface ^Class x))))
#?(:clj (defn static?    [x] (and (class? x) (Modifier/isStatic (.getModifiers ^Class x)))))
#?(:clj (defn primitive? [x] (and (class? x) (.isPrimitive ^Class x))))
#?(:clj (defn array?     [x] (and (class? x) (.isArray     ^Class x))))

