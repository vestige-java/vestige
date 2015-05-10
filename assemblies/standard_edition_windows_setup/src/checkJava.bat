@echo off

setlocal

set DIRNAME=%~dp0
if "%DIRNAME:~-1%" == "\" (set DIRNAME=%DIRNAME:~0,-1%)

if defined JAVA goto :java_found

for %%I in (java.exe) do set JAVA=%%~$PATH:I
if defined JAVA goto :java_found

if defined JAVA_HOME goto :java_home_found

echo Unable to start a JVM : %%JAVA%% is not set and java.exe is not in PATH and %%JAVA_HOME%% is not set
exit /B 1

:java_home_found
set JAVA=%JAVA_HOME%\bin\java.exe

:java_found

"%JAVA%" CheckJava || exit /B 2

echo "" > "%DIRNAME%\java_check.txt"

endlocal
