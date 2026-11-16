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
echo Pronto! Arquivos gerados para envio em dist\:
echo   - dist\InstintoSelvagem-Instalador.exe (Executavel unico autoextrator, ~18 MB)
echo   - dist\InstintoSelvagem-Windows.zip     (Pacote compactado tradicional, ~27 MB)
pause
