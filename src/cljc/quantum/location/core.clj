(ns quantum.location.core)

; Countries of the world
; (http/request! {:url "http://restcountries.eu/rest/v1/all" :parse? true})

(def us-states
  #{:ak :al :ar :az :ca :co :ct :de :fl :ga :hi :ia :id :il :in :ks :ky :la
    :ma :md :me :mi :mn :mo :ms :mt :nc :nd :ne :nh :nj :nm :nv :ny
    :oh :ok :or :pa :ri :sc :sd :tn :tx :ut :va :vt :wa :wi :wv :wy})

; (defrecord Location [city state])

; (defn location [city state]
;   (when state
;     (with-throw (in? state states)))
;   (Location. city state))