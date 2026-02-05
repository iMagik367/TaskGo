#!/bin/bash

# Script de setup do banco de dados PostgreSQL para TaskGo
# Uso: ./database/setup.sh

set -e

echo "🚀 Configurando banco de dados PostgreSQL para TaskGo..."

# Variáveis (pode ser sobrescrito por variáveis de ambiente)
DB_NAME=${DB_NAME:-taskgo}
DB_USER=${DB_USER:-postgres}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}

# Verificar se PostgreSQL está rodando
echo "📡 Verificando conexão com PostgreSQL..."
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c '\q' 2>/dev/null; then
    echo "❌ Erro: Não foi possível conectar ao PostgreSQL"
    echo "   Verifique se o PostgreSQL está rodando e as credenciais estão corretas"
    exit 1
fi

# Criar banco de dados se não existir
echo "📦 Criando banco de dados '$DB_NAME'..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres <<EOF
SELECT 'CREATE DATABASE $DB_NAME'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB_NAME')\gexec
EOF

# Executar schema
echo "📋 Executando schema principal..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f schema.sql

# Executar migrations
echo "🔄 Executando migrations..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f migrations/002_seed_states_cities.sql
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f migrations/003_seed_categories.sql

echo "✅ Banco de dados configurado com sucesso!"
echo ""
echo "📝 Próximos passos:"
echo "   1. Configure as variáveis de ambiente no backend/.env"
echo "   2. Execute: npm run migrate:firestore (para migrar dados do Firestore)"
echo "   3. Inicie o servidor: npm run dev"
