# Guia de Teste dos Endpoints de Autenticação

## Pré-requisitos

1. ✅ Migration executada no banco
2. ✅ Variáveis de ambiente configuradas no Railway
3. ✅ Backend deployado e rodando

## 1. Testar Health Check

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

## 2. Testar Registro

```bash
curl -X POST https://taskgo-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "senha123456",
    "display_name": "Usuário Teste",
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

**Resposta esperada (erro - email já existe):**
```json
{
  "status": "error",
  "code": 409,
  "message": "Email já cadastrado"
}
```

## 3. Testar Login

```bash
curl -X POST https://taskgo-production.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "senha123456"
  }'
```

**Resposta esperada (sucesso):**
```json
{
  "status": "success",
  "data": {
    "user": {
      "id": "uuid-aqui",
      "email": "teste@example.com",
      "role": "client",
      "display_name": "Usuário Teste",
      "email_verified": false,
      "two_factor_enabled": false
    },
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "uuid-uuid-uuid-uuid",
    "expires_in": 900
  }
}
```

**Resposta esperada (erro - credenciais inválidas):**
```json
{
  "status": "error",
  "code": 401,
  "message": "Credenciais inválidas"
}
```

## 4. Testar Refresh Token

```bash
# Primeiro, faça login e copie o refresh_token da resposta
curl -X POST https://taskgo-production.up.railway.app/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "seu-refresh-token-aqui"
  }'
```

**Resposta esperada:**
```json
{
  "status": "success",
  "data": {
    "access_token": "novo-token-aqui",
    "expires_in": 900
  }
}
```

## 5. Testar Endpoint Protegido

```bash
# Use o access_token obtido no login
curl -X GET https://taskgo-production.up.railway.app/api/users/me \
  -H "Authorization: Bearer seu-access-token-aqui"
```

**Resposta esperada (autenticado):**
```json
{
  "status": "success",
  "data": {
    "user": { ... }
  }
}
```

**Resposta esperada (não autenticado):**
```json
{
  "status": "error",
  "code": 401,
  "message": "Token de autenticação não fornecido"
}
```

## 6. Testar Forgot Password

```bash
curl -X POST https://taskgo-production.up.railway.app/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com"
  }'
```

**Resposta esperada:**
```json
{
  "status": "success",
  "message": "Se o email existir, um link de reset foi enviado"
}
```

## 7. Testar Reset Password

```bash
# Use o token recebido por email
curl -X POST https://taskgo-production.up.railway.app/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "token-recebido-por-email",
    "new_password": "novaSenha123456"
  }'
```

## 8. Testar Logout

```bash
curl -X POST https://taskgo-production.up.railway.app/api/auth/logout \
  -H "Authorization: Bearer seu-access-token-aqui" \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "seu-refresh-token-aqui"
  }'
```

## 9. Testar Google Login

```bash
# Primeiro, obtenha um ID token do Google (via app ou web)
curl -X POST https://taskgo-production.up.railway.app/api/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "id_token": "google-id-token-aqui"
  }'
```

## Testes no App Android

### 1. Testar Registro
- Abra o app
- Vá em "Criar conta"
- Preencha email, senha, nome
- Clique em "Registrar"
- Verifique se recebe mensagem de sucesso
- Verifique email de verificação

### 2. Testar Login
- Abra o app
- Digite email e senha
- Clique em "Entrar"
- Verifique se faz login com sucesso
- Verifique se navega para a tela principal

### 3. Testar Login com Google
- Abra o app
- Clique em "Entrar com Google"
- Selecione conta Google
- Verifique se faz login com sucesso

### 4. Testar Biometria
- Após fazer login
- Feche o app
- Abra novamente
- Verifique se oferece biometria
- Teste login com biometria

### 5. Testar Logout
- Vá em Configurações > Conta
- Clique em "Sair"
- Verifique se faz logout
- Verifique se volta para tela de login

## Checklist de Testes

### Backend
- [ ] Health check funciona
- [ ] Registro funciona
- [ ] Login funciona
- [ ] Refresh token funciona
- [ ] Endpoints protegidos funcionam
- [ ] Forgot password funciona
- [ ] Reset password funciona
- [ ] Logout funciona
- [ ] Google login funciona

### Frontend
- [ ] Registro funciona
- [ ] Login funciona
- [ ] Login Google funciona
- [ ] Biometria funciona
- [ ] Logout funciona
- [ ] Verificação de autenticação funciona
- [ ] Tokens são salvos corretamente
- [ ] Tokens são renovados automaticamente

## Troubleshooting

### Erro 401 em todos os endpoints
- Verifique se JWT_SECRET está configurado
- Verifique se o token está sendo enviado no header Authorization

### Erro ao enviar email
- Verifique configurações SMTP
- Verifique se SMTP_PASS está correto (senha de app para Gmail)
- Verifique logs do Railway

### Erro ao fazer login com Google
- Verifique se GOOGLE_CLIENT_ID está configurado
- Verifique se o Client ID está correto no Google Cloud Console

### Migration não executada
- Execute a migration (veja MIGRATION_EXECUTADA.md)
- Verifique se as tabelas foram criadas
