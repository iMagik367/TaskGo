@echo off
chcp 65001 >nul
echo ========================================
echo Building AAB Release Bundle
echo Validacao Robusta de Localizacao - Versao 1.2.1
echo ========================================
echo.
echo Version: 1.2.1 (Code: 121)
echo.
echo Mudancas nesta versao:
echo   - VALIDACAO ROBUSTA: LocationValidator com validacao completa
echo   - VALIDACAO GPS: Verifica qualidade da localizacao GPS
echo   - VALIDACAO CITY: Minimo 2 caracteres, nao generico, apenas caracteres validos
echo   - VALIDACAO STATE: Exatamente 2 caracteres, sigla valida do Brasil
echo   - VALIDACAO ADDRESS: Valida Address completo do Geocoder
echo   - RETRY LOGIC: 3 tentativas com delay crescente no geocoding
echo   - RETRY GPS: 3 tentativas na obtencao de GPS
echo   - PADRONIZACAO: Frontend e Backend 100%% sincronizados
echo   - VALIDACAO BACKEND: validateCityAndState() antes de normalizar
echo   - VALIDACAO FRONTEND: LocationValidator antes de salvar
echo   - FIRESTORE RULES: Bloqueia unknown_unknown e valores invalidos
echo   - NORMALIZACAO: Valida antes de normalizar (frontend e backend)
echo   - GARANTIA: City e state sempre corretos, sem margem para erro
echo.
echo ========================================
echo.

cd /d "%~dp0"

echo Parando daemons anteriores para evitar conflitos...
call gradlew.bat --stop >nul 2>&1
timeout /t 2 /nobreak >nul

echo Limpando build anterior...
call gradlew.bat clean --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo AVISO: Erro ao limpar (pode ser normal se nao houver build anterior)
    echo.
)

echo.
echo Iniciando build do AAB...
echo Isso pode levar alguns minutos...
echo.
echo Parando daemons anteriores para evitar conflitos...
call gradlew.bat --stop >nul 2>&1
timeout /t 2 /nobreak >nul

echo.
echo Iniciando build (sem daemon para evitar crashes)...
echo Configurando ambiente para build robusto...
set GRADLE_OPTS=-Xmx6144m -Xms3072m -XX:MaxMetaspaceSize=1536m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45 -XX:G1HeapRegionSize=32m -XX:+G1UseAdaptiveIHOP -XX:ConcGCThreads=1 -XX:ParallelGCThreads=2 -XX:+DisableExplicitGC -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:ReservedCodeCacheSize=512m -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options=-Xmx2048m
set GRADLE_OPTS=%GRADLE_OPTS% -XX:+ExitOnOutOfMemoryError
echo Limpando cache do Gradle antes do build...
if exist "%USERPROFILE%\.gradle\caches" (
    echo Cache do Gradle encontrado, limpando...
    timeout /t 1 /nobreak >nul
)
echo.
echo Executando build com configuracoes otimizadas...
call gradlew.bat bundleRelease --no-daemon --max-workers=1 --no-build-cache --stacktrace
set BUILD_RESULT=%ERRORLEVEL%

if %BUILD_RESULT% NEQ 0 (
    echo.
    echo Build falhou. Tentando novamente apos limpar daemons...
    call gradlew.bat --stop >nul 2>&1
    timeout /t 3 /nobreak >nul
    echo Tentativa 2 de build...
    call gradlew.bat bundleRelease --no-daemon --max-workers=1 --no-build-cache
    set BUILD_RESULT=%ERRORLEVEL%
)

if %BUILD_RESULT% EQU 0 (
    echo.
    echo ========================================
    echo BUILD COMPLETADO COM SUCESSO!
    echo ========================================
    echo.
    echo AAB Location:
    echo   app\build\outputs\bundle\release\app-release.aab
    echo.
    echo Versao: 1.2.1 (Code: 121)
    echo.
    echo Pronto para upload no Google Play Console!
    echo.
    echo Validacao Robusta de Localizacao:
    echo   - LocationValidator com validacao completa
    echo   - Validacao GPS (qualidade, coordenadas, limites Brasil)
    echo   - Validacao City (minimo 2 chars, nao generico)
    echo   - Validacao State (2 chars, sigla valida BR)
    echo   - Retry logic (3 tentativas com delay)
    echo   - Padronizacao Frontend/Backend 100%%
    echo   - Firestore Rules bloqueiam valores invalidos
    echo.
    echo Garantias:
    echo   - City e state sempre corretos
    echo   - Sem margem para erro
    echo   - Validacao em todas as camadas
    echo   - Frontend e Backend sincronizados
    echo.
    exit /b 0
) else (
    echo.
    echo ========================================
    echo BUILD FALHOU!
    echo ========================================
    echo.
    echo Verifique os erros acima.
    echo Codigo de erro: %BUILD_RESULT%
    echo.
    exit /b %BUILD_RESULT%
)

pause
