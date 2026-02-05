# ✅ Checklist de Configuração Railway

Use este checklist para garantir que tudo está configurado corretamente.

## 📋 Setup Inicial

### Projeto
- [ ] Conta Railway criada
- [ ] Plano Pro ativado
- [ ] Novo projeto criado
- [ ] Repositório GitHub conectado

### PostgreSQL
- [ ] Serviço PostgreSQL criado
- [ ] Status: Running (verde)
- [ ] Variáveis de ambiente anotadas:
  - [ ] PGHOST
  - [ ] PGPORT
  - [ ] PGDATABASE
  - [ ] PGUSER
  - [ ] PGPASSWORD

### Backend
- [ ] Serviço backend criado
- [ ] Build Command configurado: `cd backend && npm install && npm run build`
- [ ] Start Command configurado: `cd backend && npm start`
- [ ] Status: Running (verde)

## 🔧 Variáveis de Ambiente

### Database (Backend)
- [ ] `DB_HOST = ${{Postgres.PGHOST}}`
- [ ] `DB_PORT = ${{Postgres.PGPORT}}`
- [ ] `DB_NAME = ${{Postgres.PGDATABASE}}`
- [ ] `DB_USER = ${{Postgres.PGUSER}}`
- [ ] `DB_PASSWORD = ${{Postgres.PGPASSWORD}}`

### Server
- [ ] `PORT = 3000`
- [ ] `NODE_ENV = production`

### Stripe
- [ ] `STRIPE_SECRET_KEY = sk_live_...`
- [ ] `STRIPE_WEBHOOK_SECRET = whsec_...`

### Firebase (se necessário)
- [ ] `FIREBASE_PROJECT_ID = ...`
- [ ] `FIREBASE_CLIENT_EMAIL = ...`
- [ ] `FIREBASE_PRIVATE_KEY = ...`

## 🗄️ Database Migrations

- [ ] Schema executado: `database/schema.sql`
- [ ] Estados e cidades: `database/migrations/002_seed_states_cities.sql`
- [ ] Categorias: `database/migrations/003_seed_categories.sql`
- [ ] Dados validados no banco

## ✅ Verificações

### Deploy
- [ ] Build concluído com sucesso
- [ ] Deploy concluído com sucesso
- [ ] Sem erros nos logs
- [ ] URL do backend anotada

### Health Check
- [ ] Endpoint `/health` responde
- [ ] Retorna `{"status":"ok",...}`
- [ ] Resposta rápida (< 1s)

### Database
- [ ] Conexão com PostgreSQL funcionando
- [ ] Tabelas criadas corretamente
- [ ] Índices criados
- [ ] Triggers funcionando

### API Endpoints
- [ ] `GET /health` - OK
- [ ] `POST /api/location/update` - OK
- [ ] `GET /api/users/:id` - OK
- [ ] `POST /api/orders/service` - OK

### WebSocket
- [ ] Conexão WebSocket estabelecida
- [ ] Autenticação funcionando
- [ ] Notificações sendo recebidas
- [ ] Salas de cidade/categoria funcionando

## 🌐 Configurações Avançadas

### Domínio
- [ ] Domínio customizado configurado (opcional)
- [ ] DNS configurado corretamente
- [ ] SSL funcionando

### Monitoramento
- [ ] Métricas sendo coletadas
- [ ] Logs acessíveis
- [ ] Alertas configurados (opcional)

## 📱 App Mobile

- [ ] URL do backend atualizada no app
- [ ] WebSocket URL atualizada
- [ ] Testes de conexão realizados
- [ ] Funcionalidades testadas:
  - [ ] Login
  - [ ] Atualização de localização
  - [ ] Criação de ordens
  - [ ] Notificações em tempo real

## 🔄 Deploy Contínuo

- [ ] Auto-deploy configurado
- [ ] Branch correto selecionado
- [ ] Notificações de deploy configuradas (opcional)

## 📊 Backup

- [ ] Estratégia de backup definida
- [ ] Backup automático configurado (Railway faz automaticamente)
- [ ] Teste de restore realizado (opcional)

## 🎉 Finalização

- [ ] Tudo funcionando corretamente
- [ ] Documentação atualizada
- [ ] Equipe notificada
- [ ] Monitoramento ativo

---

## 📝 Notas

- ✅ = Concluído
- ⚠️ = Precisa atenção
- ❌ = Erro/Problema

**Data de conclusão:** ___________

**Responsável:** ___________
