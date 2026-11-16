@echo off
cd /d "%~dp0"
echo ====================================================
echo  Iniciando Sentinela: Instinto Selvagem...
echo ====================================================

if not defined JAVA_HOME (
    if exist "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot" (
        set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
    )
)
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16\bin" (
        set "PATH=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16\bin;%PATH%"
    )
)

set "MAVEN_OPTS=-Xms64m -Xmx384m"

call mvn javafx:run
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERRO] Ocorreu uma falha ao rodar o jogo.
    pause
)

