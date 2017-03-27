(ns quantum.test.ai.ml.instance.selection)

; This is useful mainly just for thinking about, and also as an example
#_(let [ct:x••     1000
      ct:x•      3
      ; Produce some vectors in R^n
      r   (java.util.Random.)
      x•• (ccoll/->doubles-nd ct:x•• ct:x•)
      _   (dotimes [i ct:x•• j ct:x•]
            (assoc-in!* x•• (.nextDouble #_.nextGaussian r) i j))
      ; Interestingly, there's a correlation here... it's due to the Gaussian distribution
      ; 1  possible label class   : ct' = 13 (this if for |x••| = 100, 1K, 100K, 1M )
      ; 2  possible label classes : ct' = 26
      ; 3  possible label classes : ct' = 39
      ; 4  possible label classes : ct' = 52
      ; 5  possible label classes : ct' = 65
      ; ...
      ; 10 possible label classes : ct' = 130
      ; 20 possible label classes : ct' = 260
      ; 28 possible label classes : ct' = 364
      ; 29 possible label classes : variance! : ct' =  375-377
      ; 30 possible label classes : ct' = 386-390
      ; 31 possible label classes : ct' = 398-403
      ; 51 possible label classes : ct' = 625-646
      ; ...
      ; 100000 possible label classes : ct' = 0-20 (0 means all noise)
      l•• (coll/->array-nd
            (coll/repeatedly ct:x•• (fn [] [#_(rand/double-between 0 500) (double (rand/int-between 0 1))])))
      ct:hash-fns 4
      ct:buckets  4
      lsh-hasher  (info.debatty.java.lsh.LSHSuperBit. ct:hash-fns ct:buckets ct:x•)
      x••' (lsh-is-f x•• l••
             (fn [^doubles x•] (.hash lsh-hasher x•))
             ct:hash-fns
             ct:buckets)]
  {:ct   (count x••)
   :ct'  (count x••')})
