@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   BUILD AAB V1.3.4 (Code: 137) - ROBUSTO
echo ========================================
echo.

cd /d "%~dp0"

REM Verificar se estamos no diretório correto
if not exist "app\build.gradle.kts" (
    echo ERRO: Execute este script na raiz do projeto TaskGoApp
    pause
    exit /b 1
)

REM Verificar versão
echo [1/6] Verificando versão...
findstr /C:"versionCode = 137" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionCode não está definido como 137
    pause
    exit /b 1
)

findstr /C:"versionName = \"1.3.4\"" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionName não está definido como 1.3.4
    pause
    exit /b 1
)

echo Versão confirmada: 1.3.4 (Code: 137)
echo.

REM Parar daemons anteriores
echo [2/6] Parando daemons Gradle anteriores...
call gradlew.bat --stop
echo.

REM Limpar build anterior
echo [3/6] Limpando build anterior...
call gradlew.bat clean --no-daemon
if errorlevel 1 (
    echo AVISO: Erro ao limpar (continuando mesmo assim)
)
echo.

REM Build do AAB
echo [4/6] Compilando AAB (isso pode levar vários minutos)...
echo Por favor, aguarde...
call gradlew.bat bundleRelease --no-daemon
if errorlevel 1 (
    echo.
    echo ERRO: Falha no build do AAB
    echo.
    pause
    exit /b 1
)

echo.

REM Verificar se o AAB foi gerado
echo [5/6] Verificando AAB gerado...
set AAB_PATH=app\build\outputs\bundle\release\app-release.aab
if not exist "%AAB_PATH%" (
    echo ERRO: AAB não foi gerado em %AAB_PATH%
    echo.
    echo Verificando diretório de saída...
    if exist "app\build\outputs\bundle\release\" (
        dir "app\build\outputs\bundle\release\"
    ) else (
        echo Diretório não existe
    )
    pause
    exit /b 1
)

REM Obter tamanho do arquivo
for %%A in ("%AAB_PATH%") do set AAB_SIZE=%%~zA
set /a AAB_SIZE_MB=!AAB_SIZE!/1048576

echo.
echo [6/6] AAB gerado com sucesso!
echo.
echo ========================================
echo   BUILD CONCLUIDO COM SUCESSO!
echo ========================================
echo.
echo Arquivo: %AAB_PATH%
echo Tamanho: !AAB_SIZE_MB! MB
echo Versão: 1.3.4 (Code: 137)
echo.
echo ========================================
echo   PRÓXIMOS PASSOS
echo ========================================
echo 1. Verificar o AAB em: %AAB_PATH%
echo 2. Fazer upload no Google Play Console
echo 3. Testar em ambiente de produção
echo.
pause
