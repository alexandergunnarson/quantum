(ns ^{:doc "Code formatting. Taken shamelessly from cljfmt.core."}
  quantum.format.clojure.core
  (:require
    [clojure.zip        :as zip]
    [rewrite-clj.parser :as p]
    [rewrite-clj.node   :as n]
    [rewrite-clj.zip    :as z]
    [quantum.core.fn    :as fn
      :refer [rcomp fn-> fn->> fn1]]
    [quantum.core.logic
      :refer [fn-and fn-not fn-or whenf1]]
    [quantum.core.vars  :as var
      :refer [def-]]
    [quantum.untyped.core.type.predicates
      :refer [regex?]]))

;(java/load-deps '[rewrite-clj "0.4.12"])

(defn- edit-all
  {:source "weavejester.cljfmt.core"}
  [zloc p? f]
  (loop [zloc (if (p? zloc) (f zloc) zloc)]
    (if-let [zloc (z/find-next zloc zip/next p?)]
      (recur (f zloc))
      zloc)))

(defn- transform {:source "weavejester.cljfmt.core"} [form zf & args]
  (z/root (apply zf (z/edn form) args)))

(defn- surrounding? {:source "weavejester.cljfmt.core"} [zloc p?]
  (and (p? zloc) (or (nil? (zip/left zloc))
                     (nil? (z/skip zip/right p? zloc)))))

(def- top? (fn-and identity (rcomp (juxt z/node z/root) (partial apply not=))))

(def- surrounding-whitespace?
  (fn-and (fn-> z/up top?)
          (fn1 surrounding? z/whitespace?)))

(defn remove-surrounding-whitespace {:source "weavejester.cljfmt.core"} [form]
  (transform form edit-all surrounding-whitespace? zip/remove))

(defn- element? {:source "weavejester.cljfmt.core"} [zloc]
  (when zloc (not (z/whitespace-or-comment? zloc))))

(def- missing-whitespace? (fn-and element? (fn-> zip/right element?)))
(def- whitespace?         (fn-> z/tag (= :whitespace)))

(defn insert-missing-whitespace {:source "weavejester.cljfmt.core"} [form]
  (transform form edit-all missing-whitespace? z/append-space))

(defn- comment? {:source "weavejester.cljfmt.core"} [zloc] (some-> zloc z/node n/comment?))
(def- line-break?     (fn-or z/linebreak? comment?))
(def- skip-whitespace (partial z/skip zip/next whitespace?))

(defn- count-newlines
  {:source "weavejester.cljfmt.core"}
  [zloc]
  (loop [zloc zloc newlines 0]
    (if (z/linebreak? zloc)
        (recur (-> zloc zip/next skip-whitespace)
               (-> zloc z/string count (+ newlines)))
        newlines)))

(def- consecutive-blank-line? (fn-> count-newlines (> 2)))

(defn- remove-whitespace-and-newlines
  {:source "weavejester.cljfmt.core"}
  [zloc]
  (if (z/whitespace? zloc)
      (recur (zip/remove zloc))
      zloc))

(def- replace-consecutive-blank-lines
  (fn-> (zip/replace (n/newlines 2)) zip/next remove-whitespace-and-newlines))

(defn remove-consecutive-blank-lines {:source "weavejester.cljfmt.core"} [form]
  (transform form edit-all consecutive-blank-line? replace-consecutive-blank-lines))

(def- indentation?     (fn-and (fn-> zip/prev line-break?) whitespace?))
(def- comment-next?    (fn-> zip/next skip-whitespace comment?))
(def- line-break-next? (fn-> zip/next skip-whitespace line-break?))
(def- should-indent?   (fn-and line-break?  (fn-not line-break-next?)))
(def- should-unindent? (fn-and indentation? (fn-not comment-next?)))

(defn unindent {:source "weavejester.cljfmt.core"} [form]
  (transform form edit-all should-unindent? zip/remove))

(def ^:private start-element
  {:meta "^" , :meta* "#^", :vector       "[" , :map              "{"
   :list "(" , :eval  "#=", :uneval       "#_", :fn               "#("
   :set  "#{", :deref "@" , :reader-macro "#" , :unquote          "~"
   :var  "#'", :quote "'" , :syntax-quote "`" , :unquote-splicing "~@"})

(defn- prior-string [zloc]
  (if-let [p (z/left* zloc)]
    (str (prior-string p) (n/string (z/node p)))
    (if-let [p (z/up* zloc)]
      (str (prior-string p) (start-element (n/tag (z/node p))))
      "")))

(defn- last-line-in-string {:source "weavejester.cljfmt.core"} [^String s]
  (subs s (inc (.lastIndexOf s "\n"))))

(def- margin (fn-> prior-string last-line-in-string count))

(defn- whitespace {:source "weavejester.cljfmt.core"} [width]
  (n/whitespace-node (apply str (repeat width " "))))

(def- coll-indent (fn-> zip/leftmost margin))

(def- index-of*
  (fn->> (iterate z/left)
         (take-while identity)
         (count)
         (dec)))

(defn- list-indent {:source "weavejester.cljfmt.core"} [zloc]
  (if (> (index-of* zloc) 1)
      (-> zloc zip/leftmost z/right margin)
      ;; effective indentation: 2
      (-> zloc coll-indent inc)))

(def indent-size 2)

(defn- indent-width {:source "weavejester.cljfmt.core"} [zloc]
  (case (z/tag zloc)
    :list indent-size
    :fn   (inc indent-size)))

(def- remove-namespace (whenf1 symbol? (fn-> name symbol)))

(defn- indent-matches?
  {:source "weavejester.cljfmt.core"}
  [k sym]
  (cond
    (symbol? k) (= k sym)
    (regex?  k) (re-find k (str sym))
    :else       (throw (ex-info "Not supported" nil))))

(def- token? (fn-> z/tag (= :token)))

(defn- token-value {:source "weavejester.cljfmt.core"} [zloc]
  (when (token? zloc) (z/value zloc)))

(def- form-symbol (fn-> z/leftmost token-value remove-namespace))

(defn- index-matches-top-argument? {:source "weavejester.cljfmt.core"} [zloc depth idx]
  (and (> depth 0)
       (= idx (index-of* (nth (iterate z/up zloc) (dec depth))))))

(defn- inner-indent
  {:source "weavejester.cljfmt.core"}
  [zloc key depth idx]
  (let [top (nth (iterate z/up zloc) depth)]
    (if (and (indent-matches? key (form-symbol top))
             (or (nil? idx) (index-matches-top-argument? zloc depth idx)))
      (let [zup (z/up zloc)]
        (+ (margin zup) (indent-width zup))))))

(defn- nth-form
  {:source "weavejester.cljfmt.core"}
  [zloc n]
  (reduce (fn [z f] (when z (f z)))
          (z/leftmost zloc)
          (repeat n z/right)))

(defn- first-form-in-line?
  {:source "weavejester.cljfmt.core"}
  [zloc]
  (if-let [zloc (zip/left zloc)]
    (if (whitespace? zloc)
        (recur zloc)
        (or (z/linebreak? zloc) (comment? zloc)))
    true))

(defn- block-indent
  {:source "weavejester.cljfmt.core"}
  [zloc key idx]
  (if (indent-matches? key (form-symbol zloc))
    (if (and (some-> zloc (nth-form (inc idx)) first-form-in-line?)
             (> (index-of* zloc) idx))
      (inner-indent zloc key 0 nil)
      (list-indent zloc))))

(def default-indents
  '{alt!            [[:block 0]]
    alt!!           [[:block 0]]
    are             [[:block 2]]
    binding         [[:block 1]]
    bound-fn        [[:inner 0]]
    case            [[:block 1]]
    catch           [[:block 2]]
    comment         [[:block 0]]
    cond            [[:block 0]]
    condp           [[:block 2]]
    cond->          [[:block 1]]
    cond->>         [[:block 1]]
    def             [[:inner 0]]
    defmacro        [[:inner 0]]
    defmethod       [[:inner 0]]
    defmulti        [[:inner 0]]
    defn            [[:inner 0]]
    defn-           [[:inner 0]]
    defonce         [[:inner 0]]
    defprotocol     [[:block 1] [:inner 1]]
    defrecord       [[:block 2] [:inner 1]]
    defstruct       [[:block 1]]
    deftest         [[:inner 0]]
    deftype         [[:block 2] [:inner 1]]
    do              [[:block 0]]
    doseq           [[:block 1]]
    dotimes         [[:block 1]]
    doto            [[:block 1]]
    extend          [[:block 1]]
    extend-protocol [[:block 1] [:inner 1]]
    extend-type     [[:block 1] [:inner 1]]
    finally         [[:block 0]]
    fn              [[:inner 0]]
    for             [[:block 1]]
    future          [[:block 0]]
    go              [[:block 0]]
    go-loop         [[:block 1]]
    if              [[:block 2]]
    if-let          [[:block 1]]
    if-not          [[:block 1]]
    if-some         [[:block 1]]
    let             [[:block 1]]
    letfn           [[:block 1] [:inner 2 0]]
    locking         [[:block 1]]
    loop            [[:block 1]]
    match           [[:block 1]]
    ns              [[:block 1]]
    proxy           [[:block 2] [:inner 1]]
    reify           [[:inner 0] [:inner 1]]
    struct-map      [[:block 1]]
    testing         [[:block 1]]
    thread          [[:block 0]]
    try             [[:block 0]]
    use-fixtures    [[:inner 0]]
    when            [[:block 1]]
    when-first      [[:block 1]]
    when-let        [[:block 1]]
    when-not        [[:block 1]]
    when-some       [[:block 1]]
    while           [[:block 1]]
    with-local-vars [[:block 1]]
    with-open       [[:block 1]]
    with-out-str    [[:block 0]]
    with-precision  [[:block 1]]
    with-redefs     [[:block 1]]})

