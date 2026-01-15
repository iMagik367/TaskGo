# Checklist Completo - APIs e Extensões Firebase Necessárias

## 📋 APIs do Google Cloud que DEVEM estar ativadas

Acesse: https://console.cloud.google.com/apis/library?project=task-go-ee85f

### APIs Essenciais (OBRIGATÓRIAS):

1. ✅ **Firebase Installations API**
   - Status: Deve estar ativada
   - Necessária para: Identificação de dispositivos

2. ✅ **Firebase Cloud Messaging API**
   - Status: Deve estar ativada
   - Necessária para: Push notifications

3. ✅ **Identity Toolkit API**
   - Status: Deve estar ativada
   - Necessária para: Firebase Authentication

4. ✅ **Cloud Firestore API**
   - Status: Deve estar ativada
   - Necessária para: Banco de dados Firestore

5. ✅ **Cloud Functions API**
   - Status: Deve estar ativada
   - Necessária para: Cloud Functions

6. ✅ **Cloud Storage API**
   - Status: Deve estar ativada
   - Necessária para: Firebase Storage

7. ✅ **Cloud Translation API**
   - Status: Recomendada se usar tradução automática
   - Necessária para: Tradução de conteúdo

8. ✅ **Cloud Vision API** (Opcional mas recomendada)
   - Status: Recomendada para melhor detecção facial
   - Necessária para: Validação de documentos e selfies (se usar)

9. ✅ **Cloud Scheduler API**
   - Status: Deve estar ativada
   - Necessária para: Cloud Functions agendadas

10. ✅ **Secret Manager API**
    - Status: Deve estar ativada
    - Necessária para: Gerenciar secrets das Cloud Functions

11. ✅ **Cloud Build API**
    - Status: Deve estar ativada
    - Necessária para: Deploy de Cloud Functions

12. ✅ **Cloud Logging API**
    - Status: Deve estar ativada
    - Necessária para: Logs das Cloud Functions

### APIs para ML Kit:

13. ✅ **ML Kit API** (se disponível)
    - Status: Verificar disponibilidade
    - Necessária para: Face Detection no device

---

## 🔌 Extensões Firebase que DEVEM estar instaladas e ativas

Acesse: https://console.firebase.google.com/project/task-go-ee85f/extensions

### Extensões OBRIGATÓRIAS:

1. ✅ **Trigger Email from Firestore** (`firebase/firestore-send-email`)
   - Status: Deve estar ACTIVE
   - Região: `nam5` (mesma do Firestore)
   - Necessária para: Enviar emails de 2FA e notificações
   - Configuração SMTP: Requer Gmail App Password ou SendGrid
   - Verificar em: Extensions > Trigger Email > Status
   - **CRÍTICO:** Sem esta extensão, códigos 2FA não serão enviados

2. ✅ **Run Payments with Stripe** (se usar pagamentos)
   - Status: Verificar se está ACTIVE
   - Necessária para: Processar pagamentos com Stripe
   - Configuração: Requer Stripe API Key e Webhook Secret

3. ✅ **Export User Data** (Opcional mas recomendada)
   - Status: Verificar se está ACTIVE
   - Necessária para: Exportar dados de usuários (LGPD/GDPR)
   - Storage bucket: `task-go-ee85f.appspot.com`

4. ✅ **Delete User Data** (Opcional mas recomendada)
   - Status: Verificar se está ACTIVE
   - Necessária para: Exclusão automática de dados (LGPD/GDPR)

5. ✅ **Stream Firestore to BigQuery** (Opcional)
   - Status: Verificar se está ACTIVE
   - Necessária para: Analytics e relatórios

---

## 📧 Configurações de Email no Firebase Auth

Acesse: https://console.firebase.google.com/project/task-go-ee85f/authentication/emails

### Templates que DEVEM estar configurados em PORTUGUÊS:

1. ✅ **Email address verification** (Verificação de endereço de e-mail)
   - Idioma: Português
   - Assunto: "Verifique seu email para %APP_NAME%"
   - Ativado: ✅ Sim

2. ✅ **Password reset** (Redefinição de senha)
   - Idioma: Português
   - Assunto: "Redefina sua senha para %APP_NAME%"
   - Ativado: ✅ Sim

3. ✅ **Email address change** (Alteração de endereço de e-mail)
   - Idioma: Português
   - Assunto: "Seu email de login foi alterado para %APP_NAME%"
   - Ativado: ✅ Sim

4. ✅ **Authentication registration notification** (Notificação de registro da autenticação - 2FA)
   - Idioma: Português
   - Assunto: "Você adicionou verificação de duas etapas à sua conta %APP_NAME%"
   - Ativado: ✅ Sim

5. ✅ **SMS Verification** (Verificação por SMS)
   - Idioma: Português
   - Mensagem: "%LOGIN_CODE% é seu código de verificação para %APP_NAME%"
   - Ativado: ✅ Sim (se usar SMS)

**⚠️ IMPORTANTE:** Todos os templates devem estar traduzidos para português. Ver guia: `GUIA_TRADUZIR_TEMPLATES_FIREBASE.md`

---

## 🔐 Configurações de Autenticação

Acesse: https://console.firebase.google.com/project/task-go-ee85f/authentication/providers

### Métodos de Login que DEVEM estar ativados:

1. ✅ **Email/Password**
   - Status: Ativado
   - Email link (passwordless): Pode estar desativado

2. ✅ **Google Sign-In**
   - Status: Ativado
   - Projeto OAuth 2.0: Configurado
   - SHA-1/SHA-256: Devem estar adicionados

3. ✅ **Phone** (se usar login por telefone)
   - Status: Verificar se está ativado
   - Provedor SMS: Configurado (Firebase ou Twilio)

