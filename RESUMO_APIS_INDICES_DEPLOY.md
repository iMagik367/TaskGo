# 📋 Resumo: APIs, Índices e Deploy

## ✅ O QUE FOI FEITO

### 1. Lista de APIs Criada ✅
Arquivo: `LISTA_APIS_GOOGLE_CLOUD.md`

**APIs Obrigatórias para Centralizar (exceto Chat com IA):**
1. Firebase Installations API
2. Firebase App Check API
3. Identity Toolkit API
4. Secure Token Service API ⚠️ (atualmente bloqueada - precisa habilitar)
5. Cloud Firestore API
6. Cloud Functions API
7. Cloud Storage API
8. Firebase Cloud Messaging API
9. Firebase Crashlytics API
10. Maps SDK for Android
11. Geocoding API
12. Google Sign-In API (OAuth2)

**APIs Opcionais (verificar uso):**
- Maps JavaScript API
- Maps Static API
- Places API
- Routes API
- Roads API
- Maps Elevation API
- Google Pay API
- Google Play Billing API

**APIs que NÃO devem ser incluídas (Chat com IA - separado):**
- Generative Language API (Gemini) - API Key: `AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4`
- Cloud Translation API (se usado apenas para chat)
- Cloud Speech-to-Text API (se usado apenas para chat)

### 2. Índices do Firestore Adicionados ✅
Arquivo: `firestore.indexes.json`

**Novos índices adicionados:**
1. ✅ `products`: `active` (ASC) + `createdAt` (ASC) + `__name__` (ASC)
2. ✅ `reviews`: `targetId` (ASC) + `type` (ASC) + `createdAt` (DESC)
3. ✅ `services`: `category` (ASC) + `active` (ASC) + `createdAt` (DESC)
4. ✅ `purchase_orders`: `clientId` (ASC) + `status` (ASC) + `createdAt` (ASC)
5. ✅ `tracking_events`: `orderId` (ASC) + `timestamp` (ASC)

**Total de índices no arquivo:** 18 índices compostos

---

## 🚀 PRÓXIMOS PASSOS

### Passo 1: Criar Nova API Key Centralizada

1. Acesse: https://console.cloud.google.com/apis/credentials?project=task-go-ee85f
2. Clique em **"Create Credentials"** > **"API Key"**
3. Nomeie: `TaskGo App - Centralized API Key`
4. Em **"API restrictions"**, selecione **"Restrict key"**
5. Selecione todas as 12 APIs obrigatórias listadas acima
6. Em **"Application restrictions"**, selecione **"Android apps"**
7. Adicione:
   - Package name: `com.taskgoapp.taskgo`
   - SHA-1 do keystore (release e debug)
8. Clique em **"Save"**
9. **Copie a nova API Key gerada**

### Passo 2: Atualizar API Key no App

Após criar a nova API Key, atualize:

**AndroidManifest.xml** (linha 43):
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="SUA_NOVA_API_KEY_AQUI"/>
```

### Passo 3: Habilitar Secure Token Service API

⚠️ **IMPORTANTE:** Esta API está bloqueada e precisa ser habilitada:

1. Acesse: https://console.cloud.google.com/apis/library/securetoken.googleapis.com?project=task-go-ee85f
2. Clique em **"Enable"**
3. Aguarde alguns minutos para a ativação

### Passo 4: Deploy dos Índices do Firestore

Os índices serão criados automaticamente quando você fizer o deploy:

```bash
firebase deploy --only firestore:indexes
```

Ou manualmente via Firebase Console:
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/indexes
2. Os índices serão criados automaticamente quando o app tentar usar as queries

### Passo 5: Deploy das Cloud Functions

```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

---

## 📝 CHECKLIST

### APIs
- [ ] Criar nova API Key centralizada
- [ ] Habilitar Secure Token Service API
- [ ] Atualizar API Key no AndroidManifest.xml
- [ ] Testar se todas as APIs estão funcionando

### Índices
- [ ] Deploy dos índices do Firestore (ou aguardar criação automática)
- [ ] Verificar se todos os índices foram criados no Firebase Console
- [ ] Testar queries que requerem índices

### Deploy
- [ ] Deploy dos índices: `firebase deploy --only firestore:indexes`
- [ ] Deploy das Cloud Functions: `firebase deploy --only functions`
- [ ] Verificar logs após deploy

---

## 🔍 VERIFICAÇÃO PÓS-DEPLOY

### 1. Verificar Índices
Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/indexes

Todos os 18 índices devem estar com status **"Enabled"** ou **"Building"**

### 2. Verificar Cloud Functions
Acesse: https://console.firebase.google.com/project/task-go-ee85f/functions

Todas as funções devem estar com status **"Deployed"**

### 3. Testar no App
- Testar carregamento de produtos
- Testar carregamento de serviços
- Testar carregamento de reviews
- Testar carregamento de pedidos
- Verificar se não há erros de `FAILED_PRECONDITION` nos logs

---

## ⚠️ NOTAS IMPORTANTES

1. **Secure Token Service API**: Esta API está bloqueada e precisa ser habilitada manualmente. Sem ela, o Firebase Authentication não funcionará corretamente.

2. **Tempo de Criação de Índices**: Os índices do Firestore podem levar alguns minutos para serem criados. O app mostrará erros `FAILED_PRECONDITION` até que os índices estejam prontos.

3. **API Key do Chat com IA**: A API Key `AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4` deve permanecer **separada** e não deve ser incluída na nova API Key centralizada.

4. **SHA-1 do Keystore**: Você precisará do SHA-1 do keystore de release e debug para configurar as restrições da API Key. Para obter:
   ```bash
   keytool -list -v -keystore caminho/para/seu/keystore.jks -alias seu_alias
   ```

