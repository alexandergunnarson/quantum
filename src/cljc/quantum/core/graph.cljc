(ns quantum.core.graph
  (:require-quantum [ns fn logic map type macros coll err log])
  #?(:clj
    (:require [clojure.java.io :as io]
      [loom.alg         :as g.alg]
      [loom.graph       :as g.graph]
      [loom.alg-generic :as g.gen-alg])))

(def inverse-map ; some better way of doing this?
  {+ -
   - +
   / *
   * /})

(defn inverse
  {:todo "Make this better. Inverse of complex functions"}
  [f]
  (or (get inverse-map f)
      (throw+ (Err. :undefined "Inverse not defined for function" f))))

(def base-map
  {+ 0
   - 0
   / 1
   * 1})

(defn base [f]
  (or (get base-map f)
      (throw+ (Err. :undefined "Base not defined for function" f))))

; alg.generic
(in-ns 'loom.alg-generic)

; {:modified "Changed to use an fn for each weight-aggregation operation.
;               Weights won't always be simply added - they might be multiplied, for instance."
;  :contributor "Alex Gunnarson"}

(defn dijkstra-traverse
  "Returns a lazy-seq of [current-node state] where state is a map in the
  format {node [distance predecessor]}. When f is provided, returns
  a lazy-seq of (f node state) for each node"
  ([successors dist start]
    (dijkstra-traverse successors dist start vector))
  ([successors dist start f]
    (dijkstra-traverse successors dist start f +))
  ([successors dist start f waf]
     (letfn [(step [[state pq]]
               (when-let [[dist-su _ u :as fpq] (first pq)]
                 (cons
                  (f u state)
                  (lazy-seq
                   (step
                    (reduce
                     (fn [[state pq] v]
                       (let [dist-suv (waf dist-su (dist u v))
                             dist-sv (first (state v))]
                         (if (and dist-sv (>= dist-suv dist-sv))
                           [state pq]
                           (let [pq (if dist-sv
                                      (disj pq [dist-sv (hash v) v])
                                      pq)]
                             [(assoc state v [dist-suv u])
                              (conj pq [dist-suv (hash v) v])]))))
                     [state (disj pq fpq)]
                     (successors u)))))))]
       (step [{start [(quantum.core.graph/base waf) nil]}
              ;; Poor man's priority queue. Caveats:
              ;; 1) Have to keep it in sync with current state
              ;; 2) Have to include hash codes for non-Comparable items
              ;; 3) O(logn) operations
              ;; Tried clojure.contrib.priority-map but it wasn't any faster
              (sorted-set [(quantum.core.graph/base waf) (hash start) start])]))))

(defn dijkstra-span
  "Finds all shortest distances from start, where successors and dist
  are functions called as (successors node) and (dist node1 node2).
  Returns a map in the format {node {successor distance}}"
  ([successors dist start] (dijkstra-span successors dist start +))
  ([successors dist start waf]
    (reduce
     (fn [span [n [d p]]]
       (if p
         (assoc-in span [p n] d)
         span))
     {}
     (second (last (dijkstra-traverse successors dist start vector waf))))))

