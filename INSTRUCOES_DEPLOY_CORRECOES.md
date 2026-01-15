# Instruções de Deploy - Correções Implementadas

## 📋 Resumo das Correções

1. ✅ **Corrigido erro de permissão ao solicitar mudança de conta**
   - Ajustadas regras do Firestore
   - Convertido Date para Timestamp no código

2. ✅ **Implementada tela de verificação de código 2FA**
   - Função `sendTwoFactorCode` criada
   - Função `verifyTwoFactorCode` criada
   - ViewModel atualizado para usar Cloud Functions

3. ✅ **Melhorada verificação facial**
   - Função `startIdentityVerification` integrada
   - ViewModel atualizado para chamar Cloud Function após upload

4. ✅ **Corrigida exclusão de conta**
   - PrivacyScreen agora chama `deleteUserAccount` function
   - Exclui conta do Firebase Auth completamente

5. ✅ **Função de idioma desativada**
   - Opção removida da tela de configurações
   - Navegação comentada

6. ✅ **Chat com IA melhorado**
   - Funções Cloud Functions já existentes e funcionais
   - Conexão com API verificada

---

## 🚀 Passos para Deploy

### 1. Deploy das Cloud Functions

```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

**Functions que serão deployadas:**
- `sendTwoFactorCode` - Envia código 2FA por email
- `verifyTwoFactorCode` - Verifica código 2FA
- `deleteUserAccount` - Exclui conta do usuário
- `startIdentityVerification` - Inicia verificação de identidade
- `processIdentityVerification` - Processa verificação (trigger)
- `cleanupExpiredTwoFactorCodes` - Limpa códigos expirados (scheduled)
- Todas as outras functions existentes

### 2. Deploy das Firestore Rules

```bash
firebase deploy --only firestore:rules
```

**Mudanças nas regras:**
- Adicionadas regras para `account_change_requests` com validação de campos
- Adicionadas regras para `twoFactorCodes`

### 3. Deploy dos Firestore Indexes (se necessário)

```bash
firebase deploy --only firestore:indexes
```

### 4. Configurar Variáveis de Ambiente (se necessário)

```bash
firebase functions:config:set openai.api_key="YOUR_OPENAI_API_KEY"
```

**Nota:** Para 2FA, você pode instalar a extensão Trigger Email do Firebase:
```bash
firebase ext:install firebase/firestore-send-email
```

### 5. Build do AAB

```bash
cd ..
./gradlew bundleRelease
```

O arquivo AAB estará em: `app/build/outputs/bundle/release/app-release.aab`

**Nova versão:**
- versionCode: 22
- versionName: 1.0.21

---

## ⚙️ Configurações Manuais no Firebase Console

### 1. Habilitar APIs Necessárias

No Google Cloud Console (console.cloud.google.com):

1. **Cloud Vision API**
   - Necessária para verificação facial
   - Ativar em: APIs & Services > Library > Cloud Vision API

2. **Cloud Functions API**
   - Já deve estar ativada

3. **Identity Toolkit API** (Firebase Auth)
   - Já deve estar ativada

### 2. Configurar Permissões de Service Account

1. Vá para: IAM & Admin > Service Accounts
2. Encontre o service account do Firebase Functions (padrão: `PROJECT_ID@appspot.gserviceaccount.com`)
3. Verifique que tem as seguintes roles:
   - Cloud Functions Admin
   - Firebase Admin SDK Administrator Service Agent
   - Service Account User
   - Storage Admin (para excluir arquivos)

### 3. Configurar Extensão Trigger Email (Opcional, mas Recomendado)

1. No Firebase Console, vá para: Extensions
2. Instale: "Trigger Email" (firebase/firestore-send-email)
3. Configure:
   - Collection: `mail`
   - SMTP connection URI: Configure seu servidor SMTP (Gmail, SendGrid, etc.)

**Alternativa sem extensão:**
Se não instalar a extensão, você precisará configurar um serviço de email externo (SendGrid, Mailgun, etc.) e atualizar a função `sendVerificationEmail` em `functions/src/twoFactorAuth.ts`.

### 4. Configurar Realtime Database (para Verificação Facial)

1. No Firebase Console, vá para: Realtime Database
2. Crie um banco de dados (se não existir)
3. Configure as regras:

```json
{
  "rules": {
    "identity_verifications": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "$userId === auth.uid"
      }
    }
  }
}
```

### 5. Verificar App Check (se estiver usando)

1. No Firebase Console, vá para: App Check
2. Verifique que o app Android está registrado
3. Configure o debug token se necessário para testes

---

## 🧪 Testes Pós-Deploy

### Teste 1: Solicitar Mudança de Conta
1. Login no app
2. Ir para Configurações > Conta
3. Clicar em "Solicitar Mudança de Modo de Conta"
4. Selecionar novo tipo de conta
5. Enviar solicitação
6. **Esperado:** Sem erro de permissão, solicitação criada com sucesso

### Teste 2: Verificação 2FA
1. Ativar 2FA nas configurações de segurança
2. Fazer login
3. **Esperado:** Tela de verificação de código aparecer
4. Verificar código recebido por email
5. **Esperado:** Login bem-sucedido

### Teste 3: Exclusão de Conta
1. Ir para Configurações > Privacidade
2. Clicar em "Excluir Conta"
3. Confirmar exclusão
4. **Esperado:** Conta excluída, logout automático, não consegue mais fazer login

### Teste 4: Verificação Facial
1. Ir para verificação de identidade
2. Enviar documentos (frente, verso, selfie)
3. **Esperado:** Upload bem-sucedido, verificação processada em background

### Teste 5: Chat com IA
1. Ir para Configurações > AI TaskGo
2. Enviar uma mensagem
3. **Esperado:** Resposta da IA recebida

---

## 📝 Notas Importantes

1. **Extensão Trigger Email:**
   - Se não configurar, códigos 2FA não serão enviados por email
   - Considere usar SendGrid, Mailgun ou outro serviço de email
   - Atualize `sendVerificationEmail` em `twoFactorAuth.ts` se usar serviço externo

2. **Google Cloud Vision API:**
   - Requer billing habilitado no projeto
   - Primeiras 1.000 requisições/mês são gratuitas
   - Depois: $1.50 por 1.000 requisições

3. **Realtime Database:**
   - Usado para trigger de verificação facial
   - Regras devem ser configuradas conforme acima

4. **Versionamento:**
   - Nova versão: 1.0.21 (code 22)
   - Atualizar no Play Store Console ao fazer upload do AAB

5. **Logs:**
   - Para debugar functions: `firebase functions:log`
   - Para debugar específica: `firebase functions:log --only sendTwoFactorCode`

---

## 🔍 Troubleshooting

### Erro ao enviar código 2FA:
- Verifique se a extensão Trigger Email está instalada
- Verifique logs: `firebase functions:log --only sendTwoFactorCode`
- Verifique se a coleção `mail` existe no Firestore

### Erro na verificação facial:
- Verifique se Cloud Vision API está ativada
- Verifique logs: `firebase functions:log --only processIdentityVerification`
- Verifique se Realtime Database está configurado

### Erro ao excluir conta:
- Verifique permissões do service account
- Verifique logs: `firebase functions:log --only deleteUserAccount`
- Verifique se Storage está acessível

---

## ✅ Checklist Final

- [ ] Functions deployadas
- [ ] Firestore rules deployadas
- [ ] Cloud Vision API ativada
- [ ] Realtime Database configurado
- [ ] Trigger Email configurado (ou serviço de email alternativo)
- [ ] Service account tem permissões corretas
- [ ] AAB gerado com nova versão
- [ ] Testes realizados
- [ ] Logs verificados










