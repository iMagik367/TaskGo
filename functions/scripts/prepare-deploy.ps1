# Script de preparação para deploy do backend TaskGo (PowerShell)

$ErrorActionPreference = "Stop"

Write-Host "🔧 Preparando backend para deploy..." -ForegroundColor Yellow

# Verificar se está no diretório correto
if (-not (Test-Path "package.json")) {
    Write-Host "❌ Erro: Execute este script do diretório functions/" -ForegroundColor Red
    exit 1
}

Write-Host "📦 Instalando dependências..." -ForegroundColor Yellow
npm install

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao instalar dependências" -ForegroundColor Red
    exit 1
}

Write-Host "🔨 Compilando TypeScript..." -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao compilar TypeScript" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path "lib")) {
    Write-Host "❌ Erro: Diretório lib/ não foi criado. Verifique erros de compilação." -ForegroundColor Red
    exit 1
}

Write-Host "🔍 Verificando lint..." -ForegroundColor Yellow
npm run lint

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Avisos de lint encontrados (não crítico)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✅ Build concluído com sucesso!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Próximos passos:"
Write-Host "  1. firebase deploy --only functions"
Write-Host "  2. firebase deploy --only firestore:rules"
Write-Host "  3. Executar migração de Custom Claims"
Write-Host "  4. Atualizar app Android"
