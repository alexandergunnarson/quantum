(ns quantum.datagrid.excel (:gen-class))
(require '[quantum.core.ns :as ns])
(ns/require-all *ns* :lib :clj)
(require
  '[clojure.java.io :refer :all])
(import
  'java.util.Date
  '[org.apache.poi.xssf.usermodel XSSFWorkbook]
  '[org.apache.poi.hssf.usermodel HSSFWorkbook]
  '[org.apache.poi.ss.usermodel Row Cell DateUtil
    WorkbookFactory CellStyle Font Hyperlink Workbook Sheet])

(def ^:dynamic *row-missing-policy* Row/CREATE_NULL_AS_BLANK)

(def data-formats
  {:general 0 :number 1 :decimal 2 :comma 3
   :accounting 4 :dollars 5 :red-neg 6 :cents 7 :dollars-red-neg 8
   :percentage 9 :decimal-percentage 10
   :scientific-notation 11
   :short-ratio 12 :ratio 13
   :date 14 :day-month-year 15 :day-month-name 16 :month-name-year 17
   :hour-am-pm 18 :time-am-pm 19 :hour 20 :time 21 :datetime 22})

(def color-indices
  {; MONOCHROMATIC
   :automatic 64 :white 9 :black 8
   :grey-25-percent 22 :grey-40-percent 55 :grey-50-percent 23 :grey-80-percent 63
   ; LIGHT RED (PINK)
   :pink 14 :lavender 46 :rose 45 :orchid 28
   ; RED
   :red 10 :dark-red 16
   ; ORANGE
   :light-orange 52 :orange 53
   ; YELLOW
   :light-yellow 43 :lemon-chiffon 26 :yellow 13 :gold 51 :dark-yellow 19
   ; GREEN
   :light-green 42 :bright-green 11 :lime 50 :green 17 :dark-green 58
   ; GREEN-BLUE
   :olive-green 59 :sea-green 57
   ; BLUE-GREEN
   :aqua 49 :coral 29 :light-turquoise 41 :turquoise 15 :teal 21 :dark-teal 56
   ; LIGHT BLUE 
   :pale-blue 44 :light-blue 48 :sky-blue 40
   :light-cornflower-blue 31 :cornflower-blue 24
   ; BLUE
   :blue 12 :dark-blue 18 :blue-grey 54
   ; BLUE-PURPLE (INDIGO)
   :royal-blue 30 :indigo 62 
   ; PURPLE/VIOLET
   :violet 20 :maroon 25 :plum 61
   ; EARTH-COLORED
   :tan 47 :brown 60})

(def underline-indices
  {:none 0 :single 1 :double 2 :single-accounting 33 :double-accounting 34})

;; Utility Constant Look Up ()

(defn constantize
  "Helper to read constants from constant like keywords within a class. 
   Reflection powered (i.e., slow)."
  [^Class class-0 kw]
  (-> class-0
      (.getDeclaredField (-> kw name (str/replace "-" "_") str/upper-case))
      (.get Object)))

(defn cell-style-constant
  ([kw] (cell-style-constant kw nil))
  ([kw prefix]
    (if (number? kw)
        (short kw)
        (short (constantize CellStyle
                 (if prefix
                     (str
                       (name prefix) "-"
                       (-> kw name
                           (.replaceFirst (str (name prefix) "-") "")
                           (.replaceFirst (str (name prefix) "_") "")
                           (.replaceFirst (name prefix) "")))
                     kw))))))

;; Workbook and Style functions
(defprotocol DataFormat ; Protocol functions don't support rest params.
  "Get dataformat by number or create new."
  (data-format
    [this sformat]
    [this ^Workbook wb sformat]))
(extend-protocol DataFormat
  Keyword
  (data-format [sformat] (get data-formats sformat))
  ; (data-format [sformat]
  ;   (if (nil? (get data-formats sformat))
  ;       (throw (Exception. (str "No matching data format: " sformat)))
  ;       (data-format (get data-formats sformat))))

  Number
  (data-format [sformat] (println "In short!")(short sformat))
  String
  (data-format [^Workbook wb sformat]
    (-> wb .getCreationHelper .createDataFormat (.getFormat ^String sformat)))
  nil
  (data-format [sformat]
    (throw (Exception. (str "No matching data format: " sformat)))))

(defn set-border
  "Set borders, css order style. Borders set CSS order."
  ([cs all]        (set-border cs all  all   all  all  ))
  ([cs caps sides] (set-border cs caps sides caps sides))
  ([^CellStyle cs top right bottom left] ; CSS ordering
     (.setBorderTop    cs (cell-style-constant top    :border))
     (.setBorderRight  cs (cell-style-constant right  :border))
     (.setBorderBottom cs (cell-style-constant bottom :border))
     (.setBorderLeft   cs (cell-style-constant left   :border))))

(defn- col-idx [v]
  (short (if (keyword? v) (color-indices v) v)))

(defn font
  "Register font with "
  [^Workbook wb fontspec]
  (if (isa? (type fontspec) Font)
      fontspec
      (let [default-font (.getFontAt wb (short 0)) ;; First font is default
            boldweight (short (get fontspec :boldweight (if (:bold fontspec)
                                                            Font/BOLDWEIGHT_BOLD
                                                            Font/BOLDWEIGHT_NORMAL)))
            color (short (if-let [k (fontspec :color)]
                           (col-idx k)
                           (.getColor default-font)))
            size       (short (get fontspec :size (.getFontHeightInPoints default-font)))
            name       (str (get fontspec :font (.getFontName default-font)))
            italic     (boolean (get fontspec :italic false))
            strikeout  (boolean (get fontspec :strikeout false))
            typeoffset (short (get fontspec :typeoffset 0))
            underline  (byte (if-let [k (fontspec :underline)]
                               (if (keyword? k)
                                   (underline-indices k)
                                   k)
                               (.getUnderline default-font)))]
        (or
         (.findFont wb boldweight size color name italic strikeout typeoffset underline)
         (doto (.createFont wb)
           (.setBoldweight boldweight)
           (.setColor color)
           (.setFontName name)
           (.setItalic italic)
           (.setStrikeout strikeout)
           (.setFontHeightInPoints size)
           (.setUnderline underline))))))

(defn create-cell-style
  "Create style for workbook"
  [^Workbook wb & {format :format alignment :alignment border :border fontspec :font
                   bg-color :background-color fg-color :foreground-color pattern :pattern}]
  (let [cell-style (.createCellStyle wb)]
    (when fontspec (.setFont cell-style (font wb fontspec)))
    (when format (.setDataFormat cell-style (data-format wb format)))
    (when alignment (.setAlignment cell-style (cell-style-constant alignment :align)))
    (when border (if (coll? border)
                 (apply set-border cell-style border)
                 (set-border cell-style border)))
    (when fg-color (.setFillForegroundColor cell-style (col-idx fg-color)))
    (when bg-color (.setFillBackgroundColor cell-style (col-idx bg-color)))
    (when pattern  (.setFillPattern cell-style (cell-style-constant pattern)))
    cell-style))

;; extract the sub-map of options supported by create-cell-style
(defn- get-style-attributes [m]
  (select-keys m [:format :alignment :border :font :background-color :foreground-color :pattern]))

(defprotocol StyleCache
  (build-style [this cell-data]))

;; iterate a nested sheet-data seq and create cell styles
(defn create-sheet-data-style [cache data]
  (for [row data]
    (for [col row]
      (if (and (map? col) (not (empty? (get-style-attributes col))))
        (assoc col :style (build-style cache (get-style-attributes col)))
        col))))

(defn caching-style-builder [wb]
  (let [cache (atom {})]
    (reify StyleCache
      (build-style [_ style-key]
        (if-let [style (get @cache style-key)]
          style
          (let [style (apply create-cell-style wb (reduce #(conj %1 (first %2) (second %2)) [] style-key))]
            (swap! cache assoc style-key style)
            style))))))

;; Reading functions

(defn cell-value
  "Return proper getter based on cell-value"
  ([^Cell cell] (cell-value cell (.getCellType cell)))
  ([^Cell cell cell-type]
     (condp = cell-type ; case doesn't work... maybe it does identity? not sure
       Cell/CELL_TYPE_BLANK   nil
       Cell/CELL_TYPE_STRING  (.getStringCellValue cell)
       Cell/CELL_TYPE_NUMERIC (if (DateUtil/isCellDateFormatted cell)
                                  (.getDateCellValue cell)
                                  (.getNumericCellValue cell))
       Cell/CELL_TYPE_BOOLEAN (.getBooleanCellValue cell)
       Cell/CELL_TYPE_FORMULA {:formula (.getCellFormula cell)}
       Cell/CELL_TYPE_ERROR   {:error (.getErrorCellValue cell)}
       :unsupported)))

(defn cell-comment [cell]
  (when cell
    (when-let     [comment (.getCellComment cell)]
      (when-let   [string  (.getString comment)]
        (when-let [string  (.getString string)]
          {:text string})))))

(defn ^Workbook xlsx
  "Create or open new excel workbook. Defaults to xlsx format."
  ([] (XSSFWorkbook.))
  ([input] (WorkbookFactory/create input)))

(defn ^Workbook xls
  "Create or open new excel workbook. Defaults to xls format."
  ([] (HSSFWorkbook.))
  ([input] (WorkbookFactory/create input)))

(defn sheets
  "Get seq of sheets."
  [^Workbook wb] (map #(.getSheetAt wb %1) (range 0 (.getNumberOfSheets wb))))

(defn rows
  "Return rows from sheet as seq. Simple seq cast via Iterable implementation."
  [sheet] (seq sheet))

(defn cells
  "Return seq of cells from row. Simple seq cast via Iterable implementation."
  [row] (seq row))

(defn values
  "Return cells from sheet as seq."
  [row] (map cell-value (cells row)))

(defn row-seq
  "Returns a lazy seq of cells of row.
  Options:
    :cell-fn function called on each cell, defaults to cell-value
    :mode    either :logical (default) or :physical
  Modes:
    :logical  returns all cells even if they are blank
    :physical returns only the physically defined cells"
  {:arglists '([row & opts])}
  [^Row row & {:keys [cell-fn mode] :or {cell-fn cell-value mode :logical}}]
  (case mode
    :logical  (map #(when-let [cell (.getCell row %)] (cell-fn cell)) (range 0 (.getLastCellNum row)))
    :physical (map cell-fn row)
    (throw (ex-info (str "Unknown mode " mode) {:mode mode}))))

(defn lazy-sheet
  "Lazy seq of seqs representing rows and cells of sheet.
  Options:
    :cell-fn function called on each cell, defaults to cell-value
    :mode    either :logical (default) or :physical
  Modes:
    :logical  returns all cells even if they are blank
    :physical returns only the physically defined cells"
  {:arglists '([sheet & opts])}
  [sheet & {:keys [cell-fn mode]
            :or   {cell-fn cell-value
                   mode :logical}}]
  (map #(row-seq % :cell-fn cell-fn :mode mode) sheet))

(defn sheet-names
  [^Workbook wb]
  (->> (.getNumberOfSheets wb) (range) (map #(.getSheetName wb %))))

(defn lazy-workbook
  "Lazy workbook report."
  ([wb] (lazy-workbook wb lazy-sheet))
  ([wb sheet-fn] (zipmap (sheet-names wb) (map sheet-fn (sheets wb)))))

(defn get-cell
  "Get cell within row"
  ([col ^Row row] (.getCell row (dec col))) ; dec to make it more natural Excel indices
  ([^Sheet sheet col row] (get-cell col (or (.getRow sheet (dec row)) (.createRow sheet (dec row))))))

;; Writing Functions

(defn get-creation-helper [cell]
  (-> cell .getSheet .getWorkbook .getCreationHelper))

(defn- get-link-type [m]
  (some #{:link-url :link-email :link-document :like-file} (keys m)))

(defn create-link [^Cell cell kw link-to]
  (let [link-type (constantize org.apache.poi.common.usermodel.Hyperlink kw)
        link      (.createHyperlink (get-creation-helper cell) link-type)]
    (.setAddress link link-to)
    (.setHyperlink cell link)))

(defn create-rich-text-string [cell text]
  (.createRichTextString (get-creation-helper cell) text))

(defn create-comment [cell {:keys [width height text] :or {width 2 height 2}}]
  (let [helper (get-creation-helper cell)
        anchor (doto (.createClientAnchor helper)
                 (.setCol1 (.getColumnIndex cell))
                 (.setCol2 (+ (.getColumnIndex cell) width))
                 (.setRow1 (.getRowIndex cell))
                 (.setRow2 (+ (.getRowIndex cell) height)))
        rich-text (create-rich-text-string cell text)
        comment (doto (.createCellComment (.createDrawingPatriarch (.getSheet cell)) anchor)
                  (.setString rich-text))]
    comment))

(defmulti  set-cell! (fn [^Cell cell val] (class val)))
(defmethod set-cell! Boolean [^Cell cell ^Boolean b   ] (.setCellValue cell b                   ))
(defmethod set-cell! Number  [^Cell cell          n   ] (.setCellValue cell (double n)          ))
(defmethod set-cell! String  [^Cell cell ^String  s   ] (.setCellValue cell s                   ))
(defmethod set-cell! Keyword [^Cell cell          kw  ] (.setCellValue cell (name kw)           ))
(defmethod set-cell! Date    [^Cell cell ^Date    date] (.setCellValue cell date                ))
(defmethod set-cell! nil     [^Cell cell          null] (.setCellType  cell Cell/CELL_TYPE_BLANK))
(defmethod set-cell! APersistentMap [^Cell cell m]
  ;(println "m:" m)
  (set-cell! cell (:value m))
  (when-let [link-key (get-link-type m)]
    (create-link cell link-key (m link-key)))
  (when-let [style (:style m)]
    (.setCellStyle cell style))
  (when-let [formula (:formula m)]
    (.setCellFormula cell formula))
  (when-let [comment (:comment m)]
    (.setCellComment cell (create-comment cell comment))))

(defn set-cell-val!
  "Set cell at specified location with value."
  ([cell value] (set-cell! cell value))
  ([col ^Row row value]
    (set-cell-val! (or (get-cell col row) (.createCell row (dec col))) value))
  ([^Sheet sheet col row value]
    (set-cell-val! col (or (.getRow sheet (dec row)) (.createRow sheet (dec row))) value)))

(defn merge-rows
  "Add rows at end of sheet."
  [sheet start rows]
  ;(println "Merging rows...")
  (->> [(range start (+ start (count rows))) rows]
       (apply map
         (fn [rownum vals-n] ; if it's map and doall, why not use reducers?
           ;(println "In fn rownum vals")
           (doall
             (map
                (fn [col val-n]
                  ;(println "In col val-n")
                  (set-cell-val! sheet col rownum val-n)
                  ;(println "Done setting cell")
                  )
                (iterate inc 1)
                vals-n))))
       doall
       (with-msg "Finished merging these rows!")))

(defn build-sheet
  "Build sheet from seq of seq (representing cells in row of rows)."
  [^Workbook wb sheetname rows]
  (let [sheet (if sheetname
                (.createSheet wb sheetname)
                (.createSheet wb))]
    (merge-rows sheet 1 rows)))

(defn build-workbook
  "Build workbook from map of sheet names to multi dimensional seqs (ie a seq of seq)."
  ([wb wb-map]
     (let [cache (caching-style-builder wb)]
       (doseq [[^String sheetname rows] wb-map]
         (println (str "Building sheet \"" sheetname "\"..."))
         (build-sheet wb sheetname (create-sheet-data-style cache rows)))
       wb))
  ([wb-map] (build-workbook (xlsx) wb-map)))