# Script PowerShell para executar migração de Custom Claims

$ErrorActionPreference = "Stop"

Write-Host "🚀 Iniciando migração de Custom Claims..." -ForegroundColor Yellow
Write-Host ""

# Verificar se está no diretório correto
if (-not (Test-Path "package.json")) {
    Write-Host "❌ Erro: Execute este script do diretório functions/" -ForegroundColor Red
    exit 1
}

# Compilar
Write-Host "🔨 Compilando TypeScript..." -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao compilar" -ForegroundColor Red
    exit 1
}

# Executar migração
Write-Host ""
Write-Host "🔄 Executando migração..." -ForegroundColor Yellow
Write-Host ""

node -e "require('./lib/scripts/migrateExistingUsers').migrateLocal()"

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ Erro na migração" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ Migração concluída!" -ForegroundColor Green
