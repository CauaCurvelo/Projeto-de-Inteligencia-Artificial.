@echo off
cd /d "%~dp0"
echo ====================================================
echo  Sentinela: Instinto Selvagem - Compilar & Executar
echo ====================================================
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Compilando e iniciando o jogo...
call mvn javafx:run
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERRO] Ocorreu uma falha ao compilar ou rodar o jogo.
    pause
)
