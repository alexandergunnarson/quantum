(ns quantum.datagrid.core
  (:require-quantum [:lib])
  (:require [quantum.datagrid.excel :as xl]))

; http://poi.apache.org/apidocs/index.html
; HELPER FUNCTIONS
; (defn ! [expr & [col-width]]
;   (binding [*print-right-margin* (iff nnil? col-width 100)]
;     (pprint expr)))
; (defn unlettered [^String str-0]
;   (-> str-0
;       str/upper-case
;       (str/replace (re-pattern (str "[" (apply str str/upper-chars) "]")) "")))
; ; FIND SOME BETTER WAY TO DO THIS
; (defprotocol Col* (col* [col-0]))
; (extend-protocol Col*
;   String
;   (col* [^String col-0]
;     (->> col-0 (index-of str/upper-chars) inc))
;   Number
;   (col* [^Number col-0]
;     (->> col-0 dec str/upper-chars)))
; (defn parse-xl-date [date]
;   (-> date
;      (time-coerce/from-date)
;      (#(time-form/unparse (time-form/formatter "MM/dd/yyyy") %))))
; (defn save-as! ; implement this in quantum.core.io as a multimethod to a base fn
;   [file-name sheet-data
;      & {:keys [directory] 
;         :or   {directory [:test "Unknowns"]}}]
;   (-> (xl/xlsx) ; new xl workbook.xml
;       (xl/build-workbook {"Results" sheet-data})
;       (#(io/write! :directory directory :data % :name file-name :file-type "xlsx"))))
; (defn color [cell]
;   (let [fore-color
;          (-> cell
;              (.getCellStyle)
;              (.getFillForegroundColorColor))
;         rgb-color (when fore-color (.getRgb fore-color))
;         rgb-arr
;           (when rgb-color
;             (getr rgb-color 0 3))] ; [0 1 2]
;   rgb-arr))
; (defn str-val [cell]
;   (try
;     (-> cell xl/cell-value
;         (condf nil? str number? (compr int str) :else str))
;     (catch NullPointerException e "")))


; Creating comments:

; {"a" [[{:value "foo" :comment {:text "Lorem Ipsum" :width 4 :height 2}}]]}

; (def workbook 
;   (xl/lazy-workbook (xl/xlsx (io/read :file-name "Unknowns/ToAnalyze.xlsx" :file-type "xlsx" :in :resources)))) 