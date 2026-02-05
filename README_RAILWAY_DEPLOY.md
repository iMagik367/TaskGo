# Railway Deploy - Configuração Completa

## ✅ Arquivos de Configuração

### 1. `nixpacks.toml` (Raiz)
Força o Railway a usar Node.js 18 e npm 9.

### 2. `railway.json`
Configura o build e deploy do Railway.

### 3. `.railwayignore`
Ignora arquivos Android durante o build.

## 🚀 Deploy

1. **Push para GitHub** - Railway detecta automaticamente
2. **Railway faz build** usando `nixpacks.toml`
3. **Instala Node.js** e dependências
4. **Compila TypeScript** (`npm run build`)
5. **Inicia servidor** (`npm start`)

## ⚙️ Variáveis de Ambiente

Configure no Railway Dashboard:

```
DB_HOST=${{Postgres.RAILWAY_PRIVATE_DOMAIN}}
DB_PORT=5432
DB_NAME=${{Postgres.POSTGRES_DB}}
DB_USER=${{Postgres.POSTGRES_USER}}
DB_PASSWORD=${{Postgres.POSTGRES_PASSWORD}}
PORT=3000
NODE_ENV=production
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

## 📱 Atualizar App Mobile

Após deploy, atualize a URL no app:

1. Edite `app/build.gradle.kts`
2. Substitua `https://SUA-URL-RAILWAY.app/api` pela URL real
3. Rebuild do app

Veja `CONFIGURAR_URL_RAILWAY.md` para detalhes.

## 🐛 Troubleshooting

### Erro: "npm: command not found"

**Solução:** O `nixpacks.toml` na raiz deve resolver. Verifique se está commitado.

### Erro: "Cannot connect to database"

**Solução:** Verifique as variáveis `DB_*` no Railway.

### Build falha

**Solução:** Veja os logs no Railway Dashboard → Deployments.
