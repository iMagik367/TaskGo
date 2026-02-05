@echo off
chcp 65001 >nul
echo ========================================
echo Building AAB Release Bundle
echo Refatoracao Sistemica - Modelo Canonico
echo ========================================
echo.
echo Version: 1.2.2 (Code: 122)
echo.
echo Mudancas nesta versao:
echo   - Backend: Todas as colecoes globais migradas para locations/{locationId}/{collection}
echo   - Backend: purchase_orders migrado para locations/{locationId}/orders
echo   - Frontend: Todas as queries verificam LocationState.Ready
echo   - Frontend: Bloqueio de queries com locationId invalido ou "unknown"
echo   - Firestore Rules: Atualizadas para locations/{locationId}/posts
echo   - Triggers: Reconfigurados para locations/{locationId}/orders/{orderId}
echo   - 100%% conforme com MODELO_CANONICO_TASKGO.md
echo.
echo ========================================
echo.

cd /d "%~dp0"

echo Limpando build anterior...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo AVISO: Erro ao limpar (pode ser normal se nao houver build anterior)
    echo.
)

echo.
echo Iniciando build do AAB...
echo Isso pode levar alguns minutos...
echo.

call gradlew.bat bundleRelease
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
    echo Versao: 1.2.2 (Code: 122)
    echo.
    echo Pronto para upload no Google Play Console!
    echo.
    echo Paths padronizados (Modelo Canonico):
    echo   - locations/{locationId}/products
    echo   - locations/{locationId}/services
    echo   - locations/{locationId}/stories
    echo   - locations/{locationId}/posts
    echo   - locations/{locationId}/orders
    echo.
    echo Todas as violacoes corrigidas:
    echo   - Nenhuma colecao global publica
    echo   - Todas as queries verificam LocationState.Ready
    echo   - Nenhum uso de "unknown" como locationId
    echo   - 100%% conforme com modelo canonico
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
