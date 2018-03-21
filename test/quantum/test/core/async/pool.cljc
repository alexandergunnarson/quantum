(ns quantum.test.core.async.pool)

;; Refine this test; the basics are there
#_(deftest interval-executor
  (let [c (comp/start
             ((fn->> >interval-executor|config (hash-map :config) map->IntervalExecutor)
                {:tasks [{:ident 'a :f #(println "aaa") :wait-ms 200}
                         {:ident 'b :f #(println "bbb") :wait-ms 1000}]}))]
    (Thread/sleep 10000)
    (comp/stop c)))
