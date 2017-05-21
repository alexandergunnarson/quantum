(ns quantum.test.core.resources
  (:require
    [com.stuartsierra.component :as comp]
    [quantum.core.resources     :as ns]
    [quantum.core.test          :as test
      :refer [deftest is testing]]))

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

(defonce starts (atom []))
(defonce stops  (atom []))

(defrecord TestSystem [id]
  comp/Lifecycle
  (start [this] (swap! starts conj id) this)
  (stop  [this] (swap! stops  conj id) this))

(deftest test:connections
  (let [system-map
         (comp/system-map
           ::system-0 (comp/using (->TestSystem 0) [::system-1])
           ::system-1 (comp/using (->TestSystem 1) [::system-2])
           ::system-2 (comp/using (->TestSystem 2) [::system-3])
           ::system-3 (->TestSystem 3))]
    (doseq [[f ks starts' stops']
            [[ns/start-components!                  [::system-1]            [1      ] [       ]]
             [ns/start-dependencies!                [::system-1]            [3 2    ] [       ]]
             [ns/start-components-and-dependencies! [::system-1]            [3 2 1  ] [       ]]
             [ns/start-dependents!                  [::system-1]            [0      ] [       ]]
             [ns/start-components-and-dependents!   [::system-1]            [1 0    ] [       ]]
             [ns/stop-components!                   [::system-1]            [       ] [1      ]]
             [ns/stop-components-and-dependencies!  [::system-1]            [       ] [1 2 3  ]]
             [ns/stop-dependents!                   [::system-1]            [       ] [0      ]]
             [ns/stop-components-and-dependents!    [::system-1]            [       ] [0 1    ]]

             [ns/start-components!                  [::system-3]            [3      ] [       ]]
             [ns/start-dependencies!                [::system-3]            [       ] [       ]]
             [ns/start-components-and-dependencies! [::system-3]            [3      ] [       ]]
             [ns/start-dependents!                  [::system-3]            [2 1 0  ] [       ]]
             [ns/start-components-and-dependents!   [::system-3]            [3 2 1 0] [       ]]
             [ns/stop-components!                   [::system-3]            [       ] [3      ]]
             [ns/stop-components-and-dependencies!  [::system-3]            [       ] [3      ]]
             [ns/stop-dependents!                   [::system-3]            [       ] [0 1 2  ]]
             [ns/stop-components-and-dependents!    [::system-3]            [       ] [0 1 2 3]]

             [ns/start-components!                  [::system-2 ::system-0] [2 0    ] [       ]]
             [ns/start-dependencies!                [::system-2 ::system-0] [3 2 1  ] [       ]]
             [ns/start-components-and-dependencies! [::system-2 ::system-0] [3 2 1 0] [       ]]
             [ns/start-dependents!                  [::system-2 ::system-0] [1 0    ] [       ]]
             [ns/start-components-and-dependents!   [::system-2 ::system-0] [2 1 0  ] [       ]]
             [ns/stop-components!                   [::system-2 ::system-0] [       ] [0 2    ]]
             [ns/stop-components-and-dependencies!  [::system-2 ::system-0] [       ] [0 1 2 3]]
             [ns/stop-dependents!                   [::system-2 ::system-0] [       ] [0 1    ]]
             [ns/stop-components-and-dependents!    [::system-2 ::system-0] [       ] [0 1 2  ]]]]
      (testing ks
        (swap! starts empty)
        (swap! stops empty)
        (f system-map ks)
        (is (= @starts starts'))
        (is (= @stops  stops'))))))

