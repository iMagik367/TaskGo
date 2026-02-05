# Script de Setup Railway - PowerShell
# Facilita a configuração inicial

Write-Host "🚀 Configuração Railway - TaskGo Backend" -ForegroundColor Cyan
Write-Host ""

# Verificar se Railway CLI está instalado
Write-Host "📦 Verificando Railway CLI..." -ForegroundColor Yellow
$railwayInstalled = Get-Command railway -ErrorAction SilentlyContinue

if (-not $railwayInstalled) {
    Write-Host "⚠️ Railway CLI não encontrado. Instalando..." -ForegroundColor Yellow
    npm install -g @railway/cli
    Write-Host "✅ Railway CLI instalado!" -ForegroundColor Green
} else {
    Write-Host "✅ Railway CLI já instalado" -ForegroundColor Green
}

Write-Host ""
Write-Host "📋 Próximos passos:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Login no Railway:" -ForegroundColor White
Write-Host "   railway login" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Linkar ao projeto:" -ForegroundColor White
Write-Host "   railway link" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Executar migrations:" -ForegroundColor White
Write-Host "   railway run psql `$DATABASE_URL -f database/schema.sql" -ForegroundColor Gray
Write-Host "   railway run psql `$DATABASE_URL -f database/migrations/002_seed_states_cities.sql" -ForegroundColor Gray
Write-Host "   railway run psql `$DATABASE_URL -f database/migrations/003_seed_categories.sql" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Ver logs:" -ForegroundColor White
Write-Host "   railway logs" -ForegroundColor Gray
Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green
