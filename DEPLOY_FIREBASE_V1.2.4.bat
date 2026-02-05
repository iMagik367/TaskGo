@echo off
chcp 65001 >nul
echo ========================================
echo Deploy Firebase - Versao 1.2.4
echo ========================================
echo.
echo Este script faz deploy de:
echo   - Cloud Functions
echo   - Firestore Rules
echo   - Firestore Indexes (se houver)
echo.
echo ========================================
echo.

cd /d "%~dp0"

echo Verificando Firebase CLI...
firebase --version
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERRO: Firebase CLI nao encontrado!
    echo Instale com: npm install -g firebase-tools
    echo.
    pause
    exit /b 1
)

echo.
echo Fazendo deploy das Cloud Functions...
cd functions
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Build das functions falhou!
    pause
    exit /b 1
)
cd ..

echo.
echo Fazendo deploy do Firestore Rules...
firebase deploy --only firestore:rules
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Deploy das rules falhou!
    pause
    exit /b 1
)

echo.
echo Fazendo deploy das Cloud Functions...
firebase deploy --only functions
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Deploy das functions falhou!
    pause
    exit /b 1
)

echo.
echo ========================================
echo DEPLOY COMPLETO COM SUCESSO!
echo ========================================
echo.
echo Versao: 1.2.4
echo.
echo Mudancas deployadas:
echo   - LocationStateManager: Sempre emite Ready
echo   - GPS: Sistema robusto com fallback
echo   - Backend: Recebe GPS do frontend
echo.
pause
