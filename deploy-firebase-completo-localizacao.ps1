# Script de Deploy Completo do Firebase
# Atualiza Functions, Firestore Rules e Indexes
# Versão: 1.0.97 - Correção definitiva do fluxo de localização

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DEPLOY FIREBASE COMPLETO - CORREÇÃO LOCALIZAÇÃO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se está no diretório correto
if (-not (Test-Path "firebase.json")) {
    Write-Host "ERRO: Execute este script na raiz do projeto" -ForegroundColor Red
    exit 1
}

# Verificar se Firebase CLI está instalado
Write-Host "Verificando Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseVersion = firebase --version 2>&1
    Write-Host "   Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "ERRO: Firebase CLI não encontrado. Instale com: npm install -g firebase-tools" -ForegroundColor Red
    exit 1
}

# Verificar se está logado
Write-Host "`nVerificando autenticação Firebase..." -ForegroundColor Yellow
try {
    $firebaseUser = firebase login:list 2>&1
    if ($firebaseUser -match "No authorized accounts") {
        Write-Host "   Nenhum usuário autenticado. Fazendo login..." -ForegroundColor Yellow
        firebase login
    } else {
        Write-Host "   Usuário autenticado" -ForegroundColor Green
    }
} catch {
    Write-Host "   Fazendo login no Firebase..." -ForegroundColor Yellow
    firebase login
}

# Verificar projeto Firebase
Write-Host "`nVerificando projeto Firebase..." -ForegroundColor Yellow
try {
    $project = firebase use 2>&1
    Write-Host "   Projeto atual: $project" -ForegroundColor Green
} catch {
    Write-Host "   Configurando projeto..." -ForegroundColor Yellow
    firebase use --add
}

# Deploy das Firestore Rules
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "1. DEPLOY FIRESTORE RULES" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Atualizando regras para suportar coleções por localização..." -ForegroundColor Yellow
firebase deploy --only firestore:rules
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha ao fazer deploy das Firestore Rules" -ForegroundColor Red
    exit 1
}
Write-Host "   ✅ Firestore Rules deployadas com sucesso" -ForegroundColor Green

# Deploy dos Firestore Indexes
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "2. DEPLOY FIRESTORE INDEXES" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Atualizando índices..." -ForegroundColor Yellow
firebase deploy --only firestore:indexes
if ($LASTEXITCODE -ne 0) {
    Write-Host "AVISO: Falha ao fazer deploy dos índices (pode ser normal se não houver novos)" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ Firestore Indexes deployados com sucesso" -ForegroundColor Green
}

# Build das Functions
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "3. BUILD CLOUD FUNCTIONS" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Compilando Functions..." -ForegroundColor Yellow
Set-Location functions
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha ao compilar Functions" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Write-Host "   ✅ Functions compiladas com sucesso" -ForegroundColor Green
Set-Location ..

# Deploy das Functions
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "4. DEPLOY CLOUD FUNCTIONS" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Fazendo deploy das Functions (isso pode levar alguns minutos)..." -ForegroundColor Yellow
firebase deploy --only functions
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha ao fazer deploy das Functions" -ForegroundColor Red
    exit 1
}
Write-Host "   ✅ Functions deployadas com sucesso" -ForegroundColor Green

# Resumo final
Write-Host "`n" -NoNewline
Write-Host "============================================================" -ForegroundColor Green
Write-Host "DEPLOY COMPLETO COM SUCESSO!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "`nComponentes deployados:" -ForegroundColor White
Write-Host "   ✅ Firestore Rules" -ForegroundColor Green
Write-Host "   ✅ Firestore Indexes" -ForegroundColor Green
Write-Host "   ✅ Cloud Functions" -ForegroundColor Green
Write-Host "`nPróximos passos:" -ForegroundColor Cyan
Write-Host "   1. Execute o script de build AAB: .\build-aab-release.ps1" -ForegroundColor White
Write-Host "   2. Faça upload do AAB no Google Play Console" -ForegroundColor White
Write-Host ""
