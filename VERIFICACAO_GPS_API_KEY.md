# Verificação GPS e API Key - TaskGo App

## ✅ Chave de API Verificada

A chave de API `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo` está configurada corretamente em:

1. **AndroidManifest.xml** (linha 45)
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo"/>
   ```

2. **GeocodingService.kt** (linha 23)
   ```kotlin
   private const val API_KEY = "AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo"
   ```

3. **google-services.json** (linha 32)
   ```json
   "current_key": "AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo"
   ```

## ✅ APIs Necessárias para GPS

### APIs Já Ativadas (da sua lista):
- ✅ **Maps SDK for Android** - Necessária para Maps
- ✅ **Geocoding API** - Necessária para converter endereços em coordenadas
- ✅ **Geolocation API** - Necessária para obter localização via IP (fallback)
- ✅ **Places API** - Necessária para busca de lugares
- ✅ **Places API (New)** - Versão nova da Places API
- ✅ **Maps JavaScript API** - Para web (se necessário)
- ✅ **Maps Static API** - Para imagens estáticas de mapas
- ✅ **Maps Embed API** - Para embed de mapas

### ⚠️ APIs que PODEM estar faltando:

1. **Maps SDK for Android** - Já está na lista ✅
2. **Geocoding API** - Já está na lista ✅
3. **Geolocation API** - Já está na lista ✅

## 🔍 Verificações Importantes no Google Cloud Console

### 1. Verificar Restrições da Chave de API

Acesse: https://console.cloud.google.com/apis/credentials

Verifique se a chave `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo` tem:

**Restrições de aplicativo:**
- ✅ Deve permitir aplicativos Android
- ✅ Package name: `com.taskgoapp.taskgo`
- ✅ SHA-1 certificate fingerprint: (verificar no Google Play Console ou keystore)

**Restrições de API:**
- ✅ Deve ter as seguintes APIs habilitadas:
  - Maps SDK for Android
  - Geocoding API
  - Geolocation API
  - Places API
  - Places API (New)

### 2. Verificar Quotas e Billing

- ✅ Verificar se o projeto tem billing habilitado
- ✅ Verificar se as quotas não foram excedidas
- ✅ Verificar se há limites de requisições

### 3. Verificar Logs de Erro

No Google Cloud Console, verifique:
- Cloud Logging para erros de API
- API & Services > Dashboard para estatísticas de uso

## 📱 Verificações no App Android

### Permissões (já configuradas):
- ✅ `ACCESS_FINE_LOCATION` - AndroidManifest.xml linha 15
- ✅ `ACCESS_COARSE_LOCATION` - AndroidManifest.xml linha 14

### Dependências (já configuradas):
- ✅ `play-services-maps:18.2.0` - build.gradle.kts linha 403
- ✅ `play-services-location:21.0.1` - build.gradle.kts linha 404

## 🐛 Possíveis Problemas e Soluções

### Problema 1: GPS não obtém localização
**Solução:**
- Verificar se o GPS está habilitado no dispositivo
- Verificar se as permissões foram concedidas
- Verificar se o app está em foreground quando solicita GPS

### Problema 2: Geocoding falha
**Solução:**
- Verificar se a Geocoding API está habilitada
- Verificar se a chave de API tem acesso à Geocoding API
- Verificar logs de erro no GeocodingService

### Problema 3: Erro de autenticação da API
**Solução:**
- Verificar restrições da chave de API
- Verificar se o package name está correto
- Verificar se o SHA-1 está configurado corretamente

## 📋 Checklist de Verificação

- [x] Chave de API configurada no AndroidManifest.xml
- [x] Chave de API configurada no GeocodingService.kt
- [x] Permissões de localização no AndroidManifest.xml
- [x] Dependências do Google Play Services configuradas
- [ ] Verificar restrições da chave de API no Google Cloud Console
- [ ] Verificar SHA-1 certificate fingerprint
- [ ] Verificar se todas as APIs estão habilitadas
- [ ] Verificar billing e quotas
- [ ] Testar GPS em dispositivo físico
- [ ] Verificar logs de erro no Logcat

## 🔧 Próximos Passos

1. **Verificar no Google Cloud Console:**
   - Acesse: https://console.cloud.google.com/apis/credentials
   - Encontre a chave `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
   - Verifique as restrições de aplicativo e API

2. **Obter SHA-1 Certificate Fingerprint:**
   ```bash
   keytool -list -v -keystore C:\Users\user\AndroidKeystores\taskgo-release-key.jks -alias taskgo-release
   ```

3. **Adicionar SHA-1 no Google Cloud Console:**
   - Vá em Credentials > Editar chave de API
   - Adicione o SHA-1 em "Restrições de aplicativo Android"

4. **Testar GPS:**
   - Execute o app em dispositivo físico
   - Verifique logs no Logcat para erros
   - Teste em local aberto (melhor sinal GPS)
