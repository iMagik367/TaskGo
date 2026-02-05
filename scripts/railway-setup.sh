#!/bin/bash

# Script de Setup Railway - Bash
# Facilita a configuração inicial

echo "🚀 Configuração Railway - TaskGo Backend"
echo ""

# Verificar se Railway CLI está instalado
echo "📦 Verificando Railway CLI..."
if ! command -v railway &> /dev/null; then
    echo "⚠️ Railway CLI não encontrado. Instalando..."
    npm install -g @railway/cli
    echo "✅ Railway CLI instalado!"
else
    echo "✅ Railway CLI já instalado"
fi

echo ""
echo "📋 Próximos passos:"
echo ""
echo "1. Login no Railway:"
echo "   railway login"
echo ""
echo "2. Linkar ao projeto:"
echo "   railway link"
echo ""
echo "3. Executar migrations:"
echo "   railway run psql \$DATABASE_URL -f database/schema.sql"
echo "   railway run psql \$DATABASE_URL -f database/migrations/002_seed_states_cities.sql"
echo "   railway run psql \$DATABASE_URL -f database/migrations/003_seed_categories.sql"
echo ""
echo "4. Ver logs:"
echo "   railway logs"
echo ""
echo "✅ Script concluído!"
