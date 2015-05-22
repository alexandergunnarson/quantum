(ns quantum.core.graph
  (:require-quantum [ns fn logic map type macros])
  #?(:clj
  	(:require [clojure.java.io :as io]
    [alembic.still])))
; https://gist.github.com/ikoblik/4465693

;(alembic.still/distill '[rhizome "0.2.5"])
;(require '[rhizome.viz :as g])


(def g
	{:component [:lib],
:hex [ :str],
:log [ :async ],
:type [   ],
:ftree [],
:red [   :macros :num :type   :vec],
:bytes [ :str  :bin :macros :ccore :arr],
:ccore [
            :macros
            :arr
            :pr
            :str
            :time
            :coll
            :num
            
            :type
            
            :sys
            :err],
:java [:lib],
:thread [ :num  :str :err  :vec :coll :err],
:sys [:str :coll],
:num [  :type  :macros],
:crypto [ :bytes  :str],
:arr [],
:json [],
:loops [   :log  :macros :type :red :ccore :arr],
:macros [ :log :pr :err     :type],
:coll [
                   
                   :type
                   :macros
                   :num
                   
                   :vec
                   
                   :log
                   :err
                   :macros
                   
                   :str
                   :async
                   :time],
:serialization [ :coll],
:nondeterministic [],
:xml [
           :err
           
           
           :type
           :num
           :coll
           :vec
           :str
           :log
           :coll
           :vec
           :str
           :log],
:sh [ :coll],
:vec [ :type],
:err [],
:queue [ :num :loops],
:pr [   :type],
:str [   :macros :loops  :red :num :type]})

(g/view-graph (keys g) g :options {:dpi 50}
   :node->descriptor (fn [n] {:label n}))

;(def cljc-dir "/Users/alexandergunnarson/Development/Source Code Projects/quantum/src/cljc")

; (let [files (-> cljc-dir io/file file-seq)]
;   (->> (for [file-n files]
;          (when (clj-file? file-n)
;            (map-entry (file-name file-n)
;               (->> file-n slurp
;                    (read-string {:read-cond :allow})
;                    (filter (fn-and listy?
;                                    (fn-> first keyword?)
;                                    (fn-> first (= :require-quantum))))
;                    first second
;                    ))))
;        (remove (fn-> second nil?))
;        (map (fn [[k v]]
;               (map-entry (keyword k)
;                 (->> v (map (fn-> name keyword)) (into [])))))
;        (apply merge {})
;        ))







; (defn dfs
;   "Depth first search. Short form of the method passes through all the 
;   nodes of the graph even if it's disconnected .
;   (nodes-fn graph) expected to return list of all the nodes in the graph.
;   (child-fn graph node) expected to return list of all the nodes linked
;    to the given node.
;   Returns hash-map where nodes are associated with a pair :idx, :leader.
;   :idx stores finishing index of the node traversal (post-order counter)
;   :leader first finishing index of the current DFS."
;   ([graph nodes-fn child-fn]
;      (second
;       (reduce ;; Start DFS from each node of the graph
;        (fn [[idx result passed :as args] next-node]
;          (if (not (passed next-node)) ;; Don't do DFS if node is marked
;            (dfs idx idx result passed graph next-node child-fn)
;            args))
;        [0 {} #{}] ;;Initial index, result, set of passed nodes
;        (nodes-fn graph))))
;   ([idx leader result passed graph node child-fn]
;      (let [[idx result passed]
;            (reduce (fn [[idx result passed :as args] child-node]
;                      (if (not (passed child-node))
;                        (dfs idx leader result passed graph child-node child-fn)
;                        args))
;                    [idx result (conj passed node)]
;                    (child-fn graph node))]
;        [(inc idx)
;         (assoc result node {:idx idx :leader leader})
;         passed])))
 
; (defn pass-two 
;   "Calls DFS making sure that traversal is done in the reverse :idx order."
;   [graph result child-fn]
;   (let [nodes-fn 
;         (constantly (->> result 
;                          ;;Sort by :idx in reverse order
;                          (sort-by (comp :idx second)) reverse 
;                          ;;Return only nodes
;                          (map first)))]
;     (dfs graph nodes-fn child-fn)))
  
; (defn scc 
;   "Finds strongly connected components of the given directed graph.
;   Returns lists of nodes grouped into SCC.
;   (nodes-fn graph) expected to return list of all the nodes in the graph.
;   (incoming-fn graph node) expected to return all the nodes with
;    transitions towards the given node.
;   (outgoing-fn graph node) expected to return all the nodes with
;    transitions from the given node."
;   [graph nodes-fn incoming-fn outgoing-fn]
;   (let [result (dfs graph nodes-fn incoming-fn)
;         leaders-idx (pass-two graph result outgoing-fn)]
;     (for [scc-group (vals (group-by (comp :leader second) leaders-idx))]
;       (for [[node & _] scc-group] node))))


; (def test-graph 
;   {6 [9], 2 [8], 4 [7], 3 [6], 8 [5 6], 1 [4], 9 [3 7], 5 [2], 7 [1]})
; ;['a 'b 'c 'd 'e 'f 'g 'h 'i 'j]
; ;  1  2  3  4  5  6  7  8  9  10
; ;(def test-graph
; ;  {'a ['g], 'b ['e], 'c ['i], 'd ['a], 'e ['h], 'f ['c 'h], 'g ['d 'i], 'h ['b], 'i ['f]})
; (def reverse-g
;   (reverse-graph g))
 
; (dfs g
;      ;;fn that returns set of nodes
;      (constantly (into #{} (flatten (seq g))))
;      ;;(get graph node) returns list of related nodes.
;      get) 
 
; (scc test-graph 
;      ;;fn that returns set of nodes
;      (constantly (into #{} (flatten (seq g))))
;      ;;works as incoming-fn using cashed reversed graph
;      #(get reverse-g %2) 
;      ;;(get graph node) returns list of related nodes
;      get)