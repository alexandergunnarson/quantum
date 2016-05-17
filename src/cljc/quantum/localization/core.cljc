(ns quantum.localization.core
  (:require [quantum.core.string      :as str     ]
            [quantum.core.collections :as coll
              :refer [#?@(:clj [containsv? join])
                      dropr filter+ remove+] ]
            [quantum.core.fn          :as fn
              :refer [#?@(:clj [fn->])]           ]
            [quantum.core.logic       :as logic
              :refer [#?@(:clj [fn-or])]          ]
            [quantum.net.http         :as http    ]))

#?(:clj
(defn iana-languages
  "The results of the IANA language, region, etc. registry."
  {:out "Vec<Map>"}
  []
  (let [raw-text
          (->> (http/request!
                 {:url "http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry"})
               :body)
        sc (java.util.Scanner. ^String raw-text)]
    (.nextLine sc) ; "File-Date: ___"
    (.nextLine sc) ; "%%"
    (loop [coll [] entry {}]
      (if-let [k-0 (.findInLine sc "[A-Za-z\\-]+: ")]
        (let [_ (.useDelimiter sc #"\n(?!  )")
              k-f (-> (dropr 2 k-0) str/keywordize)
              v-0 (.next sc)
              v   (if (containsv? v-0 \n)
                      (str/remove v-0 #"\n ")
                      v-0)
              v-f (if (= k-f :type)
                      (str/keywordize v)
                      v)]
          (.skip sc #"\n")
          (.useDelimiter sc #"\\s+")
          (recur coll (coll/assoc-with entry
                        (fn [a b] (-> a (coll/ensurec []) (conj b)))
                        k-f v-f))) ; Because there can be more than one description
        (if (.hasNext sc)
            (do (.nextLine sc)
                (recur (conj coll entry) {}))
            coll))))))

#?(:clj
(defn iana-all []
  (->> (iana-languages)
       (remove+ (fn-or :deprecated
                       (fn-> :type (= :redundant))))
       (join []))))

#?(:clj
(defn iana-regions []
  (->> (iana-languages)
       (remove+ (fn-or :deprecated
                       (fn-> :type (= :redundant))))
       (filter+ (fn-> :type (= :region)))
       (remove+ (fn-> :subtag "AA"))) ; because "Private use"
       (join [])))





; From Apache Commons Convert
; Most Java applications will require data type conversion,
; and typically those conversions are hard-coded in the application on an as-needed basis. As an application grows, so do the number of hard-coded conversions. In time, you end up with duplicate code or duplicate conversions that behave differently depending on where they appear in code. Things get worse in enterprise-class applications where data is being exchanged between dissimilar systems and data type conversion gets really complicated.

; https://en.wikipedia.org/wiki/Common_Locale_Data_Repository ; For localization
; A Locale object represents a specific geographical, political, or cultural region.
; Displaying a number is a locale-sensitive operation—
; the number should be formatted according to the customs and conventions of the user's native country, region, or culture.
; The Locale class implements identifiers interchangeable with BCP 47
; ( , "Tags for Identifying Languages"),
; with support for the LDML (UTS#35, "Unicode Locale Data Markup Language")
; BCP 47-compatible extensions for locale data exchange.

; A Locale object logically consists of the fields described below.

; language
; When a language has both an alpha-2 code and an alpha-3 code, the alpha-2 code must be used.
; Full list of valid language codes in the IANA Language Subtag Registry (including ISO 639-1, ISO 639-2, ISO 639-3)
; (search for "Type: language").

; Well-formed language values have the form [a-zA-Z]{2,8}.
; Note that this is not the the full BCP47 language production, since it excludes extlang.
; They are not needed since modern three-letter language codes replace them.

; Example: "en" (English), "ja" (Japanese), "kok" (Konkani)

; script
; ISO 15924 alpha-4 script code.
; You can find a full list of valid script codes in the IANA Language Subtag Registry (search for "Type: script").
; The script field is case insensitive, but Locale always canonicalizes to title case (the first letter is upper case and the rest of the letters are lower case).

; Well-formed script values have the form [a-zA-Z]{4}

; Example: "Latn" (Latin), "Cyrl" (Cyrillic)

; country (region)
; ISO 3166 alpha-2 country code or UN M.49 numeric-3 area code.
; You can find a full list of valid country and region codes in the IANA Language Subtag Registry (search for "Type: region").
; The country (region) field is case insensitive, but Locale always canonicalizes to upper case.

; Well-formed country/region values have the form [a-zA-Z]{2} | [0-9]{3}

; Example: "US" (United States), "FR" (France), "029" (Caribbean)

; variant
; Any arbitrary value used to indicate a variation of a Locale.
; Where there are two or more variant values each indicating its own semantics,
; these values should be ordered by importance, with most important first, separated by underscore('_').
; The variant field is case sensitive.

; Note: IETF BCP 47 places syntactic restrictions on variant subtags.
; Also BCP 47 subtags are strictly used to indicate additional variations that define a language or its dialects that are not covered by any combinations of language, script and region subtags. You can find a full list of valid variant codes in the IANA Language Subtag Registry (search for "Type: variant").
; However, the variant field in Locale has historically been used for any kind of variation, not just language variations. For example, some supported variants available in Java SE Runtime Environments indicate alternative cultural behaviors such as calendar type or number script. In BCP 47 this kind of information, which does not identify the language, is supported by extension subtags or private use subtags.


; Well-formed variant values have the form SUBTAG (('_'|'-') SUBTAG)* where SUBTAG = [0-9][0-9a-zA-Z]{3} | [0-9a-zA-Z]{5,8}.
; (Note: BCP 47 only uses hyphen ('-') as a delimiter, this is more lenient).

; Example: "polyton" (Polytonic Greek), "POSIX"

; extensions
; A map from single character keys to string values, indicating extensions apart from language identification. The extensions in Locale implement the semantics and syntax of BCP 47 extension subtags and private use subtags. The extensions are case insensitive, but Locale canonicalizes all extension keys and values to lower case. Note that extensions cannot have empty values.

; Well-formed keys are single characters from the set [0-9a-zA-Z]. Well-formed values have the form SUBTAG ('-' SUBTAG)* where for the key 'x' SUBTAG = [0-9a-zA-Z]{1,8} and for other keys SUBTAG = [0-9a-zA-Z]{2,8} (that is, 'x' allows single-character subtags).

; Example: key="u"/value="ca-japanese" (Japanese Calendar), key="x"/value="java-1-7"
; Note: Although BCP 47 requires field values to be registered in the IANA Language Subtag Registry, the Locale class does not provide any validation features. The Builder only checks if an individual field satisfies the syntactic requirement (is well-formed), but does not validate the value itself. See Locale.Builder for details.
; Unicode locale/language extension

; UTS#35, "Unicode Locale Data Markup Language" defines optional attributes and keywords to override or refine the default behavior associated with a locale. A keyword is represented by a pair of key and type. For example, "nu-thai" indicates that Thai local digits (value:"thai") should be used for formatting numbers (key:"nu").

; The keywords are mapped to a BCP 47 extension value using the extension key 'u' (UNICODE_LOCALE_EXTENSION). The above example, "nu-thai", becomes the extension "u-nu-thai".code

; Thus, when a Locale object contains Unicode locale attributes and keywords, getExtension(UNICODE_LOCALE_EXTENSION) will return a String representing this information, for example, "nu-thai". The Locale class also provides getUnicodeLocaleAttributes(), getUnicodeLocaleKeys(), and getUnicodeLocaleType(java.lang.String) which allow you to access Unicode locale attributes and key/type pairs directly. When represented as a string, the Unicode Locale Extension lists attributes alphabetically, followed by key/type sequences with keys listed alphabetically (the order of subtags comprising a key's type is fixed when the type is defined)

; A well-formed locale key has the form [0-9a-zA-Z]{2}. A well-formed locale type has the form "" | [0-9a-zA-Z]{3,8} ('-' [0-9a-zA-Z]{3,8})* (it can be empty, or a series of subtags 3-8 alphanums in length). A well-formed locale attribute has the form [0-9a-zA-Z]{3,8} (it is a single subtag with the same form as a locale type subtag).

; The Unicode locale extension specifies optional behavior in locale-sensitive services. Although the LDML specification defines various keys and values, actual locale-sensitive service implementations in a Java Runtime Environment might not support any particular Unicode locale attributes or key/type pairs.

; Creating a Locale

; There are several different ways to create a Locale object.

; Builder

; Using Locale.Builder you can construct a Locale object that conforms to BCP 47 syntax.

; Constructors

; The Locale class provides three constructors:

;      Locale(String language)
;      Locale(String language, String country)
;      Locale(String language, String country, String variant)
 
; These constructors allow you to create a Locale object with language, country and variant, but you cannot specify script or extensions.
; Factory Methods

; The method forLanguageTag(java.lang.String) creates a Locale object for a well-formed BCP 47 language tag.

; Locale Constants

; The Locale class provides a number of convenient constants that you can use to create Locale objects for commonly used locales. For example, the following creates a Locale object for the United States:

;      Locale.US
 
; Use of Locale

; Once you've created a Locale you can query it for information about itself. Use getCountry to get the country (or region) code and getLanguage to get the language code. You can use getDisplayCountry to get the name of the country suitable for displaying to the user. Similarly, you can use getDisplayLanguage to get the name of the language suitable for displaying to the user. Interestingly, the getDisplayXXX methods are themselves locale-sensitive and have two versions: one that uses the default locale and one that uses the locale specified as an argument.

; The Java Platform provides a number of classes that perform locale-sensitive operations. For example, the NumberFormat class formats numbers, currency, and percentages in a locale-sensitive manner. Classes such as NumberFormat have several convenience methods for creating a default object of that type. For example, the NumberFormat class provides these three convenience methods for creating a default NumberFormat object:

;      NumberFormat.getInstance()
;      NumberFormat.getCurrencyInstance()
;      NumberFormat.getPercentInstance()
 
; Each of these methods has two variants; one with an explicit locale and one without; the latter uses the default locale:
;      NumberFormat.getInstance(myLocale)
;      NumberFormat.getCurrencyInstance(myLocale)
;      NumberFormat.getPercentInstance(myLocale)
 
; A Locale is the mechanism for identifying the kind of object (NumberFormat) that you would like to get. The locale is just a mechanism for identifying objects, not a container for the objects themselves.
; Compatibility

; In order to maintain compatibility with existing usage, Locale's constructors retain their behavior prior to the Java Runtime Environment version 1.7. The same is largely true for the toString method. Thus Locale objects can continue to be used as they were. In particular, clients who parse the output of toString into language, country, and variant fields can continue to do so (although this is strongly discouraged), although the variant field will have additional information in it if script or extensions are present.

; In addition, BCP 47 imposes syntax restrictions that are not imposed by Locale's constructors. This means that conversions between some Locales and BCP 47 language tags cannot be made without losing information. Thus toLanguageTag cannot represent the state of locales whose language, country, or variant do not conform to BCP 47.

; Because of these issues, it is recommended that clients migrate away from constructing non-conforming locales and use the forLanguageTag and Locale.Builder APIs instead. Clients desiring a string representation of the complete locale can then always rely on toLanguageTag for this purpose.

; Special cases

; For compatibility reasons, two non-conforming locales are treated as special cases.
; These are ja_JP_JP and th_TH_TH.
; These are ill-formed in BCP 47 since the variants are too short.
; To ease migration to BCP 47, these are treated specially during construction.
; These two cases (and only these) cause a constructor to generate an extension,
; all other values behave exactly as they did prior to Java 7.

; Java has used ja_JP_JP to represent Japanese as used in Japan together with the Japanese Imperial calendar. This is now representable using a Unicode locale extension, by specifying the Unicode locale key ca (for "calendar") and type japanese. When the Locale constructor is called with the arguments "ja", "JP", "JP", the extension "u-ca-japanese" is automatically added.

; Java has used th_TH_TH to represent Thai as used in Thailand together with Thai digits. This is also now representable using a Unicode locale extension, by specifying the Unicode locale key nu (for "number") and value thai. When the Locale constructor is called with the arguments "th", "TH", "TH", the extension "u-nu-thai" is automatically added.

; Legacy language codes

; Locale's constructor has always converted three language codes to their earlier, obsoleted forms: he maps to iw, yi maps to ji, and id maps to in. This continues to be the case, in order to not break backwards compatibility.

; The APIs added in 1.7 map between the old and new language codes, maintaining the old codes internal to Locale (so that getLanguage and toString reflect the old code), but using the new codes in the BCP 47 language tag APIs (so that toLanguageTag reflects the new one). This preserves the equivalence between Locales no matter which code or API is used to construct them. Java's default resource bundle lookup mechanism also implements this mapping, so that resources can be named using either convention, see ResourceBundle.Control.

; Three-letter language/country(region) codes

; The Locale constructors have always specified that the language and the country param be two characters in length, although in practice they have accepted any length. The specification has now been relaxed to allow language codes of two to eight characters and country (region) codes of two to three characters, and in particular, three-letter language codes and three-digit region codes as specified in the IANA Language Subtag Registry. For compatibility, the implementation still does not impose a length constraint.


; language (country, variant)

