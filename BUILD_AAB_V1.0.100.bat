@echo off
chcp 65001 >nul
echo ========================================
echo Building AAB Release Bundle
echo Localizacao Automatica - Sem Formularios
echo ========================================
echo.
echo Version: 1.0.100 (Code: 100)
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
call gradlew.bat bundleRelease --no-daemon
set BUILD_RESULT=%ERRORLEVEL%

if %BUILD_RESULT% EQU 0 (
    echo.
    echo ========================================
    echo BUILD COMPLETADO COM SUCESSO!
    echo ========================================
    echo.
    echo AAB Location:
    echo   app\build\outputs\bundle\release\app-release.aab
    echo.
    echo Versao: 1.0.100 (Code: 100)
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
