# 🔧 Corrigir Restrições da Chave de API - App Bloqueado

## ❌ Problema
O erro "Requests from this Android client application com.taskgoapp.taskgo are blocked" indica que as restrições da chave de API estão bloqueando o app.

## ✅ SHA-1 Certificate Fingerprints Obtidos

### SHA-1 do RELEASE keystore (Produção):
```
FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A
```

### SHA-1 do DEBUG keystore (Desenvolvimento):
```
50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD
```

## 🚀 Solução Rápida (Temporária - Para Testar Agora)

**IMPORTANTE:** Use esta solução apenas para desbloquear o app temporariamente. Depois configure corretamente.

1. Acesse: https://console.cloud.google.com/apis/credentials
2. Encontre a chave: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
3. Clique em **"Editar"** (ícone de lápis)
4. Em **"Restrições de aplicativo"**, selecione **"Nenhuma"**
5. Clique em **"Salvar"**
6. Aguarde 2-5 minutos para as mudanças propagarem
7. Teste o app novamente

## ✅ Solução Definitiva (Configurar Corretamente)

### Passo 1: Acessar Google Cloud Console
1. Acesse: https://console.cloud.google.com/apis/credentials
2. Encontre a chave: `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
3. Clique em **"Editar"** (ícone de lápis)

### Passo 2: Configurar Restrições de Aplicativo
1. Em **"Restrições de aplicativo"**, selecione **"Aplicativos Android"**
2. Clique em **"+ Adicionar um item"**

### Passo 3: Adicionar SHA-1 do DEBUG (Desenvolvimento)
1. **Nome do pacote:** `com.taskgoapp.taskgo`
2. **Impressão digital do certificado SHA-1:** `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
3. Clique em **"OK"**

### Passo 4: Adicionar SHA-1 do RELEASE (Produção)
1. Clique em **"+ Adicionar um item"** novamente
2. **Nome do pacote:** `com.taskgoapp.taskgo` (mesmo)
3. **Impressão digital do certificado SHA-1:** `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
4. Clique em **"OK"**

### Passo 5: Configurar Restrições de API
1. Em **"Restrições de API"**, selecione **"Restringir chave"**
2. Selecione as seguintes APIs:
   - ✅ Maps SDK for Android
   - ✅ Geocoding API
   - ✅ Geolocation API
   - ✅ Places API
   - ✅ Places API (New)
   - ✅ Maps JavaScript API (se necessário)
   - ✅ Maps Static API (se necessário)
   - ✅ Maps Embed API (se necessário)

### Passo 6: Salvar
1. Clique em **"Salvar"**
2. Aguarde 2-5 minutos para as mudanças propagarem
3. Teste o app novamente

## 📋 Checklist de Configuração

- [ ] Acessou o Google Cloud Console
- [ ] Encontrou a chave de API `AIzaSyB4QiV69mSkvXuy8SdN71MAIygKIFOtmXo`
- [ ] Configurou "Restrições de aplicativo" como "Aplicativos Android"
- [ ] Adicionou SHA-1 do DEBUG: `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
- [ ] Adicionou SHA-1 do RELEASE: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
- [ ] Package name configurado: `com.taskgoapp.taskgo`
- [ ] APIs necessárias selecionadas nas restrições
- [ ] Salvou as alterações
- [ ] Aguardou 2-5 minutos
- [ ] Testou o app

## ⚠️ Importante

1. **Aguarde a propagação:** Mudanças no Google Cloud Console podem levar 2-5 minutos para serem aplicadas
2. **Teste em ambos os builds:** Teste com build DEBUG e RELEASE
3. **Verifique os logs:** Se ainda houver erro, verifique os logs do Logcat para mais detalhes
4. **Billing:** Certifique-se de que o billing está habilitado no projeto

## 🔍 Verificação Adicional

Se ainda houver problemas após configurar corretamente:

1. Verifique se o package name está correto: `com.taskgoapp.taskgo`
2. Verifique se copiou o SHA-1 corretamente (sem espaços extras)
3. Verifique se todas as APIs necessárias estão habilitadas no projeto
4. Verifique se o billing está habilitado
5. Verifique os logs do Logcat para erros específicos

## 📞 Próximos Passos

Após configurar:
1. Teste o app em modo DEBUG
2. Teste o app em modo RELEASE
3. Verifique se o GPS está funcionando
4. Verifique se o geocoding está funcionando
