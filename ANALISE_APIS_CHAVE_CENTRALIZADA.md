# 🔍 Análise de APIs para Chave Centralizada

## 📋 Chave Analisada
**API Key:** `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`

---

## ✅ APIs JÁ NA LISTA (Confirmadas no Código)

### Firebase APIs
- ✅ **Cloud Storage for Firebase API** - Usado em `FirebaseModule.kt` (FirebaseStorage)
- ✅ **Firebase App Check API** - Usado em `TaskGoApp.kt` (App Check)
- ✅ **Firebase Installations API** - Usado pelo SDK do Firebase
- ✅ **Firebase Cloud Messaging API** - Usado em `FirebaseModule.kt` (FirebaseMessaging)
- ✅ **Cloud Firestore API** - Usado em `FirebaseModule.kt` (FirebaseFirestore)
- ✅ **Identity Toolkit API** - Usado em `FirebaseModule.kt` (FirebaseAuth)
- ✅ **Security Token Service API** - Usado pelo Firebase Auth (SecureToken)

### Google Maps APIs
- ✅ **Maps SDK for Android** - Configurado em `AndroidManifest.xml`
- ✅ **Geocoding API** - Usado em `GeocodingService.kt`

### Outras APIs
- ✅ **Cloud Storage API** - Usado pelo Firebase Storage
- ✅ **Token Service API** - Usado pelo Firebase Auth

---

## ❌ API FALTANDO (Identificada no Código)

### ⚠️ **Cloud Functions API (Firebase Cloud Functions)**

**Onde é usado:**
- `app/src/main/java/com/taskgoapp/taskgo/data/firebase/FirebaseFunctionsService.kt`
- `app/src/main/java/com/taskgoapp/taskgo/di/FirebaseModule.kt` (linha 49: `provideFirebaseFunctions()`)

**Funções que dependem desta API:**
- `createOrder()` - Criar ordens de serviço
- `updateOrderStatus()` - Atualizar status de pedidos
- `createPaymentIntent()` - Processar pagamentos
- `sendPushNotification()` - Enviar notificações push
- `aiChatProxy()` - Proxy para chat com IA
- E muitas outras funções serverless

**Nome exato da API no Google Cloud Console:**
- **Cloud Functions API** ou **Cloud Functions for Firebase API**

**URL para ativar:**
- https://console.cloud.google.com/apis/library/cloudfunctions.googleapis.com

---

## 🔐 APIs de IA (NÃO devem estar na chave centralizada)

Estas APIs usam uma **chave separada** (`AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4`) definida em `AIModule.kt`:

- **Generative Language API (Gemini)** - Chat com IA
- **Cloud Translation API** - Tradução de mensagens
- **Cloud Speech-to-Text API** - Reconhecimento de voz

**✅ CORRETO:** Estas APIs NÃO devem estar na lista de restrições da chave centralizada.

---

## 📝 RESUMO

### ✅ APIs Corretas na Lista
Todas as APIs principais estão presentes, exceto uma.

### ❌ API que PRECISA ser adicionada:

1. **Cloud Functions API** (ou **Cloud Functions for Firebase API**)
   - **Crítica:** Sem esta API, todas as Cloud Functions falharão
   - **Impacto:** Criação de ordens, pagamentos, notificações, chat com IA via proxy, etc.

---

## 🔧 AÇÃO NECESSÁRIA

### Passo 1: Adicionar Cloud Functions API à lista de restrições

1. Acesse: https://console.cloud.google.com/apis/credentials?project=605187481719
2. Encontre a API Key: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
3. Clique para editar
4. Em **"API restrictions"**, adicione:
   - **Cloud Functions API** (ou **Cloud Functions for Firebase API**)
5. Salve as alterações

### Passo 2: Verificar se a API está habilitada no projeto

1. Acesse: https://console.cloud.google.com/apis/library/cloudfunctions.googleapis.com?project=605187481719
2. Clique em **"ENABLE"** se não estiver habilitada

---

## 🎯 CONCLUSÃO

**APIs que precisam ser adicionadas à lista de restrições:**
- ✅ **Cloud Functions API** (ou **Cloud Functions for Firebase API**)

**Total de APIs na lista:** 37 APIs
**Total após adição:** 38 APIs

**Status:** Quase completo! Apenas 1 API faltando.

---

## 📌 NOTA IMPORTANTE

A chave centralizada está **quase perfeita**. A única API faltando é a **Cloud Functions API**, que é essencial para o funcionamento de todas as funções serverless do Firebase.

