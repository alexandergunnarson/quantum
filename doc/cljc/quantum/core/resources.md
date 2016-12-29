#Component vs. Mount

##Component

###Disadvantages

- It becomes impossible to test any part of the application without having the resources available.
- Changes to Component `defrecord`s do not affect the instances that have already been created.

##Mount

###Advantages

- Doesn't rely on protocols, so it doesn't impact the REPL driven workflow.

###Disadvantages

- It's not possible to create multiple instances of a resource
- We have to be mindful not to couple namespaces representing resources to the business logic.
