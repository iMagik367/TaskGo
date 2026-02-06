# Como Executar a Migration de Autenticação

## Opção 1: Usando Railway CLI (Recomendado)

1. Instale o Railway CLI se ainda não tiver:
   ```bash
   npm i -g @railway/cli
   ```

2. Faça login:
   ```bash
   railway login
   ```

3. Conecte ao projeto:
   ```bash
   railway link
   ```

4. Conecte ao banco de dados:
   ```bash
   railway connect
   ```

5. Execute o SQL:
   ```sql
   -- Cole o conteúdo do arquivo scripts/executar-migration-auth-sql.sql
   -- Ou copie e cole diretamente no terminal
   ```

## Opção 2: Usando pgAdmin ou DBeaver

1. Obtenha a string de conexão do Railway:
   - Acesse o painel do Railway
   - Vá em "Variables"
   - Copie o valor de `DATABASE_URL` ou `DATABASE_PUBLIC_URL`

2. Conecte usando pgAdmin ou DBeaver:
   - Host: valor de `PGHOST` ou extraia de `DATABASE_URL`
   - Port: valor de `PGPORT` ou extraia de `DATABASE_URL`
   - Database: valor de `PGDATABASE` ou extraia de `DATABASE_URL`
   - User: valor de `PGUSER` ou extraia de `DATABASE_URL`
   - Password: valor de `PGPASSWORD` ou extraia de `DATABASE_URL`

3. Execute o SQL do arquivo `scripts/executar-migration-auth-sql.sql`

## Opção 3: Usando psql (Linha de Comando)

1. Obtenha a string de conexão do Railway:
   ```bash
   # No Railway, copie DATABASE_URL ou DATABASE_PUBLIC_URL
   ```

2. Execute:
   ```bash
   psql "postgresql://user:password@host:port/database" -f scripts/executar-migration-auth-sql.sql
   ```

## Verificar se Migration Foi Executada

Execute no banco:

```sql
-- Verificar se colunas foram adicionadas
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'users' 
AND column_name IN ('password_hash', 'google_id', 'email_verified');

-- Verificar se tabelas foram criadas
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('refresh_tokens', 'two_factor_secrets', 'password_reset_tokens', 'email_verification_tokens');
```

Se retornar as colunas e tabelas, a migration foi executada com sucesso!
