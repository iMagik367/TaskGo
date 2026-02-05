# Guia de Deploy no Railway - TaskGo Backend

## 🚀 Deploy Rápido no Railway

### Passo 1: Criar Conta e Projeto

1. Acesse https://railway.app
2. Faça login com GitHub
3. Clique em "New Project"
4. Selecione "Deploy from GitHub repo"
5. Escolha seu repositório

### Passo 2: Adicionar PostgreSQL

1. No projeto, clique em "New"
2. Selecione "Database" → "Add PostgreSQL"
3. Railway criará automaticamente
4. Anote as variáveis de ambiente (serão usadas depois)

### Passo 3: Configurar Backend

1. Railway detectará automaticamente o backend
2. Se não detectar, clique em "New" → "GitHub Repo"
3. Selecione o mesmo repositório
4. Railway criará um serviço

### Passo 4: Configurar Variáveis de Ambiente

No serviço do backend, vá em "Variables" e adicione:

```env
# Database (usar variáveis do PostgreSQL do Railway)
DB_HOST=${{Postgres.PGHOST}}
DB_PORT=${{Postgres.PGPORT}}
DB_NAME=${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}

# Stripe
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Server
PORT=3000
NODE_ENV=production

# Firebase (se necessário para migração)
FIREBASE_PROJECT_ID=...
FIREBASE_CLIENT_EMAIL=...
FIREBASE_PRIVATE_KEY=...
```

**Importante:** Use `${{Postgres.VARIAVEL}}` para referenciar variáveis do PostgreSQL automaticamente.

### Passo 5: Configurar Build e Start

Railway detecta automaticamente, mas você pode configurar manualmente:

**Build Command:**
```bash
cd backend && npm install && npm run build
```

**Start Command:**
```bash
cd backend && npm start
```

### Passo 6: Executar Migrations

Após o deploy, execute as migrations:

1. Vá em "Settings" do serviço PostgreSQL
2. Clique em "Connect" para obter connection string
3. Execute localmente ou via Railway CLI:

```bash
# Instalar Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link ao projeto
railway link

# Executar migrations
railway run psql -d $DATABASE_URL -f database/schema.sql
```

### Passo 7: Verificar Deploy

1. Railway fornecerá uma URL (ex: `https://taskgo-backend.up.railway.app`)
2. Teste o health check: `https://sua-url.railway.app/health`
3. Verifique os logs em tempo real no dashboard

### Passo 8: Configurar Domínio Customizado (Opcional)

1. No serviço do backend, vá em "Settings"
2. Clique em "Generate Domain" ou adicione domínio customizado
3. Configure DNS apontando para Railway

---

## 🔧 Configurações Adicionais

### WebSocket

Railway suporta WebSocket nativamente. Não precisa configuração especial!

### Logs

- Acesse "Deployments" → Selecione deployment → "View Logs"
- Ou use Railway CLI: `railway logs`

### Variáveis de Ambiente

- Todas as variáveis são criptografadas
- Use `${{Service.VARIAVEL}}` para referenciar entre serviços
- Exemplo: `${{Postgres.PGHOST}}` pega o host do PostgreSQL

### Health Checks

Railway verifica automaticamente o endpoint `/health` se configurado.

---

## 📊 Monitoramento

Railway fornece:
- ✅ Logs em tempo real
- ✅ Métricas de CPU/Memória
- ✅ Histórico de deployments
- ✅ Status de saúde dos serviços

---

## 🔄 Deploy Contínuo

Railway faz deploy automático quando você faz push para o branch principal.

Para desabilitar:
1. Vá em "Settings" do serviço
2. Desabilite "Auto Deploy"

---

## 🐛 Troubleshooting

### Erro: "Cannot connect to database"
- Verifique se as variáveis de ambiente estão corretas
- Use `${{Postgres.VARIAVEL}}` para referenciar o PostgreSQL

### Erro: "Build failed"
- Verifique os logs do build
- Certifique-se que `package.json` está correto
- Verifique se todas as dependências estão listadas

### WebSocket não conecta
- Railway suporta WebSocket nativamente
- Verifique se a URL está usando `https://` (não `http://`)
- Verifique os logs para erros de conexão

### Timeout
- Railway tem timeout de 5 minutos
- Para operações longas, use background jobs

---

## 💰 Custos

- **Hobby Plan:** $5/mês (créditos)
- **Pro Plan:** $20/mês (créditos + recursos extras)
- PostgreSQL incluído no plano

**Estimativa de uso:**
- Backend: ~$3-5/mês
- PostgreSQL: Incluído
- **Total: ~$5/mês**

---

## 📝 Checklist Final

- [ ] Conta Railway criada
- [ ] Projeto criado
- [ ] PostgreSQL adicionado
- [ ] Backend deployado
- [ ] Variáveis de ambiente configuradas
- [ ] Migrations executadas
- [ ] Health check funcionando
- [ ] WebSocket testado
- [ ] Domínio configurado (opcional)
- [ ] Monitoramento ativo

---

## 🔗 Links Úteis

- Railway Dashboard: https://railway.app/dashboard
- Documentação: https://docs.railway.app
- Status: https://status.railway.app
- Suporte: https://railway.app/support
