(ns
  ^{:doc "System-level (envinroment) vars such as *os*."
    :attribution "Alex Gunnarson"}
  quantum.core.system
  (:require-quantum [str coll])
  #?(:clj (:import java.io.File)))

#?(:clj
(def os ; TODO: make less naive
  (if (contains? (System/getProperty "os.name") "Windows")
      :windows
      :unix)))

#?(:clj (def separator (str (File/separatorChar)))) ; string because it's useful in certain functions that way
#?(:clj
(def os-sep-esc
  (case os
    :windows "\\\\"
    "/")))

#?(:clj
(defn env-var
  "Gets an environment variable."
  {:usage '(env-var "HOME")}
  [env-var-to-lookup]
  (-> (System/getenv) (get env-var-to-lookup))))

#?(:clj
(def user-env
  {"MAGICK_HOME"       (str/join separator ["usr" "local" "Cellar" "imagemagick"])
   "DYLD_LIBRARY_PATH" (str/join separator ["usr" "local" "Cellar" "imagemagick" "lib"])}))

