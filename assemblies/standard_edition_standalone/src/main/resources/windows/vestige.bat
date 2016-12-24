${at}echo off

setlocal

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

if defined VESTIGE_DEBUG set VESTIGE_OPTS=%VESTIGE_OPTS% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000

set MAVEN_LAUNCHER_FILE=%DATADIR%\m2\vestige-se.xml

set MAVEN_SETTINGS_FILE=%VESTIGE_BASE%\m2\settings.xml

set MAVEN_RESOLVER_CACHE_FILE=%VESTIGE_DATA%\m2\resolver-cache.ser

set LOGBACK_CONFIGURATION_FILE=%VESTIGE_BASE%\logback.xml

set VESTIGE_OPTS=%VESTIGE_OPTS% -Dvestige.mavenRepository="%DATADIR%\repository" -Djava.util.logging.manager=fr.gaellalire.vestige.core.logger.JULLogManager -Dlogback.logsDirectory="%VESTIGE_BASE%\logs" -Dlogback.configurationFile="%LOGBACK_CONFIGURATION_FILE%"

"%JAVA%" --add-modules java.xml.bind -version 2> nul > nul
if %ERRORLEVEL% equ 0 (
  set VESTIGE_OPTS=%VESTIGE_OPTS% --add-modules java.xml.bind --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.logging/java.util.logging=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/sun.security.jca=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.sql/java.sql=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED
)

"%JAVA%" %VESTIGE_OPTS% -jar "%DATADIR%\lib\vestige.core-${vestige.core.version}.jar" frcp "%DATADIR%" "%DATADIR%\windows-classpath.txt" fr.gaellalire.vestige.jvm_enhancer.boot.JVMEnhancer "%DATADIR%" "%DATADIR%/jvm_enhancer.properties"  fr.gaellalire.vestige.resolver.maven.VestigeMavenResolver "%MAVEN_LAUNCHER_FILE%" "%MAVEN_SETTINGS_FILE%" "%MAVEN_RESOLVER_CACHE_FILE%" "%VESTIGE_BASE%" "%VESTIGE_DATA%" %VESTIGE_SECURITY% %VESTIGE_LISTENER_PORT% || exit /B 2

endlocal
