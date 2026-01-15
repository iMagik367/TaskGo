# LINKS PARA VERIFICAÇÕES NO FIREBASE E GOOGLE CLOUD

## 📋 INFORMAÇÕES DO PROJETO

**Project ID:** `task-go-ee85f`  
**Project Number:** `1093466748007`  
**Application ID:** `com.taskgoapp.taskgo`  
**Package Name:** `com.taskgoapp.taskgo`

---

## ✅ ETAPA 1: VERIFICAR SHA-1 E SHA-256 NO FIREBASE CONSOLE

### Link Direto:
https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo

### O que verificar:
1. **SHA-1 do Upload Key:**
   ```
   FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A
   ```
   ✅ Deve estar cadastrado

2. **SHA-256 do Upload Key:**
   ```
   95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18
   ```
   ✅ Deve estar cadastrado

3. **SHA-256 do App Signing Key (Play Store):**
   ⚠️ Se o app está na Play Store, você precisa do SHA-256 do **App Signing Key** (não do upload key)
   - Acesse: Google Play Console → App Signing
   - Copie o SHA-256 do App Signing Certificate
   - Adicione no Firebase Console

---

## ✅ ETAPA 2: VERIFICAR APP CHECK NO FIREBASE CONSOLE

### Link Direto:
https://console.firebase.google.com/project/task-go-ee85f/appcheck

### O que verificar:

1. **Provider Ativo:**
   - Play Integrity deve estar **ATIVO** para o app Android
   - Debug App Check deve estar ativo para desenvolvimento

2. **Enforcement:**
   - Verificar se está em modo **ENFORCE** ou **MONITOR**
   - Se estiver em ENFORCE → App Check bloqueia requisições sem token válido
   - Se estiver em MONITOR → App Check apenas registra, não bloqueia

3. **SHA-256 Registrado:**
   - No App Check, verificar se o SHA-256 está registrado para Play Integrity
   - SHA-256 necessário: `95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18`

4. **Debug Tokens (para desenvolvimento):**
   - Link: https://console.firebase.google.com/project/task-go-ee85f/appcheck/apps
   - Verificar se o token de debug está cadastrado
   - Token atual: `4D4F1322-E272-454F-9396-ED80E3DBDBD7`

---

## ✅ ETAPA 3: VERIFICAR PLAY INTEGRITY API NO GOOGLE CLOUD

### Link Direto:
https://console.cloud.google.com/apis/library/playintegrity.googleapis.com?project=task-go-ee85f

### O que verificar:
1. **API Habilitada:**
   - Status deve ser **ENABLED** (Habilitada)
   - Se não estiver habilitada, clique em **ENABLE**

2. **Quotas e Limites:**
   - Verificar se há quotas configuradas
   - Verificar se não há bloqueios

---

## ✅ ETAPA 4: VERIFICAR FIREBASE APP CHECK API NO GOOGLE CLOUD

### Link Direto:
https://console.cloud.google.com/apis/library/firebaseappcheck.googleapis.com?project=task-go-ee85f

### O que verificar:
1. **API Habilitada:**
   - Status deve ser **ENABLED** (Habilitada)
   - Se não estiver habilitada, clique em **ENABLE**

---

## ✅ ETAPA 5: VERIFICAR FIREBASE INSTALLATIONS API NO GOOGLE CLOUD

### Link Direto:
https://console.cloud.google.com/apis/library/firebaseinstallations.googleapis.com?project=task-go-ee85f

### O que verificar:
1. **API Habilitada:**
   - Status deve ser **ENABLED** (Habilitada)
   - Se não estiver habilitada, clique em **ENABLE**

---

## ✅ ETAPA 6: VERIFICAR API KEYS E RESTRIÇÕES

### Link Direto:
https://console.cloud.google.com/apis/credentials?project=task-go-ee85f

### O que verificar:
1. **API Key do Firebase:**
   - API Key: `AIzaSyD9JIxB5lzJUou1hUHBxNMGC4DVjEtIY_k`
   - Verificar se não há restrições bloqueando:
     - Firebase App Check API
     - Firebase Installations API
     - Play Integrity API
     - Identity Toolkit API (Firebase Auth)

