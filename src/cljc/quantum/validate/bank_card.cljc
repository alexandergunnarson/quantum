(ns quantum.validate.bank-card)

; (def NONE       0)
; (def AMEX       (<< 1 0))
; (def VISA       (<< 1 1))
; (def MASTERCARD (<< 1 2))
; (def DISCOVER   (<< 1 3))
; (def DINERS     (<< 1 4))
; (def VPAY       (<< 1 5))
; (def card-types (atom []))

; (def LUHN_VALIDATOR nil #_(LuhnCheckDigit/LUHN_CHECK_DIGIT)) ; TODO add LuhnCheckDigit ns

; (def AMEX_VALIDATOR
;   (CodeValidator. "^(3[47]\\\\d{13})$" LUHN_VALIDATOR))

; (def DINERS_VALIDATOR
;   (CodeValidator.
;     "^(30[0-5]\\\\d{11}|3095\\\\d{10}|36\\\\d{12}|3[8-9]\\\\d{12})$"
;     LUHN_VALIDATOR))

; (def DISCOVER_REGEX
;   (RegexValidator.
;     ["^(6011\\\\d{12})$" "^(64[4-9]\\\\d{13})$" "^(65\\\\d{14})$"]))

; (def DISCOVER_VALIDATOR
;   (CodeValidator. DISCOVER_REGEX LUHN_VALIDATOR))

; (def MASTERCARD_VALIDATOR
;   (CodeValidator. "^(5[1-5]\\\\d{14})$" LUHN_VALIDATOR))

; (def VISA_VALIDATOR
;   (CodeValidator. "^(4)(\\\\d{12}|\\\\d{15})$" LUHN_VALIDATOR))

; (def VPAY_VALIDATOR
;   (CodeValidator. "^(4)(\\\\d{12,18})$" LUHN_VALIDATOR))

; (defn CreditCardValidator
;  "Create a new CreditCardValidator with default options."
;  []
;  (CreditCardValidator (str AMEX VISA MASTERCARD DISCOVER)))

; (defn CreditCardValidator
;  "Create a new CreditCardValidator with the specified options.
;   @param options Pass in CreditCardValidator.VISA + CreditCardValidator.AMEX to specify that
;   those are the only valid card types."
;  [^long options]
;  (super)
;  (when (isOn options VISA) (.add (.cardTypes this) VISA_VALIDATOR))
;  (when (isOn options VPAY) (.add (.cardTypes this) VPAY_VALIDATOR))
;  (when (isOn options AMEX) (.add (.cardTypes this) AMEX_VALIDATOR))
;  (when
;   (isOn options MASTERCARD)
;   (.add (.cardTypes this) MASTERCARD_VALIDATOR))
;  (when
;   (isOn options DISCOVER)
;   (.add (.cardTypes this) DISCOVER_VALIDATOR))
;  (when
;   (isOn options DINERS)
;   (.add (.cardTypes this) DINERS_VALIDATOR)))

; (defn
;   CreditCardValidator
;   "Create a new CreditCardValidator with the specified {@link CodeValidator}s.
;    @param creditCardValidators Set of valid code validators"
;   [^CodeValidator[] creditCardValidators]
;  (when
;   (= creditCardValidators nil)
;   (throw (IllegalArgumentException. "Card validators are missing")))
;  (.addAll Collections cardTypes creditCardValidators))

; (defn isValid
;  "Checks if the field is a valid credit card number.
;   @param card The card number to validate.
;   @return Whether the card number is valid."
;  [^String card]
;  (when (or (= card nil) (= (.length card) 0)) (return false))
;  (doseq
;   [^CodeValidator cardType cardTypes]
;   (when (.isValid cardType card) (return true)))
;  false)

; (defn validate
;  "Checks if the field is a valid credit card number.
;   @param card The card number to validate.
;   @return The card number if valid or nil if invalid."
;   [^String card]
;   (when (or (= card nil) (= (.length card) 0)) (return nil))
;    (let [^Object result nil]
;      (doseq
;       [^CodeValidator cardType cardTypes]
;       (swap! result (.validate cardType card))
;       (when (not= result nil) (return result)))
;      nil))
 