; {:source "weavejester.cljfmt.core"}
(defmulti ^:private indenter-fn
  (fn [sym [type & args]] type))

(defmethod indenter-fn :inner [sym [_ depth idx]]
  (fn [zloc] (inner-indent zloc sym depth idx)))

(defmethod indenter-fn :block [sym [_ idx]]
  (fn [zloc] (block-indent zloc sym idx)))

(defn- make-indenter [[key opts]]
  (apply some-fn (map (partial indenter-fn key) opts)))

(defn- indent-order [[k _]]
  (cond (symbol? k) (str 0 key)
        (regex?  k) (str 1 key)
        :else       (throw (ex-info "Not supported" nil))))

(defn- custom-indent
  {:source "weavejester.cljfmt.core"}
  [zloc indents]
  (let [indenter (->> (sort-by indent-order indents)
                      (map make-indenter)
                      (apply some-fn))]
    (or (indenter zloc)
        (list-indent zloc))))

(defn- indent-amount
  {:source "weavejester.cljfmt.core"}
  [zloc indents]
  (case (-> zloc z/up z/tag)
    (:list :fn) (custom-indent zloc indents)
    :meta       (indent-amount (z/up zloc) indents)
    (coll-indent zloc)))

(defn- indent-line
  {:source "weavejester.cljfmt.core"}
  [zloc indents]
  (let [width (indent-amount zloc indents)]
    (if (> width 0)
      (zip/insert-right zloc (whitespace width))
      zloc)))

(defn indent
  {:source "weavejester.cljfmt.core"}
  [form indents]
  (let [indents (into default-indents indents)]
    (transform form edit-all should-indent? #(indent-line % indents))))

(defn reindent {:source "weavejester.cljfmt.core"} [form indents]
  (indent (unindent form) indents))

(defn reformat-form
  {:source "weavejester.cljfmt.core"}
  [form & [{:as opts}]]
  (-> form
      (cond-> (:remove-consecutive-blank-lines? opts true)
        remove-consecutive-blank-lines)
      (cond-> (:remove-surrounding-whitespace? opts true)
        remove-surrounding-whitespace)
      (cond-> (:insert-missing-whitespace? opts true)
        insert-missing-whitespace)
      (cond-> (:indentation? opts true)
        (reindent (:indents opts {})))))

(defn reformat-string
  {:source "weavejester.cljfmt.core"}
  [form-string & [options]]
  (-> (p/parse-string-all form-string)
      (reformat-form options)
      (n/string)))