2. **Se houver restrições:**
   - Adicionar as APIs acima na lista de APIs permitidas
   - OU temporariamente remover restrições para teste

---

## ✅ ETAPA 7: VERIFICAR CONFIGURAÇÕES DO APP ANDROID

### Link Direto:
https://console.firebase.google.com/project/task-go-ee85f/settings/general

### O que verificar:
1. **App Android:**
   - Package Name: `com.taskgoapp.taskgo` ✅
   - App ID: `1:1093466748007:android:55d3d395716e81c4e8d0c2` ✅
   - SHA-1 e SHA-256 cadastrados ✅

---

## ✅ ETAPA 8: VERIFICAR GOOGLE SIGN-IN (OAuth Client)

### Link Direto:
https://console.cloud.google.com/apis/credentials?project=task-go-ee85f

### O que verificar:
1. **OAuth 2.0 Client IDs:**
   - Android Client ID: `1093466748007-k4vsgmdn43v5qd8q1tkhj8hg4q8j4ari.apps.googleusercontent.com`
   - Web Client ID: `1093466748007-bk95o4ouk4966bvgqbm98n5h8js8m28v.apps.googleusercontent.com`
   - Verificar se SHA-1 está cadastrado no Android Client ID

---

## ✅ ETAPA 9: VERIFICAR LOGS DE AUTENTICAÇÃO NO FIREBASE

### Link Direto:
https://console.firebase.google.com/project/task-go-ee85f/authentication/users

### O que verificar:
1. **Tentativas de Login:**
   - Verificar se há tentativas de login sendo registradas
   - Verificar se há erros sendo reportados

---

## ✅ ETAPA 10: VERIFICAR LOGS DO APP CHECK

### Link Direto:
https://console.firebase.google.com/project/task-go-ee85f/appcheck

### O que verificar:
1. **Métricas:**
   - Verificar se há tokens sendo gerados
   - Verificar se há erros de validação
   - Verificar taxa de sucesso/falha

---

## 🔧 TESTE TEMPORÁRIO: DESATIVAR APP CHECK

Para confirmar se o problema é App Check:

1. Edite `local.properties`:
   ```
   enableAppCheck=false
   ```

2. Recompile o release:
   ```bash
   ./gradlew bundleRelease
   ```

3. Teste o login:
   - Se funcionar → Problema é App Check
   - Se não funcionar → Problema é outra coisa

---

## 📝 CHECKLIST RÁPIDO

- [ ] SHA-1 cadastrado no Firebase Console
- [ ] SHA-256 cadastrado no Firebase Console
- [ ] SHA-256 do App Signing Key cadastrado (se app está na Play Store)
- [ ] Play Integrity API habilitada no Google Cloud
- [ ] Firebase App Check API habilitada no Google Cloud
- [ ] Firebase Installations API habilitada no Google Cloud
- [ ] App Check configurado no Firebase Console
- [ ] Play Integrity Provider ativo no App Check
- [ ] SHA-256 registrado no App Check
- [ ] Enforcement em modo MONITOR (para teste) ou ENFORCE (produção)
- [ ] API Keys sem restrições bloqueando APIs necessárias

---

## 🚨 PROBLEMAS COMUNS E SOLUÇÕES

### Problema: "App Check token is invalid"
**Causa:** Play Integrity não está gerando token válido  
**Solução:**
1. Verificar SHA-256 cadastrado
2. Verificar Play Integrity API habilitada
3. Verificar se app foi instalado via Play Store (Play Integrity só funciona assim)

### Problema: "API has not been used"
**Causa:** APIs não habilitadas no Google Cloud  
**Solução:**
1. Habilitar Play Integrity API
2. Habilitar Firebase App Check API
3. Habilitar Firebase Installations API

### Problema: "403 Forbidden"
**Causa:** API Key com restrições bloqueando APIs  
**Solução:**
1. Verificar restrições da API Key
2. Adicionar APIs necessárias na lista de APIs permitidas

---

## 📞 SUPORTE

Se após todas as verificações o problema persistir:
1. Capture os logs detalhados do app (agora com logs melhorados)
2. Verifique os logs no Firebase Console
3. Verifique os logs no Google Cloud Console

