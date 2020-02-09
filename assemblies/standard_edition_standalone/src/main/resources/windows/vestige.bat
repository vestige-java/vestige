${at}echo off

setlocal enableextensions enabledelayedexpansion

set "DIRNAME=%~dp0"
if [%DIRNAME:~-1%] == [\] set "DIRNAME=%DIRNAME:~0,-1%"

set "DATADIR=%DIRNAME%"
set "CONFDIR=%DIRNAME%"

if defined JAVA goto :java_found

for %%I in (java.exe) do set "JAVA=%%~$PATH:I"
if defined JAVA goto :java_found

if defined JAVA_HOME goto :java_home_found

echo Unable to start a JVM : %%JAVA%% is not set and java.exe is not in PATH and %%JAVA_HOME%% is not set
exit /B 1

:java_home_found
set "JAVA=%JAVA_HOME%\bin\java.exe"

:java_found

if not defined JAVA_OPTS set JAVA_OPTS=-Djava.net.useSystemProxies=true

if not defined VESTIGE_CONFIG set "VESTIGE_CONFIG=%DIRNAME%\config"

if not exist "%VESTIGE_CONFIG%" (
  md "%VESTIGE_CONFIG%"
  call "%DIRNAME%\deepCopy.bat" "%CONFDIR%\template" "%VESTIGE_CONFIG%"
)

if not defined VESTIGE_DATA set "VESTIGE_DATA=%DIRNAME%\data"

if not defined VESTIGE_CACHE set "VESTIGE_CACHE=%DIRNAME%\cache"

if not defined VESTIGE_SECURITY set VESTIGE_SECURITY=true

if not defined VESTIGE_LISTENER_PORT set VESTIGE_LISTENER_PORT=0

if not defined VESTIGE_OPTS set VESTIGE_OPTS=%JAVA_OPTS%

if defined VESTIGE_DEBUG (
  if not defined VESTIGE_DEBUG_SUSPEND set VESTIGE_DEBUG_SUSPEND=n
  if not defined VESTIGE_DEBUG_PORT set VESTIGE_DEBUG_PORT=8000
  set "VESTIGE_OPTS=%VESTIGE_OPTS% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=!VESTIGE_DEBUG_SUSPEND!,address=!VESTIGE_DEBUG_PORT!"
)

if defined VESTIGE_CONSOLE_ENCODING (
  if [%VESTIGE_CONSOLE_ENCODING%] == [UTF-16LE] (
    if defined DISABLE_JVM_ENCODING_WORKAROUND (
      set VESTIGE_OPTS=%VESTIGE_OPTS% -Dconsole.encoding=%VESTIGE_CONSOLE_ENCODING% -Dsun.stdout.encoding=%VESTIGE_CONSOLE_ENCODING% -Dsun.stderr.encoding=%VESTIGE_CONSOLE_ENCODING%
    ) else (
      if not defined MB2WC_ARGS (
        if not defined DISABLE_MB2WC_UTF8 (
          set VESTIGE_OPTS=%VESTIGE_OPTS% -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
          set MB2WC_ARGS=65001
        )
      )
    )
  ) else (
    set VESTIGE_OPTS=%VESTIGE_OPTS% -Dconsole.encoding=%VESTIGE_CONSOLE_ENCODING% -Dsun.stdout.encoding=%VESTIGE_CONSOLE_ENCODING% -Dsun.stderr.encoding=%VESTIGE_CONSOLE_ENCODING%
    set DISABLE_JVM_ENCODING_WORKAROUND=yes
  )
) else (
  set DISABLE_JVM_ENCODING_WORKAROUND=yes
)

set "MAVEN_SETTINGS_FILE=%VESTIGE_CONFIG%\m2\settings.xml"

set "LOGBACK_LOGS_DIRECTORY=%VESTIGE_CACHE%\logs"

if not exist "%LOGBACK_LOGS_DIRECTORY%" md "%LOGBACK_LOGS_DIRECTORY%"

