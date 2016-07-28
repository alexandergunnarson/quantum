
; http://stackoverflow.com/questions/2087522/does-javascript-have-a-built-in-stringbuilder-class
; Concatenating strings with the + or += operator are extremely slow on IE.
; This is especially true for IE6.
; On modern browsers += is usually just as fast as array joins.
(defn join [separator coll]
  ; (let [s ""]
  ;   (doseq [obj coll]
  ;     (concat! s separator obj))
  ;   s)
  (.join coll separator)
  )

(defn str [& objs]
  (console/log "OBJS" objs)
  (let [str-objs
          (for-into (array) [obj objs]
            (cond (object? obj)
                  (.toString obj)
                  :else
                  (.toString (String. obj))))]
    (console/log "STR OBJS" str-objs)
    (this.join "" str-objs)))

(defn sp [& args]
  (this.join " " args))