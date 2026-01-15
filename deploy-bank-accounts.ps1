# Script para fazer deploy das functions, secrets e rules relacionadas a contas bancárias
Write-Host "========================================" -ForegroundColor Green
Write-Host "DEPLOY: Functions, Rules e Secrets" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Verificar se está no diretório correto
if (-not (Test-Path "functions")) {
    Write-Host "ERRO: Diretorio 'functions' nao encontrado!" -ForegroundColor Red
    Write-Host "Execute este script na raiz do projeto." -ForegroundColor Red
    exit 1
}

# Verificar se Firebase CLI está instalado
try {
    $firebaseVersion = firebase --version
    Write-Host "Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "ERRO: Firebase CLI nao encontrado!" -ForegroundColor Red
    Write-Host "Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "1. Compilando TypeScript..." -ForegroundColor Yellow
Set-Location functions
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha ao compilar TypeScript!" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Set-Location ..

Write-Host ""
Write-Host "2. Fazendo deploy das Firestore Rules..." -ForegroundColor Yellow
firebase deploy --only firestore:rules
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha ao fazer deploy das rules!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "3. Fazendo deploy das Functions..." -ForegroundColor Yellow
firebase deploy --only functions
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha ao fazer deploy das functions!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "DEPLOY CONCLUIDO COM SUCESSO!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "NOTA: Secrets devem ser configurados manualmente no Firebase Console:" -ForegroundColor Yellow
Write-Host "  - Acesse: https://console.firebase.google.com" -ForegroundColor White
Write-Host "  - Vá em Functions > Secrets" -ForegroundColor White
Write-Host "  - Adicione os secrets necessários (STRIPE_SECRET_KEY, etc.)" -ForegroundColor White
Write-Host ""

