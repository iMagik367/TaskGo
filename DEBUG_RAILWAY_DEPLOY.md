# Debug Railway Deploy - Guia Completo

## 🔍 Problemas Corrigidos

### 1. Dockerfile
- ✅ Agora lida com ausência de `package-lock.json`
- ✅ Usa `npm install` se não houver lock file

### 2. Servidor
- ✅ Inicia mesmo se banco não estiver disponível
- ✅ Listen em `0.0.0.0` (aceita conexões externas)
- ✅ Health check funciona sempre

### 3. Tratamento de Erros
- ✅ Não crasha em erros de banco
- ✅ Logs mais informativos

## 📋 Como Verificar o Deploy

### 1. Verificar Logs no Railway

1. Acesse: https://railway.app/dashboard
2. Clique no seu projeto
3. Clique no serviço do **backend**
4. Vá em **Deployments**
5. Clique no deployment mais recente
6. Veja os logs em tempo real

**O que procurar:**
- ✅ `🚀 Servidor rodando na porta 3000`
- ✅ `✅ Conectado ao PostgreSQL` (ou aviso se não conectar)
- ❌ Erros de build
- ❌ Erros de conexão

### 2. Verificar Variáveis de Ambiente

No Railway Dashboard → Backend → Variables, verifique:

```
DB_HOST=${{Postgres.RAILWAY_PRIVATE_DOMAIN}}
DB_PORT=5432
DB_NAME=${{Postgres.POSTGRES_DB}}
DB_USER=${{Postgres.POSTGRES_USER}}
DB_PASSWORD=${{Postgres.POSTGRES_PASSWORD}}
PORT=3000
NODE_ENV=production
```

**Importante:** Se as variáveis `${{Postgres.*}}` não funcionarem, use os valores diretos do serviço Postgres.

### 3. Verificar Build

Nos logs, procure por:
- ✅ `npm install` executado
- ✅ `npm run build` executado
- ✅ `npm start` executado
- ❌ Erros de compilação TypeScript

### 4. Testar Health Check

Após o deploy, teste:

```bash
curl https://taskgo-production.up.railway.app/health
```

**Deve retornar:**
```json
{"status":"ok","timestamp":"2024-01-01T00:00:00.000Z","database":"checking..."}
```

Se retornar 404, o serviço não está rodando. Verifique os logs.

## 🐛 Problemas Comuns

### Erro 404 "Application not found"

**Causas possíveis:**
1. Servidor não iniciou (ver logs)
2. Variáveis de ambiente incorretas
3. Build falhou
4. Railway não está usando o Dockerfile

**Solução:**
1. Verifique os logs do Railway
2. Verifique se o Dockerfile está sendo usado
3. Verifique variáveis de ambiente

### Erro de Conexão com Banco

**Sintoma:** Logs mostram erro de conexão PostgreSQL

**Solução:**
1. Verifique variáveis `DB_*` no Railway
2. Verifique se o serviço Postgres está rodando
3. Teste conexão manualmente

### Build Falha

**Sintoma:** Logs mostram erro no `npm run build`

**Solução:**
1. Verifique se há erros de TypeScript
2. Verifique se todas as dependências estão instaladas
3. Veja os logs completos do build

## ✅ Checklist de Verificação

- [ ] Dockerfile está no repositório
- [ ] railway.json configurado para usar Dockerfile
- [ ] Variáveis de ambiente configuradas
- [ ] Serviço Postgres está rodando
- [ ] Build concluído com sucesso (ver logs)
- [ ] Servidor iniciou (ver logs)
- [ ] Health check responde (teste com curl)

## 📞 Próximos Passos

1. **Aguarde o deploy** (pode levar 2-5 minutos)
2. **Verifique os logs** no Railway Dashboard
3. **Teste o health check** com curl
4. **Se ainda não funcionar**, compartilhe os logs do Railway
