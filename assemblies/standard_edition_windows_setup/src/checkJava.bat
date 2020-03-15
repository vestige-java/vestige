@echo off

setlocal enableextensions enabledelayedexpansion

set "DIRNAME=%~dp0"
if [%DIRNAME:~-1%] == [\] set "DIRNAME=%DIRNAME:~0,-1%"

if defined VESTIGE_JAVA goto :vestige_java_set

if defined JAVA goto :java_found

for %%I in (java.exe) do set "VESTIGE_JAVA=%%~$PATH:I"
if defined VESTIGE_JAVA goto :vestige_java_set

if defined JAVA_HOME goto :java_home_found

echo Unable to start a JVM : %%VESTIGE_JAVA%% and %%JAVA%% are not set and java.exe is not in PATH and %%JAVA_HOME%% is not set
exit /B 1

:java_home_found
set "VESTIGE_JAVA=%JAVA_HOME%\bin\java.exe"
goto :vestige_java_set

:java_found
set "VESTIGE_JAVA=%JAVA%"

:vestige_java_set

"%VESTIGE_JAVA%" CheckJava || exit /B 2

echo "" > "%DIRNAME%\java_check.txt"

endlocal
