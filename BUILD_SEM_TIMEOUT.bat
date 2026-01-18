@echo off
echo ========================================
echo Building AAB Release Bundle
echo Versao: 1.0.86 (Code: 86)
echo ========================================
echo.

cd /d "%~dp0"

echo [1/4] Parando daemons do Gradle antes do build...
call .\gradlew.bat --stop >nul 2>&1
timeout /t 2 /nobreak >nul

echo [2/4] Verificando e encerrando processos que possam bloquear arquivos...
REM Verificar se Android Studio esta rodando (apenas para avisar)
tasklist /FI "IMAGENAME eq studio64.exe" 2>nul | find /I /N "studio64.exe">nul
if "%ERRORLEVEL%"=="0" (
    echo AVISO CRITICO: Android Studio detectado em execucao!
    echo O Android Studio pode estar bloqueando arquivos de build
    echo Se o build falhar, feche o Android Studio completamente e tente novamente
    echo Continuando mesmo assim...
    timeout /t 2 /nobreak >nul
)
REM Encerrar processos Java/Gradle que podem estar bloqueando arquivos (nao encerrar Android Studio)
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM gradle.exe /T >nul 2>&1
timeout /t 3 /nobreak >nul

echo [3/4] Tentando remover arquivos travados especificos...
set "LOCKED_FILE=app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\release\processReleaseResources\R.jar"
if exist "%LOCKED_FILE%" (
    del /F /Q "%LOCKED_FILE%" >nul 2>&1
    if exist "%LOCKED_FILE%" (
        timeout /t 2 /nobreak >nul
        del /F /Q "%LOCKED_FILE%" >nul 2>&1
    )
)
timeout /t 1 /nobreak >nul

echo [4/4] Executando build sem redirecionamento para evitar timeout...
REM Verificar se o arquivo R.jar ainda existe antes do build
set "LOCKED_FILE=app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\release\processReleaseResources\R.jar"
if exist "%LOCKED_FILE%" (
    echo Aviso: R.jar ainda existe, tentando remocao final...
    timeout /t 3 /nobreak >nul
    REM Tentar remover novamente com mais agressividade
    taskkill /F /FI "IMAGENAME eq java.exe" /FI "MEMUSAGE gt 100000" >nul 2>&1
    timeout /t 2 /nobreak >nul
    del /F /Q "%LOCKED_FILE%" >nul 2>&1
    if exist "%LOCKED_FILE%" (
        echo Aviso: Nao foi possivel remover R.jar antes do build
        echo Tentando build com --rerun-tasks para forcar regeneracao...
        echo IMPORTANTE: Se o build falhar, feche o Android Studio completamente e tente novamente
    )
)
REM Executar build com flags adicionais para evitar problemas com arquivos travados
REM --rerun-tasks força a reexecucao de todas as tasks, evitando cache de arquivos travados
REM --no-build-cache desabilita cache de build para evitar conflitos com arquivos travados
call .\gradlew.bat bundleRelease --no-daemon --rerun-tasks --no-build-cache

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
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
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo.
    echo Exit Code: %ERRORLEVEL%
)

pause



