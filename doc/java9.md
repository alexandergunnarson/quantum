# Java 9

Note that leiningen fails to run because it uses the bootclasspath and thus can't find java.sql.Timestamp which is used by clojure.instant.
https://github.com/technomancy/leiningen/issues/2149
