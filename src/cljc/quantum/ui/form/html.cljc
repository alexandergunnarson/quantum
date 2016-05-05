(ns
  ^{:doc "Namespace for functions related HTML. Tag domain, etc."
    :attribution "Alex Gunnarson"}
  quantum.ui.form.html)  

(def ui-tag-domain (atom #{}))

; TODO: Don't use |postwalk| and you won't have to check so many things.
#_(def ui-element?
  (fn-and
    vector?                ; element
    (fn-> first keyword?) ; tag  
    (fn-> first (not= :style)) ; to make sure it's not just an attribute ; check this further
    (fn-or
      (fn-> second coll?)
      (fn-> first name (contains? ".")))))  ; attributes

#_(defn determine-tag-domain!
  [^Vec html]
  (->> html
       (postwalk ; [:style {...}] ; should be map-entry but... and this will happen with other attributes
         (fn [elem]
           (when (ui-element? elem)
             (let [^Key tag (first elem)]
               (swap! ui-tag-domain conj tag)))
           elem))))
