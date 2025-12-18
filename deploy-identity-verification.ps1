# Script de Deploy para Verificação de Identidade
# Executa deploy das Cloud Functions, índices e regras do Firebase

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deploy - Verificação de Identidade" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se está no diretório correto
if (-not (Test-Path "firebase.json")) {
    Write-Host "ERRO: Execute este script na raiz do projeto!" -ForegroundColor Red
    exit 1
}

# 1. Instalar dependências das functions
Write-Host "[1/5] Instalando dependências das functions..." -ForegroundColor Yellow
Set-Location functions
if (-not (Test-Path "node_modules")) {
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO ao instalar dependências!" -ForegroundColor Red
        Set-Location ..
        exit 1
    }
} else {
    Write-Host "Dependências já instaladas. Atualizando..." -ForegroundColor Gray
    npm install
}
Set-Location ..

Write-Host "✓ Dependências instaladas" -ForegroundColor Green
Write-Host ""

# 2. Build das functions
Write-Host "[2/5] Compilando TypeScript..." -ForegroundColor Yellow
Set-Location functions
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO ao compilar TypeScript!" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Set-Location ..

Write-Host "✓ TypeScript compilado" -ForegroundColor Green
Write-Host ""

# 3. Deploy dos índices do Firestore
Write-Host "[3/5] Fazendo deploy dos índices do Firestore..." -ForegroundColor Yellow
firebase deploy --only firestore:indexes
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO ao fazer deploy dos índices!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Índices deployados" -ForegroundColor Green
Write-Host ""

# 4. Deploy das regras do Realtime Database
Write-Host "[4/5] Fazendo deploy das regras do Realtime Database..." -ForegroundColor Yellow
firebase deploy --only database
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO ao fazer deploy das regras!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Regras deployadas" -ForegroundColor Green
Write-Host ""

# 5. Deploy das Cloud Functions
Write-Host "[5/5] Fazendo deploy das Cloud Functions..." -ForegroundColor Yellow
Write-Host "Isso pode levar alguns minutos..." -ForegroundColor Gray
firebase deploy --only functions:processIdentityVerification,functions:startIdentityVerification
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO ao fazer deploy das functions!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Functions deployadas" -ForegroundColor Green
Write-Host ""

# Resumo
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deploy Concluído com Sucesso!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Functions deployadas:" -ForegroundColor Yellow
Write-Host "  - processIdentityVerification (trigger Realtime Database)" -ForegroundColor White
Write-Host "  - startIdentityVerification (HTTP callable)" -ForegroundColor White
Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor Yellow
Write-Host "  1. Ativar Google Cloud Vision API no console:" -ForegroundColor White
Write-Host "     https://console.cloud.google.com/apis/library/vision.googleapis.com" -ForegroundColor Cyan
Write-Host "  2. Verificar logs das functions:" -ForegroundColor White
Write-Host "     firebase functions:log" -ForegroundColor Cyan
Write-Host "  3. Testar a verificação no app" -ForegroundColor White
Write-Host ""

