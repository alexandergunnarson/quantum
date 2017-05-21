(ns quantum.test.numeric.optimization
  (:require
    [#?(:clj  clojure.test
        :cljs cljs.test)
      :refer        [#?@(:clj [deftest is testing])]
      :refer-macros [          deftest is testing]]
    [quantum.numeric.optimization :as ns]))

(def knapsack-args
  [10
   [{:item 0 :weight 1 :value 1 }
    {:item 1 :weight 2 :value 7 }
    {:item 2 :weight 5 :value 11}
    {:item 3 :weight 6 :value 21}
    {:item 4 :weight 7 :value 31}]])

(deftest test:knapsack []
  (is (= (apply ns/knapsack knapsack-args)
         [[[     ] 0 ]
          [[0    ] 1 ]
          [[1    ] 7 ]
          [[0 1  ] 8 ]
          [[1 1  ] 14]
          [[0 1 1] 15]
          [[3    ] 21]
          [[4    ] 31]
          [[0 4  ] 32]
          [[1 4  ] 38]
          [[0 1 4] 39]]))
  (is (= (apply ns/knapsack-no-repeat knapsack-args)
         [[[0 []] [0 [ ]] [0 [   ]] [0  [     ]] [0  [     ]] [0  [     ]]]
          [[0 []] [1 [0]] [1 [0  ]] [1  [0    ]] [1  [0    ]] [1  [0    ]]]
          [[0 []] [1 [0]] [7 [1  ]] [7  [1    ]] [7  [1    ]] [7  [1    ]]]
          [[0 []] [1 [0]] [8 [0 1]] [8  [0 1  ]] [8  [0 1  ]] [8  [0 1  ]]]
          [[0 []] [1 [0]] [8 [0 1]] [8  [0 1  ]] [8  [0 1  ]] [8  [0 1  ]]]
          [[0 []] [1 [0]] [8 [0 1]] [11 [2    ]] [11 [2    ]] [11 [2    ]]]
          [[0 []] [1 [0]] [8 [0 1]] [12 [0 2  ]] [21 [3    ]] [21 [3    ]]]
          [[0 []] [1 [0]] [8 [0 1]] [18 [1 2  ]] [22 [0 3  ]] [31 [4    ]]]
          [[0 []] [1 [0]] [8 [0 1]] [19 [0 1 2]] [28 [1 3  ]] [32 [0 4  ]]]
          [[0 []] [1 [0]] [8 [0 1]] [19 [0 1 2]] [29 [0 1 3]] [38 [1 4  ]]]
          [[0 []] [1 [0]] [8 [0 1]] [19 [0 1 2]] [29 [0 1 3]] [39 [0 1 4]]]])))
