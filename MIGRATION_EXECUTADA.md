# Migration de Autenticação - Status

## ✅ Arquivo SQL Criado

O arquivo `scripts/executar-migration-auth-sql.sql` foi criado com todo o SQL necessário.

## 📋 Como Executar

### Método 1: Railway CLI (Mais Fácil)

```bash
# 1. Instalar Railway CLI (se ainda não tiver)
npm i -g @railway/cli

# 2. Login
railway login

# 3. Linkar ao projeto
railway link

# 4. Conectar ao banco
railway connect

# 5. Copiar e colar o conteúdo de scripts/executar-migration-auth-sql.sql
```

### Método 2: Painel do Railway

1. Acesse o painel do Railway
2. Vá em seu projeto > PostgreSQL service
3. Clique em "Connect" ou "Query"
4. Cole o conteúdo de `scripts/executar-migration-auth-sql.sql`
5. Execute

### Método 3: pgAdmin/DBeaver

1. Obtenha as credenciais do banco no Railway
2. Conecte usando pgAdmin ou DBeaver
3. Execute o SQL de `scripts/executar-migration-auth-sql.sql`

## ✅ Verificar se Funcionou

Execute no banco:

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

## 📝 O que a Migration Faz

1. **Adiciona campos na tabela `users`:**
   - `password_hash` - Hash da senha (bcrypt)
   - `email_verified` - Email verificado
   - `email_verified_at` - Data de verificação
   - `google_id` - ID do Google OAuth
   - `last_login` - Último login
   - `failed_login_attempts` - Tentativas falhadas
   - `locked_until` - Bloqueio temporário

2. **Torna `firebase_uid` opcional** (pode ser NULL)

3. **Cria tabelas:**
   - `refresh_tokens` - Tokens de renovação
   - `two_factor_secrets` - Segredos 2FA
   - `password_reset_tokens` - Tokens de reset de senha
   - `email_verification_tokens` - Tokens de verificação de email

4. **Cria índices** para performance

5. **Cria triggers** para atualização automática

## ⚠️ Importante

- Esta migration é **idempotente** (pode ser executada múltiplas vezes sem problemas)
- Usa `IF NOT EXISTS` para evitar erros se já existir
- Não remove dados existentes
- Compatível com dados antigos (firebase_uid pode ser NULL)
