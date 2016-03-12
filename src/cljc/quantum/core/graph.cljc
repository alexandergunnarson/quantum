(ns ^{:doc "Graph operations."}
  quantum.core.graph
  (:require-quantum [:core fn logic err log])
    (:require #?(:clj [loom.alg         :as alg    ]) ; temporarily
              #?(:clj [loom.graph       :as graph  ]) ; temporarily
              #?(:clj [loom.alg-generic :as galg   ]) ; temporarily
              #?(:clj [loom.label       :as label  ]) ; temporarily
              #?(:clj [loom.attr        :as attr   ]) ; temporarily
              #?(:clj [loom.flow        :as flow   ]) ; temporarily
      #?(:clj [loom.io          :as g.io   ])
      #?(:clj [clojure.java.io  :as io     ])))

; Ubergraph goes beyond Loom's protocols, allowing a
; mixture of directed and undirected edges within a
; single graph, multiple "parallel" edges between a
; given pair of nodes, multiple weights per edge,
; and changeable weights.
; Ubergraph is a great choice for people who want to
; use Loom, but don't want to think about which specific
; Loom graph implementation to use. (Hmmm, do I need
; directed edges or undirected edges? Do I need weights
; or attributes for this project? I'm not sure yet,
; so I'll just use ubergraph because it can do it all!

; https://en.wikipedia.org/wiki/Graph_traversal
; https://en.wikipedia.org/wiki/Graph_rewriting
; https://en.wikipedia.org/wiki/Graph_theory
; https://en.wikipedia.org/wiki/Glossary_of_graph_theory
; https://en.wikipedia.org/wiki/List_of_graph_theory_topics

; ========== CREATE ==========

; SHOULD BE CLJC
#?(:clj (defalias ->graph             graph/graph            ))
#?(:clj (defalias ->undirected-graph  ->graph                ))
#?(:clj (defalias ->digraph           graph/digraph          ))
#?(:clj (defalias ->directed-graph    ->digraph              ))
#?(:clj (defalias ->weighted-digraph  graph/weighted-digraph ))
#?(:clj (defalias ->fly-graph         graph/fly-graph        ))

; ========== ACCESS ==========

; SHOULD BE CLJC
#?(:clj (defalias nodes               graph/nodes            ))
#?(:clj (defalias edges               graph/edges            ))
#?(:clj (defalias attr                attr/attr              ))
#?(:clj (defalias attrs               attr/attrs             ))
#?(:clj (defalias subgraph            graph/subgraph         ))
#?(:clj (defalias successors          graph/successors       ))
#?(:clj (defalias predecessors        graph/predecessors     ))
#?(:clj (defalias weight              graph/weight           ))
#?(:clj (defalias out-degree          graph/out-degree       ))
#?(:clj (defalias out-edges           graph/out-edges        ))
#?(:clj (defalias in-degree           graph/in-degree        ))
#?(:clj (defalias in-edges            graph/in-edges         ))

; ========== TRANSFORM ==========

; reverse-graph is same as g.graph/transpose
; SHOULD BE CLJC
#?(:clj (defalias transpose           graph/transpose        ))
#?(:clj (defalias add-nodes           graph/add-nodes        ))
#?(:clj (defalias add-labeled-nodes   label/add-labeled-nodes))
#?(:clj (defalias add-attr            attr/add-attr          ))
;#?(:clj (defalias add-attr-to-nodes   label/add-attr-to-nodes))
#?(:clj (defalias add-edges           graph/add-edges        ))
#?(:clj (defalias add-label           label/add-label        ))
#?(:clj (defalias add-labeled-edges   label/add-labeled-edges))
#?(:clj (defalias add-attr-to-edges   attr/add-attr-to-edges ))
#?(:clj (defalias add-attrs-to-all    attr/add-attrs-to-all  ))
#?(:clj (defalias remove-nodes        graph/remove-nodes     ))
#?(:clj (defalias remove-edges        graph/remove-edges     ))
#?(:clj (defalias remove-label        label/remove-label     ))
#?(:clj (defalias remove-attr         attr/remove-attr       ))

; ========== PREDICATES ==========

; SHOULD BE CLJC
#?(:clj (defalias graph?              graph/graph?           ))
#?(:clj (defalias digraph?            graph/directed?        ))
; directed acyclic graph?
#?(:clj (defalias ac-digraph?         alg/dag?               ))
#?(:clj (defalias weighted?           graph/weighted?        ))
#?(:clj (defalias labeled?            label/labeled?         ))
#?(:clj (defalias has-attrs?          attr/attr?             ))
#?(:clj (defalias connected?          alg/connected?         ))
#?(:clj (defalias strongly-connected? alg/strongly-connected?))

; ========== ALGORITHMS ==========

#?(:clj (defalias all-pairs-shortest-paths        alg/all-pairs-shortest-paths        ))
; Traverses graph depth-first from start
#?(:clj (defalias df-pre-traverse                 alg/pre-traverse                    ))
; Depth-first spanning tree
#?(:clj (defalias df-pre-span                     alg/pre-span                        ))
; Traverses graph depth-first, post-order from start
#?(:clj (defalias df-post-traverse                alg/post-traverse                   ))
; Traverses graph breadth-first from start
#?(:clj (defalias bf-traverse                     alg/bf-traverse                     ))
; Breadth-first spanning tree
#?(:clj (defalias bf-span                         alg/bf-span                         ))
; Unidirectional breath-first pathfinder. Finds path from start to
; end with the fewest hops (irrespective of edge weights)
#?(:clj (defalias bf-path                         alg/bf-path                         ))
; Bidirectional breadth-first pathfinder. Can be much faster than a
; unidirectional search on certain types of graphs
#?(:clj (defalias bf-path-bi                      alg/bf-path-bi                      ))

#?(:clj (defalias topological-sort                alg/topsort                         ))
#?(:clj (defalias topo-sort                       topological-sort                    ))

; ----- PATHFINDING -----

#?(:clj (defalias dijkstra-traverse               alg/dijkstra-traverse               ))
; Finds all shortest distances from start.
#?(:clj (defalias dijkstra-span                   alg/dijkstra-span                   ))
#?(:clj (defalias weighted-span                   dijkstra-span                       ))
; Finds the shortest path from start to end, respecting weights.
#?(:clj (defalias dijkstra-path-dist              alg/dijkstra-path-dist              ))
#?(:clj (defalias weighted-path                   dijkstra-path-dist                  ))

; bf-path + dijkstra-path
#?(:clj (defalias shortest-path                   alg/shortest-path                   ))
; Find the longest of the shortest paths
#?(:clj (defalias longest-shortest-path           alg/longest-shortest-path           ))

; Minimum spanning tree (or forest) of given graph
#?(:clj (defalias prim-mst                        alg/prim-mst                        ))
; Shortest path using A* algorithm
#?(:clj (defalias astar-path                      alg/astar-path                      ))
; length of the shortest path between src and target using A* algorithm
#?(:clj (defalias astar-dist                      alg/astar-dist                      ))

; ----- CONNECTEDNESS -----

#?(:clj (defalias connected-components            alg/connected-components     ))

; Uses Kosaraju's algorithm
#?(:clj (defalias strongly-connected-components   alg/scc                      ))
#?(:clj (defalias scc                             strongly-connected-components))
; Nodes with no connections to other node
#?(:clj (defalias loners                          alg/loners                   ))

; ----- OTHER -----

#?(:clj (defalias distinct-edges                  alg/distinct-edges           ))
; Sequence of vertices in degeneracy order
#?(:clj (defalias degeneracy-ordering             alg/degeneracy-ordering      ))
; Uses Bron-Kerbosch
#?(:clj (defalias maximal-cliques                 alg/maximal-cliques          ))

; ----- COLORING -----

; Attempts a two-coloring
#?(:clj (defalias bipartite-color                 alg/bipartite-color          ))
; Greedily color the vertices of a graph using the first-fit heuristic
#?(:clj (defalias greedy-color                    alg/greedy-coloring          ))
; Two sets of nodes, one for each color of the bipartite coloring
#?(:clj (defalias bipartite-sets                  alg/bipartite-sets           ))
#?(:clj (defalias bipartite?                      alg/bipartite?               ))

; Whether a map of nodes to colors is a proper coloring of a graph 
#?(:clj (defalias coloring?                       alg/coloring?                ))

; ----- DENSITY -----

#?(:clj (defalias density alg/density))

; TODO implement other algorithms from CS 250-whatever

; Specifically named ones
; Produces map of single source shortest paths and their costs
; if no negative-weight cycle that is reachable from the source
; exists
#?(:clj (defalias bellman-ford                    alg/bellman-ford                    ))
#?(:clj (defalias johnson                         alg/johnson                         ))


; ----- DATAFLOW -----

; TODO learn how to use this
;#?(:clj (defalias dataflow-analysis               dataflow/dataflow-analysis          ))

; ----- NETWORK -----

#?(:clj (defalias residual-capacity               flow/residual-capacity              ))
#?(:clj (defalias flow-balance                    flow/flow-balance                   ))
#?(:clj (defalias satisfies-mass-balance?         flow/satisfies-mass-balance?        ))
#?(:clj (defalias satisfies-capacity-constraints? flow/satisfies-capacity-constraints?))
#?(:clj (defalias is-admissible-flow?             flow/is-admissible-flow?            ))

; I.e., "find the network clog"
; TODO make this extensible
#?(:clj (defalias min-weight-along-path           flow/min-weight-along-path          ))

#?(:clj (defalias bf-find-augmenting-path         flow/bf-find-augmenting-path        ))
#?(:clj (defalias augment-along-path              flow/bf-find-augmenting-path        ))
#?(:clj (defalias edmonds-karp                    flow/edmonds-karp                   ))
#?(:clj (defalias max-flow                        edmonds-karp                        ))
; TODO find difference between these algorithms
#?(:clj (defalias max-flow*                       alg/max-flow                        ))

; ========== FORMAT/RENDER ==========

; Renders graph as a DOT-format string
#?(:clj (defalias ->dot-str                       g.io/dot-str                        ))

; Renders the graph in the PNG format using GraphViz and returns PNG data
; as a byte array.
; Requires GraphViz's 'dot' (or a specified algorithm) to be installed in
; the shell's path. Possible algorithms include :dot, :neato, :fdp, :sfdp
; :twopi, and :circo
#?(:clj
(defalias ->image-bytes                   g.io/render-to-bytes                ))

#?(:clj (defalias highlight                       attr/hilite                         ))
#?(:clj (defalias highlight-path                  attr/hilite-path                    ))

(declare pprint)

; ===== IMPLEMENTATION =====

#?(:clj
(defn pprint
  "Pretty print a graph"
  [g]
  (println (type g))
  (println (-> g nodes count) "Nodes:")
  (doseq [node (nodes g)] 
    (println \tab node (let [a (attrs g node)] (if (seq a) a ""))))
  ;(println (count-unique-edges g) "Edges:")
  (doseq [edge (edges g)]
    (println \tab (graph/src edge) "->" (graph/dest edge) 
      (let [a (attrs g edge)]
        (if (seq a) a ""))))))

; ========== OTHER FUNCTIONS ==========

#_(defn root-node-paths* [m depv]
  (->> (for [k sub m]
         (if (map? sub)
             (root-node-paths* sub (conj depv k))
             [[(conj depv k) sub]]))
       (apply concat)))

