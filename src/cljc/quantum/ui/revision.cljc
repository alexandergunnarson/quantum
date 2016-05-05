(ns quantum.ui.revision)

; COMMIT AND UNDO/REDO

#_(defnt commit!*
  ([^quantum.ui.core.FXObservableAtom x states]
    (swap! states coll/updates-in+
      [:last-modified :instant] (constantly (time/now))
      [:last-modified :item   ] (constantly x)
      [x :index ] (whenf*n nil? (constantly 0) (MWA inc))
      [x :states] (f*n conj (-> x :immutable deref)))))

#_(defn commit! [states x] (commit!* x states))

#_(defnt oswap!*
  ([^javafx.collections.transformation.FilteredList x states f args]
    (println "OSWAP WITH ARGS CLASS" (class args))
    (condp = f
      conj    (fx/run<!! (-> x .getSource (.addAll args)))
      assoc   (fx/run<!!
                (doseq [i v (into {} args)]
                  (.set (.getSource x) i v)))
      update  (let [i        (first args)
                    update-f (second args)]
                (fx/run<!! (.set (.getSource x) i (update-f (.get (.getSource x) i)))))
      (throw+ (Err. :illegal-operation "Not an allowed operation for oswap:" f))))
  ([^quantum.ui.core.FXObservableAtom x states commit? f args]
    (oswap!* (:observable x) states f args)
    (apply swap! (:immutable x) f args)
    (when commit? (commit!* x states))
    x))

#_(defn oswap! [states commit? x f & args]
  (oswap!* x states commit? f args))

#_(defnt oreset!*
  ([^javafx.collections.transformation.FilteredList x-0 states x-f]
    (log/pr :user 3)
    (println "ORESET WITH NEW-X CLASS" (class x-f))
    (fx/run<!!
      (log/pr :user 4)
      (.clear (.getSource x-0))
      (.addAll (.getSource x-0) x-f)))
  ([^quantum.ui.core.FXObservableAtom x states commit? x-f]
    (log/pr :user 5)
    (oreset!* (:observable x) states x-f)
    (reset! (:immutable x) x-f)
    (when commit? (commit!* x states))
    (log/pr :user 6)
    x))

#_(defn oreset! [states commit? x-0 x-f] (oreset!* x-0 states commit? x-f))

#_(defn coordinate-state! [states x]
  (log/pr :user 1)
  (let [i (get-in @states [x :index])]
    (log/pr :user 2)
    (oreset! states false x (get-in @states [x :states i]))))

; TODO doesn't fully work
#_(defn redo!
  "Increments the pointer"
  ([states] (if-let [item (-> @states :last-modified :item)]
              (redo! states item false)
              (do (log/pr :debug "Nothing to redo.")
                  false)))
  ([states x] (redo! states x false))
  ([states x full?]
    (throw-unless (contains? @states x) "Can't redo because doesn't exist")
    (if (-> @states (get x) :index (= (lasti x)))
        (do (log/pr :debug "No more to redo.")
            false)
        (do (swap! states update x
              (fn [x-n] (update x-n :index (if full?
                                               (constantly (lasti x-n))
                                               (whenf*n (f*n < (lasti x-n)) inc)))))
            (coordinate-state! states x)
            true))))

#_(defn undo!
  "Decrements the pointer"
  ([states] (if-let [item (-> @states :last-modified :item)]
              (undo! states item false)
              (do (log/pr :debug "Nothing to undo.")
                  false)))
  ([states x] (undo! states x false))
  ([states x full?]
    (throw-unless (contains? @states x) "Can't undo because doesn't exist")
    (if (-> @states (get x) :index (= 0))
        (do (log/pr :debug "No more to undo.")
            false)
        (do (swap! states update x
              (fn [x-n] (update x-n :index (if full?
                                               (constantly 0)
                                               (whenf*n (f*n > 0) dec)))))
            (coordinate-state! states x)
            true))))

#_(defnt add-undo-redo!*
  ([^quantum.ui.core.FXObservableAtom x states]
    (swap! states assoc x {:index 0 :states [(-> x :immutable deref)]})))
#_(defn add-undo-redo! [states x] (add-undo-redo!* x states) true)

#_(defnt unsaved-changes?
  [^clojure.lang.Atom states]
  (any? (fn-> val :index (> 0)) @states))

#_(defnt revert!
  ([^clojure.lang.Atom states]
    (doseq [component meta- @states]
      (undo! states component true)
      (add-undo-redo! states component))))

#_(defn append-data!
  {:todo ["Rename, etc."]}
  [states source dest]
  (oreset! states true dest
    (coll/index-with-ids
      (catvec
        (-> dest :immutable deref)
        (vec source)))))