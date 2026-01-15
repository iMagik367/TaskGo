# Script para fazer deploy das regras e funções do Firebase relacionadas a Stories
# Executa deploy das regras do Firestore, Storage e Cloud Functions

Write-Host "=== DEPLOY STORIES - Firebase ===" -ForegroundColor Cyan
Write-Host ""

# Verificar se o Firebase CLI está instalado
try {
    $firebaseVersion = firebase --version
    Write-Host "✅ Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Erro: Firebase CLI não encontrado. Instale com: npm install -g firebase-tools" -ForegroundColor Red
    exit 1
}

# Verificar se está autenticado
Write-Host ""
Write-Host "Verificando autenticação..." -ForegroundColor Yellow
try {
    $authCheck = firebase projects:list 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "⚠️  Não autenticado. Executando login..." -ForegroundColor Yellow
        firebase login
    } else {
        Write-Host "✅ Autenticado no Firebase" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Erro ao verificar autenticação. Tente fazer login manualmente: firebase login" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 1. Deploy das regras do Firestore ===" -ForegroundColor Cyan
firebase deploy --only firestore:rules
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Regras do Firestore deployadas com sucesso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao fazer deploy das regras do Firestore" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 2. Deploy das regras do Storage ===" -ForegroundColor Cyan
firebase deploy --only storage
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Regras do Storage deployadas com sucesso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao fazer deploy das regras do Storage" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 3. Build das Cloud Functions ===" -ForegroundColor Cyan
Set-Location functions
npm run build
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build das Functions concluído!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao fazer build das Functions" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Set-Location ..

Write-Host ""
Write-Host "=== 4. Deploy das Cloud Functions (Stories) ===" -ForegroundColor Cyan
firebase deploy --only functions:cleanupExpiredStories
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Cloud Function cleanupExpiredStories deployada com sucesso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao fazer deploy da Cloud Function" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== DEPLOY CONCLUÍDO ===" -ForegroundColor Green
Write-Host ""
Write-Host "Resumo:" -ForegroundColor Cyan
Write-Host "  ✅ Regras do Firestore (stories collection)" -ForegroundColor Green
Write-Host "  ✅ Regras do Storage (stories/{userId}/{filename})" -ForegroundColor Green
Write-Host "  ✅ Cloud Function: cleanupExpiredStories" -ForegroundColor Green
Write-Host ""
Write-Host "A função cleanupExpiredStories será executada automaticamente a cada 24 horas." -ForegroundColor Yellow
