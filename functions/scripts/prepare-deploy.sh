#!/bin/bash
# Script de preparação para deploy do backend TaskGo

set -e

echo "🔧 Preparando backend para deploy..."

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar se está no diretório correto
if [ ! -f "package.json" ]; then
    echo -e "${RED}❌ Erro: Execute este script do diretório functions/${NC}"
    exit 1
fi

echo -e "${YELLOW}📦 Instalando dependências...${NC}"
npm install

echo -e "${YELLOW}🔨 Compilando TypeScript...${NC}"
npm run build

if [ ! -d "lib" ]; then
    echo -e "${RED}❌ Erro: Diretório lib/ não foi criado. Verifique erros de compilação.${NC}"
    exit 1
fi

echo -e "${YELLOW}🔍 Verificando lint...${NC}"
npm run lint || echo -e "${YELLOW}⚠️  Avisos de lint encontrados (não crítico)${NC}"

echo -e "${GREEN}✅ Build concluído com sucesso!${NC}"
echo ""
echo "📋 Próximos passos:"
echo "  1. firebase deploy --only functions"
echo "  2. firebase deploy --only firestore:rules"
echo "  3. Executar migração de Custom Claims"
echo "  4. Atualizar app Android"
