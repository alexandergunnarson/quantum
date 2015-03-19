; (comment
; (require '[clojure.core.reduce :as r])
; (def v (take 1000000 (range)))
; (reduce + 0 (r/map inc [1 2 3 4]))
; (into [] (r/take 12 (range 100)))
; (into [] (r/drop 12 (range 100)))
; (reduce + 0 (r/filter even? [1 2 3 4]))
; (into [] (r/filter even? [1 2 3 4]))
; (reduce + (filter even? [1 2 3 4]))
; (dotimes [_ 10] (time (reduce + 0 (r/map inc v))))
; (dotimes [_ 10] (time (reduce + 0 (map inc v))))
; (dotimes [_ 100] (time (reduce + 0 v)))
; (dotimes [_ 100] (time (reduce + 0 v)))
; (dotimes [_ 20] (time (reduce + 0 (r/map inc (r/filter even? v)))))
; (dotimes [_ 20] (time (reduce + 0 (map inc (filter even? v)))))
; (reduce + 0 (r/take-while even? [2 4 3]))
; (into [] (r/filter even? (r/flatten (r/remove #{4} [[1 2 3] 4 [5 [6 7 8]] [9] 10]))))
; (into [] (r/flatten nil))
; )



43.51 ms (reduce+ + (range  1000000)) 
43.17 ms (fold+   + (range  1000000)) 
23.91 ms (reduce+ + (range+ 1000000)) ; because of splitting the vector in two
11.63 ms (fold+   + (range+ 1000000)) ; because of 4 cores :D

; /foldcat+/ is about 5% faster than /vec+/
; doall:    70.257915 ms
; vec+:     54.162415 ms (into)
; foldcat+: 49.738498 ms


(def a (vec (range 1000000)))

9225 ms (reduce conj #{} a)
5981 ms (persistent! (reduce conj! (transient #{}) a))
6056 ms (into #{} a)

9639 ms (r/fold   (monoid into hash-set) conj a)
6859 ms (r/fold n (monoid into hash-set) conj a)
3654 ms (r/fold   (monoid (fn [r l] (clojure.lang.PersistentHashSet/splice r l)) hash-set) conj a)
3288 ms (r/fold n (monoid (fn [r l] (clojure.lang.PersistentHashSet/splice r l)) hash-set) conj a)



; LARGE-SIZE
GC OutOfMemoryError     (->> (range  10000000) redv)
GC OutOfMemoryError     (->> (range+ 10000000) redv)
; MID-SIZE
554 µs  639  µs 931  µs (->> (range  10000) redv )
603 µs  660  µs 880  µs (->> (range+ 10000) redv )
488 µs  886  µs 1231 µs (->> (range  10000) doall) ; redo this benchmark
; SMALL-SIZE
30.0 µs 32.9 µs 42.5 µs (->> (range  10) redv)
2.72 µs 3.02 µs 3.99 µs (->> (range+ 10) redv)
0.93 µs 1.10 µs 1.62 ms (->> (range  10) doall) ; redo this benchmark



6.0  ms (doseq   [elem v]) 
7.7  ms (doseq+  [elem v]) 
7.0  ms (doseq++ [elem v]) 
6.7  ms (doseq   [elem v1])
10.6 ms (doseq+  [elem v1])
9.3  ms (doseq++ [elem v1])


31.1 ms (25.4 ms - 43.9 ms) (-> (for    [elem v]  nil) doall)
24.0 ms (21.3 ms - 30.4 ms) (-> (for+   [elem v]  nil)      )
52.3 ms (45.2 ms - 72.0 ms) (-> (r/for+ [elem v]  nil)      )
22.8 ms (21.2 ms - 30.2 ms) (-> (for++  [elem v]  nil)      )
34.8 ms (29.1 ms - 49.2 ms) (-> (for    [elem v1] nil) doall)
24.0 ms (22.8 ms - 32.4 ms) (-> (for+   [elem v1] nil)      )
54.4 ms (48.9 ms - 73.5 ms) (-> (r/for+ [elem v1] nil)      )
24.8 ms (22.9 ms - 35.0 ms) (-> (for++  [elem v1] nil)      )