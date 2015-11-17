(ns quantum.validate.regex
  (:require-quantum [:lib])
  (:import java.util.regex.Matcher
           java.util.regex.Pattern
           java.net.IDN
           java.util.Locale))

; /**
;  * Construct a validator that matches any one of the set of regular
;  * expressions with the specified case sensitivity.
;  *
;  * @param regexs The set of regular expressions this validator will
;  * validate against
;  * @param caseSensitive when <code>true</code> matching is <i>case
;  * sensitive</i>, otherwise matching is <i>case in-sensitive</i>
;  */
#?(:clj
(defn validate
  "In:  Collection of strings
   Out: PersistentVector<Pattern>"
  {:contributors ["org.apache.commons.validator.routines.RegexValidator"]
   :todo         ["Port to CLJS"]}
  ([regexs] (validate regexs true))
  ([regexs-0 case-sensitive?]
    (let [regexs (if (string? regexs-0)
                     [regexs-0]
                     regexs-0)]
      (if (empty? regexs)
          (throw (IllegalArgumentException. "Regular expressions coll can't be empty"))
          (let [flags (if case-sensitive? 0 Pattern/CASE_INSENSITIVE)]
            (fori [regex regexs i]
              (if (empty? regex)
                  (throw (IllegalArgumentException. (str "Regular expression[" i "] is missing")))
                  (Pattern/compile regex flags)))))))))

#?(:clj
(defn valid?
  {:contributors ["org.apache.commons.validator.routines.RegexValidator"]
   :tests '#{(valid? "ab-DE-1"
              (validate ["^([abc]*)(?:\\-)([DEF]*)(?:\\-)([123]*)$"] true))}}
  [s patterns]
  (and (string? s)
       (ffilter (fn-> ^Matcher (re-matcher s) (.matches)) patterns))))

#?(:clj
(defn match
  "Validate a value against the set of regular expressions,
   returning the array of matched groups."
  {:contributors ["org.apache.commons.validator.routines.RegexValidator"]}
  [s patterns]
  (and (string? s)
       (seq-loop [pattern patterns
                  ret     nil]
         (let [^Matcher matcher (re-matcher pattern s)]
           (when (.matches matcher)
             (let [ct (.groupCount matcher)]
               (break (for [j (range ct)]
                        (.group matcher (core/int (inc j))))))))))))


; /**
;  * Validate a value against the set of regular expressions
;  * returning a String value of the aggregated groups.
;  *
;  * @param value The value to validate.
;  * @return Aggregated String value comprised of the
;  * <i>groups</i> matched if valid or <code>null</code> if invalid
;  */
; public String validate(String value) {
;     if (value == null) {
;         return null;
;     }
;     for (int i = 0; i < patterns.length; i++) {
;         Matcher matcher = patterns[i].matcher(value);
;         if (matcher.matches()) {
;             int count = matcher.groupCount();
;             if (count == 1) {
;                 return matcher.group(1);
;             }
;             StringBuffer buffer = new StringBuffer();
;             for (int j = 0; j < count; j++) {
;                 String component = matcher.group(j+1);
;                 if (component != null) {
;                     buffer.append(component);
;                 }
;             }
;             return buffer.toString();
;         }
;     }
;     return null;
; }
