(ns quantum.parse.core
  (:refer-clojure :exclude
    [conj! reduce])
  (:require
    [quantum.core.data.string
      :refer [!str]]
    [quantum.core.fn          :as fn
      :refer [firsta aritoid rfn]]
    [quantum.core.collections :as coll
      :refer [remove+ map+ join reduce transduce conj!]]
    [quantum.core.logic
      :refer [fn-nil]]
    [instaparse.core          :as insta])
  #?(:cljs (:import goog.string.StringBuffer)))

(def std-defs ; newline to facilitate concatenation
  "
   newline      = <#'[\\n\\r]'>
   space        = <#'[ \\t]'>
   spaces       = <#'[ \\t]+'>
   quote        = #'[\\\"]'
   blank        = (newline | space)*
   any          = #'.'
   anys         = #'.*'
   quoted       = <quote> #'[^\\\"]*' <quote>
   non-newlines = #'[^\\n\\r]*'
   non-newline  = #'[^\\n\\r]'
   alpha        = #'[a-z]'
   alphanum     = alpha | num
   num          = #'[0-9]'")

(defmulti parse firsta)

(def java-properties-parser
  (insta/parser
    (str "S         = (line newline)+ line? / line

          line      = spaces? / <comment> / prop-pair
          comment   = spaces? '#' non-newlines
          prop-pair = spaces? prop spaces? <'='> spaces? value spaces?
          prop      = #'[a-z0-9\\-\\.]+' / quoted
          value     = #'[a-z0-9\\-\\.]+' / quoted"
         std-defs)))

(defmethod parse :java-properties
  ^{:tests `{[:java-properties
              "# COMMENT
               protocol=free
               host=\"localhost\"
               port=4334"]
             {"protocol" "free"
              "host"     "localhost"
              "port"     "4334"}}}
  [_ text]
  (->> text
       java-properties-parser
       (insta/transform
         {:S         vector
          :newline   fn-nil
          :spaces    fn-nil
          :quoted    identity
          :value     identity
          :prop      identity
          :prop-pair (fn [& args]
                       (->> args (remove+ nil?) (join [])))
          :line      (aritoid fn-nil identity)})
       (remove+ nil?)
       (join {})))

(defmulti
  ^{:doc "The inverse of |parse|."}
  output firsta)

(defmethod output :java-properties
  ^{:tests `{[:java-properties
              {"protocol"   "free"
               "host"       "localhost"
               "other-prop" "has a space"}]
             "protocol=\"free\"
              host=\"localhost\"
              other-prop=\"has a space\""}}
  [_ props & [{:keys [no-quote?] :as opts}]]
  ; TODO syntactically validate prop key
  (->> props
       (map+ (rfn [k v] (str (name k) \=
                             (when-not no-quote? \") v
                             (when-not no-quote? \"))))
       (transduce (fn ([] (!str))
                      ([ret] (str ret))
                      ([ret kv] ; TODO abstract this
                        (conj! ret \newline)
                        (conj! ret kv))))))
