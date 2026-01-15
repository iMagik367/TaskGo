@echo off
echo ========================================
echo Deploy Firebase Functions e Rules
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Building Firebase Functions...
cd functions
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Build das functions falhou!
    pause
    exit /b 1
)
cd ..

echo.
echo [2/3] Deploying Firebase Functions...
firebase deploy --only functions
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Deploy das functions falhou!
    pause
    exit /b 1
)

echo.
echo [3/3] Deploying Firestore e Storage Rules...
firebase deploy --only firestore:rules,storage:rules
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Deploy das rules falhou!
    pause
    exit /b 1
)

echo.
echo ========================================
echo DEPLOY CONCLUIDO COM SUCESSO!
echo ========================================
echo.

pause
