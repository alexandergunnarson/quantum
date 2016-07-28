(ns quantum.test.core.paths
  (:require [quantum.core.paths :as ns]))

(defn test:->file [x])
(defn test:->uri [x])
(defn test:->url [x])
(defn test:contains? [x])

(defn test:path
  [& parts])

(defn test:url-path
  [& parts])

(defn test:path->file-name [x])

(defn test:extension [x])

(defn test:find-directory
  [directory file-name])
;___________________________________________________________________________________________________________________________________
;========================================================{ PATH, EXT MGMT }=========================================================
;========================================================{                }==========================================================
(defn test:path-without-ext [path-0])

(defn test:up-dir-str [dir])

(defn test:parse-dir [x])

(defn test:file-str [x])

(defn test:exists? [x])

(defn test:directory? [x])

(defn test:clj-file? [x])

; FILE RELATIONS

(defn test:up-dir [x])

(defn test:children [x])

(defn test:siblings [x])

(defn test:descendants       [x])
(defn test:descendant-leaves [x])
(defn test:internal-nodes    [x])