# Próximos Passos - Sistema de Autenticação

## ✅ O que foi implementado

1. **Backend completo** - Sistema de autenticação próprio com JWT
2. **Frontend base** - AuthRepository, TokenManager, API Services
3. **ViewModels principais** - Login, Signup, Auth, TwoFactor atualizados
4. **Dependências instaladas** - Todas as bibliotecas necessárias

## 🎯 Próximas ações imediatas

### 1. Executar Migration no Banco de Dados

```powershell
# Opção 1: Usar script PowerShell
.\scripts\executar-migration-auth.ps1

# Opção 2: Usar Railway CLI
railway connect
# Depois execute o SQL do arquivo: database/migrations/004_add_auth_fields.sql
```

### 2. Configurar Variáveis de Ambiente no Railway

Acesse o painel do Railway e adicione:

**Obrigatórias:**
- `JWT_SECRET` - Chave secreta para JWT (mínimo 32 caracteres)
- `JWT_REFRESH_SECRET` - Chave secreta para refresh tokens
- `GOOGLE_CLIENT_ID` - ID do cliente OAuth do Google
- `SMTP_HOST` - Servidor SMTP (ex: smtp.gmail.com)
- `SMTP_PORT` - Porta SMTP (ex: 587)
- `SMTP_USER` - Usuário SMTP
- `SMTP_PASS` - Senha SMTP (use senha de app para Gmail)
- `SMTP_FROM` - Email remetente
- `APP_URL` - URL do app (ex: https://taskgo-production.up.railway.app)

**Opcionais (com valores padrão):**
- `JWT_EXPIRES_IN=15m`
- `JWT_REFRESH_EXPIRES_IN=7d`

**Veja detalhes em:** `VARIAVEIS_AMBIENTE_AUTENTICACAO.md`

### 3. Atualizar Telas do App

Atualize as seguintes telas para usar `TokenManager` em vez de `FirebaseAuth`:

1. `AccountScreen.kt`
2. `MyDataScreen.kt`
3. `ProductsScreen.kt`
4. `ServiceFormScreen.kt`
5. `SplashViewModel.kt`

**Guia completo:** `GUIA_ATUALIZACAO_TELAS_FIREBASE_AUTH.md`

### 4. Testar Endpoints

Após configurar, teste os endpoints:

```bash
# Health check
curl https://taskgo-production.up.railway.app/health

# Registro
curl -X POST https://taskgo-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@example.com","password":"senha123456","role":"client"}'

# Login
curl -X POST https://taskgo-production.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@example.com","password":"senha123456"}'
```

### 5. Testar no App Android

1. Build do app
2. Testar registro
3. Testar login com email/senha
4. Testar login com Google
5. Testar 2FA (se habilitado)
6. Testar reset de senha
7. Testar biometria

## 📋 Checklist Final

### Backend
- [x] Schema do banco criado
- [x] Serviços implementados
- [x] Rotas criadas
- [x] Middleware configurado
- [x] Dependências instaladas
- [ ] Migration executada
- [ ] Variáveis de ambiente configuradas
- [ ] Endpoints testados

### Frontend
- [x] AuthRepository criado
- [x] TokenManager criado
- [x] API Services criados
- [x] NetworkModule atualizado
- [x] ViewModels principais atualizados
- [ ] Telas restantes atualizadas
- [ ] App testado end-to-end

## 🔍 Verificações

### Backend está funcionando?
```bash
curl https://taskgo-production.up.railway.app/health
# Deve retornar: {"status":"ok",...}
```

### Migration foi executada?
```sql
-- Conecte ao banco e execute:
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'users' AND column_name IN ('password_hash', 'google_id', 'email_verified');
-- Deve retornar as 3 colunas
```

### Variáveis estão configuradas?
- Verifique no painel do Railway se todas as variáveis estão presentes
- Verifique os logs do Railway para erros de variáveis não encontradas

## 🐛 Troubleshooting

### Erro: "JWT_SECRET não encontrado"
- Adicione `JWT_SECRET` nas variáveis de ambiente do Railway

### Erro: "Cannot connect to database"
- Verifique `DATABASE_URL` no Railway
- Verifique se o serviço PostgreSQL está rodando

### Erro: "Email não enviado"
- Verifique configurações SMTP
- Para Gmail, use senha de app (não senha principal)
- Verifique logs do Railway

### Erro: "Google OAuth failed"
- Verifique `GOOGLE_CLIENT_ID`
- Verifique se o Client ID está correto no Google Cloud Console

## 📚 Documentação

- `RESUMO_IMPLEMENTACAO_AUTENTICACAO.md` - Resumo completo
- `GUIA_ATUALIZACAO_TELAS_FIREBASE_AUTH.md` - Guia de atualização
- `VARIAVEIS_AMBIENTE_AUTENTICACAO.md` - Configuração de variáveis
- `scripts/executar-migration-auth.ps1` - Script de migration

## ✨ Pronto para produção?

Antes de ir para produção, certifique-se de:

1. ✅ Migration executada
2. ✅ Variáveis de ambiente configuradas
3. ✅ Endpoints testados
4. ✅ Telas atualizadas
5. ✅ App testado
6. ✅ Logs verificados
7. ✅ Backup do banco configurado
