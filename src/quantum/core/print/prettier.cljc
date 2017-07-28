(ns quantum.core.print.prettier
  (:require
    [fipp.ednize]))

; TODO get rid of this fipp boilerplate
(in-ns 'fipp.ednize)

(defprotocol IOverride
  "Mark object as preferring its custom IEdn behavior.")

(defn override? [x]
  (satisfies? IOverride x))

(in-ns 'quantum.core.print.prettier)

(defn extend-pretty-printing! []
  (extend-type java.util.ArrayList
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '! (vec this))))
  (extend-type it.unimi.dsi.fastutil.longs.LongArrayList
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '!l (vec this))))

  (extend-type (Class/forName "[Z")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '?<> (vec this))))
  (extend-type (Class/forName "[B")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'b<> (vec this))))
  (extend-type (Class/forName "[C")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'c<> (vec this))))
  (extend-type (Class/forName "[[C")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'c<><> (into [] this)))) ; TODO faster
  (extend-type (Class/forName "[S")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 's<> (vec this))))
  (extend-type (Class/forName "[I")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'i<> (vec this))))
  (extend-type (Class/forName "[J")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'l<> (vec this)))) ; TODO ->vec for this
  (extend-type (Class/forName "[F")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'f<> (vec this))))
  (extend-type (Class/forName "[D")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'd<> (vec this)))) ; TODO ->vec for this
  (extend-type (Class/forName "[[D")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'd<><> (into [] this)))) ; TODO faster
  (extend-type (Class/forName "[Ljava.lang.Object;")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '*<> (vec this))))
  (extend-type (Class/forName "[[Ljava.lang.Object;")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '*<><> (into [] this))))
  (extend-type (Class/forName "[Ljava.lang.Class;")
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal 'class<> (vec this))))

  (extend-type java.util.HashMap
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '! (into {} this))))
  (extend-type java.util.concurrent.ConcurrentHashMap
    fipp.ednize/IOverride
    fipp.ednize/IEdn
      (-edn [this] (tagged-literal '!! (into {} this))))) ; TODO ->map
