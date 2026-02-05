@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo BUILD AAB V1.2.10 - TaskGo App
echo ========================================
echo.
echo Versão: 1.2.10 (Code: 130)
echo.
echo Mudanças nesta versão:
echo   - Correção: App Check configurado corretamente para RELEASE (Play Integrity)
echo   - Correção: Validação do App Check antes de chamadas de Cloud Functions
echo   - Correção: Login com CPF/CNPJ agora funciona corretamente em produção
echo   - Correção: Logs detalhados para diagnóstico de problemas do App Check
echo   - Correção: Timeout configurado para não bloquear chamadas
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
findstr /C:"versionCode = 130" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionCode não está configurado como 130
    pause
    exit /b 1
)

findstr /C:"versionName = \"1.2.10\"" app\build.gradle.kts >nul
if errorlevel 1 (
    echo ERRO: versionName não está configurado como 1.2.10
    pause
    exit /b 1
)

echo Versão confirmada: 1.2.10 (Code: 130)

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
echo Versão: 1.2.10 (Code: 130)
echo.
echo ========================================
echo PRÓXIMOS PASSOS
echo ========================================
echo 1. Verificar o AAB em: %AAB_PATH%
echo 2. Fazer upload no Google Play Console
echo 3. Testar login com CPF/CNPJ em produção
echo 4. Verificar logs do App Check (Play Integrity)
echo.
echo Build concluído com sucesso!
pause
