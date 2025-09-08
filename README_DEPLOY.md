## Deploy do Backend (Ktor) no Render + Banco no Neon

### Objetivo
Configurar o banco PostgreSQL no Neon e publicar o servidor Ktor no Render, ligando os dois com variáveis de ambiente. Depois, criar usuários via API (signup/login) e conectar o app Android.

### Pré‑requisitos
- Este repositório com:
  - `server/app-server` (Ktor + Dockerfile)
  - `render.yaml` na raiz
- Contas no Neon e no Render

### 1) Banco de Dados no Neon
1. Crie um projeto no Neon (Create project)
2. Habilite o Connection Pooler (Session mode) nas configurações do projeto
3. Anote credenciais do Pooler:
   - Host (ex.: ep-xxxxxx-pooler.eu-west-1.aws.neon.tech)
   - Database (ex.: neondb)
   - User: neondb_owner
   - Password: (fornecida pelo Neon)
   - Port: 5432
4. Monte a JDBC URL com SSL obrigatório:
   - `jdbc:postgresql://<POOLER_HOST>:5432/<DATABASE>?sslmode=require`

### 2) Serviço Web no Render (Docker)
1. Confirme que existem:
   - `server/app-server/Dockerfile`
   - `render.yaml` na raiz
2. No Render → New → Blueprint → selecione o repositório
3. Defina variáveis de ambiente no serviço (Environment → Environment Variables):
   - `PORT = 8080`
   - `DB_ENABLE = true`
   - `DB_URL = jdbc:postgresql://<POOLER_HOST>:5432/<DB>?sslmode=require`
   - `DB_USER = neondb_owner`
   - `DB_PASS = <sua_senha_do_neon>`
   - `JWT_SECRET = <um_segredo_forte_aleatório>`
4. Clique em Deploy. Após o build, o Render expõe uma URL tipo `https://<seu-servico>.onrender.com`.

### 3) Migrações (Flyway)
- Com `DB_ENABLE=true`, o servidor aplica as migrações no startup.
- Migração inicial: `server/app-server/src/main/resources/db/migration/V1__init.sql` (tabela `users`).
- Próximas versões: `V2__*.sql`, `V3__*.sql`...

### 4) Testar o Backend
- Health: `GET https://<SERVICO>.onrender.com/health` → 200
- Ready: `GET https://<SERVICO>.onrender.com/ready` → 200
- OpenAPI: `GET https://<SERVICO>.onrender.com/v1/spec`
- Produtos: `GET https://<SERVICO>.onrender.com/v1/products`

### 5) Criar usuários e autenticar (via API)
- Signup:
```
curl -X POST "https://<SERVICO>.onrender.com/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@exemplo.com","password":"SenhaForte!123"}'
```
- Login:
```
curl -X POST "https://<SERVICO>.onrender.com/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@exemplo.com","password":"SenhaForte!123"}'
```
- Usar token em rotas autenticadas (ex.: /v1/me, se disponível):
```
curl -H "Authorization: Bearer <jwt>" https://<SERVICO>.onrender.com/v1/me
```

### 6) Conectar o App Android
1. Em `app/build.gradle.kts`, configure:
   - `buildConfigField("String", "API_BASE_URL", "\"https://<SERVICO>.onrender.com/v1/\"")`
   - `buildConfigField("boolean", "USE_REMOTE_API", "true")`
2. Faça login no app (ou signup). O token será salvo e enviado pelo interceptor.

### 7) Solução de problemas
- SSL: garanta `?sslmode=require` na `DB_URL`.
- Pooler: use o host do Pooler do Neon.
- Flyway: confirme `DB_ENABLE=true` e credenciais corretas.
- 401: refaça login e valide header `Authorization` com Bearer.

### 8) .env de referência
- Veja `server/app-server/.env.example`.


