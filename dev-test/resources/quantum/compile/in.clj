; ==================== MACROS ===================
; oeval is |eval| in the object language

; can only be used in macros
(defmacro apply [f & args]
  (let [args-f (concat (butlast args) (last args))]
    (oeval (apply list f args-f))))

(defmacro set! [lval rval]
  (str/sp (oeval lval) "=" (oeval rval)))


; (defmacro jfn-1 [f]
;   (oeval
;     (list 'fn '[caller args]
;       (list '. 'caller f 'args))))

(defmacro splice [f & args]
  (oeval (list 'apply 'f 'args)))

(defmacro aget [coll n]
  (str (oeval coll) "[" (oeval n) "]"))

(defmacro first  [coll] (oeval (list 'aget coll 0)))
(defmacro second [coll] (oeval (list 'aget coll 1)))

(defmacro array [& args]
  (str "[" (->> args (map (partial oeval)) (str/join ", ")) "]"))

(defmacro = [a b] (str/paren (str/sp (oeval a) "==" (oeval b))))
(defmacro not= [a b] (str/paren (str/sp (oeval a) "!=" (oeval b))))

(defmacro type [obj]
  (println "IN TYPE WITH" obj)
  (->> obj oeval (str/sp "typeof") str/paren))

; TODO maybe doesn't work
(defmacro throw [errs]
  (println "ERRS IS" errs)
  (str/sp "throw" (oeval errs)))

(defmacro println [& objs]
  (oeval
    (quote+ (apply Debug/WriteLine ~objs))))

; TODO pre-register ns's and have |require| only be to def an alias 
(defmacro require [arg]
  (let [[ns-name-0 & {:keys [as]}] (-> arg (nth 1))
        _ (println "REQUIRING" (-> arg (nth 1)))
        ns-name-f (or as ns-name-0)
        file-name (str (-> ns-name-0 name) ".clj")
        file-str
          (io/read 
            :path [:test "quanta" "compile" file-name]
            :read-method :str)
        code (read-string (str "(do " file-str ")"))
        ^Fn register
          (fn [[spec-sym sym & body]]
            (let [registered-sym (keyword sym)
                  transformed-body
                    (condp = spec-sym
                      'def  (do (assert (-> body count (= 1)))
                                (first body))   
                      'defn (cons 'fn body))]
              [registered-sym transformed-body]))
        ^Fn register-ns
          (fn [ns-contents]
            (list 'def ns-name-f ns-contents))]
    (->> code
         (filter (fn-> first (in? #{'def 'defn})))
         (map register)
         (apply merge {})
         register-ns
         oeval
         ;(<- str "\n")
         )))

(defmacro conj [coll & args]
  (let [conj-sym
         (cond (-> coll metaclass (= 'Vec)) 'ppush :else 'mori/conj)]
    (oeval
      `(~'apply ~conj-sym ~coll ~args))))

; ===============================================

; ================== PLUMBING ===================

(defmacro with-transient! [obj & body]
  (oeval
    (concat
      (list 'do (list 'transient! obj))
      body
      (list (list 'persistent! obj)))))

; A good idea, but later
; (defmacro definline [sym arglist & body]
;   (let [body-f
;           (->> body (cons 'do)
;                (unquote-replacement (ns/local-context)))]
;     (identity ;oeval
;       (quote+
;         (defmacro ~sym ~arglist
;           ~body-f)))))

; (definline getr [coll a b]
;   (for-into (empty coll) [i (range a b)]
;     (get coll i)))

(defmacro getr [coll a b]
  (oeval
    (quote+
      (for-into (empty ~coll) [i (range ~a ~b)]
        (get coll i)))))

(defmacro map! [& args]
  (oeval (quote+ (apply C/hash-map ~args))))

(defmacro cast [class- obj]
  (str (-> class- str demunge-class str/paren) (oeval obj)))

(def-macro-alias conj! push)


(defmacro dot [& args] (->> args (map oeval) (str/join ".")))

(defmacro class [arg] (oeval (quote+ (.GetType ~arg))))

; (defrecord Class [a b c d e])
(defmacro defrecord [class- props]
  (let [prop-defs
         (->> props
              (map (partial list 'dot 'this))
              (map (fn [expr] (list 'set! expr (last expr)))))]
    (oeval
      (list 'defn (with-meta class- {:class true}) props
        (cons 'do prop-defs)))))

(defmacro symbol [sym] (oeval (quote+ (str ~sym))))

(defmacro ->
  [x & forms]
  (let [code (loop [x x forms forms]
      (if forms
        (let [form (first forms)
              next-form  (when (seq? form) (next  form))
              first-form (when (seq? form) (first form))
              threaded (if (seq? form)
                           (quote+ (apply ~first-form ~x ~next-form))
                           (list form x))]
          (recur threaded (next forms)))
        x))]
  (oeval code)))

(defmacro out [obj] (str/paren (str/sp "out" (oeval obj))))

(defmacro comment [obj] (str/sp "//" obj))

(defmacro inc [n] (oeval (quote+ (+ ~n 1))))
(defmacro inc! [n] (str/sp (oeval n) "++"))
(defmacro dec [n] (oeval (quote+ (- ~n 1))))
(defmacro count [coll] (oeval (quote+ (C/count ~coll))))
(defmacro lasti [coll] (oeval (quote+ (-> ~coll count dec))))

(defmacro get [& args] (oeval (quote+ (apply C/get ~args))))
(defmacro keys [arg] (oeval (quote+ (.-Keys ~arg))))
(defmacro key [arg] (oeval (quote+ (.-Key ~arg))))
(defmacro vals [arg] (oeval (quote+ (.-Values ~arg))))
(defmacro val [arg] (oeval (quote+ (.-Value ~arg))))
(defmacro atom [arg] (oeval arg))
(defmacro reset! [arg val-] (oeval (quote+ (set! ~arg ~val-))))
(defmacro array-of [type & vals-] (str/sp "new" (str type "[]") (str/sp "{" (->> vals- (map oeval) doall (str/join ", ")) "}")))
(defmacro array-map [& args] (oeval (quote+ (apply map! ~args))))
; =====

(def admin-url "http://newmarket.siretechnologies.com/sire/administrator/licenseconnections.aspx")
(def admin-key "f0ec6a575a984fb89076442323108f36")
(def connection-string "Server=tcp:urb3qb5npi.database.windows.net,1433;Database=NewMarketPublic;User ID=PrimeGov@urb3qb5npi;Password=Prime2014;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;")


(defn ^String retrieve-session-key []
  (let [response  (LHttp/request admin-url "get" nil {"sun" admin-key} nil)
        html-node (HTML/parse response)
        ^"Predicate<HtmlNode.Tag>" pred
          (fn [^HtmlNode.Tag node]
            (= (get (.-Attributes node) "id") "Grid_1"))
        filtered  (HTML/filter html-node pred)
        ^HtmlNode.Tag node-f (cast HtmlNode$Tag (C/first (HTML/filter htmlNode pred)))]
    (when (C/empty filtered)
      (throw (Exception. "No grid tag found.")))

    (get (.-Attributes nodeF) "key")))

(defn ^:protected ^:dynamic ^{:t "void"} Dump_Click [^Object sender ^EventArgs e]
  (let [session-key (retrieve-session-key)
        ws          (SIREService$Service.)
        ^System.Data.DataSet sire-return
          (.GetMeetings ws session-key 2 "Council" "1/1/1900" "1/1/2020")]
    (println "Starting with retrieved session key:" session-key)
    (Response/Write "<table>")

    (doseq [^DataRow row (-> sire-return .-Tables first .-Rows)]
      (Response/Write "<tr>")
      (Response/Write (nstr "<td>" (.ToString (get row "meet_id"  )) "</td>"))
      (Response/Write (nstr "<td>" (.ToString (get row "meet_type")) "</td>"))
      (Response/Write (nstr "<td>" (.ToString (get row "meet_date")) "</td>"))
      (Response/Write "</tr>")
      (Response/Flush))
  
    (Response/Write "</table>")
    (Response/End)))

(defn ^:protected ^:dynamic ^{:t "void"} Import_Click [^Object sender ^EventArgs e]
  (println "Starting")
  (let [ws           (SIREService$Service.)
        cn           (SqlConnection.)
        sk           (retrieve-session-key)
        headers {"Published_Meetings_Doc"
                   ["doc_id" "vault_id" "security" "date_added" "MEET_ID" "MEET_TYPE" "MEET_DESC" "MEET_DATE" "MEET_TIME" "MEET_LOCATION" "MEET_STATUS" "MEET_PUBLISHED_DATE" "file_count"]
                 "Published_Meetings_Page"
                   ["page_id" "doc_id" "alias" "subdir" "orig_extens" "pagenum" "date_added" "ocr_done" "pdf_done" "fulltext_done" "fulltext_catalog_index" "rev_num" "has_revisions" "checked_out" "pdf_searchable" "page_description" "page_count" "thumb_done" "item_id" "confidential" "item_page_id" "minutes_status" "item_outline" "minutes_confidential"]}
        headers-table
          {"Published_Meetings_Doc"
            {"doc_id"              "INT"
             "vault_id"            "INT"
             "security"            "VARCHAR(50)"
             "date_added"          "VARCHAR(10)"
             "MEET_ID"             "INT"
             "MEET_TYPE"           "VARCHAR(50)"
             "MEET_DESC"           "VARCHAR(1000)"
             "MEET_DATE"           "DATETIME"
             "MEET_TIME"           "VARCHAR(15)"
             "MEET_LOCATION"       "VARCHAR(50)"
             "MEET_STATUS"         "VARCHAR(20)"
             "MEET_PUBLISHED_DATE" "DATETIME"
             "file_count"          "INT"}
           "Published_Meetings_Page"
             {"page_id"                "INT"
              "doc_id"                 "INT"
              "alias"                  "INT"
              "subdir"                 "INT"
              "orig_extens"            "VARCHAR(10)"
              "pagenum"                "INT"
              "date_added"             "VARCHAR(10)"
              "ocr_done"               "INT"
              "pdf_done"               "INT"
              "fulltext_done"          "INT"
              "fulltext_catalog_index" "INT"
              "rev_num"                "INT"
              "has_revisions"          "INT"
              "checked_out"            "INT"
              "pdf_searchable"         "INT"
              "page_description"       "VARCHAR(1000)"
              "page_count"             "INT"
              "thumb_done"             "INT"
              "item_id"                "INT"
              "confidential"           "INT"
              "item_page_id"           "INT"
              "minutes_status"         "INT"
              "item_outline"           "VARCHAR(20)"
              "minutes_confidential"   "INT"}}]
    (set! (.-ConnectionString cn) connection-string)
    (.Open cn)

    (try
      (doseq [table-name (keys headers)]
        (let [data-set-n (DataSet.)
              sql        (nstr "SELECT * FROM " table-name)
              da         (SqlDataAdapter. sql cn)
              cmdBuilder (SqlCommandBuilder. da)]
          (.Fill da data-set-n)
      
          (println "Count before Update:" (-> data-set-n .-Tables first .-Rows .-Count))
          (DB/insert-table
            (-> headers       (get table-name))
            (-> headers-table (get table-name))
            table-name
            (-> ws (.GetDataset sk sql) .-Tables first)
            cn)))
      (finally (.Close cn) (println "")))))


; using System.Web.Services.Protocols;

; public partial class Service : System.Web.Services.Protocols.SoapHttpClientProtocol {



; [System.Web.Services.Protocols.SoapDocumentMethodAttribute("http://www.siretechnologies.com/GetDataset", RequestNamespace="http://www.siretechnologies.com/", ResponseNamespace="http://www.siretechnologies.com/", Use=System.Web.Services.Description.SoapBindingUse.Literal, ParameterStyle=System.Web.Services.Protocols.SoapParameterStyle.Wrapped)]
;         public System.Data.DataSet GetDataset(string SessionKey, string sql) {
;             object[] results = this.Invoke("GetDataset", new object[] {
;                         SessionKey,
;                         sql});
;             return ((System.Data.DataSet)(results[0]));
;         }
;         }

;"Published_Meetings_Doc"
;headers []

(defn ^{:t "void"} insert-table [ ^"ImmutableArray<String>" headers ^"ImmutableDictionary<String, String>" headers-table ^String table-name ^DataTable data ^SqlConnection conn]
  (let [drop-str   (nstr "DROP TABLE [dbo].[" table-name "]; ")
        create-str (atom "")
        insert-str (atom "")
        values-str (atom "")
        select-str (nstr "SELECT * FROM [dbo].[" table-name "];")
        n (atom 0)]
    (comment "CREATE-STR")

    (doseq [header headers]
      (reset! create-str (nstr create-str "[" header "]" " "
                           (get headers-table header)    " " "NULL"))
      (when (not= n (lasti headers))
        (reset! create-str (nstr create-str ", ")))
      (inc! n))

    (reset! create-str (nstr "CREATE TABLE [dbo].[" table-name "] (" create-str "); "))

    (comment "INSERT-STR")

    (reset! n 0)
    (doseq [header headers]
      (reset! insert-str (nstr insert-str header))
      (when (not= n (lasti headers))
        (reset! insert-str (nstr insert-str ", ")))
      (inc! n))

    (reset! insert-str (nstr "INSERT INTO " table-name " (" insert-str ") "))

    (comment "VALUES-STR")

    (reset! n 0)
    (doseq [header headers]
      (reset! values-str (nstr values-str "@" header))
      (when (not= n (lasti headers))
        (reset! values-str (nstr values-str ", ")))
      (inc! n))

    (reset! values-str (nstr "VALUES (" values-str ");"))

    (reset! insert-str (nstr insert-str values-str))

    (let [drop-command   (SqlCommand. drop-str conn)
          create-command (SqlCommand. create-str conn)
          select-command (SqlCommand. select-str conn)]
      (try (.ExecuteNonQuery drop-command)
        (catch SqlException _))

      (.ExecuteNonQuery create-command)

      (doseq [^DataRow row (.-Rows data)] 
        (let [^SqlCommand insert-command (SqlCommand. insert-str conn)]
          (doseq [header headers] 
            (-> insert-command .-Parameters (.AddWithValue (nstr "@" header) (aget row header))))
          
          (println "INSERTED ROW")
          (.ExecuteNonQuery insert-command)))

      (println "SELECTING FROM DATABASE IS:")
      (print-results select-command)
      (println "========"))))
        


(defn ^:protected ^:dynamic ^{:t "void"} VideoDownload_Click [^Object sender ^EventArgs e]
  (let [conn       (SqlConnection.)
        table-name "Published_Meetings_Doc"
        sql        (nstr "SELECT * FROM " table-name)
        command    (SqlCommand. sql conn)
        data-set-n (DataSet.)
        sda        (SqlDataAdapter. sql conn)
        cmdBuilder (SqlCommandBuilder. sda)]
    (set! (.-ConnectionString conn) connection-string)
    (.Open conn)
    
    (.Fill sda data-set-n)
    
    (doseq [^DataRow row (-> data-set-n .-Tables first .-Rows)]
      (Response/Write "<tr>")
      (Response/Write (nstr "<td>" (.ToString (get row "MEET_ID"  )) "</td>"))
      (Response/Write (nstr "<td>" (.ToString (get row "MEET_TYPE")) "</td>"))
      (Response/Write "</tr>")
      (Response/Flush))
  
    (Response/Write "</table>")
    (Response/End)

    (.Close conn)))