@echo off

setlocal

if "%1" == "" (
  set CMD=jline.ConsoleRunner org.codehaus.groovy.tools.shell.Main
) else (
  set CMD=groovy.lang.GroovyShell %*
)

call bin\classpath

java -server -Xmx1g -cp lib\jline-win-1.0.jar;%CP_LIST% %CMD%
