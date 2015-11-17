



; public class Util

(defn instance-of-class?
  "Tests if @objectClass is the same class as, or sub-class of,
   or implements @typeClass."
  {:source "Util/instanceOf"
   :todo   ["Probably should be memoized or something"]}
  [^Class objectClass ^Class typeClass]
  (or (= objectClass typeClass)
      (if (.isInterface objectClass)
          (if (.isInterface typeClass)
              (ffilter (eq? iface typeClass  ) (.getInterfaces objectClass))
              (ffilter (eq? iface objectClass) (.getInterfaces typeClass  )))
          (if (.isInterface typeClass)
              (while (nnil? objectClass)
                (doseq
                  [iface (.getInterfaces objectClass)]
                  (when (= iface typeClass) (return true)))
                (swap! objectClass (.getSuperclass objectClass)))
              (while (nnil? objectClass)
                (when (= objectClass typeClass) (return true))
                (swap! objectClass (.getSuperclass objectClass)))))))