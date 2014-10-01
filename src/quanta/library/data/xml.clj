(ns quanta.library.data.xml)

(require
  '[quanta.library.logic                 :refer :all]
  '[quanta.library.function              :refer :all]
  '[quanta.library.type                  :refer :all]
  '[quanta.library.numeric     :as num   :refer [nneg?]]
  '[quanta.library.collections           :refer :all]
  '[quanta.library.data.vector :as vec   :refer [catvec]]
  '[quanta.library.string      :as str   :refer [subs+]])

; XML PARSING FROM STRINGS PRE-BROKEN BY NEWLINES VIA THEBUSBY.IOTA

; On creation, an index of the file will be constructed so random access will be O(1),
; similar to a normal Clojure vector. This is significantly more memory efficient than
; a vector of Strings.

(defn elem-type [xml-ln]
  (if (nil? xml-ln)
      :beg
      (let [beg-bracket (subs+ xml-ln 0 1)
            beg-slash   (subs+ xml-ln 1 1)
            end-slashb  (subs+ xml-ln (-> xml-ln count+ dec dec) 2)]
        (if (and (= beg-bracket "<")
                 (not= end-slashb "/>"))
            (case beg-slash 
                  "/"   :end
                  "?"   :header
                  :beg)
            :body))))

; XML MANIPULATION FROM STRINGS UNBROKEN BY NEWLINES

(def brs-open  #(str "<" % ">"))
(def brs-close #(str "</" % ">"))
(defn split-xml [xml-str]
  (-> xml-str str
      (str/replace #"<"    "\n<")
      (str/replace #">"    ">\n")
      (str/replace #"\n\n" "\n")
      (str/split   #"\n")
      ((partial remove empty?))))
(defn tag [elem]
  (if ((fn-and string? (fn-> first str (= "<"))) elem)
      (let [space-ind
             (iff nneg?
                  (->> elem (index-of+ " ") dec)
                  (->  elem count+ dec dec))]
        (subs+ elem 1 space-ind))
      elem))
(defn parse-xml [elems] ; there must be a faster way
  ; (->> (req-io/last-resp-raw) parse-xml keys)
; StackOverflowError   quanta.library.logic/condf (logic.clj:53)
  (defn keys-n-calc [type-n elem-n keys-n type-n-1 type-n+1]
    (case type-n
      :header [0 :header]
      :beg    (if (splice-or type-n-1 = :header :end)
                  (updates-in+ keys-n
                    (-> keys-n count+ dec dec vector) inc
                    (-> keys-n count+ dec     vector) (->> elem-n tag keyword constantly))
                  (->> elem-n tag keyword (conj keys-n 0)))
      :body   keys-n
      :end    (if (= :beg type-n+1)
                  keys-n
                  (-> keys-n pop+ pop+))))
  (loop [[[type-n   elem-n  ]
          [type-n+1 elem-n+1]
          :as elems-n]
         (map (juxt elem-type identity)
              (ifn elems string? split-xml identity))
         type-n-1 nil
         keys-n   []
         elems-f  []]
    (if (empty? elems-n)
        elems-f
        (let [keys-n (keys-n-calc type-n elem-n keys-n type-n-1 type-n+1)]
          (recur (rest elems-n)
            type-n
              keys-n
              (doto
                (case type-n
                  :header (assoc-in+ elems-f keys-n elem-n)
                  :beg    (assoc-in+ elems-f keys-n [])
                  :body   (assoc-in+ elems-f keys-n elem-n)
                  :end    elems-f)))))))