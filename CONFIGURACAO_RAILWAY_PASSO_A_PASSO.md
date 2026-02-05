# Configuração Railway - Passo a Passo Completo

## ✅ Pré-requisitos Concluídos
- [x] Conta Railway criada
- [x] Plano Pro ativado

---

## 📋 Passo 1: Criar Novo Projeto

1. Acesse https://railway.app/dashboard
2. Clique no botão **"+ New Project"** (canto superior direito)
3. Selecione **"Deploy from GitHub repo"**
4. Autorize o Railway a acessar seu GitHub (se necessário)
5. Selecione o repositório **TaskGoApp**
6. Clique em **"Deploy Now"**

Railway começará a detectar automaticamente seu projeto.

---

## 🗄️ Passo 2: Adicionar PostgreSQL

1. No projeto criado, clique no botão **"+ New"** (canto superior direito)
2. Selecione **"Database"**
3. Escolha **"Add PostgreSQL"**
4. Railway criará automaticamente o banco de dados

**Aguarde alguns segundos** enquanto o PostgreSQL é provisionado.

### Obter Variáveis do PostgreSQL:

1. Clique no serviço **Postgres** que foi criado
2. Vá na aba **"Variables"**
3. Você verá as seguintes variáveis:
   - `PGHOST`
   - `PGPORT`
   - `PGDATABASE`
   - `PGUSER`
   - `PGPASSWORD`

**Anote essas variáveis** - vamos usá-las depois!

---

## 🚀 Passo 3: Configurar Backend

### 3.1. Verificar se o Backend foi Detectado

Railway deve ter detectado automaticamente o backend. Se não:

1. Clique em **"+ New"** → **"GitHub Repo"**
2. Selecione o mesmo repositório
3. Railway criará um novo serviço

### 3.2. Configurar Build e Start

1. Clique no serviço do **backend** (ou o serviço criado)
2. Vá em **"Settings"**
3. Role até **"Build & Deploy"**

Configure:

**Build Command:**
```bash
cd backend && npm install && npm run build
```

**Start Command:**
```bash
cd backend && npm start
```

**Root Directory:** (deixe vazio ou `/`)

### 3.3. Configurar Variáveis de Ambiente

1. No serviço do backend, vá na aba **"Variables"**
2. Clique em **"+ New Variable"**

Adicione as seguintes variáveis:

#### Database (usar referências do PostgreSQL):
```
DB_HOST = ${{Postgres.PGHOST}}
DB_PORT = ${{Postgres.PGPORT}}
DB_NAME = ${{Postgres.PGDATABASE}}
DB_USER = ${{Postgres.PGUSER}}
DB_PASSWORD = ${{Postgres.PGPASSWORD}}
```

**Importante:** Use exatamente `${{Postgres.VARIAVEL}}` - Railway substitui automaticamente!

#### Server:
```
PORT = 3000
NODE_ENV = production
```

#### Stripe:
```
STRIPE_SECRET_KEY = sk_live_SEU_SECRET_KEY_AQUI
STRIPE_WEBHOOK_SECRET = whsec_SEU_WEBHOOK_SECRET_AQUI
```

#### Firebase (se necessário para migração):
```
FIREBASE_PROJECT_ID = seu_project_id
FIREBASE_CLIENT_EMAIL = seu_client_email
FIREBASE_PRIVATE_KEY = sua_private_key_completa
```

**Dica:** Para `FIREBASE_PRIVATE_KEY`, cole a chave completa incluindo `-----BEGIN PRIVATE KEY-----` e `-----END PRIVATE KEY-----`

### 3.4. Salvar e Aguardar Deploy

1. Clique em **"Save"** ou **"Deploy"**
2. Railway começará a fazer o build automaticamente
3. Acompanhe os logs em tempo real na aba **"Deployments"**

---

## 📊 Passo 4: Executar Migrations do Banco

Após o deploy do backend, precisamos executar as migrations do PostgreSQL.

### Opção 1: Via Railway CLI (Recomendado)

1. **Instalar Railway CLI:**
```bash
npm install -g @railway/cli
```

2. **Login no Railway:**
```bash
railway login
```
Isso abrirá o navegador para autenticação.

3. **Linkar ao projeto:**
```bash
railway link
```
Selecione o projeto que você criou.

4. **Executar migrations:**
```bash
# Executar schema principal
railway run psql $DATABASE_URL -f database/schema.sql

# Executar seed de estados e cidades
railway run psql $DATABASE_URL -f database/migrations/002_seed_states_cities.sql

# Executar seed de categorias
railway run psql $DATABASE_URL -f database/migrations/003_seed_categories.sql
```

### Opção 2: Via Dashboard Railway

1. No serviço **Postgres**, vá em **"Connect"**
2. Copie a **Connection String**
3. Use um cliente PostgreSQL (pgAdmin, DBeaver, etc.) para conectar
4. Execute os arquivos SQL manualmente:
   - `database/schema.sql`
   - `database/migrations/002_seed_states_cities.sql`
   - `database/migrations/003_seed_categories.sql`

### Opção 3: Via Script de Migração

Se você tem o script de migração do Firestore:

```bash
railway run npm run migrate:firestore
```

---

## ✅ Passo 5: Verificar Deploy

### 5.1. Verificar URL do Backend

