# Script de Deploy do Backend Firebase - Padronização Completa
# Deploy das Cloud Functions com paths padronizados: locations/{locationId}/{collection}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DEPLOY BACKEND FIREBASE - PADRONIZAÇÃO COMPLETA" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se Firebase CLI está instalado
$firebaseCliInstalled = Get-Command firebase -ErrorAction SilentlyContinue
if (-not $firebaseCliInstalled) {
    Write-Host "ERRO: Firebase CLI não encontrado. Instale com: npm install -g firebase-tools" -ForegroundColor Red
    exit 1
}

# Verificar se está autenticado
Write-Host "Verificando autenticação Firebase..." -ForegroundColor Yellow
$firebaseAuth = firebase login:list 2>&1
if ($LASTEXITCODE -ne 0 -or $firebaseAuth -match "No authorized accounts") {
    Write-Host "ERRO: Não autenticado no Firebase. Execute: firebase login" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Autenticado no Firebase" -ForegroundColor Green
Write-Host ""

# Navegar para o diretório functions
Write-Host "Compilando Cloud Functions..." -ForegroundColor Yellow
Push-Location functions
try {
    # Instalar dependências se necessário
    if (-not (Test-Path "node_modules")) {
        Write-Host "  Instalando dependências do npm..." -ForegroundColor Gray
        npm install
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERRO: Erro ao instalar dependências" -ForegroundColor Red
            exit 1
        }
    }
    
    # Build TypeScript
    Write-Host "  Compilando TypeScript..." -ForegroundColor Gray
    npm run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Erro ao compilar TypeScript" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "✓ Functions compiladas com sucesso" -ForegroundColor Green
} finally {
    Pop-Location
}
Write-Host ""

# Voltar para raiz do projeto
Set-Location $PSScriptRoot

# Deploy Firestore Rules
Write-Host "Fazendo deploy das Firestore Rules..." -ForegroundColor Yellow
firebase deploy --only firestore:rules
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Erro ao fazer deploy das Rules" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Firestore Rules deployed" -ForegroundColor Green
Write-Host ""

# Deploy Firestore Indexes (se existir)
if (Test-Path "firestore.indexes.json") {
    Write-Host "Fazendo deploy dos Firestore Indexes..." -ForegroundColor Yellow
    firebase deploy --only firestore:indexes
    if ($LASTEXITCODE -ne 0) {
        Write-Host "AVISO: Erro ao fazer deploy dos Indexes (pode ser normal)" -ForegroundColor Yellow
    } else {
        Write-Host "✓ Firestore Indexes deployed" -ForegroundColor Green
    }
    Write-Host ""
}

# Deploy Storage Rules (se existir)
if (Test-Path "storage.rules") {
    Write-Host "Fazendo deploy das Storage Rules..." -ForegroundColor Yellow
    firebase deploy --only storage
    if ($LASTEXITCODE -ne 0) {
        Write-Host "AVISO: Erro ao fazer deploy das Storage Rules" -ForegroundColor Yellow
    } else {
        Write-Host "✓ Storage Rules deployed" -ForegroundColor Green
    }
    Write-Host ""
}

# Deploy Cloud Functions
Write-Host "Fazendo deploy das Cloud Functions..." -ForegroundColor Yellow
Write-Host "  Isso pode levar alguns minutos..." -ForegroundColor Gray
Write-Host ""

firebase deploy --only functions
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Erro ao fazer deploy das Functions" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Cloud Functions deployed" -ForegroundColor Green
Write-Host ""

# Resumo
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DEPLOY COMPLETO COM SUCESSO!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Backend padronizado - Paths gravados:" -ForegroundColor White
Write-Host "  📍 locations/{locationId}/products" -ForegroundColor Gray
Write-Host "  📍 locations/{locationId}/services" -ForegroundColor Gray
Write-Host "  📍 locations/{locationId}/stories" -ForegroundColor Gray
Write-Host "  📍 locations/{locationId}/posts" -ForegroundColor Gray
Write-Host "  📍 locations/{locationId}/orders" -ForegroundColor Gray
Write-Host ""
Write-Host "Todas as escritas incluem:" -ForegroundColor White
Write-Host "  ✓ createdAt: FieldValue.serverTimestamp()" -ForegroundColor Gray
Write-Host "  ✓ updatedAt: FieldValue.serverTimestamp()" -ForegroundColor Gray
Write-Host "  ✓ active: true (quando aplicável)" -ForegroundColor Gray
Write-Host ""
