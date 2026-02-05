@echo off
echo ========================================
echo Building AAB Release Bundle
echo Padronizacao Completa - Frontend/Backend
echo ========================================
echo.
echo Version: 1.0.98 (Code: 98)
echo.
echo Mudancas nesta versao:
echo   - Backend: Paths padronizados para locations/{locationId}/{collection}
echo   - Frontend: Leitura exclusiva de locations/{locationId}/{collection}
echo   - Removidos fallbacks para colecoes globais
echo   - Queries nao bloqueiam se locationId nao existir
echo   - Reexecucao automatica quando locationId fica disponivel
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

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD COMPLETADO COM SUCESSO!
    echo ========================================
    echo.
    echo AAB Location:
    echo   app\build\outputs\bundle\release\app-release.aab
    echo.
    echo Versao: 1.0.98 (Code: 98)
    echo.
    echo Pronto para upload no Google Play Console!
    echo.
) else (
    echo.
    echo ========================================
    echo BUILD FALHOU!
    echo ========================================
    echo.
    echo Verifique os erros acima.
    echo.
)

pause
