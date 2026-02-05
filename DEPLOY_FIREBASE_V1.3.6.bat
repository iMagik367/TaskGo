@echo off
chcp 65001 >nul
echo ========================================
echo   DEPLOY FIREBASE V1.3.6
echo ========================================
echo.

echo Verificando Firebase CLI...
firebase --version >nul 2>&1
if errorlevel 1 (
    echo ERRO: Firebase CLI não encontrado
    echo Instale com: npm install -g firebase-tools
    pause
    exit /b 1
)

echo Firebase CLI encontrado
echo.

echo Verificando login...
firebase projects:list >nul 2>&1
if errorlevel 1 (
    echo Fazendo login no Firebase...
    firebase login
    if errorlevel 1 (
        echo ERRO ao fazer login
        pause
        exit /b 1
    )
)

echo.
echo Deployando Firestore Rules...
firebase deploy --only firestore:rules
if errorlevel 1 (
    echo ERRO ao fazer deploy das rules
    pause
    exit /b 1
)

echo.
echo Deployando Firestore Indexes...
firebase deploy --only firestore:indexes
if errorlevel 1 (
    echo ERRO ao fazer deploy dos indexes
    pause
    exit /b 1
)

echo.
echo Deployando Cloud Functions...
cd functions
call npm install
if errorlevel 1 (
    echo ERRO ao instalar dependências das functions
    cd ..
    pause
    exit /b 1
)
cd ..

firebase deploy --only functions
if errorlevel 1 (
    echo ERRO ao fazer deploy das functions
    pause
    exit /b 1
)

echo.
echo ========================================
echo   DEPLOY CONCLUIDO COM SUCESSO!
echo ========================================
echo.
echo Versão: 1.3.6
echo.
pause
