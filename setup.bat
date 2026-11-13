@echo off
cd /d "%~dp0"
echo ===============================================
echo Configurando o Projeto Sentinela (Primeiro Uso)
echo ===============================================
echo.

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo 1. Verificando instalacao do Java 17...
java -version
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Java 17 nao encontrado em "%JAVA_HOME%".
    pause
    exit /b
)
echo.

echo 2. Verificando instalacao do Maven...
mvn -version
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Maven nao encontrado no PATH do sistema.
    pause
    exit /b
)
echo.

echo 3. Baixando dependencias e compilando o projeto...
call mvn clean compile
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Falha na compilacao.
    pause
    exit /b
)
echo.

echo ===============================================
echo SETUP CONCLUIDO COM SUCESSO!
echo O projeto ja pode ser executado.
echo Para rodar o jogo, utilize o arquivo: compiler.bat
echo ===============================================
pause