(in-ns 'loom.alg)

(defn dijkstra-traverse
  "Returns a lazy-seq of [current-node state] where state is a map in
  the format {node [distance predecessor]}. When f is provided,
  returns a lazy-seq of (f node state) for each node"
  ([g            ] (gen/dijkstra-traverse (graph/successors g) (graph/weight g)
                     (first (nodes g))))
  ([g start      ] (gen/dijkstra-traverse (graph/successors g) (graph/weight g) start))
  ([g start f    ] (gen/dijkstra-traverse (graph/successors g) (graph/weight g) start f))
  ([g start f waf] (gen/dijkstra-traverse (graph/successors g) (graph/weight g) start f waf)))

(defn- can-relax-edge?
  "Tests for whether we can improve the shortest path to v found so far
   by going through u."
  [[u v :as edge] weight costs waf]
  (let [vd (get costs v)
        ud (get costs u)
        aggregated (waf ud weight)] ; used fn here
    (> vd aggregated)))

(defn- relax-edge
  "If there's a shorter path from s to v via u,
    update our map of estimated path costs and
   map of paths from source to vertex v"
  [[u v :as edge] weight [costs paths :as estimates] waf]
  (let [ud (get costs u)
        aggregated (waf ud weight)]
    (if (can-relax-edge? edge weight costs waf)
      [(assoc costs v aggregated) (assoc paths v u)]
      estimates)))

(defn- relax-edges
  "Performs edge relaxation on all edges in weighted directed graph"
  [g start estimates waf]
  (->> (edges g)
       (reduce (fn [estimates [u v :as edge]]
                 (relax-edge edge (graph/weight g u v) estimates waf))
               estimates)))

(defn bellman-ford
  "Given a weighted, directed graph G = (V, E) with source start,
   the Bellman-Ford algorithm produces map of single source shortest
   paths and their costs if no negative-weight cycle that is reachable
   from the source exists, and false otherwise, indicating that no
   solution exists."
  ([g start] (bellman-ford g start +))
  ([g start waf] ; @waf is weight-aggregation-function
    (let [initial-estimates (init-estimates g start)
          ;;relax-edges is calculated for all edges V-1 times
          [costs paths] (reduce (fn [estimates _]
                                  (relax-edges g start estimates waf))
                                initial-estimates
                                (-> g nodes count dec range))
          edges (edges g)]
      (if (some
           (fn [[u v :as edge]]
             (can-relax-edge? edge (graph/weight g u v) costs waf))
           edges)
        false
        [costs
         (->> (keys paths)
              ;;remove vertices that are unreachable from source
              (remove #(= Double/POSITIVE_INFINITY (get costs %)))
              (reduce
               (fn [final-paths v]
                 (assoc final-paths v
                        ;; follows the parent pointers
                        ;; to construct path from source to node v
                        (loop [node v
                               path ()]
                          (if node
                            (recur (get paths node) (cons node path))
                            path))))
               {}))]))))

(defn- bellman-ford-transform
  "Helper function for Johnson's algorithm. Uses Bellman-Ford to remove negative weights."
  ([wg] (bellman-ford-transform +))
  ([wg waf] ; @waf is weight-aggregation-function
    (let [q (first (drop-while (partial graph/has-node? wg) (repeatedly gensym)))
          es (for [v (graph/nodes wg)] [q v 0])
          ; "Add-edges" just associates edges with a graph; it doesn't add their weights together
          bf-results (bellman-ford (graph/add-edges* wg es) q waf)]
      (if bf-results
        (let [[dist-q _] bf-results
              new-es (map (juxt first second (fn [[u v]]
                                               (waf (weight wg u v)
                                                    ((quantum.core.graph/inverse waf)
                                                       (dist-q u)
                                                       (dist-q v)))))
                          (graph/edges wg))]
          (graph/add-edges* wg new-es))
        false))))

(defn johnson
  "Finds all-pairs shortest paths using Bellman-Ford to remove any negative edges before
  using Dijkstra's algorithm to find the shortest paths from each vertex to every other.
  This algorithm is efficient for sparse graphs.
  If the graph is unweighted, a default weight of 1 will be used. Note that it is more efficient
  to use breadth-first spans for a graph with a uniform edge weight rather than Dijkstra's algorithm.
  Most callers should use shortest-paths and allow the most efficient implementation be selected
  for the graph."
  ([g] (johnson g +))
  ([g waf]
    (let [g (if (and (weighted? g) (some (partial > 0) (map (graph/weight g) (graph/edges g))))
              (bellman-ford-transform g waf)
              g)]
      (if (false? g)
        false
        (let [dist (if (weighted? g)
                     (weight g)
                     (fn [u v] (when (graph/has-edge? g u v) 1)))]
          (reduce (fn [acc node]
                    (assoc acc node (gen/dijkstra-span (successors g) dist node waf)))
                  {}
                  (nodes g)))))))

; I don't know why this is necessary
(defn bf-all-pairs-shortest-paths
  "Uses bf-span on each node in the graph."
  [g]
  (reduce (fn [spans node]
            (assoc spans node (bf-span g node)))
          {}
          (nodes g)))

(defn all-pairs-shortest-paths
  "Finds all-pairs shortest paths in a graph. Uses Johnson's algorithm for weighted graphs
  which is efficient for sparse graphs. Breadth-first spans are used for unweighted graphs."
  ([g] (all-pairs-shortest-paths g +))
  ([g waf]
    (if (weighted? g)
      (johnson g waf)
      (bf-all-pairs-shortest-paths g waf))))

(in-ns 'quantum.core.graph)

(defalias graph            g.graph/graph)
(defalias undirected-graph graph)
(defalias digraph          g.graph/digraph)
(defalias directed-graph   digraph)
(defalias weighted-digraph g.graph/weighted-digraph)
(defalias nodes            g.graph/nodes)
(defalias edges            g.graph/edges)

(defalias all-pairs-shortest-paths g.alg/all-pairs-shortest-paths)

(defn root-node-paths* [m depv]
  (->> (for [k sub m]
         (if (map? sub)
             (root-node-paths* sub (conj depv k))
             [[(conj depv k) sub]]))
       (apply concat)))

(defn root-node-paths
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
       (map (juxt (compr first (juxt first last)) second))
       (into (sorted-map+))))