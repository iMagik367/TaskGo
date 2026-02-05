# 🔧 Corrigir Login Bloqueado - Firebase Auth

## ❌ Problema
O erro "Requests from this Android client application com.taskgoapp.taskgo are blocked" está ocorrendo no **LOGIN**, não no GPS.

## ✅ Chave de API Verificada
A chave de API do Firebase é a mesma do Maps:
- **Chave:** `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
- **Localização:** `google-services.json` e `AndroidManifest.xml`

## 🔍 Causa Provável
O problema não é o SHA-1 (que já está configurado), mas sim:

1. **APIs do Firebase Auth não habilitadas** no projeto
2. **Restrições de API** bloqueando as APIs necessárias
3. **Identity Toolkit API** não habilitada

## ✅ APIs Necessárias para Firebase Auth

### APIs que DEVEM estar habilitadas no projeto:

1. ✅ **Identity Toolkit API** - Já está na sua lista
2. ❓ **Firebase Authentication API** - Verificar se está habilitada
3. ❓ **Google Sign-In API** - Verificar se está habilitada
4. ❓ **Firebase App Check API** - Já está na sua lista

### Verificar APIs Habilitadas:

1. Acesse: https://console.cloud.google.com/apis/library
2. Procure por cada uma das APIs acima
3. Verifique se estão **habilitadas** (não apenas na lista de restrições)

## 🔧 Solução Passo a Passo

### Passo 1: Verificar e Habilitar APIs

1. Acesse: https://console.cloud.google.com/apis/library
2. Procure e **habilite** (se não estiver):
   - **Firebase Authentication API**
   - **Google Sign-In API**
   - **Identity Toolkit API** (já deve estar)

### Passo 2: Verificar Restrições da Chave de API

1. Acesse: https://console.cloud.google.com/apis/credentials
2. Encontre a chave: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
3. Clique em **"Editar"**

### Passo 3: Configurar Restrições de API

1. Em **"Restrições de API"**, selecione **"Restringir chave"**
2. **Selecione TODAS as APIs necessárias:**
   - ✅ Maps SDK for Android
   - ✅ Geocoding API
   - ✅ Geolocation API
   - ✅ Places API
   - ✅ **Identity Toolkit API** ⚠️ IMPORTANTE
   - ✅ **Firebase Authentication API** ⚠️ IMPORTANTE
   - ✅ **Google Sign-In API** ⚠️ IMPORTANTE
   - ✅ Firebase App Check API
   - ✅ Cloud Firestore API
   - ✅ Cloud Storage API
   - ✅ Cloud Functions API
   - ✅ Firebase Cloud Messaging API
   - ✅ Firebase Remote Config API

### Passo 4: Verificar Restrições de Aplicativo

1. Em **"Restrições de aplicativo"**, verifique:
   - ✅ Tipo: **"Aplicativos Android"**
   - ✅ Package name: `com.taskgoapp.taskgo`
   - ✅ SHA-1 DEBUG: `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
   - ✅ SHA-1 RELEASE: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`

### Passo 5: Salvar e Aguardar

1. Clique em **"Salvar"**
2. **Aguarde 5-10 minutos** para as mudanças propagarem
3. Teste o login novamente

## 🚨 Solução Temporária (Para Testar Agora)

Se precisar testar imediatamente:

1. Acesse: https://console.cloud.google.com/apis/credentials
2. Encontre a chave: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
3. Clique em **"Editar"**
4. Em **"Restrições de API"**, selecione **"Não restringir chave"**
5. Clique em **"Salvar"**
6. Aguarde 2-5 minutos e teste

**⚠️ IMPORTANTE:** Depois de testar, configure as restrições corretamente!

## 📋 Checklist Completo

- [ ] Identity Toolkit API habilitada no projeto
- [ ] Firebase Authentication API habilitada no projeto
- [ ] Google Sign-In API habilitada no projeto
- [ ] Restrições de API configuradas com todas as APIs necessárias
- [ ] Restrições de aplicativo configuradas com SHA-1 corretos
- [ ] Package name correto: `com.taskgoapp.taskgo`
- [ ] Aguardou 5-10 minutos após salvar
- [ ] Testou o login novamente

## 🔍 Verificação Adicional

Se ainda não funcionar:

1. **Verifique os logs do Logcat:**
   - Procure por erros relacionados a "API_KEY_SERVICE_BLOCKED"
   - Procure por erros de "403" ou "blocked"

2. **Verifique o billing:**
   - Certifique-se de que o billing está habilitado
   - Verifique se há quotas disponíveis

3. **Verifique o projeto Firebase:**
   - Acesse: https://console.firebase.google.com/
   - Verifique se o projeto está ativo
   - Verifique se o Authentication está habilitado

4. **Teste com outra chave:**
   - Crie uma nova chave de API sem restrições
   - Atualize o `google-services.json` temporariamente
   - Teste se funciona

## 📞 Próximos Passos

Após configurar:
1. Aguarde 5-10 minutos
2. Teste o login com email/senha
3. Teste o login com Google
4. Verifique os logs se ainda houver erro
