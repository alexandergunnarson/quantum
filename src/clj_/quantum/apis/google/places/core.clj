(ns quantum.apis.google.places.core
  (:require-quantum [:lib conv http auth]))

(def search-url "https://maps.googleapis.com/maps/api/place/nearbysearch/json")

(defrecord GeoCoordinate [lat long])
; (GeoCoordinate. 0 0 #_lat #_long)

(def valid-place-types
  #{"accounting"
    "airport"
    "amusement_park"
    "aquarium"
    "art_gallery"
    "atm"
    "bakery"
    "bank"
    "bar"
    "beauty_salon"
    "bicycle_store"
    "book_store"
    "bowling_alley"
    "bus_station"
    "cafe"
    "campground"
    "car_dealer"
    "car_rental"
    "car_repair"
    "car_wash"
    "casino"
    "cemetery"
    "church"
    "city_hall"
    "clothing_store"
    "convenience_store"
    "courthouse"
    "dentist"
    "department_store"
    "doctor"
    "electrician"
    "electronics_store"
    "embassy"
    "establishment"
    "finance"
    "fire_station"
    "florist"
    "food"
    "funeral_home"
    "furniture_store"
    "gas_station"
    "general_contractor"
    "grocery_or_supermarket"
    "gym"
    "hair_care"
    "hardware_store"
    "health"
    "hindu_temple"
    "home_goods_store"
    "hospital"
    "insurance_agency"
    "jewelry_store"
    "laundry"
    "lawyer"
    "library"
    "liquor_store"
    "local_government_office"
    "locksmith"
    "lodging"
    "meal_delivery"
    "meal_takeaway"
    "mosque"
    "movie_rental"
    "movie_theater"
    "moving_company"
    "museum"
    "night_club"
    "painter"
    "park"
    "parking"
    "pet_store"
    "pharmacy"
    "physiotherapist"
    "place_of_worship"
    "plumber"
    "police"
    "post_office"
    "real_estate_agency"
    "restaurant"
    "roofing_contractor"
    "rv_park"
    "school"
    "shoe_store"
    "shopping_mall"
    "spa"
    "stadium"
    "storage"
    "store"
    "subway_station"
    "synagogue"
    "taxi_stand"
    "train_station"
    "travel_agency"
    "university"
    "veterinary_care"
    "zoo"})

