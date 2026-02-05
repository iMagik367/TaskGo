@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo BUILD AAB V1.2.5 - TaskGo App
echo ========================================
echo.
echo Versão: 1.2.5 (Code: 125)
echo.
echo Mudanças nesta versão:
echo   - Correção: Exclusão de conta detecta e força logout imediato
echo   - Correção: Dialog Google aparece sempre que necessário
echo   - Correção: Usuários salvos em locations/{locationId}/users
echo   - Correção: Removidos campos de endereço do cadastro
echo   - Correção: Select box de cidade/estado implementado
echo   - Correção: Travamento no cadastro normal resolvido
echo   - Correção: GPS não trava mais (timeouts reduzidos, cache priorizado)
echo   - Correção: Erros de compilação no SignUpScreen resolvidos
echo.
echo ========================================
echo.

cd /d "%~dp0"

:: Verificar se estamos no diretório correto
if not exist "app\build.gradle.kts" (
    echo ERRO: Execute este script na raiz do projeto TaskGoApp
    pause
    exit /b 1
)

:: Verificar se o keystore existe
if not exist "keystore.properties" (
    echo ERRO: keystore.properties não encontrado!
    echo É necessário configurar o keystore antes de gerar o AAB.
    pause
    exit /b 1
)

echo [1/5] Limpando build anterior...
call gradlew.bat clean --no-daemon
if errorlevel 1 (
    echo AVISO: Erro ao limpar (pode ser normal se não houver build anterior)
    echo.
)

echo.
echo [2/5] Verificando versão no build.gradle.kts...
findstr /C:"versionCode = 125" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionCode não está configurado como 125
    pause
    exit /b 1
)

findstr /C:"versionName = \"1.2.5\"" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionName não está configurado como 1.2.5
    pause
    exit /b 1
)

echo Versão confirmada: 1.2.5 (Code: 125)

echo.
echo [3/5] Compilando release...
echo Isso pode levar alguns minutos...
call gradlew.bat :app:bundleRelease --no-daemon
if errorlevel 1 (
    echo ERRO: Falha ao compilar release
    pause
    exit /b 1
)

echo.
echo [4/5] Verificando AAB gerado...
set AAB_PATH=app\build\outputs\bundle\release\app-release.aab
if not exist "%AAB_PATH%" (
    echo ERRO: AAB não foi gerado em %AAB_PATH%
    pause
    exit /b 1
)

:: Obter tamanho do arquivo
for %%A in ("%AAB_PATH%") do set AAB_SIZE=%%~zA
set /a AAB_SIZE_MB=!AAB_SIZE!/1048576

echo.
echo [5/5] AAB gerado com sucesso!
echo.
echo ========================================
echo INFORMAÇÕES DO BUILD
echo ========================================
echo Arquivo: %AAB_PATH%
echo Tamanho: !AAB_SIZE_MB! MB
echo Versão: 1.2.5 (Code: 125)
echo.
echo ========================================
echo PRÓXIMOS PASSOS
echo ========================================
echo 1. Verificar o AAB em: %AAB_PATH%
echo 2. Fazer upload no Google Play Console
echo 3. Testar em ambiente de produção
echo.
echo Build concluído com sucesso!
pause
