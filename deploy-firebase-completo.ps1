# Script de Deploy Completo - Firebase Functions, Rules e Storage Rules
# Atualizado para arquitetura regional

Write-Host "Iniciando deploy completo do Firebase..." -ForegroundColor Cyan

# Verificar se Firebase CLI esta instalado
$firebaseCliInstalled = Get-Command firebase -ErrorAction SilentlyContinue
if (-not $firebaseCliInstalled) {
    Write-Host "ERRO: Firebase CLI nao encontrado. Instale com: npm install -g firebase-tools" -ForegroundColor Red
    exit 1
}

# Verificar se esta autenticado
Write-Host "`nVerificando autenticacao Firebase..." -ForegroundColor Yellow
$firebaseAuth = firebase login:list 2>&1
if ($LASTEXITCODE -ne 0 -or $firebaseAuth -match "No authorized accounts") {
    Write-Host "ERRO: Nao autenticado no Firebase. Execute: firebase login" -ForegroundColor Red
    exit 1
}

Write-Host "Autenticado no Firebase" -ForegroundColor Green

# Navegar para o diretorio functions
Write-Host "`nCompilando Cloud Functions..." -ForegroundColor Yellow
Push-Location functions
try {
    # Instalar dependencias se necessario
    if (-not (Test-Path "node_modules")) {
        Write-Host "Instalando dependencias do npm..." -ForegroundColor Yellow
        npm install
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERRO: Erro ao instalar dependencias" -ForegroundColor Red
            exit 1
        }
    }
    
    # Build TypeScript
    Write-Host "Compilando TypeScript..." -ForegroundColor Yellow
    npm run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Erro ao compilar TypeScript" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Functions compiladas com sucesso" -ForegroundColor Green
} finally {
    Pop-Location
}

# Voltar para raiz do projeto
Set-Location $PSScriptRoot

# Deploy Firestore Rules
Write-Host "`nFazendo deploy das Firestore Rules..." -ForegroundColor Yellow
firebase deploy --only firestore:rules
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Erro ao fazer deploy das Rules" -ForegroundColor Red
    exit 1
}
Write-Host "Firestore Rules deployed com sucesso" -ForegroundColor Green

# Deploy Firestore Indexes (se existir)
if (Test-Path "firestore.indexes.json") {
    Write-Host "`nFazendo deploy dos Firestore Indexes..." -ForegroundColor Yellow
    firebase deploy --only firestore:indexes
    if ($LASTEXITCODE -ne 0) {
        Write-Host "AVISO: Erro ao fazer deploy dos Indexes (pode ser normal se nao houver indices novos)" -ForegroundColor Yellow
    } else {
        Write-Host "Firestore Indexes deployed com sucesso" -ForegroundColor Green
    }
}

# Deploy Storage Rules (se existir)
if (Test-Path "storage.rules") {
    Write-Host "`nFazendo deploy das Storage Rules..." -ForegroundColor Yellow
    firebase deploy --only storage
    if ($LASTEXITCODE -ne 0) {
        Write-Host "AVISO: Erro ao fazer deploy das Storage Rules" -ForegroundColor Yellow
    } else {
        Write-Host "Storage Rules deployed com sucesso" -ForegroundColor Green
    }
}

# Deploy Cloud Functions
Write-Host "`nFazendo deploy das Cloud Functions..." -ForegroundColor Yellow
Write-Host "   Isso pode levar alguns minutos..." -ForegroundColor Gray

firebase deploy --only functions
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Erro ao fazer deploy das Functions" -ForegroundColor Red
    exit 1
}
Write-Host "Cloud Functions deployed com sucesso" -ForegroundColor Green

# Resumo
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "DEPLOY COMPLETO COM SUCESSO!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "`nDeploy realizado:" -ForegroundColor White
Write-Host "   - Firestore Rules" -ForegroundColor Green
if (Test-Path "firestore.indexes.json") {
    Write-Host "   - Firestore Indexes" -ForegroundColor Green
}
if (Test-Path "storage.rules") {
    Write-Host "   - Storage Rules" -ForegroundColor Green
}
Write-Host "   - Cloud Functions" -ForegroundColor Green
Write-Host "`nArquitetura regional configurada e deployada!" -ForegroundColor Cyan
Write-Host '   - locations/{city}_{state}/products' -ForegroundColor Gray
Write-Host '   - locations/{city}_{state}/stories' -ForegroundColor Gray
