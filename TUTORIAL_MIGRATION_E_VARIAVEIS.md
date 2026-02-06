# Tutorial Completo: Executar Migration e Configurar Variáveis

## 📋 Índice

1. [Executar Migration no Banco de Dados](#executar-migration)
2. [Configurar Variáveis de Ambiente no Railway](#configurar-variaveis)
3. [Verificar se Tudo Está Funcionando](#verificar)

---

## 1. Executar Migration no Banco de Dados {#executar-migration}

### Opção 1: Usando Railway CLI (Recomendado)

#### Passo 1: Instalar Railway CLI

```bash
npm i -g @railway/cli
```

#### Passo 2: Fazer Login

```bash
railway login
```

Isso abrirá o navegador para autenticação.

#### Passo 3: Linkar ao Projeto

```bash
railway link
```

Selecione o projeto TaskGo quando solicitado.

#### Passo 4: Conectar ao Banco de Dados

```bash
railway connect
```

Isso abrirá uma conexão interativa com o PostgreSQL.

#### Passo 5: Executar o SQL

1. Abra o arquivo `scripts/executar-migration-auth-sql.sql`
2. Copie TODO o conteúdo do arquivo
3. Cole no terminal que abriu com `railway connect`
4. Pressione Enter

Você deve ver a mensagem:
```
Migration 004_add_auth_fields executada com sucesso!
```

#### Passo 6: Verificar se Funcionou

Execute no terminal:

```sql
-- Verificar colunas adicionadas
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'users' 
AND column_name IN ('password_hash', 'google_id', 'email_verified', 'last_login', 'failed_login_attempts', 'locked_until');

-- Verificar tabelas criadas
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('refresh_tokens', 'two_factor_secrets', 'password_reset_tokens', 'email_verification_tokens');
```

Se retornar todas as colunas e tabelas, a migration foi executada com sucesso! ✅

---

### Opção 2: Usando pgAdmin ou DBeaver

#### Passo 1: Obter Credenciais do Banco

1. Acesse o painel do Railway: https://railway.app
2. Vá em seu projeto > PostgreSQL service
3. Clique em "Variables" ou "Connect"
4. Copie os valores de:
   - `PGHOST` ou extraia de `DATABASE_URL`
   - `PGPORT` ou extraia de `DATABASE_URL`
   - `PGDATABASE` ou extraia de `DATABASE_URL`
   - `PGUSER` ou extraia de `DATABASE_URL`
   - `PGPASSWORD` ou extraia de `DATABASE_URL`

#### Passo 2: Conectar no pgAdmin/DBeaver

**pgAdmin:**
1. Abra pgAdmin
2. Clique com botão direito em "Servers" > "Create" > "Server"
3. Na aba "Connection":
   - Host: valor de `PGHOST`
   - Port: valor de `PGPORT`
   - Database: valor de `PGDATABASE`
   - Username: valor de `PGUSER`
   - Password: valor de `PGPASSWORD`
4. Clique em "Save"

**DBeaver:**
1. Abra DBeaver
2. Clique em "New Database Connection"
3. Selecione "PostgreSQL"
4. Preencha:
   - Host: valor de `PGHOST`
   - Port: valor de `PGPORT`
   - Database: valor de `PGDATABASE`
   - Username: valor de `PGUSER`
   - Password: valor de `PGPASSWORD`
5. Clique em "Test Connection" e depois "Finish"

#### Passo 3: Executar SQL

1. Abra o arquivo `scripts/executar-migration-auth-sql.sql`
2. Copie TODO o conteúdo
3. No pgAdmin/DBeaver, abra o Query Tool / SQL Editor
4. Cole o SQL
5. Execute (F5 ou botão "Execute")

---

### Opção 3: Usando Painel do Railway

1. Acesse o painel do Railway: https://railway.app
2. Vá em seu projeto > PostgreSQL service
3. Clique em "Query" ou "Connect"
4. Abra o arquivo `scripts/executar-migration-auth-sql.sql`
5. Copie TODO o conteúdo
6. Cole no editor SQL do Railway
7. Clique em "Run" ou "Execute"

---

## 2. Configurar Variáveis de Ambiente no Railway {#configurar-variaveis}

### Passo 1: Acessar Variáveis

1. Acesse o painel do Railway: https://railway.app
2. Vá em seu projeto > **Backend Service** (não o PostgreSQL)
3. Clique em **"Variables"** na barra lateral

### Passo 2: Adicionar Variáveis

Para cada variável abaixo, clique em **"New Variable"** e adicione:

#### 2.1. JWT_SECRET

**Nome:** `JWT_SECRET`  
**Valor:** Gere uma chave segura de 32+ caracteres

**Como gerar:**
- Opção 1: Use um gerador online seguro (https://randomkeygen.com/)
- Opção 2: Execute no terminal: `openssl rand -base64 32`
- Opção 3: Use uma string aleatória longa e complexa

**Exemplo:** `aB3$kL9#mN2@pQ7&rS5*tU1!vW4^xY6%zA8`

#### 2.2. JWT_REFRESH_SECRET

**Nome:** `JWT_REFRESH_SECRET`  
**Valor:** Gere OUTRA chave segura (diferente do JWT_SECRET)

⚠️ **IMPORTANTE:** Use um valor DIFERENTE do JWT_SECRET!

#### 2.3. JWT_EXPIRES_IN (Opcional)

**Nome:** `JWT_EXPIRES_IN`  
**Valor:** `15m`

Este é opcional, o padrão já é 15m.

#### 2.4. JWT_REFRESH_EXPIRES_IN (Opcional)

**Nome:** `JWT_REFRESH_EXPIRES_IN`  
**Valor:** `7d`

Este é opcional, o padrão já é 7d.

#### 2.5. GOOGLE_CLIENT_ID

**Nome:** `GOOGLE_CLIENT_ID`  
**Valor:** Seu Client ID do Google OAuth

**Como obter:**
1. Acesse: https://console.cloud.google.com/
2. Crie um projeto ou selecione existente
3. Vá em **"APIs & Services"** > **"Credentials"**
4. Clique em **"Create Credentials"** > **"OAuth 2.0 Client ID"**
5. Tipo: **"Web application"**
6. Copie o **"Client ID"** (formato: `xxxxx.apps.googleusercontent.com`)

#### 2.6. SMTP_HOST

**Nome:** `SMTP_HOST`  
**Valor:** `smtp.gmail.com`

(Se usar outro provedor, ajuste conforme necessário)

#### 2.7. SMTP_PORT

**Nome:** `SMTP_PORT`  
**Valor:** `587`

#### 2.8. SMTP_USER

**Nome:** `SMTP_USER`  
**Valor:** Seu email (ex: `seu-email@gmail.com`)

#### 2.9. SMTP_PASS

**Nome:** `SMTP_PASS`  
**Valor:** Senha de app do Gmail (NÃO use sua senha principal!)

**Como obter senha de app (Gmail):**
1. Acesse: https://myaccount.google.com/apppasswords
2. Se necessário, ative "Verificação em duas etapas" primeiro
3. Selecione "Mail" e "Other (Custom name)"
4. Digite "TaskGo"
5. Clique em "Generate"
6. Copie a senha de 16 caracteres (sem espaços)

#### 2.10. SMTP_FROM

**Nome:** `SMTP_FROM`  
**Valor:** `TaskGo <seu-email@gmail.com>`

Substitua `seu-email@gmail.com` pelo seu email.

#### 2.11. APP_URL

**Nome:** `APP_URL`  
**Valor:** URL do seu serviço no Railway

**Como encontrar:**
1. No Railway, vá em seu projeto > Backend Service
2. Clique em **"Settings"**
3. Vá em **"Domains"**
4. Copie a URL (ex: `https://taskgo-production.up.railway.app`)

---

### Passo 3: Verificar Variáveis

Após adicionar todas, você deve ter:

✅ JWT_SECRET  
✅ JWT_REFRESH_SECRET  
✅ GOOGLE_CLIENT_ID  
✅ SMTP_HOST  
✅ SMTP_PORT  
✅ SMTP_USER  
✅ SMTP_PASS  
✅ SMTP_FROM  
✅ APP_URL  

(Opcional: JWT_EXPIRES_IN, JWT_REFRESH_EXPIRES_IN)

### Passo 4: Reiniciar Serviço

O Railway reinicia automaticamente quando você adiciona variáveis. Mas se quiser forçar:

1. Vá em **"Deployments"**
2. Clique nos três pontos do último deployment
3. Selecione **"Redeploy"**

---

## 3. Verificar se Tudo Está Funcionando {#verificar}

### 3.1. Verificar Health Check

```bash
curl https://taskgo-production.up.railway.app/health
```

**Resposta esperada:**
```json
{
  "status": "ok",
  "timestamp": "2025-02-05T...",
  "database": "checking..."
}
```

### 3.2. Testar Registro

```bash
curl -X POST https://taskgo-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "senha123456",
    "role": "client"
  }'
```

**Resposta esperada (sucesso):**
```json
{
  "status": "success",
  "message": "Usuário criado com sucesso. Verifique seu email para ativar a conta.",
  "data": {
    "user_id": "uuid-aqui",
    "email": "teste@example.com"
  }
}
```

### 3.3. Verificar Logs

1. No Railway, vá em **"Deployments"**
2. Clique no último deployment
3. Vá em **"Logs"**
4. Verifique se não há erros relacionados a:
   - Variáveis não encontradas
   - Conexão com banco
   - SMTP

### 3.4. Verificar Email

Após registrar um usuário, verifique se recebeu o email de verificação na caixa de entrada (ou spam).

---

## 🐛 Troubleshooting

### Erro: "JWT_SECRET não encontrado"

- Verifique se adicionou `JWT_SECRET` nas variáveis
- Verifique se o nome está correto (case-sensitive)
- Reinicie o serviço

### Erro: "Cannot connect to database"

- Verifique se a migration foi executada
- Verifique se `DATABASE_URL` está configurado (já vem do Railway)
- Verifique logs do Railway

### Erro: "Email não enviado"

- Verifique configurações SMTP
- Para Gmail, use senha de app (não senha principal)
- Verifique se `SMTP_FROM` está no formato correto
- Verifique logs do Railway

### Erro: "Google OAuth failed"

- Verifique se `GOOGLE_CLIENT_ID` está correto
- Verifique se o Client ID está ativo no Google Cloud Console
- Verifique logs do Railway

### Migration não executada

- Execute a migration novamente (é idempotente, pode executar múltiplas vezes)
- Verifique se as tabelas foram criadas (veja seção de verificação)

---

## ✅ Checklist Final

- [ ] Migration executada no banco
- [ ] Todas as variáveis configuradas no Railway
- [ ] Health check funcionando
- [ ] Registro de usuário funcionando
- [ ] Email de verificação sendo enviado
- [ ] Logs sem erros críticos

---

## 📚 Arquivos de Referência

- `scripts/executar-migration-auth-sql.sql` - SQL da migration
- `VARIAVEIS_RAILWAY_COMPLETO.txt` - Lista de variáveis
- `TESTE_ENDPOINTS.md` - Guia de testes

---

**Pronto!** Seu sistema de autenticação está configurado e funcionando! 🚀
