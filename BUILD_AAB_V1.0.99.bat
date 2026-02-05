@echo off
chcp 65001 >nul
echo ========================================
echo Building AAB Release Bundle
echo Padronizacao Completa - Frontend/Backend
echo ========================================
echo.
echo Version: 1.0.99 (Code: 99)
echo.
echo Mudancas nesta versao:
echo   - Backend: 100%% padronizado - todas operacoes usam locations/{locationId}/{collection}
echo   - Frontend: 100%% padronizado - todas leituras usam locations/{locationId}/{collection}
echo   - CORRIGIDO: Backend agora grava e le de locations/{locationId}/{collection}
echo   - CORRIGIDO: Removidas todas referencias a colecoes globais no backend
echo   - CORRIGIDO: Trigger onServiceOrderCreated atualizado para locations/{locationId}/orders
echo   - CORRIGIDO: Payments, webhooks, notifications agora usam paths corretos
echo   - CORRIGIDO: deleteAccount removido queries em colecoes globais
echo   - Backend e Frontend 100%% sincronizados
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
    echo Versao: 1.0.99 (Code: 99)
    echo.
    echo Pronto para upload no Google Play Console!
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
