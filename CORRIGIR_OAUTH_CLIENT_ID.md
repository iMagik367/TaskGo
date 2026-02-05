# 🔧 Corrigir OAuth Client ID - Login Bloqueado

## ❌ Problema Real Identificado

O erro "Requests from this Android client application com.taskgoapp.taskgo are blocked" **NÃO é da chave de API**, mas sim do **OAuth Client ID** do Google Sign-In!

## 🔍 OAuth Client IDs Encontrados

No `google-services.json` há dois OAuth Client IDs:

1. **Android Client ID:**
   - ID: `1093466748007-k4vsgmdn43v5qd8q1tkhj8hg4q8j4ari.apps.googleusercontent.com`
   - SHA-1 no arquivo: `fbaef1168afe519dcfba5f670e37f7fcbb9b407a`
   - SHA-1 que obtivemos: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`

2. **Web Client ID:**
   - ID: `1093466748007-bk95o4ouk4966bvgqbm98n5h8js8m28v.apps.googleusercontent.com`

## ✅ Solução: Configurar OAuth Client ID Corretamente

### Passo 1: Acessar Google Cloud Console

1. Acesse: https://console.cloud.google.com/apis/credentials
2. **NÃO procure por "Chaves de API"**
3. Procure por **"OAuth 2.0 Client IDs"** (na seção de Credenciais)

### Passo 2: Encontrar o OAuth Client ID Android

1. Procure pelo Client ID: `1093466748007-k4vsgmdn43v5qd8q1tkhj8hg4q8j4ari`
2. Ou procure por um Client ID do tipo **"Android"**
3. Clique em **"Editar"** (ícone de lápis)

### Passo 3: Configurar Package Name e SHA-1

1. **Nome do pacote:** `com.taskgoapp.taskgo`
2. **Impressão digital do certificado SHA-1:**
   - Adicione o SHA-1 do RELEASE: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
   - Adicione o SHA-1 do DEBUG: `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`

### Passo 4: Se Não Encontrar, Criar Novo

Se não encontrar o OAuth Client ID Android:

1. Clique em **"+ Criar credenciais"**
2. Selecione **"ID do cliente OAuth"**
3. Tipo de aplicativo: **"Android"**
4. Nome: `TaskGo Android`
5. Nome do pacote: `com.taskgoapp.taskgo`
6. Adicione os SHA-1:
   - RELEASE: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
   - DEBUG: `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
7. Clique em **"Criar"**

### Passo 5: Atualizar google-services.json (Se Criou Novo)

Se criou um novo OAuth Client ID:

1. Baixe o novo `google-services.json` do Firebase Console
2. Substitua o arquivo `app/google-services.json`
3. Recompile o app

## 🔍 Verificação Adicional

### Verificar no Firebase Console:

1. Acesse: https://console.firebase.google.com/
2. Selecione o projeto: `task-go-ee85f`
3. Vá em **Configurações do projeto** (ícone de engrenagem)
4. Role até **"Seus apps"**
5. Clique no app Android
6. Verifique se o **SHA-1** está configurado corretamente
7. Se não estiver, adicione:
   - RELEASE: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
   - DEBUG: `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
8. Baixe o novo `google-services.json` e substitua

## ⚠️ Importante

- O **OAuth Client ID** é diferente da **Chave de API**
- O OAuth Client ID precisa ter o **package name** e **SHA-1** configurados
- Se o SHA-1 não estiver correto no OAuth Client ID, o Google Sign-In será bloqueado
- Após configurar, pode levar alguns minutos para propagar

## 📋 Checklist

- [ ] Acessou Google Cloud Console > Credenciais
- [ ] Encontrou OAuth 2.0 Client IDs (não chaves de API)
- [ ] Encontrou o Client ID Android: `1093466748007-k4vsgmdn43v5qd8q1tkhj8hg4q8j4ari`
- [ ] Verificou/corrigiu package name: `com.taskgoapp.taskgo`
- [ ] Adicionou SHA-1 RELEASE: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
- [ ] Adicionou SHA-1 DEBUG: `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
- [ ] Salvou as alterações
- [ ] Verificou no Firebase Console se o SHA-1 está correto
- [ ] Baixou novo google-services.json (se necessário)
- [ ] Aguardou 5-10 minutos
- [ ] Testou o login novamente
