@echo off
chcp 65001 >nul
echo ========================================
echo Building AAB Release Bundle
echo Sistema GPS Robusto - Nunca Falha
echo ========================================
echo.
echo Version: 1.2.4 (Code: 124)
echo.
echo Mudancas nesta versao:
echo   - GPS: LocationStateManager corrigido - sempre emite Ready
echo   - GPS: callbackFlow com awaitClose para garantir emissao
echo   - GPS: Fallback automatico para Brasilia/DF quando GPS falha
echo   - GPS: Sistema robusto com retry (10 tentativas, backoff exponencial)
echo   - GPS: Cache persistente de ultima localizacao valida
echo   - GPS: Fallback para cache, sistema Android e Brasilia
echo   - GPS: Funcoes garantidas que nunca retornam null
echo   - Backend: Recebe GPS do frontend (latitude, longitude, city, state)
echo   - Frontend: Obtem GPS no momento da operacao (nao depende do perfil)
echo   - LocationStateManager: Obtem GPS automaticamente quando necessario
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
    echo Versao: 1.2.4 (Code: 124)
    echo.
    echo Pronto para upload no Google Play Console!
    echo.
    echo Sistema GPS Robusto:
    echo   - Retry: 10 tentativas com backoff exponencial
    echo   - Cache: Ultima localizacao valida persistida
    echo   - Fallback: Cache -^> Sistema Android -^> Brasilia
    echo   - Garantia: GPS nunca falha, sempre retorna localizacao
    echo   - LocationStateManager: Sempre emite Ready (com fallback)
    echo.
    echo Backend/Frontend:
    echo   - GPS obtido no momento da operacao
    echo   - Backend recebe GPS do frontend
    echo   - Nao depende de city/state no perfil
    echo   - Mudanca de cidade detectada automaticamente
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
