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

DOS AND DONT'S: http://martintrojer.github.io/clojure/2015/06/03/datomic-dos-and-donts/

DO

Keep metrics on your query times
Datomic lacks query planning. Queries that look harmless can be real hogs.
The solution is usually blindly swapping lines in your query until you get an order of magnitude speedup.

Always use memcached with Datomic
When new peers connect, a fair bit of data needs to be transferred to them.
If you don't use memcached this data needs to be queried from the store and will slow down the 'peer connect time' (among other things).

Datomic was designed with AWS/Dynamo in mind, use it
It will perform best with this backend.

Prefer dropping databases to excising data
If you want to keep logs, or other data with short lifespan in Datomic,
put them in a different database and rotate the databases on a daily / weekly basis.

Use migrators for your attributes, and consider squashing unused attributes before going to prod
Don't be afraid to rev the schemas, you will end up with quite a few unused attributes.
It's OK, but squash them before its too late.

Trigger transactor GCs periodically when load is low
If you are churning many datoms, the transactor is going have to GC. When this happens writes will be very slow.

Consider introducing a Datomic/peer tier in your infrastructure
Since Datomic's licensing is peer-count limited, you might have to start putting
your peers together in a Datomic-tier which the webserver nodes (etc) queries via the Datomic REST API.

DON'T
Don't put big strings into Datomic
If your strings are bigger than 1kb put them somewhere else (and keep a link to them in Datomic).
Datomic's storage model is optimized for small datoms, so if you put big stuff in there perf will drop dramatically.

Don't load huge datasets into Datomic. It will take forever, with plenty transactor GCs.
Keep an eye on the DynamoDB write throughput since it might bankrupt you.
Also, there is a limit to the number of datoms Datomic can handle.

Don't use Datomic for stuff it wasn't intended to do
Don't run your geospatial queries or query-with-aggregations in Datomic, it's OK to have multiple datastores in your system.
