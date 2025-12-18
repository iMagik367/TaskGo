# 📋 Lista Completa de APIs do Google Cloud Necessárias

## 🎯 APIs para Centralizar em um Único Token (EXCETO Chat com IA)

### 🔥 Firebase APIs (Essenciais)
1. **Firebase Installations API**
   - API ID: `firebaseinstallations.googleapis.com`
   - Uso: Gerenciamento de instalações do Firebase
   - Status: ✅ Obrigatória

2. **Firebase App Check API**
   - API ID: `firebaseappcheck.googleapis.com`
   - Uso: Verificação de integridade do app
   - Status: ✅ Obrigatória

3. **Identity Toolkit API**
   - API ID: `identitytoolkit.googleapis.com`
   - Uso: Firebase Authentication
   - Status: ✅ Obrigatória

4. **Secure Token Service API**
   - API ID: `securetoken.googleapis.com`
   - Uso: Geração de tokens de autenticação
   - Status: ✅ Obrigatória (atualmente bloqueada - precisa habilitar)

5. **Cloud Firestore API**
   - API ID: `firestore.googleapis.com`
   - Uso: Banco de dados NoSQL
   - Status: ✅ Obrigatória

6. **Cloud Functions API**
   - API ID: `cloudfunctions.googleapis.com`
   - Uso: Funções serverless
   - Status: ✅ Obrigatória

7. **Cloud Storage API**
   - API ID: `storage-component.googleapis.com`
   - Uso: Armazenamento de arquivos
   - Status: ✅ Obrigatória

8. **Firebase Cloud Messaging API**
   - API ID: `fcm.googleapis.com`
   - Uso: Notificações push
   - Status: ✅ Obrigatória

9. **Firebase Crashlytics API**
   - API ID: `firebasecrashlytics.googleapis.com`
   - Uso: Relatórios de crash
   - Status: ✅ Obrigatória

### 🗺️ Google Maps & Location APIs
10. **Maps SDK for Android**
    - API ID: `maps-android-backend.googleapis.com`
    - Uso: Exibição de mapas no app
    - Status: ✅ Obrigatória

11. **Maps JavaScript API**
    - API ID: `maps-js-backend.googleapis.com`
    - Uso: Mapas (se houver versão web)
    - Status: ⚠️ Opcional (se não houver versão web)

12. **Maps Static API**
    - API ID: `maps-static-backend.googleapis.com`
    - Uso: Imagens estáticas de mapas
    - Status: ⚠️ Opcional

13. **Geocoding API**
    - API ID: `geocoding-backend.googleapis.com`
    - Uso: Conversão de endereços em coordenadas
    - Status: ✅ Obrigatória (usado no app)

14. **Places API**
    - API ID: `places-backend.googleapis.com`
    - Uso: Busca de lugares e estabelecimentos
    - Status: ⚠️ Verificar se está sendo usado

15. **Places API (New)**
    - API ID: `places-backend.googleapis.com`
    - Uso: Nova versão da Places API
    - Status: ⚠️ Verificar se está sendo usado

16. **Routes API**
    - API ID: `routes-backend.googleapis.com`
    - Uso: Cálculo de rotas
    - Status: ⚠️ Opcional (se houver cálculo de rotas)

17. **Roads API**
    - API ID: `roads-backend.googleapis.com`
    - Uso: Informações sobre estradas
    - Status: ⚠️ Opcional

18. **Maps Elevation API**
    - API ID: `elevation-backend.googleapis.com`
    - Uso: Dados de elevação
    - Status: ⚠️ Opcional

### 🔐 Google Sign-In & Authentication
19. **Google Sign-In API**
    - API ID: `oauth2.googleapis.com`
    - Uso: Login com Google
    - Status: ✅ Obrigatória

### 📍 Location Services
20. **Google Play Services Location**
    - API ID: `android-location.googleapis.com`
    - Uso: Serviços de localização
    - Status: ✅ Obrigatória (via Play Services)

### 💳 Google Pay & Billing (se usado)
21. **Google Pay API**
    - API ID: `pay-api.googleapis.com`
    - Uso: Pagamentos via Google Pay
    - Status: ⚠️ Opcional (se implementado)

22. **Google Play Billing API**
    - API ID: `androidpublisher.googleapis.com`
    - Uso: Assinaturas e compras in-app
    - Status: ⚠️ Opcional (se implementado)

---

## ❌ APIs que NÃO devem ser incluídas (Chat com IA - separado)

1. **Generative Language API (Gemini)**
   - API ID: `generativelanguage.googleapis.com`
   - Uso: Chat com IA
   - Status: ❌ **MANTER SEPARADA** - API Key: `AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4`

2. **Cloud Translation API** (se usado apenas para chat)
   - API ID: `translate.googleapis.com`
   - Uso: Tradução no chat
   - Status: ⚠️ Verificar se é usado apenas para chat ou também em outras partes

3. **Cloud Speech-to-Text API** (se usado apenas para chat)
   - API ID: `speech.googleapis.com`
   - Uso: Reconhecimento de voz no chat
   - Status: ⚠️ Verificar se é usado apenas para chat ou também em outras partes

---

## 📝 Resumo para Criação da Nova API Key

### APIs Obrigatórias (Mínimo):
1. Firebase Installations API
2. Firebase App Check API
3. Identity Toolkit API
4. Secure Token Service API
5. Cloud Firestore API
6. Cloud Functions API
7. Cloud Storage API
8. Firebase Cloud Messaging API
9. Firebase Crashlytics API
10. Maps SDK for Android
11. Geocoding API
12. Google Sign-In API (OAuth2)

### APIs Opcionais (Verificar uso):
- Maps JavaScript API
- Maps Static API
- Places API
- Routes API
- Roads API
- Maps Elevation API
- Google Pay API
- Google Play Billing API

---

## 🔧 Como Criar a Nova API Key

1. Acesse: https://console.cloud.google.com/apis/credentials?project=task-go-ee85f
2. Clique em **"Create Credentials"** > **"API Key"**
3. Nomeie a chave: `TaskGo App - Centralized API Key`
4. Em **"API restrictions"**, selecione **"Restrict key"**
5. Selecione todas as APIs obrigatórias listadas acima
6. Em **"Application restrictions"**, selecione **"Android apps"**
7. Adicione o package name: `com.taskgoapp.taskgo`
8. Adicione o SHA-1 do seu keystore (release e debug)
9. Clique em **"Save"**
10. Copie a nova API Key gerada

---

## 📱 Onde Atualizar a API Key

Após criar a nova API Key, você precisará atualizar:

1. **AndroidManifest.xml** (linha 43):
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="SUA_NOVA_API_KEY_AQUI"/>
   ```

2. **google-services.json** (gerenciado pelo Firebase - atualizar no Console)

3. **Firebase Console** > **Project Settings** > **Your apps** > **API Keys**

---

## ⚠️ Nota Importante

A API Key do Chat com IA (`AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4`) deve permanecer **separada** e configurada em:
- `app/src/main/java/com/taskgoapp/taskgo/di/AIModule.kt`

