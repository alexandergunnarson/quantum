# Naming conventions

## Symbols

- `->`+         : constructor
                : 'convert to'
- +`->`         : 'convert from'
- `<`+          : calculate/compute (of functions) — as if to "take off the chan" of computed vals
- <A>`:`<B>     : specificity relationship; <A> of type <B>
- +`?`          : predicate
- `?`+          : 'maybe' — if null, return null, otherwise do something
- +`*`          : 'variant' — as ambiguous as it sounds
                : 'relaxed' — in the context of numerics
- +`'`          : 'strict' — esp. if numeric
                : 'prime'/'next'
- +`+`          : 'reducer'
- +`$`          : 'end' — as in the end-of-stack dollar symbol in pushdown automata
- +`!`          : 'side-effecting' — function causes side effects
- `!`+          : 'mutable' — denotes presence of mutable state
- `+`+          : 'immutable' — denotes presence of immutable state
- +`&`          : 'exact' — in the context of `defnt`, ensures compile-time, non-protocol dispatch
- `a`+          : 'array'
- `l`+          : 'lazy'
- `r`+          : 'reverse'
- +`l`          : 'left'
- +`r`          : 'right'
- +`>!`         : 'green-thread-blocking'
- +`>!!`        : 'thread-blocking'
- `i`(`:`|`-`)+ : 'index'

## Comparison to Clojure in specific function naming

`nil?`  -> `nil?`
`some?` -> `val?`, because `nil` represents unvaluedness : `(when (val? x) ...)`

`seq` (to convert to a sequence) -> `->seq`
`seq` (to test non-emptiness)    -> `contains?` (1-arity) because if it contains anything at all, it's non-empty : `(when (contains? xs) ...)`
`empty?`                         -> `empty?` (only of non-nil collections)

`some`   -> `coll/or` , perhaps `seq-or`
`every?` -> `coll/and`, perhaps `seq-and`

*Logician George Boolos strongly urged that "contains" be used for membership only.*
`contains?`                       -> `contains?` (including for non-associative structures)
is subsequence within sequential  -> `subseq?`
*Logician George Boolos strongly urged that "includes" be used for the subset relation only.*
matches pattern within collection -> `includes?`
