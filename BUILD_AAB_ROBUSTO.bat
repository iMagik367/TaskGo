@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Building AAB Release Bundle (Robust)
echo ========================================
echo.
echo Version: 1.0.79 (Code: 80)
echo Data: %DATE% %TIME%
echo.

cd /d "%~dp0"

REM Criar diretorio de logs se nao existir
if not exist "build_logs" mkdir build_logs

REM Nome do arquivo de log com timestamp
set "LOG_FILE=build_logs\build_%DATE:~-4,4%%DATE:~-7,2%%DATE:~-10,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%.log"
set "LOG_FILE=!LOG_FILE: =0!"

echo Log será salvo em: !LOG_FILE!
echo.

REM Executar build e redirecionar output para log
echo Iniciando build... Aguarde, pode levar 10-20 minutos...
echo.

call .\gradlew.bat bundleRelease > "!LOG_FILE!" 2>&1
set BUILD_EXIT_CODE=%ERRORLEVEL%

echo.
echo ========================================
if %BUILD_EXIT_CODE% EQU 0 (
    echo BUILD COMPLETED SUCCESSFULLY!
    echo ========================================
    echo.
    echo AAB Location:
    echo app\build\outputs\bundle\release\app-release.aab
    echo.
    if exist "app\build\outputs\bundle\release\app-release.aab" (
        for %%F in ("app\build\outputs\bundle\release\app-release.aab") do (
            echo Tamanho: %%~zF bytes
            echo Data: %%~tF
        )
    )
) else (
    echo BUILD FAILED!
    echo ========================================
    echo.
    echo Exit Code: %BUILD_EXIT_CODE%
    echo Verifique o log: !LOG_FILE!
    echo.
)

echo.
pause




