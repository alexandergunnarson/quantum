# Naming conventions

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
