#[Datomic](http://datomic.com)

The definitive closed-source Clojure implementation, intended for persistent storage.

#[DataScript](https://github.com/tonsky/datascript)

A Clojure(Script) open-source implementation of Datomic’s ideas, intended for in-memory use.

#[Datomish](https://github.com/mozilla/datomish)

A Clojure(Script) open-source implementation of Datomic’s ideas, intended for persistent storage.

Adding persistence to DataScript seems easy. You might think you'd be able to leverage all of the work that went into DataScript, and just flush data to disk. It soon becomes apparent that you can't resolve the impedance mismatch between a synchronous in-memory store and asynchronous persistence. There are also concerns about memory usage with large datasets.

Datomish is built on top of SQLite, so it gets all of SQLite’s reliability and features: full-text search, transactionality, durable storage, and a small memory footprint.

Achieves similar query speeds to an application-specific normalized schema. Even the storage space overhead is acceptable.

See: https://medium.com/project-tofino/introducing-datomish-a-flexible-embedded-knowledge-store-1d7976bff344

#References

https://github.com/kristianmandrup/datascript-tutorial
