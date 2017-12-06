(ns quantum.location.core
  (:require
    [quantum.core.convert       :as conv]
    [quantum.core.error         :as err
      :refer [err!]]
    [quantum.core.fn            :as fn
      :refer [fn->]]
    [quantum.core.spec          :as s
      :refer [validate]]
    [quantum.core.string.format :as strf]
    [quantum.core.time.core     :as time]
    [quantum.net.http           :as http]))

; TO EXPLORE
; - http://developer.factual.com for Factual (Geo, etc.) API
; - Factual/geo
; - Countries of the world
;   - (http/request! {:url http://restcountries.eu/rest/v1/all :parse? true})
; - mpenet/sextant — Geo location utility functions
; ============================

(def us-states
  #{:ak :al :ar :az :ca :co :ct :de :fl :ga :hi :ia :id :il :in :ks :ky :la
    :ma :md :me :mi :mn :mo :ms :mt :nc :nd :ne :nh :nj :nm :nv :ny
    :oh :ok :or :pa :ri :sc :sd :tn :tx :ut :va :vt :wa :wi :wv :wy})

(s/def ::latitude  double?)
(s/def ::longitude double?)

(defn location->time-zone-info
  [arg api-key]
  (validate api-key string?)
  (let [{:keys [latitude longitude epoch-millis]}
          (validate arg (s/keys :req-un [::latitude ::longitude] :opt-un [::time/epoch-millis]))
        {:keys [status body] :as resp}
          (http/request!
            {:url (str "https://maps.googleapis.com/maps/api/timezone/json"
                       ;; TODO we wouldn't have to do this custom munging if the comma separation were
                       ;; supported in our HTTP client
                       "?"     "location"  "=" latitude "," longitude
                           "&" "timestamp" "=" (or epoch-millis 0)
                           "&" "key"       "=" api-key)})]
    (if (= status 200)
        (conv/json-> body (fn-> strf/->lisp-case keyword))
        (err! "Error in HTTP request" resp))))

(defn location->time-zone-id
  [arg api-key]
  (:time-zone-id (location->time-zone-info arg api-key)))

; (defrecord Location [city state])

; (defn location [city state]
;   (when state
;     (with-throw (in? state states)))
;   (Location. city state))

; ===== FACTUAL ===== ;

; From http://developer.factual.com/data-docs/
; Factual API/tables and schema — use this for inspiration

; Global Places
; Over 90 million businesses and points of interest from 50 countries around the world, updated in real time.

; Place categories
; The full Factual places category taxonomy.

; Restaurants
; Over 1.1 million restaurants in the U.S with up to 42 extended attributes, such as meal type, alcohol served, hours, and ratings.
; t/restaurants-gb
; Over 300,000 restaurants in the U.K.
; t/restaurants-fr
; Over 400,000 restaurants in France
; t/restaurants-de
; Over 400,000 restaurants in Germany
; t/restaurants-au
; Over 100,000 restaurants in the Australia

; US Hotels
; Over 180K hotel and other lodgings in the US with up to 37 extended attributes.

; Healthcare Providers (US)
; Database of over 1 million doctor, dentist, and healthcare provider listings with up to 8 extended attributes.

; Places Crosswalk
; Places Crosswalk enables you to translate between Factual IDs, third party IDs, and URLs that represent the same entity across the internet.    500/500 50/500

; CPG Products
; t/products-cpg
; Detailed product data for over 750,000 of the most popular consumer packaged goods, including your favorite health, beauty, food, beverage, and household products. With CPG Products, you can easily access key product attributes, find products using powerful search tools or UPC lookup, and connect to product pages across the web.  500/100 20/100
; t/products-cpg-nutrition
; Same as above, however includes ingredients and nutrition.  500/100 20/100

; Products Crosswalk
; Products Crosswalk enables you to translate between Factual IDs, third party IDs, and URLs that represent the same product across the web.  500/20  20/20

; ================= ;
