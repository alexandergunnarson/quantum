#Vision

Courtesy of [Web after tomorrow](http://tonsky.me/blog/the-web-after-tomorrow/)

We want to connect data source and a client as tight as possible, with the library taking care of all the negotiation details. These are the things we are interested in:

- Consistent view of the data.
- Always fresh data. All data we see on the client should be relevant right now. Everything up to the tiniest detail. Ideally including all resources and even code that runs the app. If I upload a new userpic, I want it to be reloaded on all the screens where people might be seeing it at the moment. Even if it’s displayed in a one-second-long, self-disposing notification popup.
- Instant response. UI should not wait until server confirms user’s actions.
- Handle network failures.
- Offline. Obviously data will not be up-to-date, but at least I should be able to do local modifications, then merge changes when I get back online.

Whole database
-> accessible data (pass through security/auth filter)
-> looking-at data

Web pages may track hundreds of different objects.
They may track results of complex queries.
They may track aggregations.
The database might have thousands of live clients connected at the same time.

Client still defines its needs via queries.
Queries might be used for initial data fetch, just as usual.
The same queries will then be used to filter whole-DB changelog and decide what parts of it the server should push to which client.
Fetch is about trying to get the data given the query.
Push is about finding the affected subscriptions given the changed data.


Filters and subscriptions (change-listeners) change over time.

- How do you track subscriptions?
- How do you detect dead ones and do garbage collection?
  - On a client we can benefit from a insufficiently praised React feature: component lifecycles. By listening for didMount and willUnmount we can reliably track when components (and their subscriptions) come and go. That way we always know exactly what our visitor is looking at. On a server, though, it’s just timeouts, refcounting, periodic cleaning and careful coding.

A single DELETE lost from changefeed during reconnect (with no integrity validation) might lead to catastrophic UI glitches. Reordering is out of the question too, for the same reasons.
This is a territory of distributed databases here, where browser plays the role of one of the peers.
The DB could provide a strongly consistent event log, and DB-client synchronization protocol can take advantage of that. The browser is not traditionally viewed as a peer (CouchDB is moving in that direction though), but that doesn't matter.

Initiating changes

Every local action should be captured, transformed into a change/delta, put into some sort of a queue and sent to the server in the background.

Lag compensation and offline work?

We should display effects of all user actions immediately, even before sending them to the server.
Offline is just a special case of lag compensation where lag is infinite. Offline mode should look just like a normal app without real-time updates. We just embrace that deltas queue could grow indefinitely and make the process of making local changes sustainable.

System-level synchronization is achieved via a single source of truth, e.g. DataScript.
You would also need explicit change management to send deltas over network, track them, conserve in local storage and apply them to the local state.

Meteor.js has latency compensation, local storage, server push, subscriptions, and server data filtering. I can’t find any information about consistency and reliability guarantees of their DDP protocol or how well server push scales with the number of subscriptions.

Datomic's transaction queue enables reactive pushes.

Datomic's datom model is at a great granularity level for security and subscription filters. A datom is the smallest piece of information that could be synchronized.

If your UI is big and complex and needs granular updates based on what have changed in a DB, it’s still pretty straightforward, although there is no ready-made library for that at the moment.

A subscription language is the biggest missing piece of this stack. Datomic and DataScript speak Datalog which is very powerful language, but it is hard to reverse efficiently. If you have a very high volume of transactions going through your DB, for each of them you’ll have to determine which clients should get an update. Running a Datalog query per client is not an option.





[Datview](https://github.com/metasoarous/datview): Compose queries which know how to render themselves.
Instead of decorating components with information about where they get their data, decorate queries with information about how they render. This data driven approach leads to effortlessly composable view code, meaning complex UI can be programmatically constructed from simple pieces.
We do this with metadata on parts of our pull expressions and queries.

[Datreactor](https://github.com/metasoarous/datreactor)
Re-frame style event handling transaction coordination for DataScript connections.
*TODO* look at this more



#Platforms

##Electron

As an application platform, Electron is fantastic for building small simple client applications that involve a single main window. For larger applications it’s likely that you will need to fork Electron in order to get what you need. Project Tofino runs on a fork of Electron already and Brave does too.

Electron is designed around use-cases like Atom (obviously), VS Code, Slack, etc.

Electron ships with video and audio codecs that require licenses for use.

Electron is not designed to build browsers.

Electron uses one process per window and another process per tab. This leads to many processes when used at the sorts of scale we’ve seen from Firefox users. Since each process requires significant OS-level resources a browser is forced to do non-trivial process management (like clever shared forking) to keep overhead low. That’s likely to be hard with Electron.

See: https://medium.com/project-tofino/engineering-update-on-tofino-8381d82398e8#.60j77l1yr

#Dataflow Frameworks

#[re-frame](https://github.com/Day8/re-frame)

- event handler middleware
- coeffect accretion
- de-duplicated signal graphs
- effectively manages mutation

*TODO* [read re-frame docs](https://github.com/Day8/re-frame/tree/master/docs#introduction)

1. Event dispatch/generation (on-* handlers) (User interacts with UI)
2. Event handling (re-frame pure transformers) (In response to an event, an application must decide what action to take.)
  - Event handlers return a data structure which says, declaratively, how the application state should change because of the event (pure).
3. Effect Handling (`ds/transact!` + side effects) (The descriptions of effects are realized/actioned)
  - This is where mutations happen
  - Sometimes the outside world must also be affected (localstore, cookies, databases, emails, logs, etc) (impure).
4. Query/Subscription (posh / re-frame subscriptions) (Extracting data from the app state and providing it in the right format for view functions)
- A novel and efficient de-duplicated signal graph which runs query functions on the app state
- It efficiently computes reactive, multi-layered, "materialized views" of the app state.
5. View (reagent components) (functions/components that compute the UI DOM that should be displayed to the user)
- Need to source application state, which is delivered reactively via the queries of Domino 4.
6. DOM
- Reagent+React handles it for you

#View Frameworks

##Rum

###Advantages

- Simple semantics: Rum is arguably smaller, simpler and more straightforward than React itself.

- Decomplected: Rum is a library, not a framework. Use only the parts you need, throw away or replace what you don’t need, combine different approaches in a single app, or even combine Rum with other frameworks.

- No enforced state model: Unlike Om, Reagent or Quiescent, Rum does not dictate where to keep your state. Instead, it works well with any storage / data structures.

- Extensible: the API is stable and explicitly defined, including the API between Rum internals. It lets you build custom behaviours that change components in significant ways.

- Minimal codebase: You can become a Rum expert just by reading its source code (~900 lines).

- Has server-side rendering

###Disadvantages

- Maybe won't work with React Native (yet?)
- Not as proven a track record, or as much developer attention, as Reagent

##Freactive

- Doesn't rely on ReactJS and aims to deliver good animation performance.
- Handily works with JavaFX and JS alike.

#Browsers created using JS

https://medium.com/project-tofino/
https://brave.com/
https://github.com/browserhtml/browserhtml
https://vivaldi.com/
https://minbrowser.github.io/min/
https://github.com/k88hudson/browser
https://github.com/vingtetun/planula
