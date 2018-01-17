(ns quantum.core.untyped.vars)

(def update-meta vary-meta)

(defn merge-meta-from   [to from] (update-meta to merge (meta from)))
(defn replace-meta-from [to from] (with-meta to (meta from)))
