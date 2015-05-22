(ns quantum.parse)

(require
  '[quantum.core.ns :as ns :refer [defalias alias-ns]]
  '[quantum.tokenize :as t])
(ns/require-all *ns* :clj :lib)

; (def *unary-prefix*  #{:typeof :void :delete :-- :++ :! (keyword "~") :- :+})
; (def *unary-postfix* #{:-- :++})
; (def *assignment*
;   #{:+= :-= (keyword "/=") :*= :%= :>>= :<<= :>>>= :|= (keyword "^=") :&=})

; (def *precedence*
;   {1  #{:||}
;    2  #{:&&}
;    3  #{:|}
;    4  #{(keyword "^")}
;    5  #{:&}
;    6  #{:== :=== :!= :!==}
;    7  #{:< :> :<= :>= :in :instanceof}
;    8  #{:>> :<< :>>>}
;    9  #{:+ :-}
;    10 #{:* :/ :%}})

; (def *in-function* nil)
; (def *label-scope* nil)
; (defmacro with-label-scope [type label & body]
;   `(let [~'*label-scope* (cons (cons ~type ~label) *label-scope*)]
;      ~@body))

; (declare parse-js*)
; (def stream (volatile! nil))

; (defn parse-js [^String input & {:keys [strict-semicolons? ecma-version reserved-words] :or {ecma-version 3}}]
;   (assert (splice-or ecma-version = [3 5]))
;   (let [*ecma-version*             ecma-version
;         *check-for-reserved-words* reserved-words
;         *line*                     0
;         *char*                     0
;         *position*                 0]
;     (if (string? input)
;         (with-open [in (StringReader. input)]
;           (parse-js* in strict-semicolons?))
;         (parse-js* input strict-semicolons?))))

; (defn input [] (whenf stream (fn-not fn?) lex-js))
; (def  token  (atom (input)))
; (def  peeked nil)

; (defn peek []
;   (or peeked (reset! peeked (input))))
; (defn next- []
;   (if peeked
;       (do (reset! token peeked)
;           (reset! peeked nil))
;       (reset! token (input)))
;   token)
; (defn skip [n]
;   (dotimes (i n) (next-)))

; (defn token-error [token control & args]
;   (let [*line* (token-line token) *char* (token-char token)]
;     (apply js-parse-error control args)))
; (defn error* [control & args]
;   (apply token-error token control args))
; (defn unexpected [token]
;   (token-error token "Unexpected token '~a'." (token-id token)))

; (defn expect-token [type val]
;   (if (token? token type val)
;       (next-)
;       (error* "Unexpected token '~a', expected '~a'." (token-id token) val)))
; (defn expect [punc]
;   (expect-token :punc punc))
; (defn expect-key [kw]
;   (expect-token :keyword kw))
; (defn can-insert-semicolon []
;   (and (not strict-semicolons)
;        (or (token-newline-before token)
;            (:type? token :eof)
;            (token? token :punc (char "}")))))
; (defn semicolonp [] (token? token :punc \;))
; (defn semicolon []
;   (cond ((semicolonp) (next-))
;         ((not (can-insert-semicolon)) (unexpected token))))

; (defn as [type & args]
;   (cons type args))

; (defn parenthesised []
;   (expect (char "(")) (do-with (expression) (expect (char ")"))))

; (def statement (&optional label)
;   ;; if expecting a statement and found a slash as operator,
;   ;; it must be a literal regexp.
;   (when (and (eq (:type token) :operator)
;              (eq (:value token) :/))
;     (setf peeked nil
;           token (input true)))
;   (condpc = (:type token)
;     (coll-or :num :string :regexp :operator :atom)
;       (simple-statement)
;     :name (if (token? (peek) :punc \:)
;               (let [label (do-with (:value token) (skip 2))]
;                 (as :label label (with-label-scope :label label (statement label))))
;               (simple-statement))
;     :punc (condpc = (:value token)
;             (char "{") 
;               (do (next-) (block*))
;             (coll-or (char "[") (char "("))
;               (simple-statement)
;             \;
;               (do (next-) (as :block '()))
;             :else
;               (unexpected token))
;     (:keyword
;      (case (let [token-val (:value token)] (next-) token-val)
;        :break    (break/cont :break)
;        :continue (break/cont :continue)
;        :debugger (do (semicolon) (as :debugger))
;        :do (let [body (with-label-scope :loop label (statement))]
;               (expect-key :while)
;               (as :do (parenthesised) body))
;        :for (for* label)
;        :function (function* true)
;        :if (if*)
;        :return (do (when-not *in-function* (error* "'return' outside of function."))
;                     (as :return
;                         (cond ((semicolonp) (next-) nil)
;                               ((can-insert-semicolon) nil)
;                               (t (do-with (expression) (semicolon))))))
;        (:switch (let ((val (parenthesised))
;                       (cases nil))
;                   (with-label-scope :switch label
;                     (expect #\{)
;                     (loop :until (token? token :punc #\}) :do
;                        (case (:value token)
;                          (:case (next-)
;                            (push (cons (do-with (expression) (expect #\:)) nil) cases))
;                          (:default (next-) (expect #\:) (push (cons nil nil) cases))
;                          (t (unless cases (unexpected token))
;                             (push (statement) (rest (first cases))))))
;                     (next-)
;                     (as :switch val (loop :for case :in (nreverse cases) :collect
;                                        (cons (first case) (nreverse (rest case))))))))
;        (:throw (let ((ex (expression))) (semicolon) (as :throw ex)))
;        (:try (try*))
;        (:var (do-with (var*) (semicolon)))
;        (:while (as :while (parenthesised) (with-label-scope :loop label (statement))))
;        (:with (as :with (parenthesised) (statement)))
;        (t (unexpected token))))
;     (t (unexpected token))))

; (defn simple-statement []
;   (let [exp (expression)]
;     (semicolon)
;     (as :stat exp)))

; (defn break/cont [type]
;   (as type (cond ((or (and (semicolonp) (next-)) (can-insert-semicolon))
;                   (unless (loop :for (ltype) :in *label-scope* :do
;                              (when (or (eq ltype :loop) (and (eq type :break) (eq ltype :switch)))
;                                (return true)))
;                     (error* "'~a' not inside a loop or switch." type))
;                   nil)
;                  (:type? token :name)
;                    (let [name (:value token)]
;                      (ecase type
;                        (:break (unless (some (lambda (lb) (equal (rest lb) name)) *label-scope*)
;                                  (error* "Labeled 'break' without matching labeled statement.")))
;                        (:continue (unless (find (cons :loop name) *label-scope* :test #'equal)
;                                     (error* "Labeled 'continue' without matching labeled loop."))))
;                      (next-) (semicolon)
;                      name))))

; (defn block* []
;   (do-with (as :block (loop :until   (token? token :punc (char "}") )
;                           :collect (statement)))
;     (next-)))

; (defn for-in [label init lhs]
;   (let [obj (do (next-) (expression))]
;     (expect #\))
;     (as :for-in init lhs obj (with-label-scope :loop label (statement)))))

; (defn regular-for [label init]
;   (expect #\;)
;   (let ((test (do-with (unless (semicolonp) (expression)) (expect \;)))
;         (step (if (token? token :punc \)) nil (expression))))
;     (expect #\))
;     (as :for init test step (with-label-scope :loop label (statement)))))

; (defn for* [label]
;   (expect #\()
;   (cond ((semicolonp) (regular-for label nil))
;         ((token? token :keyword :var)
;          (let* [var- (do (next-) (var* true)]
;                 (defs (second var-)))
;            (if (and (not (rest defs)) (token? token :operator :in))
;                (for-in label var- (as :name (caar defs)))
;                (regular-for label var-))))
;         (t (let ((init (expression t true)))
;              (if (token? token :operator :in)
;                  (for-in label nil init)
;                  (regular-for label init))))))

; (defn function* [statement]
;   (with-defs
;     (def name (and (:type? token :name)
;                    (do-with (:value token) (next-))))
;     (when (and statement (not name)) (unexpected token))
;     (expect #\()
;     (def argnames (loop :for first := t :then nil
;                         :until (token? token :punc #\))
;                         :unless first :do (expect #\,)
;                         :unless (:type? token :name) :do (unexpected token)
;                         :collect (do-with (:value token) (next-))))
;     (next-)
;     (expect #\{)
;     (def body (let ((*in-function* true) (*label-scope* ()))
;                 (loop :until (token? token :punc #\}) :collect (statement))))
;     (next-)
;     (as (if statement :defun :function) name argnames body)))

; (defn if* []
;   (let ((condition (parenthesised))
;         (body (statement))
;         else)
;     (when (token? token :keyword :else)
;       (next-)
;       (setf else (statement)))
;     (as :if condition body else)))

; (def ensure-block ()
;   (expect #\{)
;   (block*))

; (def try* ()
;   (let ((body (ensure-block)) catch finally)
;     (when (token? token :keyword :catch)
;       (next-) (expect #\()
;       (unless (:type? token :name) (error* "Name expected."))
;       (let ((name (:value token)))
;         (next-) (expect #\))
;         (setf catch (cons name (ensure-block)))))
;     (when (token? token :keyword :finally)
;       (next-)
;       (setf finally (ensure-block)))
;     (as :try body catch finally)))

; (def vardefs (no-in)
;   (unless (:type? token :name) (unexpected token))
;   (let ((name (:value token)) val)
;     (next-)
;     (when (token? token :operator :=)
;       (next-) (setf val (expression nil no-in)))
;     (if (token? token :punc #\,)
;         (progn (next-) (cons (cons name val) (vardefs no-in)))
;         (list (cons name val)))))

; (defn var* [& [no-in]]
;   (as :var (vardefs no-in)))

; (defn new* []
;   (let ((newexp (expr-atom nil)))
;     (let [args nil]
;       (when (token? token :punc #\()
;         (next-) (setf args (expr-list #\))))
;       (subscripts (as :new newexp args) true))))

; (defn expr-atom [allow-calls]
;   (cond (token? token :operator :new) (do (next-) (new*))
;         (:type? token :punc)
;           (case (:value token)
;             (char "(") (do (next-) (subscripts (do-with (expression) (expect (char ")"))) allow-calls))
;             (char "[") (do (next-) (subscripts (array*) allow-calls))
;             (char "{") (do (next-) (subscripts (object*) allow-calls))
;             :else (unexpected token))
;         (token? token :keyword :function)
;          (do (next-)
;              (subscripts (function* nil) allow-calls))
;         (member (:type token) '(:atom :num :string :regexp :name))
;           (let [atom- (if (eq (:type token) :regexp)
;                           (as :regexp (first (:value token)) (rest (:value token)))
;                           (as (:type token) (:value token)))]
;             (subscripts (do-with atom- (next-)) allow-calls))
;         :else (unexpected token)))

; (defn expr-list [closing & [allow-trailing-comma allow-empty]]
;   (let [elts ()]
;     (loop :for first := t :then nil :until (token? token :punc closing) :do
;        (unless first (expect #\,))
;        (when (and allow-trailing-comma (token? token :punc closing)) (return))
;        (push (unless (and allow-empty (token? token :punc #\,)) (expression nil)) elts))
;     (next-)
;     (nreverse elts)))

; (defn array* []
;   (as :array (expr-list #\] t true)))

; (defn object* []
;   (as :object (loop :for first := t :then nil
;                     :until (token? token :punc #\})
;                     :unless first :do (expect #\,)
;                     :until (token? token :punc #\}) :collect
;                  (let ((name (as-property-name)))
;                    (cond ((token? token :punc #\:)
;                           (next-) (cons name (expression nil)))
;                          ((and (= *ecma-version* 5) (or (= name "get") (= name "set")))
;                           (let ((name1 (as-property-name))
;                                 (body (progn (unless (token? token :punc (char "(")) (unexpected token))
;                                              (function* nil))))
;                             (list* name1 (if (= name "get") :get :set) body)))
;                          (t (unexpected token))))
;                  :finally (next-))))

; (defn as-property-name []
;   (if (member (:type token) '(:num :string))
;       (do-with (:value token) (next-))
;       (as-name)))

; (defn as-name []
;   (case (:type token)
;     :name (do-with (:value token) (next-))
;     (:operator :keyword :atom) (do-with (string-downcase (symbol-name (:value token))) (next-))
;     :else (unexpected token)))

; (defn subscripts [expr allow-calls]
;   (cond ((token? token :punc #\.)
;          (next-)
;          (subscripts (as :dot expr (as-name)) allow-calls))
;         ((token? token :punc #\[)
;          (next-)
;          (let ((sub (expression)))
;            (expect #\])
;            (subscripts (as :sub expr sub) allow-calls)))
;         ((and (token? token :punc #\() allow-calls)
;          (next-)
;          (let ((args (expr-list #\))))
;            (subscripts (as :call expr args) true)))
;         (t expr)))

; (defn maybe-unary [allow-calls]
;   (if (and (:type? token :operator) (member (:value token) *unary-prefix*))
;       (as :unary-prefix (do-with (:value token) (next-)) (maybe-unary allow-calls))
;       (let ((val (expr-atom allow-calls)))
;         (loop :while (and (:type? token :operator)
;                           (member (:value token) *unary-postfix*)
;                           (not (token-newline-before token))) :do
;            (setf val (as :unary-postfix (:value token) val))
;            (next-))
;         val)))

; (defn expr-op [left min-prec no-in]
;   (let* ((op (and (:type? token :operator) (or (not no-in) (not (eq (:value token) :in)))
;                   (:value token)))
;          (prec (and op (gethash op *precedence*))))
;     (if (and prec (> prec min-prec))
;         (let [right (progn (next-) (expr-op (maybe-unary true) prec no-in))]
;           (expr-op (as :binary op left right) min-prec no-in))
;         left)))

; (defn expr-ops [no-in]
;   (expr-op (maybe-unary true) 0 no-in))

; (defn maybe-conditional (no-in)
;   (let [expr (expr-ops no-in)]
;     (if (token? token :operator :?)
;         (let [yes (do (next-) (expression nil))]
;           (expect \:)
;           (as :conditional expr yes (expression nil no-in)))
;         expr)))

; (defn maybe-assign [no-in]
;   (let [left (maybe-conditional no-in)]
;     (if (and (:type? token :operator) (gethash (:value token) *assignment*))
;         (as :assign (gethash (:value token) *assignment*) left (progn (next-) (maybe-assign no-in)))
;         left)))

; (defn expression [& [commas no-in]]
;   (let [commas (or commas true)
;         no-in  (or no-in  nil)
;         expr (maybe-assign no-in)]
;     (if (and commas (token? token :punc \,))
;         (as :seq expr (do (next-) (expression)))
;         expr)))

; (defn parse-js* [stream-n & [strict-semicolons?]]
;   (vreset! stream stream-n)
;   (as :toplevel (loop :until (:type? token :eof)
;                     :collect (statement))))

; (defn parse-js-string [& args]
;   (apply parse-js args))
