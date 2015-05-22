; (defn = [a b]
;   )
; find : in?
; string= : =
; char=   : =
; defun -> defn
; defvar -> def
; defparameter -> def

(ns quantum.tokenize
  (:import java.io.StringReader java.io.EOFException))

; (require
;   '[quantum.core.ns :as ns :refer [defalias alias-ns]]
;   '[quantum.core.numeric :as num :refer [int+]])
; (ns/require-all *ns* :clj :lib)

; (defnt prs
;   char? ([c] (.append *out* c) nil))

; (defn peek-char
;   "A la Common Lisp."
;   [^StringReader in]
;   (.mark in (int 1))
;   (with-do (.read in) (.reset in)))

; (defn read-char
;   "A la Common Lisp."
;   [^StringReader in & [eof-error?]]
;   (.read in))

; (def digit-chars {\0 0 \1 1 \2 2 \3 3 \4 4 \5 5 \6 6 \7 7 \8 8 \9 9
;                   \a 10 \b 11 \c 12 \d 13 \e 14 \f 15
;                   \A 10 \B 11 \C 12 \D 13 \E 14 \F 15})

; (defn digit-char?
;   "A la Common Lisp."
;   ([c] (digit-char? c 10))
;   ([c radix]
;     (when (> radix 16)
;       (throw+ {:msg "Radix > 16 not supported yet."}))
;     (and (get digit-chars c)
;          (< (get digit-chars c) radix))))

; (defn identifier-char? [ch]
;   (or (and (str/alphanumeric? ch)
;            (not (str/whitespace? ch)))
;       (= ch \$)
;       (= ch \_)))

; (defrecord Return [ret])
; (defn     escape  [ret] (throw+ (Return. ret)))
; (defmacro ret-val [exprs] `(try+ ~@exprs (catch Return ret# (:ret ret#))))


; (defrecord Token [type value line char pos newline-before? comments-before?])

; (defn token? [token type value]
;   (and (= (:type  token) type)
;        (= (:value token) value)))

; (defn token-type? [token type]
;   (= (:type token) type))
; (defn token-id [token]
;   (:value token))

; (def *line*)
; (def *char*)
; (def *position*)

; (defn js-parse-error []
;   (throw+ {:type     :js-parse-error
;            :line     *line*
;            :position *position*
;            :char     *char*}))

; (def *operator-chars* #{\+ \- \* \& \% \= \< \> \! \? \| \~ \^})
; (def *operators*
;   (->> #{"in" "instanceof" "typeof" "new" "void" "delete" "++" "--" "+" "-" "!" "~" "&" "|" "^" "*" "/" "%"
;          ">>" "<<" ">>>" "<" ">" "<=" ">=" "==" "===" "!=" "!==" "?" "=" "+=" "-=" "/=" "*=" ">>=" "<<="
;          ">>>=" "~=" "%=" "|=" "^=" "&=" "&&" "||"}
;        (map+ keyword)
;        (into #{})))

; (def *keywords*
;   #{:break :case :catch :continue :debugger :default :delete :do :else :false
;     :finally :for :function :if :in :instanceof :new :null :return :switch
;     :throw :true :try :typeof :var :void :while :with})

; (def *keywords-before-expression* #{:return :new :delete :throw :else :case})
; (def *atom-keywords* #{:false :null :true :undefined})
; (def *reserved-words-ecma-3*
;   #{"abstract" "enum" "int" "short" "boolean" "export" "interface" "static"
;     "byte" "extends" "long" "super" "char" "final" "native" "synchronized"
;     "class" "float" "package" "throws" "const" "goto" "private" "transient"
;     "debugger" "implements" "protected" "volatile" "double" "import" "public"})
; (def *reserved-words-ecma-5*
;   #{"class" "enum" "extends" "super" "const" "export" "import"})

; (def *check-for-reserved-words* nil)
; (def *ecma-version* 3)

; (defn read-js-number-1
;   {:todo ["Change StringBuilder code"]
;    :throws "Return"}
;   [^Fn peek-fn ^Fn next-fn & {:keys [junk-allowed?]}]
;   (let [^String digits
;           (fn [radix]
;             (let [s (StringBuilder.)]
;               (loop [ch (peek-fn)]
;                 (when (and ch (digit-char? ch radix))
;                   (.append s (next-fn)))
;                   (recur (peek-fn)))
;               (.toString s)))
;         minus? (case (peek-fn)  
;                       \+ (do (next-fn) false)
;                       \- (do (next-fn) true))
;         body  (digits 10)
;         *read-default-float-format* 'double-float
;         ret (fn [x]
;               (escape
;                 (and x (or junk-allowed? (= (peek-fn) nil))
;                      (if minus?
;                          (if (= x :infinity)
;                              :-infinity
;                              (- x))
;                          x))))]
;     (cond (and (= body "0")
;                (in? (peek-fn) "xX")
;                (next-fn))
;             (ret (int+ (digits 16) 16)) ; :junk-allowed true  
;           (in? (peek-fn) ".eE")
;             (let [base      (atom (if (= body "") 0 (int+ body)))
;                   expt      0
;                   expt-neg? (atom nil)]
;               (if (and (= (peek-fn) \.) (next-fn))
;                   (let [digs (atom (digits 10))]
;                     (if (= @digs "")
;                         (when (= body "") (ret nil))
;                         (loop [recur? (volatile! false)]
;                           (try
;                             (swap! base inc
;                               (/ (int+ @digs)
;                                  (expt 10 (count @digs))))
;                             (catch ArithmeticException _
;                               (throw+ {:msg "Could not determine overflow vs. underflow."})
;                               (swap! digs popr) ; (subs= digs 0 (dec (count @digs)))
;                               (swap! recur? true) ; how's that for "can't recur from tail position"?
;                               ))
;                           (recur @recur?))))
;                   (when (= body "") (ret nil)))
;               (when (and (in? (peek-fn) "eE")
;                          (next-fn))
;                 (reset! expt-neg? (and (in? (peek-fn) "+-")
;                                        (= (next-fn) \-)))
;                 (let [digs (digits 10)]
;                   (when (= digs "") (ret nil))
;                   (reset! expt (int+ digs))))
;               (try
;                 (ret (* @base (expt 10 (if @expt-neg? (- expt) expt))))
;                 (catch ArithmeticException _
;                   (throw+ {:msg "Could not determine overflow vs. underflow."}))
;                 ;(catch floating-point-overflow  '() (ret :infinity))
;                 ;(catch floating-point-underflow '() (ret 0))
;                 ))
;           (= body "")
;             (ret nil)
;           (and (= (get body 0) \0)
;                (loop [i 1]
;                  (if (>= i (count body))
;                      true
;                      (if (not (digit-char? (get body i)))
;                          false
;                          (recur (inc i))))))
;              (ret (int+ body 8))
;           (= body "")
;             (ret nil)
;           :else
;             (ret (int+ body)))))

; (defn read-js-number [stream & {:keys [junk-allowed?]}]
;   (let [peek-1 #(peek-char stream)
;         next-1 #(read-char stream nil    nil)]
;     (ret-val (read-js-number-1 peek-1 next-1 :junk-allowed? junk-allowed?))))

; (def stream (atom nil))

; ;defn/defs lex-js [stream & {:keys [include-comments?]}]

; (def include-comments?   (atom nil))
; (def expression-allowed? (atom true))
; (def newline-before?     (atom false))
; (def line                1)
; (def char-n              (atom 0))
; (def position            (atom 0))
; (def comments-before?    false)

; (defn start-token []
;   (reset! *line*     line)
;   (reset! *char*     char)
;   (reset! *position* position))
; (defn token [type value]
;   (reset! expression-allowed?
;         (or (and (= type :operator)
;                  (not (in? value #{"++" "--"})))
;             (and (= type :keyword)
;                  (in? value *keywords-before-expression*))
;             (and (= type :punc)
;                  (in? value #{(char "[") (char "{") (char "(") \, \. \; \:}))))
;   (let [temp (map->Token
;                {:type type :value value :line *line* :char *char* :pos *position*
;                 :newline-before? @newline-before?
;                 :comments-before? (reverse @comments-before?)})]
;     (reset! newline-before? nil)
;     (reset! comments-before? nil)
;     temp))

; (defn peek- []
;   (peek-char @stream))
; (defn next- [& [eof-error in-string]]
;   (let [ch (read-char @stream eof-error)]
;     (when ch
;       (swap! position inc)
;       (if (str/line-terminator? ch)
;           (do
;             (reset! line (inc line))
;             (reset! char-n 0)
;             (when-not in-string (reset! newline-before? true)))
;           (swap! char-n inc)))
;     ch))

; (defn skip-whitespace []
;   (loop [ch (peek-)]
;     (when (str/whitespace? ch)
;       (next-)
;       (recur (peek-)))))

; (defn read-while [pred]
;   (loop [ch (peek-)]
;     (when (pred ch)
;       (prs (next-))
;       (recur (peek-)))))

; (defn read-num [& [start]]
;   (let [num (or (read-js-number-1  (fn [] (if start start (peek-)))
;                                    (fn [] (if start (reset! start nil) (next-)))
;                                    :junk-allowed? true)
;                 (js-parse-error "Invalid syntax."))]
;     (token :num num)))

; (defn handle-dot []
;   (next-)
;   (if (digit-char? (peek-))
;       (read-num \.)
;       (token :punc \.)))

; (defn hex-bytes [n char]
;   (let [n (volatile! 0)]
;     (doseq [pos (range (dec @n) 0)]
;       (let [digit (digit-char? (next- true) 16)]
;              (if digit
;                  (vswap! n + (* digit (num/exp 16 pos)))
;                  (js-parse-error "Invalid \\~a escape pattern." char))))
;     @n))
; (defn read-escaped-char
;   {:todo "For performance reasons, use hash-map to avoid 12n performance."}
;   [& [in-string]]
;   (let [ch (next- true in-string)]
;     (case ch
;       \n \newline
;       \r \return
;       \t \tab
;       \b \backspace
;       \v (char 11)
;       \f (char 12)
;       \0 (char 0)
;       \x (char (hex-bytes 2 \x))
;       \u (char (hex-bytes 4 \u))
;       \newline nil
;       :else
;         (let [num (atom (digit-char? ch 8))]
;            (if @num
;                (loop [nx (digit-char? (peek-) 8)]
;                   (if (or (not nx) (>= @num 32))
;                       (char @num)
;                       (do (next-)
;                           (reset! num (+ nx (* @num 8)))
;                           (recur (digit-char? (peek-) 8)))))
;                ch)))))
; (defn read-str []
;   (let [str-quote (next-)]
;     (try
;       (token :string
;              (loop []
;                (let [ch (next- true)]
;                  (cond (= ch \\)
;                          (let [ch (read-escaped-char true)]
;                            (when ch (prs ch))
;                            (recur))
;                        (str/line-terminator? ch)
;                          (js-parse-error "Line terminator inside of string.")
;                        (= ch str-quote)
;                          nil
;                        :else
;                          (do (prs ch) (recur))))))
;       (catch EOFException _ (js-parse-error "Unterminated string constant.")))))

; (defn add-comment [type c]
;   (when @include-comments?
;     ;; doing this instead of calling (token) as we don't want
;     ;; to put comments-before into a comment token
;     ; was originally "(push (Token. ...) comments-before?)"
;     (map->Token
;       {:type             type
;        :value            c
;        :line             *line*
;        :char             *char*
;        :pos              *position*
;        :newline-before?  @newline-before?
;        :comments-before? @comments-before?})))

; (defn read-line-comment []
;   (next-)
;   (if include-comments?
;       (add-comment :comment1
;                    (let [sb (StringBuilder.)]
;                      (loop [ch (next-)]
;                        (if (or (str/line-terminator? ch) (not ch))
;                            (.toString sb)
;                            (do (.append sb ch)
;                                (recur (next-)))))))
;       (loop [ch (next-)]
;         (when-not (or (str/line-terminator? ch) (not ch))
;           (recur (next-))))))

; (defn read-multiline-comment []
;   (next-)
;   (let [unterm-fn #(or (next-) (js-parse-error "Unterminated comment."))
;         letted-fn
;           #(let [sb   (StringBuilder.)
;                  star (atom nil)]
;              (loop [ch (unterm-fn)]
;                (if (and @star (= ch \/))
;                    (.toString sb)
;                    (do (reset! star (= ch \*))
;                        (.append sb ch)
;                        (recur (unterm-fn))))))]
;     (if include-comments?
;         (add-comment :comment2
;                      (letted-fn))
;         (letted-fn))))

; (defn read-regexp []
;   (try
;     (token :regexp
;            (cons
;              (let [backslash (atom nil)
;                    inset     (atom nil)]
;                (loop [ch (next- true)]
;                  (when-not (and (not @backslash) (not @inset) (= ch \/))
;                    (when-not @backslash
;                      (when (= ch (char "["))
;                        (reset! inset true))
;                      (when (and @inset
;                                 (not @backslash)
;                                 (= ch (char "]")))
;                        (reset! inset nil)))
;                    (reset! backslash (and (= ch \\) (not @backslash)))
;                    ;; Handle \u sequences, since CL-PPCRE does not understand them.
;                    ; (if (and @backslash (= (peek-) \u))
;                    ;     (let [code (do (reset! backslash nil)
;                    ;                    (next-)
;                    ;                    (hex-bytes 4 \u))
;                    ;           ch   (char code)]
;                    ;       ;; on CCL, parsing /\uFFFF/ fails because (code-char #xFFFF) returns NIL.
;                    ;       ;; so when NIL, we better use the original sequence.
;                    ;       (if ch
;                    ;           (prs ch)
;                    ;           (format true "\\u~4,'0X" code)))
;                    ;     (prs ch))
;                    (recur (next- true)))))
;             (read-while identifier-char?)))
;     (catch EOFException _ (js-parse-error "Unterminated regular expression."))))

; (defn read-operator [& [start]]
;   (let [grow
;          (fn [s]
;            (let [bigger (str s (peek-))]
;              (if (get *operators* bigger)
;                  (do (next-) (recur bigger))
;                  (token :operator (get *operators* s)))))]
;     (grow (or start (string (next-))))))

; (defn handle-slash []
;   (next-)
;   (case (peek-)
;     \/ (do (read-line-comment)
;            (next-token))
;     \* (do (read-multiline-comment)
;            (next-token))
;     :else (if @expression-allowed?
;               (read-regexp)
;               (read-operator "/"))))

; (defn read-word []
;   (let [unicode-escape nil
;         word (with-output-to-string (*standard-output*)
;                (loop :for ch := (peek-) :do
;                  (cond (= ch \\)
;                          (do (next-)
;                              (when-not (= (next-) \u) (js-parse-error "Unrecognized escape in identifier."))
;                              (write-char (code-char (hex-bytes 4 \u)))
;                              (reset! unicode-escape true))
;                        ((and ch (identifier-char? ch)) (write-char (next-)))
;                        :else (return))))
;         kw (and (not unicode-escape) (get *keywords* word))]
;     (cond (and *check-for-reserved-words* (not unicode-escape)
;                 (get (condp = *ecma-version*
;                        3 *reserved-words-ecma-3*
;                        5 *reserved-words-ecma-5*) word))
;             (js-parse-error "'~a' is a reserved word." word)
;           (not kw)
;             (token :name word)
;           (get *operators* word)
;             (token :operator kw)
;           (in? kw *atom-keywords*)
;             (token :atom kw)
;           :else (token :keyword kw))))

; (defn next-token [& [force-regexp?]]
;   (if force-regexp?
;       (read-regexp)
;       (do
;         (skip-whitespace)
;         (start-token)
;         (let [next-char (peek-)]
;           (cond (not next-char)
;                   (token :eof "EOF")
;                 (digit-char? next-char)
;                   (read-num)
;                 (in? next-char "'\"")
;                   (read-str)
;                 (= next-char \.)
;                   (handle-dot)
;                 (in? next-char "[]{}(),;:")
;                   (token :punc (next-))
;                 (= next-char \/)
;                   (handle-slash)
;                 (in? next-char *operator-chars*)
;                   (read-operator)
;                 (or (identifier-char? next-char) (= next-char \\))
;                   (read-word)
;                 :else (js-parse-error "Unexpected character '~a'." next-char))))))

; (defn lex-js [stream-n & {:as opts}]
;   (reset! include-comments? (:include-comments? opts))
;   (reset! stream stream-n)
;   next-token)
