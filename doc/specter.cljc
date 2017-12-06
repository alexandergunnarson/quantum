; Increment every even number nested within map of vector of maps
(def data {:a [{:aa 1 :bb 2}
               {:cc 3}]
           :b [{:dd 4}]})
; Manual Clojure
(map-vals
  (fn [v]
    (mapv
      (fn [m]
        (map-vals
          (fn [v] (if (even? v) (inc v) v))
          m))
      v))
  data)
; Quantum
(->> data  (map-vals' (map' (map-vals' (whenf even?  inc)))))
; Specter
(transform [MAP-VALS   ALL   MAP-VALS         even?] inc data)

; Append a sequence of elements to a nested vector
(def data {:a [1 2 3]})
; Quantum
(update data :a (fn1 join [4 5]))
; Specter
(setval [:a END] [4 5] data)

; Increment the last odd number in a sequence
(def data [1 2 3 4 5 6 7 8])
; Quantum
(update data (last-index-of odd? data) inc)
; Specter
(transform [(filterer odd?) LAST] inc data)

; Map a function over a sequence without changing the type or order of the sequence
; Quantum
(map' inc data)
; Specter
(transform ALL inc data)

;Increment all the values in maps of maps:
(def data {:a {:aa 1} :b {:ba -1 :bb 2}})
; Quantum
(->> data (map-vals' (map-vals' inc)))
; Specter
(transform [MAP-VALS MAP-VALS] inc)
=> {:a {:aa 2}, :b {:ba 0, :bb 3}}

; Increment all the even values for :a keys in a sequence of maps:
(def data [{:a 1} {:a 2} {:a 4} {:a 3}])
; Quantum
(->> data (map' (fn1 update :a (whenf1 even? inc))))
; Specter
(transform [ALL :a even?] inc data)
=> [{:a 1} {:a 3} {:a 5} {:a 3}]

; Retrieve every number divisible by 3 out of a sequence of sequences:
(def data [[1 2 3 4] [] [5 3 2 18] [2 4 6] [12]])
; Quantum
(->> data (mapcat' (filter' (fn1 divisible? 3))))
; Specter
(select [ALL ALL #(= 0 (mod % 3))] data)
=> [3 3 18 6 12]

; Increment the last odd number in a sequence:
(def data [2 1 3 6 9 4 8])
; Quantum
(update data (last-index-of odd? data) inc) ; update data nil leaves data as-is?
; Specter
(transform [(filterer odd?) LAST] inc data)
=> [2 1 3 6 10 4 8]

;Increment all the odd numbers between indices 1 (inclusive) and 4 (exclusive):
(def data [0 1 2 3 4 5 6 7])
; Quantum
(->> data (filter+ odd?) indexed+ (map' #(if ((range? 1 4) %1) (inc %2) %2)))
; Specter
(->> data (transform [(srange 1 4) ALL odd?] inc))
=> [0 2 2 4 4 5 6 7]

; Replace the subsequence from indices 2 to 4 with [:a :b :c :d :e]:
(def data [0 1 2 3 4 5 6 7 8 9])
; Quantum
(join (getr 0 1) data (getr 4 :last)) ; TODO this is not perfectly general
; Specter
(setval (srange 2 4) [:a :b :c :d :e])
[0 1 :a :b :c :d :e 4 5 6 7 8 9]

; Concatenate the sequence [:a :b] to every nested sequence of a sequence:
(def data [[1] '(1 2) [:c]])
; Quantum
(->> data (map' (fn1 joinr [:a :b])))
; Specter
(setval [ALL END] [:a :b] data)
=> [[1 :a :b] (1 2 :a :b) [:c :a :b]]

; Get all the numbers out of a data structure, no matter how they're nested:
(def data {2 [1 2 [6 7]] :a 4 :c {:a 1 :d [2 nil]}})
; Quantum
(postwalk-filter number? data)
; Specter
(select (walker number?) data)
=> [2 1 2 1 2 6 7 4]

; Navigate via non-keyword keys:
(def data {"a" {"b" 10}})
; Quantum
(get-in data ["a" "b"])
; Specter
(select [(keypath "a") (keypath "b")])
=> [10]

; Reverse the positions of all even numbers between indices 4 and 11:
(def data [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15])
; Quantum
; TODO
; Specter
(transform [(srange 4 11) (filterer even?)] reverse data)
=> [0 1 2 3 10 5 8 7 6 9 4 11 12 13 14 15]

; Append [:c :d] to every subsequence that has at least two even numbers:
(def data [[1 2 3 4 5 6] [7 0 -1] [8 8] []])
; Quantum
(->> data (map' (whenf1 #(-> (filter even? %) count (>= 2)) (fn1 joinr [:c :d]))))
; Specter
(setval [ALL (selected? (filterer even?) (view count) #(>= % 2)) END] [:c :d] data)
=> [[1 2 3 4 5 6 :c :d] [7 0 -1] [8 8 :c :d] []]

; When doing more involved transformations, you often find you lose context when navigating deep within a data structure and need information "up" the data structure to perform the transformation. Specter solves this problem by allowing you to collect values during navigation to use in the transform function. Here's an example which transforms a sequence of maps by adding the value of the :b key to the value of the :a key, but only if the :a key is even:
(def data [{:a 1 :b 3} {:a 2 :b -10} {:a 4 :b 10} {:a 3}])
; Quantum
(->> data (map' #(whenf % (fn-> :a even?) (updatef :a + (:b %)))))
; Specter
(transform [ALL (collect-one :b) :a even?] + data)
=> [{:b 3, :a 1} {:b -10, :a -8} {:b 10, :a 14} {:a 3}]

The transform function receives as arguments all the collected values followed by the navigated to value. So in this case + receives the value of the :b key followed by the value of the :a key, and the transform is performed to :a's value.

The four built-in ways for collecting values are VAL, collect, collect-one, and putval. VAL just adds whatever element it's currently on to the value list, while collect and collect-one take in a selector to navigate to the desired value. collect works just like select by finding a sequence of values, while collect-one expects to only navigate to a single value. Finally, putval adds an external value into the collected values list.

; Increment the value for :a key by 10:
(def data {:a 1 :b 3})
; Quantum
(update data :a (fn1 + 10))
; Specter
(transform [:a (putval 10)] + data)
=> {:b 3 :a 11}

; For every map in a sequence, increment every number in :c's value if :a is even, or increment :d if :a is odd:
(def data [{:a 2 :c [1 2] :d 4} {:a 4 :c [0 10 -1]} {:a -1 :c [1 1 1] :d 1}])
; Quantum
(->> data (map' (ifn1 (fn-> :a even?) (updatef :c (map' inc)) (updatef :d inc))))

(->> data (map' #(apply update % (if (-> % :a even?) [:c (map' inc)] [:d inc]))))

(->> data (map' (fn1 cond-apply update (fn-> :a even?) [:c (map' inc)] [:d inc])))
; Specter
(->> data (transform [ALL (if-path [:a even?] [:c ALL] :d)] inc))
[{:c [2 3], :d 4, :a 2} {:c [1 11 0], :a 4} {:c [1 1 1], :d 2, :a -1}]

