# Script de setup do banco de dados PostgreSQL para TaskGo (PowerShell)
# Uso: .\database\setup.ps1

$ErrorActionPreference = "Stop"

Write-Host "🚀 Configurando banco de dados PostgreSQL para TaskGo..." -ForegroundColor Cyan

# Variáveis (pode ser sobrescrito por variáveis de ambiente)
$DB_NAME = if ($env:DB_NAME) { $env:DB_NAME } else { "taskgo" }
$DB_USER = if ($env:DB_USER) { $env:DB_USER } else { "postgres" }
$DB_HOST = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DB_PORT = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }

# Verificar se PostgreSQL está rodando
Write-Host "📡 Verificando conexão com PostgreSQL..." -ForegroundColor Yellow
try {
    $result = psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c "\q" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Erro ao conectar"
    }
} catch {
    Write-Host "❌ Erro: Não foi possível conectar ao PostgreSQL" -ForegroundColor Red
    Write-Host "   Verifique se o PostgreSQL está rodando e as credenciais estão corretas" -ForegroundColor Red
    exit 1
}

# Criar banco de dados se não existir
Write-Host "📦 Criando banco de dados '$DB_NAME'..." -ForegroundColor Yellow
$createDbQuery = @"
SELECT 'CREATE DATABASE $DB_NAME'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB_NAME')
"@
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c $createDbQuery | Out-Null

# Executar schema
Write-Host "📋 Executando schema principal..." -ForegroundColor Yellow
Get-Content "schema.sql" | psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME

# Executar migrations
Write-Host "🔄 Executando migrations..." -ForegroundColor Yellow
Get-Content "migrations\002_seed_states_cities.sql" | psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
Get-Content "migrations\003_seed_categories.sql" | psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME

Write-Host "✅ Banco de dados configurado com sucesso!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Próximos passos:" -ForegroundColor Cyan
Write-Host "   1. Configure as variáveis de ambiente no backend/.env"
Write-Host "   2. Execute: npm run migrate:firestore (para migrar dados do Firestore)"
Write-Host "   3. Inicie o servidor: npm run dev"
