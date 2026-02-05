# Variáveis de Ambiente - Railway Backend

## 📋 Variáveis para o Serviço Backend

Copie e cole estas variáveis no serviço do **backend** (não no PostgreSQL):

### Database (Referenciando o PostgreSQL)

```
DB_HOST = ${{Postgres.RAILWAY_PRIVATE_DOMAIN}}
DB_PORT = 5432
DB_NAME = ${{Postgres.POSTGRES_DB}}
DB_USER = ${{Postgres.POSTGRES_USER}}
DB_PASSWORD = ${{Postgres.POSTGRES_PASSWORD}}
```

**OU** (se preferir usar as variáveis PGHOST, etc.):

```
DB_HOST = ${{Postgres.PGHOST}}
DB_PORT = ${{Postgres.PGPORT}}
DB_NAME = ${{Postgres.PGDATABASE}}
DB_USER = ${{Postgres.PGUSER}}
DB_PASSWORD = ${{Postgres.PGPASSWORD}}
```

### Server

```
PORT = 3000
NODE_ENV = production
```

### Stripe (Adicione suas chaves reais)

```
STRIPE_SECRET_KEY = sk_live_SUA_CHAVE_AQUI
STRIPE_WEBHOOK_SECRET = whsec_SEU_SECRET_AQUI
```

### Firebase (Se necessário para migração)

```
FIREBASE_PROJECT_ID = seu_project_id
FIREBASE_CLIENT_EMAIL = seu_client_email
FIREBASE_PRIVATE_KEY = -----BEGIN PRIVATE KEY-----\nsua_chave_completa_aqui\n-----END PRIVATE KEY-----
```

---

## 🔍 Como Adicionar no Railway

1. No dashboard do Railway, clique no serviço do **backend**
2. Vá na aba **"Variables"**
3. Clique em **"+ New Variable"**
4. Cole cada variável acima (nome = valor)
5. Clique em **"Add"** para cada uma
6. Salve

---

## ⚠️ Importante

- **NÃO edite as variáveis do PostgreSQL** - elas estão corretas!
- Adicione essas variáveis apenas no serviço do **backend**
- Use `${{Postgres.VARIAVEL}}` para referenciar variáveis do PostgreSQL
- O Railway substitui automaticamente essas referências

---

## ✅ Verificação

Após adicionar, verifique:

1. No serviço do backend, vá em **"Variables"**
2. Você deve ver todas as variáveis listadas
3. As variáveis que começam com `${{Postgres.` devem mostrar o valor real quando você clicar nelas

---

## 🐛 Se não funcionar

Se as referências `${{Postgres.VARIAVEL}}` não funcionarem, use os valores diretos do PostgreSQL:

1. No serviço **Postgres**, vá em **"Variables"**
2. Anote os valores de:
   - `RAILWAY_PRIVATE_DOMAIN` (ou `PGHOST`)
   - `POSTGRES_DB` (ou `PGDATABASE`)
   - `POSTGRES_USER` (ou `PGUSER`)
   - `POSTGRES_PASSWORD` (ou `PGPASSWORD`)
3. Use esses valores diretamente no backend (sem `${{Postgres.`)

**Exemplo direto:**
```
DB_HOST = dpg-xxxxx-a.railway.app
DB_PORT = 5432
DB_NAME = railway
DB_USER = postgres
DB_PASSWORD = zvaeSjfZVeeGhoyznVDirVEfxZiRWFMk
```
