@echo off
setlocal

call bin\classpath

java -server %DATOMIC_JAVA_OPTS% -Xmx1g -cp lib\console\*;%CP_LIST% clojure.main -i bin\bridge.clj --main datomic.console %*

endlocal




