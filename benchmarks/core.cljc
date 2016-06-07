
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