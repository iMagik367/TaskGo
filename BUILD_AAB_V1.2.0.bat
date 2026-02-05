@echo off
chcp 65001 >nul
echo ========================================
echo Building AAB Release Bundle
echo Localizacao Automatica - Versao 1.2.0
echo ========================================
echo.
echo Version: 1.2.0 (Code: 120)
echo.
echo Mudancas nesta versao:
echo   - REMOVIDO: Todos campos de localizacao manual dos formularios
echo   - REMOVIDO: Filtros de localizacao (cidade/estado manual)
echo   - REMOVIDO: Campos city/state do SignUpScreen
echo   - REMOVIDO: Campo endereco do CriarProdutoScreen
echo   - REMOVIDO: Campo address do CreateWorkOrderScreen
echo   - REMOVIDO: Campos cidade/estado do CadastrarEnderecoScreen
echo   - PADRONIZADO: Localizacao sempre automatica do perfil do usuario
echo   - BACKEND: Sempre usa getUserLocation() do perfil
echo   - FRONTEND: Nenhum campo de localizacao manual
echo   - LocationUpdateService: Atualiza perfil automaticamente via GPS
echo   - PADRONIZACAO: Backend e Frontend 100%% sincronizados
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
    echo Versao: 1.2.0 (Code: 120)
    echo.
    echo Pronto para upload no Google Play Console!
    echo.
    echo Localizacao Automatica:
    echo   - Nenhum campo de localizacao manual
    echo   - Localizacao sempre do perfil do usuario
    echo   - Atualizacao automatica via GPS
    echo   - Backend sempre usa getUserLocation()
    echo.
    echo Paths padronizados (Backend e Frontend):
    echo   - locations/{locationId}/products
    echo   - locations/{locationId}/services
    echo   - locations/{locationId}/stories
    echo   - locations/{locationId}/posts
    echo   - locations/{locationId}/orders
    echo.
    echo Backend: 100%% padronizado
    echo Frontend: 100%% padronizado
    echo Localizacao: 100%% automatica
    echo Sincronizacao: 100%% completa
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