set "LOGBACK_CONFIGURATION_FILE=%VESTIGE_CONFIG%\logback.xml"

if not exist "%LOGBACK_CONFIGURATION_FILE%" set "LOGBACK_CONFIGURATION_FILE=%CONFDIR%\logback.xml"

set "MAVEN_CACERTS=%VESTIGE_CONFIG%\cacert.jks"

if not exist "%MAVEN_CACERTS%" set "MAVEN_CACERTS=%CONFDIR%\cacert.jks"

set VESTIGE_OPTS=%VESTIGE_OPTS% -Dvestige.mavenRepository="%DATADIR%\repository" -Djava.util.logging.manager=fr.gaellalire.vestige.core.logger.JULLogManager

set VESTIGE_CORE_FILE_ENCODING=UTF-8

set "VESTIGE_CORE_RELATIVE_DIRECTORY=%DATADIR%"

"%JAVA%" --add-modules java.base -version 2> nul > nul
if %ERRORLEVEL% equ 0 (
  set "VESTIGE_CORE_MODULEPATH_FILE=%DATADIR%\windows-classpath.txt"
  set VESTIGE_OPTS=%VESTIGE_OPTS% --add-modules ALL-DEFAULT --patch-module "java.base=%DATADIR%\lib\moduleEncapsulationBreaker.jar"
  set VESTIGE_ARGS=-p "%DATADIR%\lib\vestige.core-${vestige.core.version}.jar" -m fr.gaellalire.vestige.core --env-to-prop LOGBACK_LOGS_DIRECTORY logback.logsDirectory --env-to-prop LOGBACK_CONFIGURATION_FILE logback.configurationFile --add-modules fr.gaellalire.vestige.edition.maven_main_launcher emp fr.gaellalire.vestige.jvm_enhancer.boot
  set VESTIGE_ARGS=!VESTIGE_ARGS! "%DATADIR%" "%DATADIR%\jvm_enhancer.properties" fr.gaellalire.vestige.edition.maven_main_launcher
  set "MAVEN_LAUNCHER_FILE=%DATADIR%\m2\vestige-se.xml"
  set "MAVEN_RESOLVER_CACHE_FILE=%VESTIGE_CACHE%\m2\resolver-cache.ser"
) else (
  set "VESTIGE_CORE_CLASSPATH_FILE=%DATADIR%\windows-classpath-6to8.txt"
  set VESTIGE_ARGS=-jar "%DATADIR%\lib\vestige.core-${vestige.core.version}.jar" --env-to-prop LOGBACK_LOGS_DIRECTORY logback.logsDirectory --env-to-prop LOGBACK_CONFIGURATION_FILE logback.configurationFile --before javax/xml/bind/.* ecp fr.gaellalire.vestige.jvm_enhancer.boot.JVMEnhancer
  set VESTIGE_ARGS=!VESTIGE_ARGS! "%DATADIR%" "%DATADIR%\jvm_enhancer.properties" fr.gaellalire.vestige.edition.maven_main_launcher.MavenMainLauncher
  set "MAVEN_LAUNCHER_FILE=%DATADIR%\m2\vestige-se-6to8.xml"
  set "MAVEN_RESOLVER_CACHE_FILE=%VESTIGE_CACHE%\m2\resolver-cache-6to8.ser"
)

if defined DISABLE_JVM_ENCODING_WORKAROUND (
  "%JAVA%" %VESTIGE_OPTS% %VESTIGE_ARGS%
) else (
  if defined MB2WC_ARGS (
    "%JAVA%" %VESTIGE_OPTS% %VESTIGE_ARGS% 2>&1 | "%DATADIR%\mb2wc.exe" %MB2WC_ARGS%
  ) else (
    "%JAVA%" %VESTIGE_OPTS% %VESTIGE_ARGS% 2>&1 | "%DATADIR%\mb2wc.exe"
  )
)

endlocal
