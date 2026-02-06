# Variáveis de Ambiente para Autenticação

## Variáveis necessárias no Railway

Adicione estas variáveis no painel do Railway para o serviço do backend:

### JWT e Segurança
```
JWT_SECRET=sua-chave-secreta-jwt-muito-segura-aqui-minimo-32-caracteres
JWT_REFRESH_SECRET=sua-chave-secreta-refresh-token-muito-segura-aqui-minimo-32-caracteres
JWT_EXPIRES_IN=15m
JWT_REFRESH_EXPIRES_IN=7d
```

**Como gerar chaves seguras:**
```bash
# No terminal (Linux/Mac)
openssl rand -base64 32

# Ou use um gerador online seguro
```

### Google OAuth
```
GOOGLE_CLIENT_ID=seu-google-client-id.apps.googleusercontent.com
```

**Como obter:**
1. Acesse [Google Cloud Console](https://console.cloud.google.com/)
2. Crie um projeto ou selecione existente
3. Vá em "APIs & Services" > "Credentials"
4. Crie "OAuth 2.0 Client ID"
5. Tipo: "Web application"
6. Copie o Client ID

### Email (SMTP)
```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=seu-email@gmail.com
SMTP_PASS=sua-senha-de-app
SMTP_FROM=TaskGo <seu-email@gmail.com>
APP_URL=https://taskgo-production.up.railway.app
```

**Para Gmail:**
1. Ative "Senhas de app" em [Google Account](https://myaccount.google.com/apppasswords)
2. Gere uma senha de app
3. Use essa senha no `SMTP_PASS`

**Alternativas de SMTP:**
- **SendGrid:** `smtp.sendgrid.net`, porta 587
- **Mailgun:** `smtp.mailgun.org`, porta 587
- **Amazon SES:** `email-smtp.us-east-1.amazonaws.com`, porta 587

### Banco de Dados (já configurado)
```
DATABASE_URL=postgresql://...
DB_HOST=...
DB_PORT=5432
DB_NAME=railway
DB_USER=postgres
DB_PASSWORD=...
```

## Checklist de Configuração

- [ ] JWT_SECRET configurado (mínimo 32 caracteres)
- [ ] JWT_REFRESH_SECRET configurado (diferente do JWT_SECRET)
- [ ] GOOGLE_CLIENT_ID configurado
- [ ] SMTP configurado e testado
- [ ] APP_URL configurado com URL do Railway
- [ ] Migration `004_add_auth_fields.sql` executada no banco

## Testar Configuração

Após configurar, teste os endpoints:

```bash
# Health check
curl https://taskgo-production.up.railway.app/health

# Testar registro
curl -X POST https://taskgo-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@example.com","password":"senha123456","role":"client"}'
```

## Segurança

⚠️ **IMPORTANTE:**
- Nunca commite essas variáveis no Git
- Use variáveis de ambiente sempre
- Rotacione as chaves periodicamente
- Use senhas de app para SMTP (não senha principal)
