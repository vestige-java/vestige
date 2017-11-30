${at}echo off

setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME:~-1%" == "\" set DIRNAME=%DIRNAME:~0,-1%

set DATADIR=%DIRNAME%
set CONFDIR=%DIRNAME%

if defined JAVA goto :java_found

for %%I in (java.exe) do set JAVA=%%~$PATH:I
if defined JAVA goto :java_found

if defined JAVA_HOME goto :java_home_found

echo Unable to start a JVM : %%JAVA%% is not set and java.exe is not in PATH and %%JAVA_HOME%% is not set
exit /B 1

:java_home_found
set JAVA=%JAVA_HOME%\bin\java.exe

:java_found

if not defined JAVA_OPTS set JAVA_OPTS=-Djava.net.useSystemProxies=true

if not defined VESTIGE_BASE set VESTIGE_BASE=%DIRNAME%\base

if not exist "%VESTIGE_BASE%" (
  md "%VESTIGE_BASE%"
  call "%DIRNAME%\deepCopy.bat" "%CONFDIR%\template" "%VESTIGE_BASE%"
)

if not defined VESTIGE_DATA set VESTIGE_DATA=%DIRNAME%\data

if not defined VESTIGE_SECURITY set VESTIGE_SECURITY=true

if not defined VESTIGE_LISTENER_PORT set VESTIGE_LISTENER_PORT=0

if not defined VESTIGE_OPTS set VESTIGE_OPTS=%JAVA_OPTS%

if defined VESTIGE_DEBUG set VESTIGE_OPTS=%VESTIGE_OPTS% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000

set MAVEN_LAUNCHER_FILE=%DATADIR%\m2\vestige-se.xml

set MAVEN_SETTINGS_FILE=%VESTIGE_BASE%\m2\settings.xml

set MAVEN_RESOLVER_CACHE_FILE=%VESTIGE_DATA%\m2\resolver-cache.ser

set LOGBACK_CONFIGURATION_FILE=%VESTIGE_BASE%\logback.xml

if not exist "%LOGBACK_CONFIGURATION_FILE%" copy /Y "%CONFDIR%\template\logback.xml" "%LOGBACK_CONFIGURATION_FILE%" 2> nul > nul

set VESTIGE_OPTS=%VESTIGE_OPTS% -Dvestige.mavenRepository="%DATADIR%\repository" -Djava.util.logging.manager=fr.gaellalire.vestige.core.logger.JULLogManager -Dlogback.logsDirectory="%VESTIGE_BASE%\logs" -Dlogback.configurationFile="%LOGBACK_CONFIGURATION_FILE%"

"%JAVA%" --add-modules java.base -version 2> nul > nul
if %ERRORLEVEL% equ 0 (
  set VESTIGE_OPTS=!VESTIGE_OPTS! --add-modules ALL-DEFAULT --patch-module "java.base=%DATADIR%\lib\moduleEncapsulationBreaker.jar"
  set VESTIGE_ARGS=-p "%DATADIR%\lib\vestige.core-${vestige.core.version}.jar" -m fr.gaellalire.vestige.core --add-modules fr.gaellalire.vestige.edition.maven_main_launcher frmp "%DATADIR%" "%DATADIR%\windows-classpath.txt" fr.gaellalire.vestige.jvm_enhancer.boot
  set VESTIGE_ARGS=!VESTIGE_ARGS! "%DATADIR%" "%DATADIR%\jvm_enhancer.properties" fr.gaellalire.vestige.edition.maven_main_launcher
) else (
  set VESTIGE_ARGS=-jar "%DATADIR%\lib\vestige.core-${vestige.core.version}.jar" --before javax/xml/bind/.* frcp "%DATADIR%" "%DATADIR%\windows-classpath.txt" fr.gaellalire.vestige.jvm_enhancer.boot.JVMEnhancer
  set VESTIGE_ARGS=!VESTIGE_ARGS! "%DATADIR%" "%DATADIR%\jvm_enhancer.properties" fr.gaellalire.vestige.edition.maven_main_launcher.MavenMainLauncher
)

set VESTIGE_ARGS=%VESTIGE_ARGS% "%MAVEN_LAUNCHER_FILE%" "%MAVEN_SETTINGS_FILE%" "%MAVEN_RESOLVER_CACHE_FILE%" "%VESTIGE_BASE%" "%VESTIGE_DATA%" %VESTIGE_SECURITY% %VESTIGE_LISTENER_PORT%

"%JAVA%" %VESTIGE_OPTS% %VESTIGE_ARGS% || exit /B 2

endlocal
