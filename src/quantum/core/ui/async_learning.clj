(ns quanta.async-learning)
(require '[clojure.core.async :as async :refer
  [chan go close! >!! <!! >! <! thread timeout alts! alts!!]])
;;;;; https://github.com/clojure/core.async/blob/master/examples/walkthrough.clj

; http://adambard.com/blog/clojure-concurrency-smorgasbord/
; http://adambard.com/blog/why-clojure-part-2-async-magic/
; http://swannodette.github.io/2013/07/12/communicating-sequential-processes/

; Note that, unlike other Clojure concurrency constructs, channels,
; like all communications, are subject to deadlocks, the simplest
; being waiting for a message that will never arrive, which must be dealt
; with manually via timeouts etc. CSP proper is amenable to certain kinds
; of automated correctness analysis.

; Also note that async channels are not intended for fine-grained computational
; parallelism, though you might see examples in that vein.

; goroutines don't map 1-1 with threads; they get their own thread pool
; go blocks are lightweight processes not bound to threads, we can have LOTS of them! (1000 or more!)
; number of cores + 2 for max recommended threads
; threads spend most of their time waiting for stuff to happen... put it to good use!
; channels allow goroutines to talk to each other - concurrent blocking queues
; Creating too many threads can bring your machine (or VM) down
; due to the amount of stack allocated to each one.
; Goroutines are cheap to create so you can have hundreds of thousands of them,
; and the runtime will multiplex them into a thread pool.
; So far it sounds awfully similar to using futures with a pre-configured
; thread pool and a bit of syntactic sugar. But this is not the end of it.
; Channels are first-class citizens - meaning you can pass them as arguments
; to functions as well as the return value of functions.

;;;; CHANNELS

;; Data is transmitted on queue-like channels. By default channels
;; are unbuffered (0-length) - they require producer and consumer to
;; rendezvous for the transfer of a value through the channel.

;; Use `chan` to make an unbuffered channel:
(chan)
; => #<ManyToManyChannel clojure.core.async.impl.channels.ManyToManyChannel@5b1ff8cd>

;; Pass a number to create a channel with a fixed buffer:
(chan 10)

;; `close!` a channel to stop accepting puts. Remaining values are still
;; available to take. Drained channels return nil on take. Nils may
;; not be sent over a channel explicitly! ; "Can't put nil on channel"
(let [c (chan)]
  (close! c))
; => nil

;;;; ORDINARY THREADS
;; In ordinary (real / non-go) threads,
;; we use `>!!` (blocking put) (?)
;; and `<!!` (blocking take) (?)
;; to communicate via channels.

(let [c (chan 10)]
  (>!! c "hello") ; puts "hello" on channel c
  (assert (= "hello" (<!! c))) ; is "hello" the first item on the channel?
  (close! c))

;; Because these are blocking calls, if we try to put on an
;; unbuffered channel, (?)
;; we will block the main thread. We can use
;; `thread` (like `future`) to execute a body in a pool thread ; thread pool?
;; and return a channel with the result. Here we launch a background task
;; to put "hello" on a channel, then read that value in the current thread.

(let [c (chan)]
  (thread (>!! c "hello")) ; a "background task"
  (assert (= "hello" (<!! c))) ; the current task
  (close! c))

;;;; GO BLOCKS AND IOC THREADS
;; The `go` macro asynchronously executes its body in a special thread pool.
;; Channel operations that would block will pause
;; execution instead, blocking no threads. This mechanism encapsulates
;; the inversion of control that is external in event/callback systems. (?)
;; Inside `go` blocks, we use `>!` (put) and `<!` (take).

;; Here we convert our prior channel example to use go blocks:
(let [c (chan)]
  (go (>! c "hello")) ; Instead of the explicit thread and blocking call, we use a go block for the producer.
  (assert (-> (go (<! c))
               ; The consumer uses a go block to take
              (<!!) ; and returns a result channel, from which we do a blocking take.
              (= "hello")))
  (close! c))
  
(let [c (chan)]
  (go (>! c "hello")) ; put "hello" on the channel-queue /c/
  (assert (= "hello" (go (<! c)))) ; AssertionError
  (close! c))

;;;; ALTS

;; One killer feature for channels over queues is the ability to wait
;; on many channels at the same time. This is
;; done with `alts!!` (ordinary threads) or `alts!` in go blocks.
;; We can create a background thread with alts that //combines inputs on//
;; either of two channels.
;; `alts!!` takes either a set of operations to perform -
;;   either a channel to take from [or?] a [channel value] to put
;; and returns the value (nil for put) and channel that succeeded:

(let [c1 (chan)
      c2 (chan)]
  (thread (while true
            (let [[v ch] (alts!! [c1 c2])]
              (println "Read" v "from" ch))))
  (>!! c1 "hi")
  (>!! c2 "there"))

;; Prints (on stdout, possibly not visible at your repl):
;;   Read hi from #<ManyToManyChannel ...>
;;   Read there from #<ManyToManyChannel ...>

;; We can use alts! to do the same thing with go blocks:

(let [c1 (chan)
      c2 (chan)]
  (go (while true ; item in queue
        (let [[item-n ch] (alts! [c1 c2])]
          (println "Read" item-n "from" ch))))
  (go (>! c1 "hi"))
  (go (>! c2 "there")))

;; Here we create 1000 go blocks that say hi on 1000 channels.
;; We use alts!! to read them as they're ready.
(let [msg-ct 1000
      chans (repeatedly msg-ct #(timeout 100)) ; 1000 channels ; when does the timeout start??
      begin-time (System/currentTimeMillis)]
  (doseq [chan-n chans] (go (>! chan-n "hi"))) ; creates new go blocks
  (dotimes [n msg-ct]
    (let [[q-n chan-n] (alts!! chans)]
      ;(assert (= "hi" q-n)) ; doesn't work because some are nil because some time out
      (println "msg" n q-n)
      ))
  (println "Read" msg-ct "msgs in" (- (System/currentTimeMillis) begin-time) "ms"))

(let [msg-ct 1000
      chans (repeatedly msg-ct chan) ; 1000 channels ; when does the timeout start??
      begin-time (System/currentTimeMillis)]
  (doseq [chan-n chans] (go (>! chan-n "hi"))) ; creates new go blocks
  (dotimes [n msg-ct]
    (let [[q-n chan-n] (alts!! chans)]
      ;(assert (= "hi" q-n))
      (println "msg" n q-n)
      ))
  (println "Read" msg-ct "msgs in" (- (System/currentTimeMillis) begin-time) "ms"))

;; `timeout` creates a channel that waits for a specified ms, then closes:
(let [t (timeout 100)
      begin (System/currentTimeMillis)]
  (<!! t)
  (println "Waited" (- (System/currentTimeMillis) begin)))

;; We can combine timeout with `alts!` to do timed channel waits.
;; Here
(let [c (chan)
      begin (System/currentTimeMillis)]
  (alts!! [c (timeout 100)]) ;  we wait for 100 ms for a value to arrive on the channel, then give up:
  (println "Gave up after" (- (System/currentTimeMillis) begin)))

(let [c0 (go (>! (timeout 100) "Hi from the 100"))
      c1 (go (>! (timeout 200) "Hi from the 200"))]
  (go (let [[q-n chans] (alts! [c0 c1])] (println "q-n:" q-n))))

;;; OTHER BUFFERS

;; Channels can also use custom buffers that have different policies
;; for the "full" case.  Two useful examples are provided in the API.

;; Use `dropping-buffer` to drop newest values when the buffer is full:
(chan (dropping-buffer 10))

;; Use `sliding-buffer` to drop oldest values when the buffer is full:
(chan (sliding-buffer 10))

(def c (chan))

(defn render [q]
  (apply str
    (for [p (reverse q)] ; because it's a queue
      (str "Process" p "\n"))))
(defn peekn
  "Returns vector of (up to) n items from the end of vector v"
  [v n]
  (if (> (count v) n)
    (subvec v (- (count v) n))
    v))
(go (dotimes [i 10] (<! (timeout 2500)) (>! c 1)))
(go (dotimes [i 10] (<! (timeout 3000)) (>! c 2)))
(go (dotimes [i 10] (<! (timeout 3500)) (>! c 3)))
(go (loop [q []]
      (-> q render println)
      (recur (-> (conj q (<! c))
                 (peekn 10))))) ; takes a look at the first 10 items in the queue?

; Converts events on a DOM element into a channel we can read from:
(defn listen [elem type] ; elem: a DOM element | type: an event type
  (let [c (chan)]
    (events/listen elem type #(put! c %)) ; puts the DOM event into the channel
    c))                        ;since we're not in a go block we do this with an async put!.
(let [el  (by-id "ex1")
      out (by-id "ex1-mouse")
      c   (listen el :mousemove)]
  (go (while true
        (let [e (<! c)]
          (set-html! out (str (.-offsetX e) ", " (.-offsetY e)))))))

; it shows you the offset in the box, with (0, 0) at the top-left-hand corner











; Rock Paper Scissors with core.async.
; Each player is modeled as a go process that generates moves on a channel.
; A judge is modeled as a go process that takes moves from each player
; via their channel and reports the match results on its own channel.

(def MOVES [:rock :paper :scissors])
(def BEATS {:rock :scissors, :paper :rock, :scissors :paper})
; Let’s make a player that randomly throws moves on an output channel:
(defn rand-player
  "Create a named player and return a channel to report moves."
  [name]
  (let [out (chan)]
    (go (while true (>! out [name (rand-nth MOVES)])))
    out))
; Here, chan creates an unbuffered (0-length) channel.
; We create a go process.
; You can think of a go process as a lightweight thread.
; In this case, it will loop forever and create random moves (represented as a vector of [name throw]) and placing them on the out channel.
; However, because the channel is unbuffered, //what does this have to do with it?//
; the put (>!) will not succeed until someone is ready to read it.
;  Inside a go process, these puts will NOT block a thread; the go process is simply parked waiting.
; To create our judge, we’ll need a helper method to decide the winner:
(defn winner
  "Based on two moves, return the name of the winner."
  [[name1 move1] [name2 move2]]
  (cond (= move1 move2) "no one"
        (= move2 (BEATS move1)) name1
        :else name2))
; And now we’re ready to create our judging process:
(defn judge
  "Given two channels on which players report moves, create and return an
   output channel to report the results of each match as [move1 move2 winner]."
  [player-1 player-2]
  (let [out (chan)]
    (go
     (while true
       (let [move-1 (<! player-1) ; 
             move-2 (<! player-2)]
         (>! out [move-1 move-2 (winner move-1 move-2)]))))
    out))
; The judge is a go process that sits in a loop forever.
; Each time through the loop it takes a move from each player,
; computes the winner and reports the match results on the out channel (as [move1 move2 winner]).
(defn init
  "Create 2 players (by default Alice and Bob) and return an output channel of match results."
  ([] (init "Alex" "Some guy"))
  ([n1 n2] (judge (rand-player n1) (rand-player n2))))
; And then we can play the game by simply taking a match result from the output channel and reporting.
(defn report
  "Report results of a match to the console."
  [[name1 move1] [name2 move2] winner]
  (println)
  (println name1 "plays" move1)
  (println name2 "plays" move2)
  (println winner "wins!"))

(defn play
  "Play by taking a match reporting channel and reporting the results of the latest match."
  [out-chan]
  (apply report (<!! out-chan)))
; Here we use the actual blocking form of take (<!!).
; This is because we are outside a go block in the main thread.
We can then play like this:
(def one-round (init))
(play one-round)

We might also want to play a whole bunch of games:
(defn play-many
  "Play n matches from out-chan and report a summary of the results."
  [out-chan n]
  (loop [remaining n
         results {}]
    (if (zero? remaining)
      results
      (let [[m1 m2 winner] (<!! out-chan)]
        (recur (dec remaining)
               (merge-with + results {winner 1}))))))
; Which you’ll find is pretty fast:
; user> (time (play-many game 10000))
; "Elapsed time: 145.405 msecs"
; {"no one" 3319, "Bob" 3323, "Alice" 3358}


; Say you’re Google. And you need to write code that takes user input, -
; a search string - hits 3 different search services, - web, images and video -
; aggregates the results and presents them to the user.
; Since they are three different services, you wish to do this concurrently.
; To simulate these services, Rob presented a function that has unpredictable
; performance based of a random amount of sleep, shown below:

; We hit the services concurrently, wait for them to respond and then return the results:
(defn fake-search [kind]
  (fn [query]
    (Thread/sleep (rand-int 1000)) ; it takes a random amount of time to complete the search
    (str kind " result for '" query "'")))

(def web   (fake-search "Web"))
(def image (fake-search "Image"))
(def video (fake-search "Video"))
(defn google [query] ; "Type this into Google search"
  (let [c (chan)]
    (go (>! c (web   query))) ; put onto the channel
    (go (>! c (image query))) ; same
    (go (>! c (video query))) ; same
    ; (map #(<!! (go (<! %))) c) ; can't create a seq from a channel
     (reduce (fn [results _]
               (conj results (<!! (go (<! c))))) ; block on channel /c/ until we get the values
             []
             (range 3))
            )) ; 0 1 2 of channel
(defn mapc [func channel]
  (loop [
         accum-n []]
    (conj accum-n (func channel))))
; You’ll notice the use of /alts!!/ here.
; /alts!!/ waits on multiple channels and returns as soon as *any*
; of them has something to say. (not nil)
; One of these channels times out, at which point it returns nil,
; effectively moving on to the next iteration and ignoring that slow server(s) response.
(defn google2 [query]
  (let [c (chan)
        t (timeout 500)]
    (go (>! c (web query)))
    (go (>! c (image query)))
    (go (>! c (video query)))
    (reduce (fn [results _] ; is _ just a useless parameter?
              (conj results (first (alts!! [c t])))) ; t is the timeout channel
            []
            (range 3))))
(google "Clojure") ; always returns all results
(google2 "Clojure")

; We don't wish to wait on slow servers.
; So we’ll return whatever results we have after a pre-defined timeout.
; If a given func is going to block or do I/O, then you might want to create a thread instead of a go.
; Threads are more expensive.
; For visualization of threads vs. go-blocks:
; http://martinsprogrammingblog.blogspot.com/2011/12/asynchronous-workflows-in-clojure.html
; If you want to write some code that can scale to thousands of connections,
; one thread per connection simply doesn't work. If you are using futures like above,
; when you try to make thousands of simultaneous connections, they will just queue on the
; thread pool, and the threads that are claimed will spend almost all their time blocked on IO
; (plus it will be dominated by accesses to slow servers).
; So either you kill you system spawning thousands of threads (concur 1000)
; or you take forever to complete when the thread pool slowly easts through the queued up work.
; (concur 10 on a huge sequence of items to be processed)

; EXAMPLE OF A SEARCH ENGINE WITH 3 VARIABLE-TIMING PROCESSES : GO

(require '[clojure.core.async :as async :refer [<! >! <!! timeout chan alt! go]])

(defn fake-search [kind]
  (fn [c query]
    (go
     (<! (timeout (rand-int 100)))
     (>! c [kind query]))))

(def web1 (fake-search :web1))
(def web2 (fake-search :web2))
(def image1 (fake-search :image1))
(def image2 (fake-search :image2))
(def video1 (fake-search :video1))
(def video2 (fake-search :video2))

(defn fastest [query & replicas]
  (let [c (chan)]
    (doseq [replica replicas]
      (replica c query))
    c))

(defn google [query]
  (let [c (chan)
        t (timeout 80)]
    (go (>! c (<! (fastest query web1 web2))))
    (go (>! c (<! (fastest query image1 image2))))
    (go (>! c (<! (fastest query video1 video2))))
    (go (loop [i 0 ret []]
          (if (= i 3)
            ret
            (recur (inc i) (conj ret (alt! [c t] ([v] v)))))))))

(<!! (google "clojure"))

; EXAMPLE OF A SEARCH ENGINE WITH 3 VARIABLE-TIMING PROCESSES : ASYNC

(require '[clojure.core.async :as async :refer [<!! >!! timeout chan alt!!]])

(defn fake-search [kind]
  (fn [c query]
    (future
     (<!! (timeout (rand-int 100)))
     (>!! c [kind query]))))

(def web1 (fake-search :web1))
(def web2 (fake-search :web2))
(def image1 (fake-search :image1))
(def image2 (fake-search :image2))
(def video1 (fake-search :video1))
(def video2 (fake-search :video2))

(defn fastest [query & replicas]
  (let [c (chan)]
    (doseq [replica replicas]
      (replica c query))
    c))

(defn google [query]
  (let [c (chan)
        t (timeout 80)]
    (future (>!! c (<!! (fastest query web1 web2))))
    (future (>!! c (<!! (fastest query image1 image2))))
    (future (>!! c (<!! (fastest query video1 video2))))
    (loop [i 0 ret []]
      (if (= i 3)
        ret
        (recur (inc i) (conj ret (alt!! [c t] ([v] v))))))))

(google "clojure")

; http://adambard.com/blog/clojure-reducers-for-mortals/
; core.reducers
; how to use??
; Don't get ahead of yourself...
(defn reducer-test [nums]
    (into [] (r/filter even? (r/map inc nums))))

  ;; Eager test:  1442 ms 
  ;; Lazy test:   982 ms
  ;; Reducers test:  643 ms