#_(defn+ root-node-paths
  "Outputs paths from the root nodes to all their respective
   leaf nodes. Keeps only the root node and the leaf node,
   as well as the immediate parent of the leaf node.

   Very useful for conversion tables."
  {:in {:zs          {:ps           {:ns         1000000000000N}
                      :zs           {:as         1000N
                                     :ys         1/1000}
                      :ns           {:mcs        1000000000000000N}
                      :as           {:fs         1000000N}
                      :fs           {:ps         1000000000N}}
       :common-years {:days         {:fortnights 14/365
                                     :leap-years 366/365
                                     :weeks      7/365}
                      :common-years {:days       1/365}
                      :years        {:centuries  7305/73
                                     :millennia  73050/73}}}
   :out {[:common-years :centuries ] 7305/73
         [:common-years :days      ] 1/365
         [:common-years :fortnights] 14/365
         [:common-years :leap-years] 366/365
         [:common-years :millennia ] 73050/73
         [:common-years :weeks     ] 7/365
         [:zs           :as        ] 1000N
         [:zs           :fs        ] 1000000N
         [:zs           :mcs       ] 1000000000000000N
         [:zs           :ns        ] 1000000000000N
         [:zs           :ps        ] 1000000000N
         [:zs           :ys        ] 1/1000}}
  [m]
  (->> (root-node-paths* m [])
       (map (juxt (compr (extern (mfn 1 first)) (juxt (extern (mfn 1 first)) (extern (mfn 1 last)))) (extern (mfn 1 second))))
       (into (sorted-map))))



