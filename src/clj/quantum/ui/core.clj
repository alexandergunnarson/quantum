(ns
  ^{:doc "Originally filled with a terribly old and inefficient JavaFX library
          based off of zilti/clojurefx. The multimethods and reflection
          here was incredibly slow. This ns awaits aaronc/freactive."
    :attribution  "zilti/clojurefx"
    :contributors #{"Alex Gunnarson"}}
  quantum.ui.core
  (:gen-class))

(require '[quantum.core.ns :as ns])
(ns/require-all *ns* :lib :clj :java-fx)
(ns/nss *ns*)