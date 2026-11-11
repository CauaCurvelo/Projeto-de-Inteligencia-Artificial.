@echo off
cd /d "%~dp0"
echo ====================================================
echo  Iniciando Instinto Selvagem (Wild Instinct)...
echo ====================================================
set "JAVA_BIN=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\java.exe"
set "MODULE_PATH=target\classes;C:\Users\pedro\.m2\repository\org\openjfx\javafx-base\17.0.6\javafx-base-17.0.6-win.jar;C:\Users\pedro\.m2\repository\org\openjfx\javafx-controls\17.0.6\javafx-controls-17.0.6-win.jar;C:\Users\pedro\.m2\repository\org\openjfx\javafx-graphics\17.0.6\javafx-graphics-17.0.6-win.jar"

"%JAVA_BIN%" --module-path "%MODULE_PATH%" --add-modules br.edu.unex.sentinela -m br.edu.unex.sentinela/br.edu.unex.sentinela.app.Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo Ocorreu um erro ao executar. Pressione qualquer tecla para fechar.
    pause
)