; ======================== CLOJURE/ALGO.GRAPH ========================

;;  Basic Graph Theory Algorithms
;;
;;  straszheimjeffrey (gmail)
;;  Created 23 June 2009


; (ns 
;   ^{:author "Jeffrey Straszheim",
;      :doc "Basic graph theory algorithms"}
;   clojure.algo.graph
;   (use [clojure.set :only (union)]))


; (defrecord DirectedGraph
;   [nodes       ; The nodes of the graph, a collection
;    neighbors]) ; A function that, given a node, returns a collection of
;                ; neighbor nodes.

; (defn get-neighbors
;   "Get the neighbors of a node."
;   {:source "clojure.algo.graph"
;    :attribution "Jeffrey Straszheim"}
;   [g n]
;   ((:neighbors g) n))

; ;; Graph Modification

; (defn reverse-graph
;   "Given a directed graph, return another directed graph with the
;    order of the edges reversed."
;   {:source "clojure.algo.graph"
;    :attribution "Jeffrey Straszheim"}
;   [g]
;   (let [op (fn [rna idx]
;              (let [ns (get-neighbors g idx)
;                    am (fn [m val]
;                         (assoc m val (conj (get m val #{}) idx)))]
;                (reduce am rna ns)))
;         rn (reduce op {} (:nodes g))]
;     (DirectedGraph. (:nodes g) rn)))

; (defn add-loops
;   "For each node n, add the edge n->n if not already present."
;   [g]
;   (struct directed-graph
;           (:nodes g)
;           (into {} (map (fn [n]
;                           [n (conj (set (get-neighbors g n)) n)]) (:nodes g)))))

; (defn remove-loops
;   "For each node n, remove any edges n->n."
;   [g]
;   (struct directed-graph
;           (:nodes g)
;           (into {} (map (fn [n]
;                           [n (disj (set (get-neighbors g n)) n)]) (:nodes g)))))


; ;; Graph Walk

; (defn lazy-walk
;   "Return a lazy sequence of the nodes of a graph starting a node n.  Optionally,
;    provide a set of visited notes (v) and a collection of nodes to
;    visit (ns)."
;   ([g n]
;      (lazy-walk g [n] #{}))
;   ([g ns v]
;      (lazy-seq (let [s (seq (drop-while v ns))
;                      n (first s)
;                      ns (rest s)]
;                  (when s
;                    (cons n (lazy-walk g (concat (get-neighbors g n) ns) (conj v n))))))))

; (defn transitive-closure
;   "Returns the transitive closure of a graph.  The neighbors are lazily computed.
;    Note: some version of this algorithm return all edges a->a
;    regardless of whether such loops exist in the original graph.  This
;    version does not.  Loops will be included only if produced by
;    cycles in the graph.  If you have code that depends on such
;    behavior, call (-> g transitive-closure add-loops)"
;   [g]
;   (let [nns (fn [n]
;               [n (delay (lazy-walk g (get-neighbors g n) #{}))])
;         nbs (into {} (map nns (:nodes g)))]
;     (struct directed-graph
;             (:nodes g)
;             (fn [n] (force (nbs n))))))
          
                
; ;; Strongly Connected Components

; (defn- post-ordered-visit
;   "Starting at node n, perform a post-ordered walk."
;   [g n [visited acc :as state]]
;   (if (visited n)
;     state
;     (let [[v2 acc2] (reduce (fn [st nd] (post-ordered-visit g nd st))
;                             [(conj visited n) acc]
;                             (get-neighbors g n))]
;       [v2 (conj acc2 n)])))
  
; (defn post-ordered-nodes
;   "Return a sequence of indexes of a post-ordered walk of the graph."
;   [g]
;   (fnext (reduce #(post-ordered-visit g %2 %1)
;                  [#{} []]
;                  (:nodes g))))

; (defn scc
;   "Returns, as a sequence of sets, the strongly connected components
;    of g."
;   [g]
;   (let [po (reverse (post-ordered-nodes g))
;         rev (reverse-graph g)
;         step (fn [stack visited acc]
;                (if (empty? stack)
;                  acc
;                  (let [[nv comp] (post-ordered-visit rev
;                                                      (first stack)
;                                                      [visited #{}])
;                        ns (remove nv stack)]
;                    (recur ns nv (conj acc comp)))))]
;     (step po #{} [])))

; (defn component-graph
;   "Given a graph, perhaps with cycles, return a reduced graph that is acyclic.
;    Each node in the new graph will be a set of nodes from the old.
;    These sets are the strongly connected components.  Each edge will
;    be the union of the corresponding edges of the prior graph."
;   ([g]
;      (component-graph g (scc g)))
;   ([g sccs]
;      (let [find-node-set (fn [n]
;                            (some #(if (% n) % nil) sccs))
;            find-neighbors (fn [ns]
;                             (let [nbs1 (map (partial get-neighbors g) ns)
;                                   nbs2 (map set nbs1)
;                                   nbs3 (apply union nbs2)]
;                               (set (map find-node-set nbs3))))
;            nm (into {} (map (fn [ns] [ns (find-neighbors ns)]) sccs))]
;        (struct directed-graph (set sccs) nm))))

; (defn recursive-component?
;   "Is the component (recieved from scc) self recursive?"
;   [g ns]
;   (or (> (count ns) 1)
;       (let [n (first ns)]
;         (some #(= % n) (get-neighbors g n)))))

; (defn self-recursive-sets
;   "Returns, as a sequence of sets, the components of a graph that are
;    self-recursive."
;   [g]
;   (filter (partial recursive-component? g) (scc g)))
                          

; ;; Dependency Lists

; (defn fixed-point
;   "Repeatedly apply fun to data until (equal old-data new-data)
;    returns true.  If max iterations occur, it will throw an
;    exception.  Set max to nil for unlimited iterations."
;   [data fun max equal]
;   (let [step (fn step [data idx]
;                (when (and idx (= 0 idx))
;                  (throw (Exception. "Fixed point overflow")))
;                (let [new-data (fun data)]
;                  (if (equal data new-data)
;                    new-data
;                    (recur new-data (and idx (dec idx))))))]
;     (step data max)))
                  
; (defn- fold-into-sets
;   [priorities]
;   (let [max (inc (apply max 0 (vals priorities)))
;         step (fn [acc [n dep]]
;                (assoc acc dep (conj (acc dep) n)))]
;     (reduce step
;             (vec (replicate max #{}))
;             priorities)))
            
; (defn dependency-list
;   "Similar to a topological sort, this returns a vector of sets. The
;    set of nodes at index 0 are independent.  The set at index 1 depend
;    on index 0; those at 2 depend on 0 and 1, and so on.  Those withing
;    a set have no mutual dependencies.  Assume the input graph (which
;    much be acyclic) has an edge a->b when a depends on b."
;   [g]
;   (let [step (fn [d]
;                (let [update (fn [n]
;                               (inc (apply max -1 (map d (get-neighbors g n)))))]
;                  (into {} (map (fn [[k v]] [k (update k)]) d))))
;         counts (fixed-point (zipmap (:nodes g) (repeat 0))
;                             step
;                             (inc (count (:nodes g)))
;                             =)]
;     (fold-into-sets counts)))
    
; (defn stratification-list
;   "Similar to dependency-list (see doc), except two graphs are
;    provided.  The first is as dependency-list.  The second (which may
;    have cycles) provides a partial-dependency relation.  If node a
;    depends on node b (meaning an edge a->b exists) in the second
;    graph, node a must be equal or later in the sequence."
;   [g1 g2]
;   (assert (= (-> g1 :nodes set) (-> g2 :nodes set)))
;   (let [step (fn [d]
;                (let [update (fn [n]
;                               (max (inc (apply max -1
;                                                (map d (get-neighbors g1 n))))
;                                    (apply max -1 (map d (get-neighbors g2 n)))))]
;                  (into {} (map (fn [[k v]] [k (update k)]) d))))
;         counts (fixed-point (zipmap (:nodes g1) (repeat 0))
;                             step
;                             (inc (count (:nodes g1)))
;                             =)]
;     (fold-into-sets counts)))


; ;; End of file