; Implement a version of DaisyDisk.
; Have a tree of z-order. Possibly a vector like so:
; [a]     ; height 0
; [b]     ; height 1
; [c d e] ; height 2
; [f g]   ; height 3

; Incorporate /get-size/ into the different defmethods for the :height key, etc
; Or, maybe make a tree that a definer can parse?
; set fill, etc. by replacing strings in :style property
