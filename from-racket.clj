== checks for mathematical equivalence.
= with numbers checks for equivalence in a way that is agnostic to size where applicable,
  but is strict about representation (decimal vs ratio)

; ===== NUMBERS ===== ;

; Whether x is a number
number?
; Whether x is representable by a ratio (integers and non-repeating decimals)
rational?
; Whether x is inexact (primitive/boxed double or float)
inexact?
; Whether x is a non-complex number
real?
; Whether x is a complex number
complex?
decimal?
float?

; Create complex numbers
(complex 0+1i)


; Single-threaded mutable
(!hash-map)  ; java.util.HashMap
(!hash-set)  ; java.util.HashSet
(!vector)    ; java.util.ArrayList
(!list)      ; java.util.LinkedList
; Concurrent mutable
(!phash-map) ; java.util.concurrent.ConcurrentHashMap
#_(pvector!) ; ?
#_(plist!) ; ?

; TODO optional arguments as opposed to creating an arity manually

; TODO continuations?

; TODO `raise`


sqrt (from `integer-sqrt`) — handles sqrt(neg. numbers) -> complex numbers

https://docs.racket-lang.org/reference/generic-numbers.html
- 4.2.2.5 Complex Numbers
`integer-length` "https://docs.racket-lang.org/reference/generic-numbers.html#%28def._%28%28quote._~23~25kernel%29._integer-length%29%29"
`random-sample` "https://docs.racket-lang.org/reference/generic-numbers.html#%28def._%28%28lib._racket%2Frandom..rkt%29._random-sample%29%29"


; TODO should be able to create site from .md
