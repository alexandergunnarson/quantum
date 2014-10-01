; FORKED FROM: async-ui.ex-master-detail
(ns quanta.async-ui-arg
  (:require [clojure.core.async :refer [go <! >!]]
            [examine.core :as e]
            [examine.constraints :as c]
            [async-ui.core :as core]
            [async-ui.javafx.tk :as tk])) ; toolkit... probably that's where it's initialized, unless there's a defonce somewhere
; probably it's in async-ui.forml where the functions like window and panel are made
;(require '[async-ui.forml :refer :all])
(require '[async-ui.forml :as ui])
(require '[async-ui.core :as ui-core])
; (import '(java.util.concurrent Executors))

; https://www.youtube.com/watch?v=f6kdp27TYZs ; Go concurrency patterns
(comment
  "STEPS TO UNDERSTANDING THIS:
  1. Learn what reactive programming and goroutines (?) are")


; http://swannodette.github.io/2013/07/12/communicating-sequential-processes/
; http://martinsprogrammingblog.blogspot.com/2011/12/asynchronous-workflows-in-clojure.html
; http://adambard.com/blog/clojure-reducers-for-mortals/
; http://adambard.com/blog/clojure-concurrency-smorgasbord/

; ----------------------------------------------------------------------------
;; TODOs
;; - Demonstrate testing support, event recording and play-back
;; - Simulate a long-running call
;; - 1-arg setter-fn becomes 2-arg update-fn

;; Run this snippet
#_(do
  (ns async-ui.ex-master-detail)
  (def t (tk/make-toolkit))
  (ui-core/run-tk t))

;; Start process for master view
; You can see that two function-vars are referenced, one points to the factory function
; that creates the initial data that represents the view. The other one points to the event handler function.
#_(ui-core/run-view
    #'item-manager-view ; #'foo -> (var foo)
    #'item-manager-handler ; You're not passing around unevaluated fns; you're passing around vars which contain fns.
    {:item ""
     :items ["Foo" "Bar" "Baz"]})

;; We could start the process for the details view directly
;; Well that's really convenient! And just what I was thinking...
#_(ui-core/run-view #'item-editor-view
              #'item-editor-handler
              {:text "Foo"})

"A Spec is a map representing a UI form. A spec can be created with expressions like this:"
(ui/window "Item Editor"
  :content
  (ui/panel "Content"
    :lygeneral "wrap 2, fill" ; ?
    :lycolumns "[|100,grow]" ; ?
    :components ; A Component Path is a vector of visual component names. ; Why path, particularly?
    [(ui/label "Text") 
     (ui/textfield "text" :lyhint "growx")
     (ui/panel "Actions"
       :lygeneral "ins 0" ; ?
       :lyhint "span, right" ; ?
       :components
       [(ui/button "OK") (ui/button "Cancel")])])))

Here's an example of a simple event handler:

(defn item-editor-handler
  [view event]
  (go (case ((juxt :source :type) event) ; juxt?
        ["OK" :action]
        (assoc view :terminated true)
        ["Cancel" :action]
        (assoc view
          :terminated true
          :cancelled true)
        view)))



(defprotocol Toolkit ; So this is an interface, i.e., grouping of like functions. Yes? Kind of like a namespace but not a namespace.
  (run-now [tk func] "Executes function func in toolkit's event processing thread.")
  (show-view! [tk view] "Makes the root of the visual component tree visible.")
  (hide-view! [tk view] "Makes the root of the visual component tree invisible.")
  (build-vc-tree [tk view]
    "Creates a visual component tree from the data in the :spec slot of the view.
  Returns the view with an updated :vc slot.")
  (bind-vc-tree! [tk view]
    "Attaches listeners to visual components that put events to the :events channel of the view.
  Returns the view with :setter-fns slot updated.")
  (vc-name [tk vc] "Returns the name of the visual component.")
  (vc-children [tk vc] "Returns a seq with the children of the visual component or [] if it doesn't have any.")
  (set-vc-error! [tk vc msgs]
    "Updates the error state of a visual component according to the messages seq msgs.
  Empty msgs remove the error state."))

; ----------------------------------------------------------------------------
;; A Detail View

(defn item-editor-view
  [data]
  (-> (ui-core/make-view "item-editor"
        (ui/window "Item Editor"
          :content
          (ui/panel "Content" :lygeneral "wrap 2, fill" :lycolumns "[|100,grow]" 
            :components
            [(ui/label "Text") (ui/textfield "text" :lyhint "growx")
             (ui/panel "Actions" :lygeneral "ins 0" :lyhint "span, right"
                :components
                [(ui/button "OK") (ui/button "Cancel")])])))
      (assoc :mapping (ui-core/make-mapping :text ["text" :text])
             :validation-rule-set (e/rule-set :text (c/min-length 1))
             :data data)))

(defn item-editor-handler
  [view event]
  (go (case ((juxt :source :type) event)
        ["OK" :action]
        (assoc view :terminated true)
        ["Cancel" :action]
        (assoc view
          :terminated true
          :cancelled true)
        view)))


; ----------------------------------------------------------------------------
(defn item-manager-view
  [data]
  (let [spec ; Window
          (ui/window "Item Manager" ; title... should make a key for that
            :content
            (ui/panel "Content"
              :lygeneral "wrap 2, fill"
              :lycolumns "[|100,grow]" ; ? ly means what?
              :lyrows "[|200,grow|]" ; ?
              :components
              [(ui/label "Item")
               (ui/textfield "item" :lyhint "growx")
               (ui/listbox "items" :lyhint "span, grow")
               (ui/panel "Actions"
                :lygeneral "ins 0"
                :lyhint "span, right"
                :components
                [(ui/button "Add Item")
                 (ui/button "Edit Item")
                 (ui/button "Remove Item")])]))]
    (-> (ui-core/make-view "item-manager" spec)
        (assoc :mapping
          (ui-core/make-mapping
            :item ["item" :text]
            :items ["items" :items]
            :selection ["items" :selection])
               :data data))))

(defn replace-at
  [xs n ys]
  (concat (take n xs)
    ys
    (drop (inc n) xs)))

(defn item-manager-handler [view event]
  (go ; understand what this means
    ;The go block is needed here because the master may start another asynchronous view process for
    ; displaying and handling the detail view.
    ; The communication between master and detail is almost like an ordinary function invocation.
    ; The master view process is paused until the detail process finishes.
    (assoc view
      :data
      (let [data (:data view)]
        (case ((juxt :source :type) event)
          ["Add Item" :action]
          (-> data
              (update-in [:items] conj (:item data))
              (assoc :item ""))
          ["Edit Item" :action]
          (let [index (or (first (:selection data)) -1)]
            (if (not= index -1)
              (let [items (:items data)
                    editor-view
                      (<! (ui-core/run-view
                            #'item-editor-view ; sharp-quote means a new var, basically?
                            #'item-editor-handler
                            {:text (nth items index)}))]
                (if-not (:cancelled editor-view)
                  (assoc data
                    :items (replace-at items index [(-> editor-view :data :text)]))
                  data))
              data))
          ["Remove Item" :action]
          (assoc data
            :items
            (let [items (:items data)
                  index (or (first (:selection data)) -1)]
              (if (not= index -1)
                (replace-at items index [])
                items)))
          data)))))

