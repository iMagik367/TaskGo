# Próximos Passos Após Configurar Variáveis - Railway

## ✅ Variáveis Configuradas

Agora vamos:
1. Verificar o deploy
2. Executar migrations do banco
3. Testar o backend
4. Configurar webhook do Stripe
5. Atualizar app mobile

---

## 📊 Passo 1: Verificar Deploy do Backend

### 1.1. Verificar Status

1. No Railway, clique no serviço do **backend**
2. Vá em **"Deployments"**
3. Verifique se o último deployment está:
   - ✅ **Status: Success** (verde)
   - ⚠️ Se estiver em erro, veja os logs

### 1.2. Verificar Logs

1. Clique no deployment mais recente
2. Veja os logs em tempo real
3. Procure por:
   - ✅ "Servidor rodando na porta 3000"
   - ✅ "Conectado ao PostgreSQL"
   - ❌ Erros de conexão ou build

### 1.3. Obter URL do Backend

1. No serviço do backend, vá em **"Settings"**
2. Role até **"Networking"**
3. Você verá a URL gerada (ex: `https://taskgo-backend-production.up.railway.app`)
4. **Anote essa URL** - você vai precisar!

---

## 🗄️ Passo 2: Executar Migrations do Banco

### 2.1. Instalar Railway CLI

```powershell
npm install -g @railway/cli
```

### 2.2. Login no Railway

```powershell
railway login
```

Isso abrirá o navegador para autenticação.

### 2.3. Linkar ao Projeto

```powershell
railway link
```

Selecione o projeto que você criou.

### 2.4. Executar Migrations

Execute na ordem:

```powershell
# 1. Schema principal
railway run psql $DATABASE_URL -f database/schema.sql

# 2. Seed de estados e cidades
railway run psql $DATABASE_URL -f database/migrations/002_seed_states_cities.sql

# 3. Seed de categorias
railway run psql $DATABASE_URL -f database/migrations/003_seed_categories.sql
```

**Aguarde cada comando terminar** antes de executar o próximo.

### 2.5. Verificar Migrations

```powershell
# Verificar tabelas criadas
railway run psql $DATABASE_URL -c "\dt"

# Contar registros
railway run psql $DATABASE_URL -c "SELECT COUNT(*) FROM states;"
railway run psql $DATABASE_URL -c "SELECT COUNT(*) FROM cities;"
railway run psql $DATABASE_URL -c "SELECT COUNT(*) FROM categories;"
```

---

## ✅ Passo 3: Testar Backend

### 3.1. Health Check

Abra no navegador ou use curl:

```bash
curl https://sua-url-railway.app/health
```

**Deve retornar:**
```json
{"status":"ok","timestamp":"2024-01-01T00:00:00.000Z"}
```

### 3.2. Testar Endpoints

#### Testar Atualização de Localização:

```bash
curl -X POST https://sua-url-railway.app/api/location/update \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-id",
    "latitude": -23.5505,
    "longitude": -46.6333
  }'
```

#### Testar Criação de Ordem:

```bash
curl -X POST https://sua-url-railway.app/api/orders/service \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "test-client-id",
    "created_in_city_id": 1,
    "category": "Pintura",
    "details": "Preciso pintar minha casa"
  }'
```

### 3.3. Verificar Logs em Tempo Real

No Railway:
1. Vá em **"Deployments"**
2. Clique no deployment
3. Veja os logs em tempo real
4. Teste os endpoints e veja os logs aparecerem

---

## 🔔 Passo 4: Configurar Webhook do Stripe

### 4.1. Obter URL do Webhook

Sua URL do webhook será:
```
https://sua-url-railway.app/api/stripe/webhook
```

**Substitua** `sua-url-railway.app` pela URL real do seu backend.

### 4.2. Configurar no Stripe Dashboard

1. Acesse: **https://dashboard.stripe.com/webhooks**
2. Clique em **"Add endpoint"** (ou edite o existente)
3. **Endpoint URL:** Cole a URL acima
4. **Events to send:** Selecione:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `account.updated`
5. Clique em **"Add endpoint"**
6. **Copie o "Signing secret"** (whsec_...)
7. Adicione no Railway como `STRIPE_WEBHOOK_SECRET` (se ainda não adicionou)

---

## 📱 Passo 5: Atualizar App Mobile

### 5.1. Encontrar Arquivos de Configuração

Procure por arquivos que contêm URLs da API:

```bash
# Buscar arquivos com URLs da API
grep -r "firebase.*functions" app/src/
grep -r "api.*url" app/src/
grep -r "BASE_URL" app/src/
```

### 5.2. Atualizar URLs

Substitua as URLs antigas pela nova URL do Railway:

**Antes (Firebase Functions):**
```
https://us-central1-task-go-ee85f.cloudfunctions.net
```

**Depois (Railway):**
```
https://sua-url-railway.app
```

### 5.3. Atualizar WebSocket

Se houver configuração de WebSocket, atualize:

**Antes:**
```
wss://us-central1-task-go-ee85f.cloudfunctions.net
```

**Depois:**
```
wss://sua-url-railway.app
```

Ou simplesmente:
```
https://sua-url-railway.app
```

(Socket.io detecta automaticamente)

---

## 🧪 Passo 6: Testar Funcionalidades Completas

### 6.1. Testar Localização Dinâmica

1. Abra o app
2. Permita acesso à localização
3. Verifique se a localização é atualizada no backend
4. Veja os logs no Railway

### 6.2. Testar Notificações em Tempo Real

1. Como parceiro, entre em uma cidade
2. Como cliente, crie uma ordem de serviço
3. Verifique se o parceiro recebe notificação em tempo real

### 6.3. Testar Pagamentos

1. Faça um pedido de produto
2. Teste pagamento (use cartão de teste do Stripe)
3. Verifique se o webhook é chamado
4. Veja os logs no Railway

---

## 📋 Checklist Final

- [ ] Backend deployado com sucesso
- [ ] URL do backend anotada
- [ ] Migrations executadas
- [ ] Health check funcionando
- [ ] Endpoints testados
- [ ] Webhook do Stripe configurado
- [ ] App mobile atualizado com nova URL
- [ ] Localização dinâmica testada
- [ ] Notificações em tempo real testadas
- [ ] Pagamentos testados

---

## 🐛 Troubleshooting

### Erro: "Cannot connect to database"

**Solução:**
1. Verifique se as variáveis `DB_*` estão corretas
2. Verifique se o PostgreSQL está rodando (Status: Running)
3. Teste a conexão: `railway run psql $DATABASE_URL -c "SELECT 1;"`

### Erro: "Build failed"

**Solução:**
1. Veja os logs do build
2. Verifique se `backend/package.json` está correto
3. Verifique se todas as dependências estão listadas

### WebSocket não conecta

**Solução:**
1. Use `https://` (não `http://`)
2. Verifique os logs do backend
3. Teste com um cliente WebSocket simples

### Migrations não executam

**Solução:**
1. Verifique se os arquivos SQL existem
2. Verifique se o caminho está correto
3. Execute manualmente via Railway CLI

---

## 🎉 Pronto!

Seu backend está rodando no Railway! 

**Próximos passos:**
1. Testar todas as funcionalidades
2. Migrar dados do Firestore (se necessário)
3. Monitorar logs e métricas
4. Configurar alertas (opcional)

---

## 📞 Precisa de Ajuda?

- Railway Docs: https://docs.railway.app
- Logs: Railway Dashboard → Deployments → View Logs
- Suporte: https://railway.app/support