---

## 💾 Configurações do Firestore

Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore

### Configurações importantes:

1. ✅ **Região do Firestore**
   - Região atual: `nam5` (US multi-region)
   - **CRÍTICO:** Extensões devem usar a MESMA região

2. ✅ **Regras de Segurança**
   - Arquivo: `firestore.rules`
   - Status: Deployadas
   - Verificar: Firestore > Rules

3. ✅ **Índices Compostos**
   - Arquivo: `firestore.indexes.json`
   - Status: Deployados
   - Verificar: Firestore > Indexes

---

## ☁️ Configurações das Cloud Functions

Acesse: https://console.firebase.google.com/project/task-go-ee85f/functions

### Funções que DEVEM estar deployadas:

1. ✅ **onUserCreate**
   - Região: `us-central1` (padrão)
   - Status: Deve estar deployada e funcionando

2. ✅ **deleteUserAccount**
   - Região: `us-central1`
   - Status: Deve estar deployada

3. ✅ **sendTwoFactorCode**
   - Região: `us-central1`
   - Status: Deve estar deployada

4. ✅ **verifyTwoFactorCode**
   - Região: `us-central1`
   - Status: Deve estar deployada

5. ✅ **verifyIdentity** (se existir)
   - Região: `us-central1`
   - Status: Verificar se está deployada

### Verificar logs:
```powershell
firebase functions:log
```

---

## 📱 Configurações do App Android

Acesse: https://console.firebase.google.com/project/task-go-ee85f/settings/general

### App Android - `com.taskgoapp.taskgo`:

1. ✅ **Package Name**: `com.taskgoapp.taskgo`
2. ✅ **SHA-1**: `87:d7:77:5d:c6:21:9c:3a:6d:f7:b6:2e:02:49:05:1b:05:8a:f2:18`
3. ✅ **SHA-256**: `465aTqmr9mjfSWYUMssSppD5y6ecDCBY3cQE5YngJXZhKvViWVK7446RPyBZRCE6pQKuT1bdwjRx5LAsfknBxL8YTrr97Hf`
4. ✅ **google-services.json**: Deve estar atualizado no projeto

---

## 🔍 App Check

Acesse: https://console.firebase.google.com/project/task-go-ee85f/appcheck

### Configurações:

1. ✅ **Debug Token**
   - Token: `4D4F1322-E272-454F-9396-ED80E3DBDBD7`
   - Status: Deve estar registrado

2. ✅ **Play Integrity API** (para release)
   - Status: Deve estar configurado
   - Necessário para builds de produção

---

## ✅ Checklist de Verificação Rápida

### APIs (Google Cloud Console):
- [ ] Firebase Installations API - ATIVADA
- [ ] Firebase Cloud Messaging API - ATIVADA
- [ ] Identity Toolkit API - ATIVADA
- [ ] Cloud Firestore API - ATIVADA
- [ ] Cloud Functions API - ATIVADA
- [ ] Cloud Storage API - ATIVADA
- [ ] Cloud Scheduler API - ATIVADA
- [ ] Secret Manager API - ATIVADA
- [ ] Cloud Build API - ATIVADA
- [ ] Cloud Logging API - ATIVADA

### Extensões (Firebase Console):
- [ ] Trigger Email from Firestore - ACTIVE, região `nam5`
- [ ] Run Payments with Stripe - ACTIVE (se usar pagamentos)
- [ ] Export User Data - ACTIVE (opcional)
- [ ] Delete User Data - ACTIVE (opcional)

### Templates de Email (Firebase Auth):
- [ ] Verificação de email - Traduzido para português
- [ ] Redefinição de senha - Traduzido para português
- [ ] Alteração de email - Traduzido para português
- [ ] Notificação 2FA - Traduzido para português
- [ ] SMS Verification - Traduzido para português (se usar)

### Cloud Functions:
- [ ] onUserCreate - Deployada
- [ ] deleteUserAccount - Deployada
- [ ] sendTwoFactorCode - Deployada
- [ ] verifyTwoFactorCode - Deployada

---

## 🚨 Problemas Comuns e Soluções

### Problema 1: Códigos 2FA não chegam
**Solução:**
1. Verificar se extensão Trigger Email está ACTIVE
2. Verificar região: deve ser `nam5`
3. Verificar configuração SMTP
4. Testar criando documento na coleção `mail`

### Problema 2: Verificação facial não funciona
**Solução:**
1. Verificar se ML Kit está configurado no app
2. Verificar se `LANDMARK_MODE_ALL` está habilitado (já corrigido)
3. Verificar se threshold está apropriado (já ajustado para 0.45)

### Problema 3: Logout não funciona após exclusão
**Solução:**
1. Verificar se `auth.signOut()` está sendo chamado (já corrigido)
2. Verificar se navegação está sendo feita (já corrigido com recreate)

---

## 📝 Comandos Úteis

### Verificar status das APIs:
```powershell
gcloud services list --enabled --project=task-go-ee85f
```

### Listar extensões:
```powershell
firebase ext:list
```

### Ver logs das functions:
```powershell
firebase functions:log
```

### Ver logs de uma extensão específica:
```powershell
firebase functions:log --only ext-firestore-send-email-processQueue
```

---

## 🔗 Links Úteis

- Firebase Console: https://console.firebase.google.com/project/task-go-ee85f
- Google Cloud Console: https://console.cloud.google.com/?project=task-go-ee85f
- APIs Library: https://console.cloud.google.com/apis/library?project=task-go-ee85f
- Extensions: https://console.firebase.google.com/project/task-go-ee85f/extensions
- Authentication Templates: https://console.firebase.google.com/project/task-go-ee85f/authentication/emails







