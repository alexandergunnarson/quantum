@echo off
setlocal

call bin\classpath

java -server -XX:+UseG1GC -XX:MaxGCPauseMillis=50 %DATOMIC_JAVA_OPTS% -Xmx1g -cp lib\jline-win-1.0.jar;%CP_LIST% clojure.main -i bin\bridge.clj --main datomic.rest %*

endlocal




