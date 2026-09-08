@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "JAVA_OPTS=-Xms512M -Xmx2G --enable-native-access=ALL-UNNAMED"

set "VER_FILE=%TEMP%\icestom-java-version.txt"

call :find_jar
if not defined JAR (
    echo.
    echo   No IceStom jar found.
    echo.
    echo   Put IceStom-^<version^>.jar next to this script, or build one:
    echo       gradlew build
    echo.
    pause
    exit /b 1
)

call :find_java
if not defined JAVA_CMD (
    echo.
    echo   No Java 25 or newer found.
    echo.
    echo   IceStom needs JDK 25 - Minestom is compiled for it, and the server's entrypoint
    echo   relies on Java 25 accepting a non-public main method.
    echo.
    echo   Install a JDK 25, or point ICESTOM_JAVA at one:
    echo       set ICESTOM_JAVA=C:\path\to\jdk-25\bin\java.exe
    echo.
    pause
    exit /b 1
)

echo Starting IceStom
echo   jar:  !JAR!
echo   java: !JAVA_CMD!
echo.

"!JAVA_CMD!" %JAVA_OPTS% -jar "!JAR!" %*

set "EXIT_CODE=!ERRORLEVEL!"

if not "!EXIT_CODE!"=="0" (
    echo.
    echo IceStom exited with code !EXIT_CODE!.
    pause
)

exit /b !EXIT_CODE!

:find_jar
set "JAR="
call :scan_jars "%~dp0"
if defined JAR exit /b 0
call :scan_jars "%~dp0..\build\libs\"
exit /b 0

:scan_jars
if not exist "%~1" exit /b 0
for /f "delims=" %%f in ('dir /b /o-d "%~1IceStom-*.jar" 2^>nul') do (
    if not defined JAR call :take_jar "%~1%%f"
)
exit /b 0

:take_jar
echo %~n1 | findstr /i "plain" >nul && exit /b 0
set "JAR=%~f1"
exit /b 0

:find_java
set "JAVA_CMD="
if defined ICESTOM_JAVA call :check_java "%ICESTOM_JAVA%"
if defined JAVA_CMD exit /b 0

if defined JAVA_HOME call :check_java "%JAVA_HOME%\bin\java.exe"
if defined JAVA_CMD exit /b 0

call :check_java "java"
if defined JAVA_CMD exit /b 0

call :scan_jdks "%USERPROFILE%\.jdks"
if defined JAVA_CMD exit /b 0

call :scan_jdks "%USERPROFILE%\.gradle\jdks"
if defined JAVA_CMD exit /b 0

call :scan_jdks "%ProgramFiles%\Eclipse Adoptium"
if defined JAVA_CMD exit /b 0

call :scan_jdks "%ProgramFiles%\Java"
exit /b 0

:scan_jdks
if not exist "%~1" exit /b 0
for /d %%d in ("%~1\*") do (
    if not defined JAVA_CMD call :check_java "%%~fd\bin\java.exe"
)
exit /b 0

:check_java
set "VER="
set "MAJOR="

if not "%~1"=="java" if not exist "%~1" exit /b 0

"%~1" -version > "%VER_FILE%" 2>&1
if errorlevel 1 goto :check_java_done

for /f "tokens=3" %%v in ('findstr /i " version " "%VER_FILE%" 2^>nul') do set "VER=%%~v"
if not defined VER goto :check_java_done

for /f "delims=.-+_ " %%m in ("!VER!") do set "MAJOR=%%m"
if not defined MAJOR goto :check_java_done

if !MAJOR! GEQ 25 set "JAVA_CMD=%~1"

:check_java_done
del "%VER_FILE%" >nul 2>&1
exit /b 0
