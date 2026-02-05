# Script para deploy do Firebase - Versão 1.3.0
# Deploy de Functions, Firestore Rules, Storage Rules e Database Rules

Write-Host "========================================"
Write-Host "DEPLOY FIREBASE V1.3.0 - TaskGo App"
Write-Host "========================================"
Write-Host ""

$ErrorActionPreference = "Stop"

# Verificar se estamos no diretório correto
if (-not (Test-Path "firebase.json")) {
    Write-Host "ERRO: Execute este script na raiz do projeto TaskGoApp"
    exit 1
}

# Verificar se o Firebase CLI está instalado
try {
    $firebaseVersion = firebase --version 2>&1
    Write-Host "Firebase CLI encontrado: $firebaseVersion"
} catch {
    Write-Host "ERRO: Firebase CLI não encontrado!"
    Write-Host "Instale com: npm install -g firebase-tools"
    exit 1
}

# Compilar functions
Write-Host ""
Write-Host "[1/4] Compilando Functions..."
Set-Location "functions"
try {
    npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Erro ao compilar functions"
    }
    Write-Host "✓ Functions compiladas com sucesso"
} catch {
    Write-Host "ERRO: Falha ao compilar functions"
    Set-Location ..
    exit 1
}
Set-Location ..

# Deploy das Rules primeiro (mais rápido)
Write-Host ""
Write-Host "[2/4] Fazendo deploy das Firestore Rules..."
try {
    firebase deploy --only firestore:rules
    if ($LASTEXITCODE -ne 0) {
        throw "Erro ao fazer deploy das Firestore Rules"
    }
    Write-Host "✓ Firestore Rules deployadas com sucesso"
} catch {
    Write-Host "ERRO: Falha ao fazer deploy das Firestore Rules"
    exit 1
}

Write-Host ""
Write-Host "[3/4] Fazendo deploy das Storage Rules..."
try {
    firebase deploy --only storage:rules
    if ($LASTEXITCODE -ne 0) {
        throw "Erro ao fazer deploy das Storage Rules"
    }
    Write-Host "✓ Storage Rules deployadas com sucesso"
} catch {
    Write-Host "ERRO: Falha ao fazer deploy das Storage Rules"
    exit 1
}

Write-Host ""
Write-Host "[4/4] Fazendo deploy das Database Rules..."
try {
    firebase deploy --only database:rules
    if ($LASTEXITCODE -ne 0) {
        throw "Erro ao fazer deploy das Database Rules"
    }
    Write-Host "✓ Database Rules deployadas com sucesso"
} catch {
    Write-Host "ERRO: Falha ao fazer deploy das Database Rules"
    exit 1
}

Write-Host ""
Write-Host "[5/5] Fazendo deploy das Functions..."
try {
    firebase deploy --only functions
    if ($LASTEXITCODE -ne 0) {
        throw "Erro ao fazer deploy das Functions"
    }
    Write-Host "OK: Functions deployadas com sucesso"
} catch {
    Write-Host "ERRO: Falha ao fazer deploy das Functions"
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host "DEPLOY CONCLUIDO COM SUCESSO!"
Write-Host "========================================"
Write-Host ""
Write-Host "Deploy realizado:"
Write-Host "  - Firestore Rules"
Write-Host "  - Storage Rules"
Write-Host "  - Database Rules"
Write-Host "  - Cloud Functions"
Write-Host ""