(defn+ search
  "Called 'Nearby Search / Place Search'"
  {:usage '(search {:long -111.90948486 :lat 40.56180797}
             (auth/datum :google "fake@gmail.com" :api-key))
   :info "https://developers.google.com/places/webservice/search"}
  [coord api-key & [{:keys [radius search-type place-types parse?]
                       :or {radius 50000}}]]
  (assert (nnil? api-key))
  (assert (in? search-type #{:fuzzy :exact}))
  (assert (and (<= radius 50000) (> radius 0)) #{radius})

  (let [location (str (:lat coord) "," (:long coord))
        search-opts (condp = search-type
                      :fuzzy {"radius" radius}
                      :exact {"rankby" "distance"
                              "types"  (if place-types
                                           (str/join "|" place-types)
                                           (extern (str/join "|" valid-place-types)))})]
   
    (http/request!
      {:url search-url
       :parse? parse?
       :query-params
         (mergel search-opts
           {"key"      api-key
            "location" location
            ; Defines the distance (in meters) within which to return place results. 
            })})))


;  types=food&name=cruise&key=API_KEY



; If rankby=distance (described under Optional parameters below) is specified, then one or more of keyword, name, or types is required.
; Optional parameters

; keyword — A term to be matched against all content that Google has indexed for this place, including but not limited to name, type, and address, as well as customer reviews and other third-party content.
; language — The language code, indicating in which language the results should be returned, if possible. See the list of supported languages and their codes. Note that we often update supported languages so this list may not be exhaustive.
; minprice and maxprice (optional) — Restricts results to only those places within the specified range. Valid values range between 0 (most affordable) to 4 (most expensive), inclusive. The exact amount indicated by a specific value will vary from region to region.
; name — One or more terms to be matched against the names of places, separated with a space character. Results will be restricted to those containing the passed name values. Note that a place may have additional names associated with it, beyond its listed name. The API will try to match the passed name value against all of these names. As a result, places may be returned in the results whose listed names do not match the search term, but whose associated names do.
; opennow — Returns only those places that are open for business at the time the query is sent. Places that do not specify opening hours in the Google Places database will not be returned if you include this parameter in your query.
; rankby — Specifies the order in which results are listed. Possible values are:
; prominence (default). This option sorts results based on their importance. Ranking will favor prominent places within the specified area. Prominence can be affected by a place's ranking in Google's index, global popularity, and other factors.
; distance. This option sorts results in ascending order by their distance from the specified location. When distance is specified, one or more of keyword, name, or types is required.
; types — Restricts the results to places matching at least one of the specified types. Types should be separated with a pipe symbol (type1|type2|etc). See the list of supported types.
; pagetoken — Returns the next 20 results from a previously run search. Setting a pagetoken parameter will execute a search with the same parameters used previously — all parameters other than pagetoken will be ignored.
; zagatselected — Add this parameter (just the parameter name, with no associated value) to restrict your search to locations that are Zagat selected businesses. This parameter must not include a true or false value. The zagatselected parameter is experimental, and is only available to Google Places API for Work customers.
; Maps API for Work customers should not include a client or signature parameter with their requests.

; The following example is a search request for places of type 'food' within a 500m radius of a point in Sydney, Australia, containing the word 'cruise' in their name:


; Note that you'll need to replace the key in this example with your own key in order for the request to work in your application.

; Text Search Requests

; The Google Places API Text Search Service is a web service that returns information about a set of places based on a string — for example "pizza in New York" or "shoe stores near Ottawa". The service responds with a list of places matching the text string and any location bias that has been set. The search response will include a list of places, you can send a Place Details request for more information about any of the places in the response.

; The Google Places search services share the same usage limits. However, the Text Search service is subject to a 10-times multiplier. That is, each Text Search request that you make will count as 10 requests against your quota. If you've purchased the Google Places API as part of your Google Maps API for Work contract, the multiplier may be different. Please refer to the Google Maps API for Work documentation for details.

; A Text Search request is an HTTP URL of the following form:

; https://maps.googleapis.com/maps/api/place/textsearch/output?parameters
; where output may be either of the following values:

; json (recommended) indicates output in JavaScript Object Notation (JSON)
; xml indicates output as XML
; Certain parameters are required to initiate a search request. As is standard in URLs, all parameters are separated using the ampersand (&) character.

; Required parameters

; query — The text string on which to search, for example: "restaurant". The Google Places service will return candidate matches based on this string and order the results based on their perceived relevance.
; key — Your application's API key. This key identifies your application for purposes of quota management and so that places added from your application are made immediately available to your app. Visit the Google Developers Console to create an API Project and obtain your key.
; Optional parameters

; location — The latitude/longitude around which to retrieve place information. This must be specified as latitude,longitude. If you specify a location parameter, you must also specify a radius parameter.
; radius — Defines the distance (in meters) within which to bias place results. The maximum allowed radius is 50 000 meters. Results inside of this region will be ranked higher than results outside of the search circle; however, prominent results from outside of the search radius may be included.
; language — The language code, indicating in which language the results should be returned, if possible. See the list of supported languages and their codes. Note that we often update supported languages so this list may not be exhaustive.
; minprice and maxprice (optional) — Restricts results to only those places within the specified price level. Valid values are in the range from 0 (most affordable) to 4 (most expensive), inclusive. The exact amount indicated by a specific value will vary from region to region.
; opennow — Returns only those places that are open for business at the time the query is sent. places that do not specify opening hours in the Google Places database will not be returned if you include this parameter in your query.
; types — Restricts the results to places matching at least one of the specified types. Types should be separated with a pipe symbol (type1|type2|etc). See the list of supported types.
; pagetoken — Returns the next 20 results from a previously run search. Setting a pagetoken parameter will execute a search with the same parameters used previously — all parameters other than pagetoken will be ignored.
; zagatselected — Add this parameter (just the parameter name, with no associated value) to restrict your search to locations that are Zagat selected businesses. This parameter must not include a true or false value. The zagatselected parameter is experimental, and is only available to Google Places API for Work customers.
; You may bias results to a specified circle by passing a location and a radius parameter. This will instruct the Google Places service to prefer showing results within that circle. Results outside the defined area may still be displayed.

; Maps API for Work customers should not include a client or signature parameter with their requests.

; The below example shows a search for restaurants near Sydney.

; https://maps.googleapis.com/maps/api/place/textsearch/xml?query=restaurants+in+Sydney&key=API_KEY
; Note that you'll need to replace the key in this example with your own key in order for the request to work in your application.




; Place Search

; The Google Places API Web Service is for use in server applications. If you're building a client-side application, take a look at the Google Places API for Android and the Places Library in the Google Maps JavaScript API.

; Note: The id and reference fields are deprecated as of June 24, 2014. They are replaced by the new place ID, a textual identifier that uniquely identifies a place and can be used to retrieve information about the place. The usual deprecation period of one year has been extended, as we’re looking into ways of ensuring this change will not break existing code. We’ll update this page with a final notice at least 90 days before we change the way the API handles the id and reference fields. We recommend that you update your code to use the new place ID instead of id and reference as soon as possible.
; The Google Places API Web Service allows you to query for place information on a variety of categories, such as: establishments, prominent points of interest, geographic locations, and more. You can search for places either by proximity or a text string. A Place Search returns a list of places along with summary information about each place; additional information is available via a Place Details query.

; Nearby Search Requests

; Earlier versions of the Places API referred to Nearby Search as Place Search.

; A Nearby Search lets you search for places within a specified area. You can refine your search request by supplying keywords or specifying the type of place you are searching for.

; A Nearby Search request is an HTTP URL of the following form:

; https://maps.googleapis.com/maps/api/place/nearbysearch/output?parameters
; where output may be either of the following values:

; json (recommended) indicates output in JavaScript Object Notation (JSON)
; xml indicates output as XML
; Certain parameters are required to initiate a Nearby Search request. As is standard in URLs, all parameters are separated using the ampersand (&) character.

; Required parameters

; key — Your application's API key. This key identifies your application for purposes of quota management and so that places added from your application are made immediately available to your app. Visit the Google Developers Console to create an API Project and obtain your key.
; location — The latitude/longitude around which to retrieve place information. This must be specified as latitude,longitude.
; radius — Defines the distance (in meters) within which to return place results. The maximum allowed radius is 50 000 meters. Note that radius must not be included if rankby=distance (described under Optional parameters below) is specified.
; If rankby=distance (described under Optional parameters below) is specified, then one or more of keyword, name, or types is required.
; Optional parameters

; keyword — A term to be matched against all content that Google has indexed for this place, including but not limited to name, type, and address, as well as customer reviews and other third-party content.
; language — The language code, indicating in which language the results should be returned, if possible. See the list of supported languages and their codes. Note that we often update supported languages so this list may not be exhaustive.
; minprice and maxprice (optional) — Restricts results to only those places within the specified range. Valid values range between 0 (most affordable) to 4 (most expensive), inclusive. The exact amount indicated by a specific value will vary from region to region.
; name — One or more terms to be matched against the names of places, separated with a space character. Results will be restricted to those containing the passed name values. Note that a place may have additional names associated with it, beyond its listed name. The API will try to match the passed name value against all of these names. As a result, places may be returned in the results whose listed names do not match the search term, but whose associated names do.
; opennow — Returns only those places that are open for business at the time the query is sent. Places that do not specify opening hours in the Google Places database will not be returned if you include this parameter in your query.
; rankby — Specifies the order in which results are listed. Possible values are:
; prominence (default). This option sorts results based on their importance. Ranking will favor prominent places within the specified area. Prominence can be affected by a place's ranking in Google's index, global popularity, and other factors.
; distance. This option sorts results in ascending order by their distance from the specified location. When distance is specified, one or more of keyword, name, or types is required.
; types — Restricts the results to places matching at least one of the specified types. Types should be separated with a pipe symbol (type1|type2|etc). See the list of supported types.
; pagetoken — Returns the next 20 results from a previously run search. Setting a pagetoken parameter will execute a search with the same parameters used previously — all parameters other than pagetoken will be ignored.
; zagatselected — Add this parameter (just the parameter name, with no associated value) to restrict your search to locations that are Zagat selected businesses. This parameter must not include a true or false value. The zagatselected parameter is experimental, and is only available to Google Places API for Work customers.
; Maps API for Work customers should not include a client or signature parameter with their requests.

; The following example is a search request for places of type 'food' within a 500m radius of a point in Sydney, Australia, containing the word 'cruise' in their name:

; https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=500&types=food&name=cruise&key=API_KEY
; Note that you'll need to replace the key in this example with your own key in order for the request to work in your application.

; Text Search Requests

; The Google Places API Text Search Service is a web service that returns information about a set of places based on a string — for example "pizza in New York" or "shoe stores near Ottawa". The service responds with a list of places matching the text string and any location bias that has been set. The search response will include a list of places, you can send a Place Details request for more information about any of the places in the response.

; The Google Places search services share the same usage limits. However, the Text Search service is subject to a 10-times multiplier. That is, each Text Search request that you make will count as 10 requests against your quota. If you've purchased the Google Places API as part of your Google Maps API for Work contract, the multiplier may be different. Please refer to the Google Maps API for Work documentation for details.

; A Text Search request is an HTTP URL of the following form:

; https://maps.googleapis.com/maps/api/place/textsearch/output?parameters
; where output may be either of the following values:

; json (recommended) indicates output in JavaScript Object Notation (JSON)
; xml indicates output as XML
; Certain parameters are required to initiate a search request. As is standard in URLs, all parameters are separated using the ampersand (&) character.

; Required parameters

; query — The text string on which to search, for example: "restaurant". The Google Places service will return candidate matches based on this string and order the results based on their perceived relevance.
; key — Your application's API key. This key identifies your application for purposes of quota management and so that places added from your application are made immediately available to your app. Visit the Google Developers Console to create an API Project and obtain your key.
; Optional parameters

; location — The latitude/longitude around which to retrieve place information. This must be specified as latitude,longitude. If you specify a location parameter, you must also specify a radius parameter.
; radius — Defines the distance (in meters) within which to bias place results. The maximum allowed radius is 50 000 meters. Results inside of this region will be ranked higher than results outside of the search circle; however, prominent results from outside of the search radius may be included.
; language — The language code, indicating in which language the results should be returned, if possible. See the list of supported languages and their codes. Note that we often update supported languages so this list may not be exhaustive.
; minprice and maxprice (optional) — Restricts results to only those places within the specified price level. Valid values are in the range from 0 (most affordable) to 4 (most expensive), inclusive. The exact amount indicated by a specific value will vary from region to region.
; opennow — Returns only those places that are open for business at the time the query is sent. places that do not specify opening hours in the Google Places database will not be returned if you include this parameter in your query.
; types — Restricts the results to places matching at least one of the specified types. Types should be separated with a pipe symbol (type1|type2|etc). See the list of supported types.
; pagetoken — Returns the next 20 results from a previously run search. Setting a pagetoken parameter will execute a search with the same parameters used previously — all parameters other than pagetoken will be ignored.
; zagatselected — Add this parameter (just the parameter name, with no associated value) to restrict your search to locations that are Zagat selected businesses. This parameter must not include a true or false value. The zagatselected parameter is experimental, and is only available to Google Places API for Work customers.
; You may bias results to a specified circle by passing a location and a radius parameter. This will instruct the Google Places service to prefer showing results within that circle. Results outside the defined area may still be displayed.

; Maps API for Work customers should not include a client or signature parameter with their requests.

; The below example shows a search for restaurants near Sydney.

; https://maps.googleapis.com/maps/api/place/textsearch/xml?query=restaurants+in+Sydney&key=API_KEY
; Note that you'll need to replace the key in this example with your own key in order for the request to work in your application.

; Radar Search Requests

; The Google Places API Radar Search Service allows you to search for up to 200 places at once, but with less detail than is typically returned from a Text Search or Nearby Search request. With Radar Search, you can create applications that help users identify specific areas of interest within a geographic area.

; The search response will include up to 200 places, and will include only the following information about each place:

; The geometry field containing geographic coordinates.
; The place_id, which you can use in a Place Details request to get more information about the place. For more information about place IDs, see the place ID overview.
; The deprecated reference field. See the deprecation notice on this page.
; A Radar Search request is an HTTP URL of the following form:

; https://maps.googleapis.com/maps/api/place/radarsearch/output?parameters
; where output may be either of the following values:

; json (recommended) indicates output in JavaScript Object Notation (JSON)
; xml indicates output as XML
; Certain parameters are required to initiate a search request. As is standard in URLs, all parameters are separated using the ampersand (&) character.

; Required parameters

; key — Your application's API key. This key identifies your application for purposes of quota management and so that places added from your application are made immediately available to your app. Visit the Google Developers Console to create an API Project and obtain your key.
; location — The latitude/longitude around which to retrieve place information. This must be specified as latitude,longitude.
; radius — Defines the distance (in meters) within which to return place results. The maximum allowed radius is 50 000 meters.
; A Radar Search request must include at least one of keyword, name, or types.
; Optional parameters

; keyword — A term to be matched against all content that Google has indexed for this place, including but not limited to name, type, and address, as well as customer reviews and other third-party content.
; minprice and maxprice (optional) — Restricts results to only those places within the specified price level. Valid values are in the range from 0 (most affordable) to 4 (most expensive), inclusive. The exact amount indicated by a specific value will vary from region to region.
; name — One or more terms to be matched against the names of places, separated by a space character. Results will be restricted to those containing the passed name values. Note that a place may have additional names associated with it, beyond its listed name. The API will try to match the passed name value against all of these names. As a result, places may be returned in the results whose listed names do not match the search term, but whose associated names do.
; opennow — Returns only those places that are open for business at the time the query is sent. Places that do not specify opening hours in the Google Places database will not be returned if you include this parameter in your query.
; types — Restricts the results to places matching at least one of the specified types. Types should be separated with a pipe symbol (type1|type2|etc). See the list of supported types.
; zagatselected — Add this parameter (just the parameter name, with no associated value) to restrict your search to locations that are Zagat selected businesses. This parameter must not include a true or false value. The zagatselected parameter is experimental, and is only available to Google Places API for Work customers.
; Maps API for Work customers should not include a client or signature parameter with their requests.

; The below example returns a list of museums near London, England.

; https://maps.googleapis.com/maps/api/place/radarsearch/json?location=51.503186,-0.126446&radius=5000&types=museum&key=API_KEY
; Using a combination of the keyword, name and types parameters, you can perform more precise queries. The below example shows restaurants and cafes in Paris that users have described as vegetarian.

; https://maps.googleapis.com/maps/api/place/radarsearch/json?location=48.859294,2.347589&radius=5000&types=food|cafe&keyword=vegetarian&key=API_KEY
; Note that you'll need to replace the key in these examples with your own key in order for the request to work in your application.

; Search Responses

; Search responses are returned in the format indicated by the output flag within the URL request's path.

; The following example shows a Nearby Search response. A Text Search response is similar, except that it returns a formatted_address instead of a vicinity property. 

; JSONXML
; {
;    "html_attributions" : [],
;    "results" : [
;       {
;          "geometry" : {
;             "location" : {
;                "lat" : -33.870775,
;                "lng" : 151.199025
;             }
;          },
;          "icon" : "http://maps.gstatic.com/mapfiles/place_api/icons/travel_agent-71.png",
;          "id" : "21a0b251c9b8392186142c798263e289fe45b4aa",
;          "name" : "Rhythmboat Cruises",
;          "opening_hours" : {
;             "open_now" : true
;          },
;          "photos" : [
;             {
;                "height" : 270,
;                "html_attributions" : [],
;                "photo_reference" : "CnRnAAAAF-LjFR1ZV93eawe1cU_3QNMCNmaGkowY7CnOf-kcNmPhNnPEG9W979jOuJJ1sGr75rhD5hqKzjD8vbMbSsRnq_Ni3ZIGfY6hKWmsOf3qHKJInkm4h55lzvLAXJVc-Rr4kI9O1tmIblblUpg2oqoq8RIQRMQJhFsTr5s9haxQ07EQHxoUO0ICubVFGYfJiMUPor1GnIWb5i8",
;                "width" : 519
;             }
;          ],
;          "place_id" : "ChIJyWEHuEmuEmsRm9hTkapTCrk",
;          "scope" : "GOOGLE",
;          "alt_ids" : [
;             {
;                "place_id" : "D9iJyWEHuEmuEmsRm9hTkapTCrk",
;                "scope" : "APP"
;             }
;          ],
;          "reference" : "CoQBdQAAAFSiijw5-cAV68xdf2O18pKIZ0seJh03u9h9wk_lEdG-cP1dWvp_QGS4SNCBMk_fB06YRsfMrNkINtPez22p5lRIlj5ty_HmcNwcl6GZXbD2RdXsVfLYlQwnZQcnu7ihkjZp_2gk1-fWXql3GQ8-1BEGwgCxG-eaSnIJIBPuIpihEhAY1WYdxPvOWsPnb2-nGb6QGhTipN0lgaLpQTnkcMeAIEvCsSa0Ww",
;          "types" : [ "travel_agency", "restaurant", "food", "establishment" ],
;          "vicinity" : "Pyrmont Bay Wharf Darling Dr, Sydney"
;       },
;       {
;          "geometry" : {
;             "location" : {
;                "lat" : -33.866891,
;                "lng" : 151.200814
;             }
;          },
;          "icon" : "http://maps.gstatic.com/mapfiles/place_api/icons/restaurant-71.png",
;          "id" : "45a27fd8d56c56dc62afc9b49e1d850440d5c403",
;          "name" : "Private Charter Sydney Habour Cruise",
;          "photos" : [
;             {
;                "height" : 426,
;                "html_attributions" : [],
;                "photo_reference" : "CnRnAAAAL3n0Zu3U6fseyPl8URGKD49aGB2Wka7CKDZfamoGX2ZTLMBYgTUshjr-MXc0_O2BbvlUAZWtQTBHUVZ-5Sxb1-P-VX2Fx0sZF87q-9vUt19VDwQQmAX_mjQe7UWmU5lJGCOXSgxp2fu1b5VR_PF31RIQTKZLfqm8TA1eynnN4M1XShoU8adzJCcOWK0er14h8SqOIDZctvU",
;                "width" : 640
;             }
;          ],
;          "place_id" : "ChIJqwS6fjiuEmsRJAMiOY9MSms",
;          "scope" : "GOOGLE",
;          "reference" : "CpQBhgAAAFN27qR_t5oSDKPUzjQIeQa3lrRpFTm5alW3ZYbMFm8k10ETbISfK9S1nwcJVfrP-bjra7NSPuhaRulxoonSPQklDyB-xGvcJncq6qDXIUQ3hlI-bx4AxYckAOX74LkupHq7bcaREgrSBE-U6GbA1C3U7I-HnweO4IPtztSEcgW09y03v1hgHzL8xSDElmkQtRIQzLbyBfj3e0FhJzABXjM2QBoUE2EnL-DzWrzpgmMEulUBLGrtu2Y",
;          "types" : [ "restaurant", "food", "establishment" ],
;          "vicinity" : "Australia"
;       },
;       {
;          "geometry" : {
;             "location" : {
;                "lat" : -33.870943,
;                "lng" : 151.190311
;             }
;          },
;          "icon" : "http://maps.gstatic.com/mapfiles/place_api/icons/restaurant-71.png",
;          "id" : "30bee58f819b6c47bd24151802f25ecf11df8943",
;          "name" : "Bucks Party Cruise",
;          "opening_hours" : {
;             "open_now" : true
;          },
;          "photos" : [
;             {
;                "height" : 600,
;                "html_attributions" : [],
;                "photo_reference" : "CnRnAAAA48AX5MsHIMiuipON_Lgh97hPiYDFkxx_vnaZQMOcvcQwYN92o33t5RwjRpOue5R47AjfMltntoz71hto40zqo7vFyxhDuuqhAChKGRQ5mdO5jv5CKWlzi182PICiOb37PiBtiFt7lSLe1SedoyrD-xIQD8xqSOaejWejYHCN4Ye2XBoUT3q2IXJQpMkmffJiBNftv8QSwF4",
;                "width" : 800
;             }
;          ],
;          "place_id" : "ChIJLfySpTOuEmsRsc_JfJtljdc",
;          "scope" : "GOOGLE",
;          "reference" : "CoQBdQAAANQSThnTekt-UokiTiX3oUFT6YDfdQJIG0ljlQnkLfWefcKmjxax0xmUpWjmpWdOsScl9zSyBNImmrTO9AE9DnWTdQ2hY7n-OOU4UgCfX7U0TE1Vf7jyODRISbK-u86TBJij0b2i7oUWq2bGr0cQSj8CV97U5q8SJR3AFDYi3ogqEhCMXjNLR1k8fiXTkG2BxGJmGhTqwE8C4grdjvJ0w5UsAVoOH7v8HQ",
;          "types" : [ "restaurant", "food", "establishment" ],
;          "vicinity" : "37 Bank St, Pyrmont"
;       },
;       {
;          "geometry" : {
;             "location" : {
;                "lat" : -33.867591,
;                "lng" : 151.201196
;             }
;          },
;          "icon" : "http://maps.gstatic.com/mapfiles/place_api/icons/travel_agent-71.png",
;          "id" : "a97f9fb468bcd26b68a23072a55af82d4b325e0d",
;          "name" : "Australian Cruise Group",
;          "opening_hours" : {
;             "open_now" : true
;          },
;          "photos" : [
;             {
;                "height" : 242,
;                "html_attributions" : [],
;                "photo_reference" : "CnRnAAAABjeoPQ7NUU3pDitV4Vs0BgP1FLhf_iCgStUZUr4ZuNqQnc5k43jbvjKC2hTGM8SrmdJYyOyxRO3D2yutoJwVC4Vp_dzckkjG35L6LfMm5sjrOr6uyOtr2PNCp1xQylx6vhdcpW8yZjBZCvVsjNajLBIQ-z4ttAMIc8EjEZV7LsoFgRoU6OrqxvKCnkJGb9F16W57iIV4LuM",
;                "width" : 200
;             }
;          ],
;          "place_id" : "ChIJrTLr-GyuEmsRBfy61i59si0",
;          "scope" : "GOOGLE",
;          "reference" : "CoQBeQAAAFvf12y8veSQMdIMmAXQmus1zqkgKQ-O2KEX0Kr47rIRTy6HNsyosVl0CjvEBulIu_cujrSOgICdcxNioFDHtAxXBhqeR-8xXtm52Bp0lVwnO3LzLFY3jeo8WrsyIwNE1kQlGuWA4xklpOknHJuRXSQJVheRlYijOHSgsBQ35mOcEhC5IpbpqCMe82yR136087wZGhSziPEbooYkHLn9e5njOTuBprcfVw",
;          "types" : [ "travel_agency", "restaurant", "food", "establishment" ],
;          "vicinity" : "32 The Promenade, King Street Wharf 5, Sydney"
;       }
;    ],
;    "status" : "OK"
; }
; A JSON response contains up to four root elements:

; "status" contains metadata on the request. See Status Codes below.
; "results" contains an array of places, with information about each. See Search Results for information about these results. The Places API returns up to 20 establishment results per query. Additionally, political results may be returned which serve to identify the area of the request.
; html_attributions contain a set of attributions about this listing which must be displayed to the user.
; next_page_token contains a token that can be used to return up to 20 additional results. A next_page_token will not be returned if there are no additional results to display. The maximum number of results that can be returned is 60. There is a short delay between when a next_page_token is issued, and when it will become valid.
; See Processing JSON with Javascript for help parsing JSON responses.
; Status Codes

; The "status" field within the search response object contains the status of the request, and may contain debugging information to help you track down why the request failed. The "status" field may contain the following values:

; OK indicates that no errors occurred; the place was successfully detected and at least one result was returned.
; ZERO_RESULTS indicates that the search was successful but returned no results. This may occur if the search was passed a latlng in a remote location.
; OVER_QUERY_LIMIT indicates that you are over your quota.
; REQUEST_DENIED indicates that your request was denied, generally because of lack of an invalid key parameter.
; INVALID_REQUEST generally indicates that a required query parameter (location or radius) is missing.
