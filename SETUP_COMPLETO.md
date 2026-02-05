# Setup Completo - TaskGo PostgreSQL

## ✅ Checklist de Configuração

### 1. Banco de Dados PostgreSQL
- [ ] PostgreSQL instalado e rodando
- [ ] Banco de dados `taskgo` criado
- [ ] Schema executado (`database/schema.sql`)
- [ ] Migrations executadas
- [ ] Estados e cidades populados
- [ ] Categorias populadas

### 2. Backend
- [ ] Dependências instaladas (`npm install`)
- [ ] Arquivo `.env` configurado
- [ ] Variáveis de ambiente definidas
- [ ] Servidor inicia sem erros (`npm run dev`)

### 3. Migração de Dados
- [ ] Credenciais do Firebase configuradas
- [ ] Migração executada (`npm run migrate:firestore`)
- [ ] Validação executada (`npm run validate:migration`)
- [ ] Dados validados e corretos

### 4. Testes
- [ ] Health check funcionando (`/health`)
- [ ] Atualização de localização funcionando
- [ ] WebSocket conectando
- [ ] Notificações em tempo real funcionando
- [ ] Criação de ordens funcionando

## 🚀 Comandos Rápidos

```bash
# 1. Setup do banco
cd database
./setup.sh  # ou .\setup.ps1 no Windows

# 2. Configurar backend
cd ../backend
npm install
cp .env.example .env
# Editar .env com suas credenciais

# 3. Popular cidades (opcional)
npm run populate:cities

# 4. Migrar dados do Firestore
npm run migrate:firestore

# 5. Validar migração
npm run validate:migration

# 6. Iniciar servidor
npm run dev
```

## 📋 Verificação Final

Execute estes comandos para verificar se tudo está funcionando:

```bash
# Verificar conexão com banco
psql -d taskgo -c "SELECT COUNT(*) FROM users;"

# Verificar estrutura
psql -d taskgo -c "\dt"

# Verificar triggers
psql -d taskgo -c "SELECT * FROM pg_trigger WHERE tgname LIKE '%service_order%';"

# Testar API
curl http://localhost:3000/health
```

## 🎯 Próximos Passos Após Setup

1. **Integrar com App Mobile**
   - Atualizar endpoints no app
   - Configurar WebSocket no app
   - Testar fluxo completo

2. **Configurar Produção**
   - Servidor PostgreSQL em produção
   - SSL/TLS configurado
   - Backup automático
   - Monitoramento

3. **Otimizações**
   - Índices adicionais se necessário
   - Cache se necessário
   - Load balancing se necessário

## 📞 Suporte

Em caso de problemas, consulte:
- `GUIA_CONFIGURACAO_POSTGRESQL.md` - Guia detalhado
- `MIGRACAO_POSTGRESQL_RESUMO.md` - Resumo da implementação
- Logs do servidor para erros específicos
