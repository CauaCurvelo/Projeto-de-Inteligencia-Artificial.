@echo off
cd /d "%~dp0"
echo ====================================================
echo  Gerando pacote portatil Instinto Selvagem (.zip)...
echo ====================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build_release.ps1"
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERRO] Falha ao gerar o pacote portatil.
    pause
    exit /b %ERRORLEVEL%
)
echo.
echo Pronto! Envie o arquivo em dist\InstintoSelvagem-Windows.zip
pause
