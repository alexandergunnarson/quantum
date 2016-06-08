(in-ns 'quantum.core.collections.core)
(require '[quantum.core.meta.bench :refer [bench]])

(def test:byte (byte 1))
(def test:Byte (Byte. (byte 1)))
(def test:long (long 2))
(def test:Long (Long. (long 2)))

; TEST 1, VARIATION 1 (protocol)

5.327  ms (baseline)          : (bench (dotimes [i 100000] (core// test:Long test:byte)))
3.515  ms (1.52 times faster) : (bench (dotimes [i 100000] (div-2  test:Long test:byte)))

; TEST 1, VARIATION 2 (protocol)

7.453  ms (baseline)          : (bench (dotimes [i 100000] (core// test:byte test:Long)))
4.144  ms (1.80 times faster) : (bench (dotimes [i 100000] (div-2  test:byte test:Long)))

; TEST 2, VARIATION 1 (heterogeneous primitive arguments, interface)

4.873  ms (baseline)          : (let [l (long test:Long)
                                      b (byte test:byte)]
                                  (bench (dotimes [i 100000] (core// l b))))
80.995 µs (60 times faster)   : (let [l (long test:Long)
                                      b (byte test:byte)]
                                  (bench (dotimes [i 100000] (div-2- l b))))

; TEST 2, VARIATION 2 (homogeneous primitive arguments, interface)

6.948  ms (baseline)          : (let [l1 (long test:byte)
                                      l2 (long test:long)]
                                  (bench (dotimes [i 100000] (core// l1 l2))))

80.754 µs (86 times faster)   : (let [l1 (long test:byte)
                                      l2 (long test:long)]
                                  (bench (dotimes [i 100000] (div-2- l1 l2))))


; ===== ARRAYS ===== ;

(def test:arr-float1 (float-array 30))
(def test:arr-float5 (make-array Float/TYPE 5 4 3 2 1))

; AGET

112        ms : (bench (dotimes [i 10000] (core/aget                             test:arr-float1 3))) ; reflection
4.326      ms : (bench (dotimes [i 10000] (core/get                              test:arr-float1 3)))
1.111      ms : (bench (dotimes [i 10000] (java.lang.reflect.Array/get           test:arr-float1 3)))
58.890     µs : (bench (dotimes [i 10000] (aget                                  test:arr-float1 3))) ; protocol
14.789     µs : (bench (dotimes [i 10000] (aget                        ^floats   test:arr-float1 3))) ; extra time because extra (reify) fn call
9.754      µs : (bench (dotimes [i 10000] (core/aget                   ^floats   test:arr-float1 3))) ; no reflection
9.531      µs : (bench (dotimes [i 10000] (Array/get                   ^floats   test:arr-float1 3)))
   
; AGET-IN*
(def cache (doto (java.util.HashMap.)
                 (.put (class (apply make-array Float/TYPE (repeat 5 0)))
                       (fn [x i1 i2 i3 i4 i5] (aget-in* ^"[[[[[F" x (int i1) (int i2) (int i3) (int i4) (int i5))))))


314.878    µs : (bench (dotimes [i 10000] (aget-in*                              test:arr-float5 4 3 2 1 0)))
100.789    µs : (bench (dotimes [i 10000] ((.get ^java.util.HashMap cache (class test:arr-float5))
                                             test:arr-float5 4 3 2 1 0)))
39.328     µs : (bench (dotimes [i 10000] (aget-in*                    ^"[[[[[F" test:arr-float5 4 3 2 1 0)))
38.000     µs : (bench (dotimes [i 10000] (Array/get                   ^"[[[[[F" test:arr-float5 4 3 2 1 0)))
; 4.22 times as slow as the single-depth array — should have been 5, but close enough to expected.



; AGET-IN

6.139      ms : (let [f #(java.lang.reflect.Array/get %1 %2)
                      v [4 3 2 1 0]]
                  (bench (dotimes [i 10000] (reduce f test:arr-float5 v))))   
4.674      ms : (let [f #(aget %1 %2)
                      v [4 3 2 1 0]]
                  (bench (dotimes [i 10000] (reduce f test:arr-float5 v))))      
4.353      ms : (let [v [4 3 2 1 0]]
                  (bench (dotimes [i 10000] (aget-in                   ^"[[[[[F" test:arr-float5 v))))
4.299      ms : (let [v [4 3 2 1 0]]
                  (bench (dotimes [i 10000] (apply aget-in*-protocol             test:arr-float5 v))))
3.180      ms : (let [v [4 3 2 1]]
                  (bench (dotimes [i 10000] (apply aget-in*-protocol             test:arr-float5 v))))
2.6681     ms : (let [v [4 3 2]]
                  (bench (dotimes [i 10000] (apply aget-in*-protocol             test:arr-float5 v))))
2.187      ms : (let [v [4 3]]
                  (bench (dotimes [i 10000] (apply aget-in*-protocol             test:arr-float5 v))))
1.447      ms : (let [v [4]]
                  (bench (dotimes [i 10000] (apply aget-in*-protocol             test:arr-float5 v))))
910.083    µs : (let [v [4]]
                  (bench (dotimes [i 10000] (aget-in                   ^"[[[[[F" test:arr-float5 v))))
; So most of the time is spent in processing and unpacking the arguments




