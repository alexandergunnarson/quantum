(ns quantum.validate.isbn)
; (def serialVersionUID 4319515687976420405)
; (def SEP       "(?:\\\\-|\\\\s)")
; (def GROUP     "(\\\\d{1,5})")
; (def PUBLISHER "(\\\\d{1,7})")
; (def TITLE     "(\\\\d{1,6})")
; (def ISBN10_REGEX
;   (str "^(?:(\\\\d{9}[0-9X])|(?:" GROUP SEP PUBLISHER
;        SEP TITLE SEP "([0-9X])))$"))
; (def ISBN13_REGEX
;   (str "^(978|979)(?:(\\\\d{10})|(?:" SEP GROUP SEP PUBLISHER
;        SEP TITLE SEP "([0-9])))$"))
; (def ^:private ^ISBNValidator ISBN_VALIDATOR (ISBNValidator.))
; (def
;  ^:private
;  ^ISBNValidator
;  ISBN_VALIDATOR_NO_CONVERT
;  (ISBNValidator. false))
; (def
;  ^:private
;  ^CodeValidator
;  isbn10Validator
;  (CodeValidator.
;   ISBN10_REGEX
;   10
;   (.ISBN10_CHECK_DIGIT ISBN10CheckDigit)))
; (def
;  ^:private
;  ^CodeValidator
;  isbn13Validator
;  (CodeValidator.
;   ISBN13_REGEX
;   13
;   (.EAN13_CHECK_DIGIT EAN13CheckDigit)))
; (def ^:private ^boolean convert nil)
; (defn getInstance
;  "/*\n\nReturn a singleton instance of the ISBN validator which\nconverts ISBN-10 codes to ISBN-13.\n\n@return A singleton instance of the ISBN validator.\n     */\n"
;  []
;  ISBN_VALIDATOR)
; (defn getInstance
;  "/*\n\nReturn a singleton instance of the ISBN validator specifying\nwhether ISBN-10 codes should be converted to ISBN-13.\n\n@param convert <code>true</code> if valid ISBN-10 codes\nshould be converted to ISBN-13 codes or <code>false</code>\nif valid ISBN-10 codes should be returned unchanged.\n@return A singleton instance of the ISBN validator.\n     */\n"
;  [^boolean convert]
;  (if convert ISBN_VALIDATOR ISBN_VALIDATOR_NO_CONVERT))

; (defn ^:constructor ISBNValidator
;  "/*\n\nConstruct an ISBN validator which converts ISBN-10 codes\nto ISBN-13.\n     */\n"
;  []
;  ["EXPLICIT CONSTRUCTOR" "this(true);"])
; (defn ^:constructor ISBNValidator
;  "/*\n\nConstruct an ISBN validator indicating whether\nISBN-10 codes should be converted to ISBN-13.\n\n@param convert <code>true</code> if valid ISBN-10 codes\nshould be converted to ISBN-13 codes or <code>false</code>\nif valid ISBN-10 codes should be returned unchanged.\n     */\n"
;  [^boolean convert]
;  (swap! (.convert this) convert))
; (defn isValid
;  "/*\n\nCheck the code is either a valid ISBN-10 or ISBN-13 code.\n\n@param code The code to validate.\n@return <code>true</code> if a valid ISBN-10 or\nISBN-13 code, otherwise <code>false</code>.\n     */\n"
;  [^String code]
;  (or (isValidISBN13 code) (isValidISBN10 code)))
; (defn isValidISBN10
;  "/*\n\nCheck the code is a valid ISBN-10 code.\n\n@param code The code to validate.\n@return <code>true</code> if a valid ISBN-10\ncode, otherwise <code>false</code>.\n     */\n"
;  [^String code]
;  (.isValid isbn10Validator code))

; (defn isValidISBN13
;  "/*\n\nCheck the code is a valid ISBN-13 code.\n\n@param code The code to validate.\n@return <code>true</code> if a valid ISBN-13\ncode, otherwise <code>false</code>.\n     */\n"
;  [^String code]
;  (.isValid isbn13Validator code))

; (defn validate
;   "Check the code is either a valid ISBN-10 or ISBN-13 code.
;    If valid, this method returns the ISBN code with formatting characters removed
;    (i.e. space or hyphen).
;    Converts an ISBN-10 codes to ISBN-13 if |convertToISBN13| is true
;    @param code The code to validate.
;    @return A valid ISBN code if valid, otherwise nil"
;   [^String code]
;   (let [^String result (validateISBN13 code)]
;     (when (= result nil)
;       (swap! result (validateISBN10 code))
;       (when (and (not= result nil) convert)
;         (swap! result (convertToISBN13 result))))
;     result))

; (defn validateISBN10
;  "Check the code is a valid ISBN-10 code
;   If valid, this method returns the ISBN-10 code with formatting characters removed
;   (i.e. space or hyphen).
;   @param code The code to validate
;   @return A valid ISBN-10 code if valid, otherwise nil."
;  [^String code]
;  (let [^Object result (.validate isbn10Validator code)]
;    (if (= result nil) nil (.toString result))))

; (defn validateISBN13
;  "Check the code is a valid ISBN-13 code.
;   If valid, this method returns the ISBN-13 code with formatting characters removed
;   (i.e. space or hyphen).
;   @param code The code to validate.
;   @return A valid ISBN-13 code if valid, otherwise nil."
;  [^String code]
;  (let [^Object result (.validate isbn13Validator code)]
;    (if (= result nil) nil (.toString result))))

; (defn ->ISBN13
;   "Convert an ISBN-10 code to an ISBN-13 code.
;    This method requires a valid ISBN-10 with NO formatting characters.
;    @param isbn10 The ISBN-10 code to convert
;    @return A converted ISBN-13 code or |nil| if the ISBN-10 code is not valid"
;   [^String isbn10]
;   (when (nnil? isbn10)
;     (let [^String input (.trim isbn10)]
;       (when (not= (.length input) 10)
;         (throw
;           (IllegalArgumentException.
;             (str "Invalid length " (.length input) " for '" input "'"))))
;       (let [^String isbn13 (str "978" (.substring input 0 9))]
;         (try
;           (let [^String
;                   checkDigit
;                   (.calculate (.getCheckDigit isbn13Validator) isbn13)])
;             (swap! isbn13 + checkDigit)
;             isbn13
;           (catch CheckDigitException e
;             (throw
;               (IllegalArgumentException.
;                 (str "Check digit error for '" input "' - " (.getMessage e)))))))))) 