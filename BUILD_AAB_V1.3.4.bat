@echo off
chcp 65001 >nul
echo ========================================
echo   BUILD AAB V1.3.4 (Code: 137)
echo ========================================
echo.

REM Verificar se a versão está correta
echo Verificando versão no build.gradle.kts...
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

echo Versão verificada: 1.3.4 (Code: 137)
echo.

REM Limpar build anterior
echo Limpando build anterior...
call gradlew.bat clean
if errorlevel 1 (
    echo ERRO ao limpar build
    pause
    exit /b 1
)

echo.
echo Iniciando build do AAB...
echo.

REM Build do AAB
call gradlew.bat bundleRelease
if errorlevel 1 (
    echo.
    echo ERRO no build do AAB
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   BUILD CONCLUIDO COM SUCESSO!
echo ========================================
echo.
echo AAB gerado em: app\build\outputs\bundle\release\app-release.aab
echo Versão: 1.3.4 (Code: 137)
echo.
pause
