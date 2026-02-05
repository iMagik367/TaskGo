@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo DEPLOY FIREBASE V1.2.12 - TaskGo
echo ========================================
echo.
echo Versão: 1.2.12
echo.
echo Componentes a serem deployados:
echo   - Firestore Rules (com suporte a locations/{locationId}/users)
echo   - Cloud Functions
echo   - Secrets (verificação)
echo.
echo Mudanças nesta versão:
echo   - CRÍTICO: observeUser agora observa TANTO users global QUANTO locations/{locationId}/users
echo   - CRÍTICO: SyncManager.syncUserProfile salva em ambas as coleções
echo   - CRÍTICO: InitialDataSyncManager lê city/state da raiz do documento
echo   - Correção: Garantia de sincronização entre escrita e leitura
echo   - Correção: LocalServiceOrdersViewModel usa apenas city/state do perfil
echo   - Correção: ProfileViewModel remove fallback para address?.city/state
echo   - Correção: PublicUserProfileScreen remove fallback para address?.city/state
echo   - Correção: updateField, approveDocuments, setStripeAccount atualizam ambas coleções
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

:: Usar npx diretamente (Firebase CLI pode estar corrompido)
echo Usando npx firebase-tools (mais confiável)...
set FIREBASE_CMD=npx --yes firebase-tools@latest

echo.
echo [1/4] Verificando configuração do Firebase...
if not exist ".firebaserc" (
    echo AVISO: .firebaserc não encontrado
    echo Certifique-se de que o projeto Firebase está configurado
)

echo.
echo [2/4] Fazendo deploy das Firestore Rules...
echo (Incluindo suporte a locations/{locationId}/users)
%FIREBASE_CMD% deploy --only firestore:rules --force
if errorlevel 1 (
    echo ERRO: Falha ao fazer deploy das rules
    pause
    exit /b 1
)

echo.
echo [3/4] Compilando Functions...
cd functions
call npm run build
if errorlevel 1 (
    echo ERRO: Falha ao compilar functions
    cd ..
    pause
    exit /b 1
)
cd ..

echo.
echo [4/4] Fazendo deploy das Cloud Functions...
echo (Pode demorar alguns minutos...)
%FIREBASE_CMD% deploy --only functions --force
if errorlevel 1 (
    echo AVISO: Deploy das functions pode ter falhado ou dado timeout
    echo Isso é normal se as functions já estão deployadas
    echo Verifique manualmente no Firebase Console
)

echo.
echo ========================================
echo VERIFICAÇÃO DE SECRETS
echo ========================================
echo.
echo IMPORTANTE: Verifique se os secrets estão configurados:
echo.
echo Para verificar secrets:
echo   %FIREBASE_CMD% functions:secrets:access SECRET_NAME
echo.
echo Secrets comuns:
echo   - STRIPE_SECRET_KEY
echo   - STRIPE_WEBHOOK_SECRET
echo   - OPENAI_API_KEY
echo   - GEMINI_API_KEY
echo   - SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS
echo.
echo Para configurar um secret:
echo   %FIREBASE_CMD% functions:secrets:set SECRET_NAME
echo.

echo ========================================
echo DEPLOY CONCLUÍDO
echo ========================================
echo.
echo Componentes deployados:
echo   - Firestore Rules: OK
echo   - Cloud Functions: Verificar no console
echo.
echo Verifique o status no Firebase Console:
echo https://console.firebase.google.com/
echo.
pause
