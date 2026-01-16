@echo off
echo ========================================
echo Limpando e Building AAB Release Bundle
echo Versao: 1.0.79 (Code: 80)
echo ========================================
echo.

cd /d "%~dp0"

echo [1/5] Parando daemons do Gradle...
call .\gradlew.bat --stop >nul 2>&1
timeout /t 3 /nobreak >nul

echo [2/5] Verificando e encerrando processos que possam estar bloqueando arquivos...
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

echo [3/5] Tentando remover diretorios intermediarios travados...
REM Tentar remover arquivo R.jar travado especifico
set "LOCKED_FILE=app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\release\processReleaseResources\R.jar"
if exist "%LOCKED_FILE%" (
    echo    Tentando remover R.jar travado...
    REM Tentar deletar o arquivo diretamente
    del /F /Q "%LOCKED_FILE%" >nul 2>&1
    if exist "%LOCKED_FILE%" (
        echo    Arquivo ainda travado, aguardando liberacao...
        timeout /t 3 /nobreak >nul
        del /F /Q "%LOCKED_FILE%" >nul 2>&1
    )
)
REM Tentar remover diretorio completo se ainda existir
set "LOCKED_DIR=app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\release\processReleaseResources"
if exist "%LOCKED_DIR%" (
    rmdir /S /Q "%LOCKED_DIR%" >nul 2>&1
    if exist "%LOCKED_DIR%" (
        timeout /t 2 /nobreak >nul
        rmdir /S /Q "%LOCKED_DIR%" >nul 2>&1
    )
)
REM Tentar remover todo o diretorio intermediates se necessario
if exist "app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar" (
    rmdir /S /Q "app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar" >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo [4/5] Limpando build anterior...
call .\gradlew.bat clean --no-daemon
if errorlevel 1 (
    echo Aviso: Clean falhou, tentando remocao manual...
    REM Aguardar um pouco mais para garantir que processos foram encerrados
    timeout /t 3 /nobreak >nul
    REM Tentar remover todo o diretorio build se clean falhou
    if exist "app\build" (
        echo    Tentando remover app\build manualmente...
        rmdir /S /Q "app\build" >nul 2>&1
        if exist "app\build" (
            echo    Aviso: Nao foi possivel remover app\build completamente
            echo    Continuando com build - o Gradle tentara sobrescrever arquivos...
        ) else (
            echo    app\build removido com sucesso manualmente
        )
    )
    timeout /t 2 /nobreak >nul
)
echo Clean concluido, continuando para o build...

echo.
echo [5/5] Iniciando build do AAB...
REM Verificar novamente se o arquivo R.jar ainda existe antes do build
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
    ) else (
        echo R.jar removido com sucesso antes do build
    )
)
REM Executar build com flags adicionais para evitar problemas com arquivos travados
REM --rerun-tasks força a reexecucao de todas as tasks, evitando cache de arquivos travados
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


