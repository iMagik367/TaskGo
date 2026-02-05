@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   BUILD AAB V1.4.0 - TaskGo App
echo ========================================
echo.

set VERSION_CODE=143
set VERSION_NAME=1.4.0

echo Verificando versão no build.gradle.kts...
findstr /C:"versionCode = %VERSION_CODE%" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionCode não está configurado como %VERSION_CODE% no build.gradle.kts
    pause
    exit /b 1
)

findstr /C:"versionName = \"%VERSION_NAME%\"" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionName não está configurado como "%VERSION_NAME%" no build.gradle.kts
    pause
    exit /b 1
)

echo ✓ Versão verificada: %VERSION_NAME% (Code: %VERSION_CODE%)
echo.

echo Limpando build anterior...
call gradlew.bat clean --no-daemon
if errorlevel 1 (
    echo ERRO: Falha ao limpar build anterior
    pause
    exit /b 1
)

echo.
echo Compilando código...
call gradlew.bat compileReleaseKotlin --no-daemon
if errorlevel 1 (
    echo ERRO: Falha na compilação
    pause
    exit /b 1
)

echo.
echo Gerando AAB (Android App Bundle)...
call gradlew.bat bundleRelease --no-daemon
if errorlevel 1 (
    echo ERRO: Falha ao gerar AAB
    pause
    exit /b 1
)

echo.
echo ========================================
echo   BUILD CONCLUÍDO COM SUCESSO!
echo ========================================
echo.
echo Arquivo AAB gerado em:
echo app\build\outputs\bundle\release\app-release.aab
echo.
echo Versão: %VERSION_NAME% (Code: %VERSION_CODE%)
echo.
pause
