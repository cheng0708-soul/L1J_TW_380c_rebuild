@ECHO OFF
SETLOCAL

SET "JAVA_CMD=java"
IF DEFINED JAVA_HOME IF EXIST "%JAVA_HOME%\bin\java.exe" SET "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

"%JAVA_CMD%" -version 2>&1 | findstr /c:"\"1.8" /c:" version \"8" >NUL
IF ERRORLEVEL 1 ECHO [warn] Java 8 not detected, using current java runtime.

START "" "%JAVA_CMD%" -server -Xmx8m -Xms8m -jar l1jloader.jar
