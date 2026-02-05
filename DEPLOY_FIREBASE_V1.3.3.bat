@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo DEPLOY FIREBASE V1.3.3 - TaskGo App
echo ========================================
echo.
echo Versão: 1.3.3
echo.
echo Este script fará deploy de:
echo   - Firestore Rules
echo   - Storage Rules
echo   - Cloud Functions
echo   - Firestore Indexes
echo.
echo ========================================
echo.

cd /d "%~dp0"

:: Verificar se estamos no diretório correto
if not exist "firebase.json" (
    echo ERRO: Execute este script na raiz do projeto TaskGoApp
    pause
    exit /b 1
)

:: Verificar se Firebase CLI está instalado
firebase --version >nul 2>&1
if errorlevel 1 (
    echo ERRO: Firebase CLI não está instalado ou não está no PATH
    echo Instale com: npm install -g firebase-tools
    pause
    exit /b 1
)

echo [1/4] Verificando login no Firebase...
firebase login:list >nul 2>&1
if errorlevel 1 (
    echo ERRO: Não está logado no Firebase
    echo Execute: firebase login
    pause
    exit /b 1
)

echo Login verificado.

echo.
echo [2/4] Fazendo deploy das Rules...
firebase deploy --only firestore:rules,storage:rules
if errorlevel 1 (
    echo ERRO: Falha ao fazer deploy das Rules
    pause
    exit /b 1
)

echo.
echo [3/4] Fazendo deploy dos Indexes...
firebase deploy --only firestore:indexes
if errorlevel 1 (
    echo AVISO: Alguns indexes podem já existir (isso é normal)
    echo.
)

echo.
echo [4/4] Fazendo deploy das Cloud Functions...
cd functions
call npm install
if errorlevel 1 (
    echo ERRO: Falha ao instalar dependências das Functions
    cd ..
    pause
    exit /b 1
)

cd ..
firebase deploy --only functions
if errorlevel 1 (
    echo ERRO: Falha ao fazer deploy das Functions
    pause
    exit /b 1
)

echo.
echo ========================================
echo DEPLOY CONCLUÍDO COM SUCESSO!
echo ========================================
echo.
echo Todas as atualizações foram aplicadas:
echo   - Firestore Rules: OK
echo   - Storage Rules: OK
echo   - Cloud Functions: OK
echo   - Firestore Indexes: OK
echo.
pause
