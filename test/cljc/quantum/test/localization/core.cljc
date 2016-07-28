(ns quantum.test.localization.core
  (:require [quantum.core.string      :as str     ]
            [quantum.core.collections :as coll
              :refer [#?@(:clj [containsv? join])
                      dropr filter+ remove+] ]
            [quantum.core.fn          :as fn
              :refer [#?@(:clj [fn->])]           ]
            [quantum.core.logic       :as logic
              :refer [#?@(:clj [fn-or])]          ]
            [quantum.net.http         :as http    ]))

#?(:clj
(defn test:iana-languages []))

#?(:clj
(defn test:iana-all []))

#?(:clj
(defn test:iana-regions []))