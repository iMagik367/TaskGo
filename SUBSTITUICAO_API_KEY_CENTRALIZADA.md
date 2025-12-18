# ✅ Substituição da API Key Centralizada - Concluída

## 🎯 Nova API Key Centralizada

**Token:** `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`  
**Nome:** API Centralizada TaskGo

---

## ✅ Arquivos Atualizados

### 1. AndroidManifest.xml ✅
**Arquivo:** `app/src/main/AndroidManifest.xml`  
**Linha 43:** Substituído `AIzaSyAf0r0Zqz2-np2W1oRjuiMMR2F8_We8nTs` → `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo"/>
```

### 2. GeocodingService.kt ✅
**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/core/location/GeocodingService.kt`  
**Linha 23:** Substituído `AIzaSyAf0r0Zqz2-np2W1oRjuiMMR2F8_We8nTs` → `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`

```kotlin
private const val API_KEY = "AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo"
```

### 3. google-services.json ✅
**Arquivo:** `app/google-services.json`  
**Linha 32:** Substituído `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw` → `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`

```json
"api_key": [
  {
    "current_key": "AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo"
  }
]
```

---

## ❌ Tokens Antigos Removidos

### Tokens Substituídos:
1. ✅ `AIzaSyAf0r0Zqz2-np2W1oRjuiMMR2F8_We8nTs` (Maps API Key antiga)
2. ✅ `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw` (Firebase API Key antiga)

### Token Mantido (Chat com IA - Separado):
- ✅ `AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4` - **MANTIDO** em `AIModule.kt`

---

## 📋 APIs Ativadas na Nova API Key

### ✅ APIs Ativadas:
1. Cloud Storage for Firebase API
2. Firebase AI Logic API
3. Firebase App Check API
4. Firebase App Distribution API
5. Firebase App Hosting API
6. Firebase App Testers API
7. Firebase Cloud Messaging API
8. Firebase Data Connect API
9. Firebase Extensions API
10. Firebase Hosting API
11. Firebase Installations API
12. Firebase Management API
13. Firebase Realtime Database Management API
14. Firebase Remote Config API
15. Firebase Remote Config Realtime API
16. Firebase Rules API
17. Identity Toolkit API
18. Identity and Access Management (IAM) API
19. Security Token Service API ✅ (Importante - estava bloqueada)
20. Cloud Firestore API
21. Cloud Storage API
22. Maps SDK for Android
23. Maps JavaScript API
24. Maps Static API
25. Geocoding API
26. Geolocation API
27. Places API (New)
28. Places API
29. Routes API
30. Route Optimization API
31. Roads API
32. Maps Elevation API
33. Google Maps for Fleet Routing
34. Google Play Android Developer API
35. Google Play Integrity API
36. Google Play EMM API
37. Local Services API

### ⚠️ APIs que Ainda Precisam ser Ativadas:
1. **Firebase Crashlytics API** - Precisa ativar manualmente
2. **Google Sign-In API** - Precisa ativar manualmente (pode ser OAuth2 API)
3. **Google Play Services Location** - Não é uma API separada, é parte do Play Services
4. **Google Pay API** - Precisa ativar manualmente (se usar)
5. **Google Play Billing API** - Precisa ativar manualmente (se usar)

---

## 🔍 Verificação

### Tokens no Código:
- ✅ `AndroidManifest.xml`: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo` (Nova API Key Centralizada)
- ✅ `GeocodingService.kt`: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo` (Nova API Key Centralizada)
- ✅ `google-services.json`: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo` (Nova API Key Centralizada)
- ✅ `AIModule.kt`: `AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4` (Chat com IA - Mantido separado)

### Tokens Antigos Removidos:
- ✅ Nenhum token antigo encontrado no código

---

## ⚠️ IMPORTANTE: Atualizar no Firebase Console

O arquivo `google-services.json` foi atualizado localmente, mas você **PRECISA atualizar no Firebase Console**:

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/settings/general
2. Vá em **"Your apps"** > Selecione o app Android
3. Em **"API Keys"**, atualize para: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
4. Baixe o novo `google-services.json` e substitua o arquivo local

---

## 📝 Próximos Passos

1. ✅ Substituição do token concluída
2. ⏳ Ativar APIs faltantes manualmente:
   - Firebase Crashlytics API
   - Google Sign-In API (OAuth2 API)
   - Google Pay API (se usar)
   - Google Play Billing API (se usar)
3. ⏳ Atualizar `google-services.json` no Firebase Console
4. ⏳ Testar o app para verificar se todas as APIs estão funcionando

---

## ✅ Status Final

- ✅ Token substituído em todos os arquivos necessários
- ✅ Token do Chat com IA mantido separado
- ✅ Tokens antigos removidos
- ⏳ Aguardando ativação de APIs faltantes
- ⏳ Aguardando atualização no Firebase Console

