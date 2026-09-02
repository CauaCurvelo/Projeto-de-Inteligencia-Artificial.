@echo off
echo ===============================================
echo Configurando o Projeto Sentinela (Primeiro Uso)
echo ===============================================
echo.
echo 1. Verificando instalacao do Java...
java -version
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Java nao encontrado! Instale o JDK 17+.
    pause
    exit /b
)
echo.

echo 2. Verificando instalacao do Maven...
mvn -version
if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Maven nao encontrado! Instale o Apache Maven e coloque nas variaveis de ambiente.
    pause
    exit /b
)
echo.

echo 3. Baixando dependencias e compilando o projeto...
mvn clean compile
echo.

echo ===============================================
echo SETUP CONCLUIDO COM SUCESSO!
echo O projeto ja pode ser executado.
echo Para rodar o jogo, utilize o arquivo: start_game.bat
echo ===============================================
pause
