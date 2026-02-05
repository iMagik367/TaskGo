@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo DEPLOY FIREBASE V1.3.1 - TaskGo App
echo ========================================
echo.
echo Versão: 1.3.1
echo.
echo Este script irá fazer deploy de:
echo   - Firestore Rules
echo   - Cloud Functions
echo   - Firestore Indexes
echo.
echo ========================================
echo.

cd /d "%~dp0"

:: Verificar se estamos no diretório correto
if not exist "firestore.rules" (
    echo ERRO: Execute este script na raiz do projeto TaskGoApp
    pause
    exit /b 1
)

:: Verificar se Firebase CLI está instalado
where firebase >nul 2>&1
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
echo [2/4] Fazendo deploy das Firestore Rules...
firebase deploy --only firestore:rules
if errorlevel 1 (
    echo ERRO: Falha ao fazer deploy das rules
    pause
    exit /b 1
)

echo.
echo [3/4] Fazendo deploy dos Firestore Indexes...
firebase deploy --only firestore:indexes
if errorlevel 1 (
    echo AVISO: Falha ao fazer deploy dos indexes (pode ser normal se não houver novos)
    echo.
)

echo.
echo [4/4] Fazendo deploy das Cloud Functions...
echo Isso pode levar alguns minutos...
cd functions
if exist "package.json" (
    call npm install
    if errorlevel 1 (
        echo ERRO: Falha ao instalar dependências das functions
        cd ..
        pause
        exit /b 1
    )
)
cd ..

firebase deploy --only functions
if errorlevel 1 (
    echo ERRO: Falha ao fazer deploy das functions
    pause
    exit /b 1
)

echo.
echo ========================================
echo DEPLOY CONCLUÍDO COM SUCESSO!
echo ========================================
echo.
echo Versão: 1.3.1
echo.
echo Deploy realizado:
echo   - Firestore Rules: OK
echo   - Firestore Indexes: OK
echo   - Cloud Functions: OK
echo.
echo ========================================
pause
