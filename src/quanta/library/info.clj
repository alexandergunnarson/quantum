(ns quanta.library.info)

; Mechanical Turk - people test your interface!! For a certain fee per person

; Each data type created once within 1000000 |reduce+|s
; Vector:     268 ms
; Map-entry:  211 ms
; List:       354 ms
; Hash-map:  1946 ms
; Each data type created once within 100000 |reduce+|s, conj'ed, and apply'ed
; Vector:     346 ms
; Map-entry:  293 ms
; List:       -
; Hash-map:   619 ms

; DATASETS
; Map         {:a 1, :b 2, :c 3, :d 4, :e 5, :f 6, :g 7, :h 8, :i 9, :j 10}
; Record      (TestRecord. 1 2 3 4 5 6 7 8 9 10)
; Sorted Map+ (sorted-map+ :a 1, :b 2, :c 3, :d 4, :e 5, :f 6, :g 7, :h 8, :i 9, :j 10)
; Sorted Map  (sorted-map  :a 1, :b 2, :c 3, :d 4, :e 5, :f 6, :g 7, :h 8, :i 9, :j 10)
; All benchmarks using Criterium
; ALL TIMES IN ms
; LOOKUP
; (let [r <data>]
;   (bench (dotimes _ <n> (:a r))
;            Records |  Maps    | Sorted Map+ | Sorted Map
; 10000        0.028 |    0.196 |    0.358    | ...
; 100000       0.268 |    1.9   |    3.6      | ...
; 1000000      2.6   |   18.3   |   35.3      | ...
; 10000000    27.0   |  194.5   |  348.6      | ...
; 100000000  267.0   | 1912.6   | 3639        | ~3300
; Records are an average of 7.1 times faster than maps and 12.8 more than smaps.
; CREATION
; (bench (dotimes [_ <n>]
;   <data>))
;            Records |  Maps   | Sorted Map+ | Sorted Map
; 10000      0.00379 |  0.0038 | 16.55       | 20.71
; 100000     0.0366  |  0.0382 | ...         | ...
; 1000000    0.402   |  0.374  | ...         | ...
; 10000000   3.81    |  3.88   | ...         | ...
; 100000000 40.0     | 40.8    | 15961       | Didn't even finish...
; Pretty much exactly the same for records and maps.

; 11 ms
(loop [ret [] init (range 0 100000)]
  (if (empty? init)
      ret
      (recur (conj ret (first init)) (rest init))))
; 8.19 ms
(reduce+ ; reduction
  (fn [ret elem]
    (conj ret elem))
  []
  (range 0 100000))
; 5.5 ms
; "In the case of concatenating large vectors,
; the use of transients is ~4.5 times faster than the
; purely functional approach." - Joy of Clojure
(persistent!
  (reduce+ 
    (fn [ret elem]
      (conj! ret elem))
    (transient []) ; transience
    (range 0 100000)))
; 3.68 ms
(persistent!
  (reduce+ 
    (fn [ret elem]
      (conj! ret elem))
    (transient [])
    (range+ 0 100000))) ; non-laziness
; 3.8 ms
(into+ [] (range+ 0 100000))
; 5.28 ms
(let [init (->> (range+ 0 100000) (into+ []))] ; local variable
  (persistent!
    (reduce+ 
      (fn [ret elem]
        (conj! ret elem))
      (transient [])
      init)))
