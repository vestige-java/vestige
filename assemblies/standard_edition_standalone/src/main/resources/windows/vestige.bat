${at}echo off

setlocal

set DIRNAME=%~dp0
if "%DIRNAME:~-1%" == "\" (set DIRNAME=%DIRNAME:~0,-1%)

set LIBDIR=%DIRNAME%\lib
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

if not exist "%VESTIGE_BASE%" md "%VESTIGE_BASE%"

if not defined VESTIGE_DATA set VESTIGE_DATA=%DIRNAME%\data

if not defined VESTIGE_LISTENER_PORT set VESTIGE_LISTENER_PORT=0

if not defined VESTIGE_OPTS set VESTIGE_OPTS=%JAVA_OPTS%

if defined VESTIGE_DEBUG set VESTIGE_OPTS=%VESTIGE_OPTS% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000

set MAVEN_LAUNCHER_FILE=%VESTIGE_BASE%\m2\vestige-se.xml
if not exist "%MAVEN_LAUNCHER_FILE%" (
  md "%VESTIGE_BASE%\m2"
  copy /Y "%CONFDIR%\m2\vestige-se.xml" "%MAVEN_LAUNCHER_FILE%" > nul
)

set MAVEN_SETTINGS_FILE=%VESTIGE_BASE%\m2\settings.xml

set MAVEN_RESOLVER_CACHE_FILE=%VESTIGE_DATA%\m2\resolver-cache.ser

set LOGBACK_CONFIGURATION_FILE=%VESTIGE_BASE%\logback.xml
if not exist "%LOGBACK_CONFIGURATION_FILE%" copy /Y "%CONFDIR%\logback.xml" "%LOGBACK_CONFIGURATION_FILE%" > nul

set VESTIGE_OPTS=%VESTIGE_OPTS% -Dvestige.mavenRepository="%DIRNAME%\repository" -Djava.util.logging.manager=fr.gaellalire.vestige.core.logger.JULLogManager -Dlogback.logsDirectory="%VESTIGE_BASE%\logs" -Dlogback.configurationFile="%LOGBACK_CONFIGURATION_FILE%"

"%JAVA%" %VESTIGE_OPTS% -jar "%LIBDIR%\vestige.core-${vestige.core.version}.jar" "%LIBDIR%\vestige.assemblies.standard_edition_bootstrap-${project.version}-jar-with-dependencies.jar" fr.gaellalire.vestige.jvm_enhancer.JVMEnhancer fr.gaellalire.vestige.resolver.maven.VestigeMavenResolver "%MAVEN_LAUNCHER_FILE%" "%MAVEN_SETTINGS_FILE%" "%MAVEN_RESOLVER_CACHE_FILE%" "%VESTIGE_BASE%" "%VESTIGE_DATA%" %VESTIGE_LISTENER_PORT% || exit /B 2

endlocal
