;; TODO lazy-load jars based on desired features (or optionally force load all)
;; ssh -t -t localhost; export COLUMNS=10000 && export TERM=screen.linux && screen -Dr quantum

(load-file "project-base.clj")
(require '[quantum.meta.project-base :as base])

(base/defproject
  {:artifact-base-name "quantum"
   :print-config?      true}
  (merge base/base-config|quantum
    {:name        'quantum/core
     :description "Some quanta of computational abstraction, assembled."
     :url         "https://www.github.com/alexandergunnarson/quantum"
     :source-paths ["src-untyped"]}))
