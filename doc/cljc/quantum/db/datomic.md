#Related resources

- https://github.com/cloojure/tupelo-datomic — 'Datomic with a spoonful of honey'

#TODOs
- Show effect of modifying schema (we shouldn't be afraid of modifying them at all!)
- Have a "database dashboard" which shows the whole database, navigable + editable in the browser (with sufficient permissions)
- Transact the transaction functions for all schemas (not in separate partitions)
  - something like :fn/credential>aws:service.db-invariant | invariant
  - As for storing them locally, store {ident source} where source is
    - ([requires]? [imports]? [args] body)
  - This will be different for DS, in which case you eval the source code and ignore the requires/imports
  - Do Datomic first; DS will follow suit
