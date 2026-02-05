# 🚀 Início Rápido - Railway

## ⚡ Começar em 5 Minutos

### 1️⃣ Criar Projeto e PostgreSQL

1. Acesse: https://railway.app/dashboard
2. Clique em **"+ New Project"**
3. Selecione **"Deploy from GitHub repo"**
4. Escolha seu repositório **TaskGoApp**
5. Clique em **"Deploy Now"**
6. Depois clique em **"+ New"** → **"Database"** → **"Add PostgreSQL"**

✅ Aguarde o PostgreSQL ser criado (30-60 segundos)

---

### 2️⃣ Configurar Backend

1. Railway já deve ter detectado o backend automaticamente
2. Clique no serviço do **backend**
3. Vá em **"Settings"** → **"Build & Deploy"**

**Build Command:**
```
cd backend && npm install && npm run build
```

**Start Command:**
```
cd backend && npm start
```

4. Vá em **"Variables"** e adicione:

```
DB_HOST = ${{Postgres.PGHOST}}
DB_PORT = ${{Postgres.PGPORT}}
DB_NAME = ${{Postgres.PGDATABASE}}
DB_USER = ${{Postgres.PGUSER}}
DB_PASSWORD = ${{Postgres.PGPASSWORD}}
PORT = 3000
NODE_ENV = production
STRIPE_SECRET_KEY = sua_chave_stripe
```

5. Clique em **"Deploy"** ou aguarde o auto-deploy

---

### 3️⃣ Executar Migrations

Abra o terminal e execute:

```bash
# Instalar Railway CLI (se ainda não tiver)
npm install -g @railway/cli

# Login
railway login

# Linkar ao projeto
railway link

# Executar migrations
railway run psql $DATABASE_URL -f database/schema.sql
railway run psql $DATABASE_URL -f database/migrations/002_seed_states_cities.sql
railway run psql $DATABASE_URL -f database/migrations/003_seed_categories.sql
```

---

### 4️⃣ Testar

1. No serviço do backend, vá em **"Settings"** → **"Networking"**
2. Copie a URL gerada (ex: `https://taskgo-backend.up.railway.app`)
3. Teste: `https://sua-url.railway.app/health`

Deve retornar: `{"status":"ok",...}`

---

## ✅ Pronto!

Seu backend está rodando! 🎉

**Próximo passo:** Atualizar o app mobile com a nova URL do backend.

---

## 📚 Documentação Completa

Para detalhes completos, veja: `CONFIGURACAO_RAILWAY_PASSO_A_PASSO.md`