1. No serviço do backend, vá em **"Settings"**
2. Role até **"Networking"**
3. Você verá a URL gerada (ex: `https://taskgo-backend-production.up.railway.app`)
4. Clique em **"Generate Domain"** se ainda não tiver

### 5.2. Testar Health Check

Abra no navegador ou use curl:
```bash
curl https://sua-url.railway.app/health
```

Deve retornar:
```json
{"status":"ok","timestamp":"2024-01-01T00:00:00.000Z"}
```

### 5.3. Verificar Logs

1. No serviço do backend, vá em **"Deployments"**
2. Clique no deployment mais recente
3. Veja os logs em tempo real
4. Procure por erros ou avisos

---

## 🔧 Passo 6: Configurar Domínio Customizado (Opcional)

1. No serviço do backend, vá em **"Settings"**
2. Role até **"Networking"**
3. Clique em **"Custom Domain"**
4. Adicione seu domínio (ex: `api.taskgo.com`)
5. Configure os registros DNS conforme instruções do Railway:
   - Tipo: `CNAME`
   - Nome: `api` (ou `@` para domínio raiz)
   - Valor: `sua-url.railway.app`

---

## 🌐 Passo 7: Configurar WebSocket

Railway suporta WebSocket nativamente! Não precisa configuração especial.

### Testar WebSocket:

1. Use a URL do backend (deve usar `https://`)
2. Conecte via Socket.io:
```javascript
const io = require('socket.io-client');
const socket = io('https://sua-url.railway.app');

socket.on('connect', () => {
  console.log('Conectado!');
});
```

---

## 📝 Passo 8: Atualizar App Mobile

Após o deploy, atualize o app mobile para usar a nova URL:

1. **Backend URL:** `https://sua-url.railway.app`
2. **WebSocket URL:** `wss://sua-url.railway.app` (ou `https://` - Socket.io detecta automaticamente)

### Onde atualizar no app:

Procure por arquivos de configuração como:
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/.../config/ApiConfig.kt`
- Ou arquivos de constantes de API

---

## 🔍 Passo 9: Verificar Funcionalidades

### Testar Endpoints:

```bash
# Health check
curl https://sua-url.railway.app/health

# Atualizar localização
curl -X POST https://sua-url.railway.app/api/location/update \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","latitude":-23.5505,"longitude":-46.6333}'

# Criar ordem
curl -X POST https://sua-url.railway.app/api/orders/service \
  -H "Content-Type: application/json" \
  -d '{"client_id":"test","created_in_city_id":1,"category":"Pintura","details":"Teste"}'
```

---

## 🐛 Troubleshooting

### Erro: "Cannot connect to database"

**Solução:**
1. Verifique se as variáveis de ambiente estão corretas
2. Use `${{Postgres.VARIAVEL}}` (não valores hardcoded)
3. Verifique se o PostgreSQL está rodando (Status: Running)

### Erro: "Build failed"

**Solução:**
1. Veja os logs do build
2. Verifique se `backend/package.json` está correto
3. Verifique se todas as dependências estão listadas
4. Tente fazer build local: `cd backend && npm install && npm run build`

### Erro: "Module not found"

**Solução:**
1. Verifique se todas as dependências estão em `package.json`
2. Execute `npm install` localmente e verifique erros
3. Verifique se o `Root Directory` está correto

### WebSocket não conecta

**Solução:**
1. Use `https://` (não `http://`)
2. Railway suporta WebSocket nativamente
3. Verifique os logs para erros de conexão
4. Teste com um cliente WebSocket simples primeiro

### Timeout no deploy

**Solução:**
1. Railway tem timeout de 5 minutos no build
2. Otimize o build (remova dependências desnecessárias)
3. Use cache do npm se possível

---

## 📊 Monitoramento

### Ver Métricas:

1. No serviço do backend, vá em **"Metrics"**
2. Veja:
   - CPU usage
   - Memory usage
   - Network traffic
   - Request count

### Ver Logs:

1. Vá em **"Deployments"**
2. Clique no deployment
3. Veja logs em tempo real
4. Use filtros para buscar erros

---

## 🔄 Deploy Contínuo

Railway faz deploy automático quando você faz push para o branch principal.

### Para configurar branch específico:

1. Vá em **"Settings"** do serviço
2. Role até **"Source"**
3. Configure o branch desejado

### Para desabilitar auto-deploy:

1. Vá em **"Settings"**
2. Desabilite **"Auto Deploy"**

---

## ✅ Checklist Final

- [ ] Projeto criado no Railway
- [ ] PostgreSQL adicionado e rodando
- [ ] Backend deployado com sucesso
- [ ] Variáveis de ambiente configuradas
- [ ] Migrations executadas
- [ ] Health check funcionando (`/health`)
- [ ] URL do backend anotada
- [ ] WebSocket testado
- [ ] App mobile atualizado com nova URL
- [ ] Domínio customizado configurado (opcional)
- [ ] Monitoramento ativo

---

## 🎉 Pronto!

Seu backend está rodando no Railway! 

**Próximos passos:**
1. Testar todas as funcionalidades
2. Migrar dados do Firestore (se necessário)
3. Configurar monitoramento e alertas
4. Fazer backup do banco de dados regularmente

---

## 📞 Suporte

- Railway Docs: https://docs.railway.app
- Railway Status: https://status.railway.app
- Suporte Railway: https://railway.app/support
