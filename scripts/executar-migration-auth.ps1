# Script para executar migration de autenticação no Railway PostgreSQL
# Uso: .\scripts\executar-migration-auth.ps1

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Executar Migration de Autenticação" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se DATABASE_URL está configurada
if (-not $env:DATABASE_URL) {
    Write-Host "ERRO: Variável DATABASE_URL não encontrada!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Configure a variável DATABASE_URL do Railway:" -ForegroundColor Yellow
    Write-Host "1. Acesse o painel do Railway" -ForegroundColor Yellow
    Write-Host "2. Vá em Variables" -ForegroundColor Yellow
    Write-Host "3. Copie o valor de DATABASE_URL" -ForegroundColor Yellow
    Write-Host "4. Execute: `$env:DATABASE_URL = 'postgresql://...'" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host "Conectando ao banco de dados..." -ForegroundColor Yellow

# Ler arquivo de migration
$migrationFile = "database\migrations\004_add_auth_fields.sql"

if (-not (Test-Path $migrationFile)) {
    Write-Host "ERRO: Arquivo de migration não encontrado: $migrationFile" -ForegroundColor Red
    exit 1
}

Write-Host "Lendo migration: $migrationFile" -ForegroundColor Yellow
$sql = Get-Content $migrationFile -Raw

# Usar psql se disponível, senão usar conexão direta
if (Get-Command psql -ErrorAction SilentlyContinue) {
    Write-Host "Executando migration com psql..." -ForegroundColor Yellow
    $sql | psql $env:DATABASE_URL
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Migration executada com sucesso!" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "❌ Erro ao executar migration" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host ""
    Write-Host "psql não encontrado. Instruções alternativas:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Opção 1: Instalar PostgreSQL client" -ForegroundColor Cyan
    Write-Host "  - Windows: https://www.postgresql.org/download/windows/" -ForegroundColor White
    Write-Host ""
    Write-Host "Opção 2: Usar Railway CLI" -ForegroundColor Cyan
    Write-Host "  railway connect" -ForegroundColor White
    Write-Host "  Depois execute o SQL manualmente" -ForegroundColor White
    Write-Host ""
    Write-Host "Opção 3: Usar pgAdmin ou DBeaver" -ForegroundColor Cyan
    Write-Host "  Conecte ao banco e execute o SQL do arquivo:" -ForegroundColor White
    Write-Host "  $migrationFile" -ForegroundColor White
    Write-Host ""
}

Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor Cyan
Write-Host "1. Configure as variáveis de ambiente no Railway (veja VARIAVEIS_AMBIENTE_AUTENTICACAO.md)" -ForegroundColor White
Write-Host "2. Teste os endpoints de autenticação" -ForegroundColor White
Write-Host "3. Atualize as telas do app (veja GUIA_ATUALIZACAO_TELAS_FIREBASE_AUTH.md)" -ForegroundColor White
Write-Host ""
