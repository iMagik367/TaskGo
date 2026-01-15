# 🔧 CORREÇÃO CRÍTICA: App Check - App Attestation Failed

## ❌ Erro Identificado

```
Error returned from API. code: 403 body: App attestation failed.
Firebase App Check token is invalid.
```

## 🎯 Causa Raiz

O erro **"App attestation failed"** ocorre quando o **SHA-256 do App Signing Key** (não do Upload Key) não está cadastrado no Firebase Console.

### ⚠️ IMPORTANTE

Quando você faz upload de um AAB para a Google Play Store:
- A Play Store **reassina** o app com o **App Signing Key** (chave gerenciada pela Google)
- O Firebase precisa do SHA-256 **dessa chave de reassinatura**, não da sua Upload Key

## ✅ Solução Passo a Passo

### ETAPA 1: Obter SHA-256 do App Signing Key

1. Acesse: [Google Play Console](https://play.google.com/console)
2. Selecione seu app: **TaskGo**
3. No menu lateral, vá em: **Release** → **Setup** → **App signing**
4. Na seção **"App signing certificate"**, copie o **SHA-256 certificate fingerprint**

   Exemplo:
   ```
   SHA-256: 95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18
   ```

### ETAPA 2: Cadastrar SHA-256 no Firebase Console

1. Acesse: [Firebase Console](https://console.firebase.google.com/project/task-go-ee85f/settings/general)
2. Na seção **"Your apps"**, clique no app Android: **com.taskgoapp.taskgo**
3. Role até a seção **"SHA certificate fingerprints"**
4. Clique em **"Add fingerprint"**
5. Cole o SHA-256 do **App Signing Key** (obtido na ETAPA 1)
6. Clique em **"Save"**

### ETAPA 3: Verificar App Check no Firebase Console

1. Acesse: [Firebase App Check](https://console.firebase.google.com/project/task-go-ee85f/appcheck)
2. Verifique se o app Android está listado
3. Verifique se o provider **Play Integrity** está **ATIVO**
4. Verifique se o SHA-256 está registrado no App Check (pode aparecer automaticamente)

### ETAPA 4: Aguardar Propagação

- Aguarde **5-10 minutos** para as mudanças se propagarem
- Reinicie o app após aguardar

## 🔍 Verificação

Após seguir os passos acima, os logs devem mostrar:

```
✅ App Check token obtido com sucesso (Play Integrity)
Token (primeiros 20 chars): ...
```

## 📋 Checklist

- [ ] SHA-256 do **App Signing Key** copiado do Play Console
- [ ] SHA-256 cadastrado no Firebase Console (seção Android App)
- [ ] Play Integrity API habilitada no Google Cloud Console
- [ ] App Check configurado no Firebase Console com Play Integrity ativo
- [ ] Aguardado 5-10 minutos para propagação
- [ ] App reinstalado/testado novamente

## 🚨 Diferença Crítica: Upload Key vs App Signing Key

### Upload Key (Sua chave local)
- Usada para assinar o AAB antes do upload
- SHA-256: `95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18`
- **NÃO é suficiente** para Play Integrity em produção

### App Signing Key (Chave da Google Play)
- Usada pela Play Store para reassinar o app
- SHA-256: **OBTER DO PLAY CONSOLE** (pode ser diferente!)
- **OBRIGATÓRIO** para Play Integrity funcionar em produção

## 🔗 Links Úteis

- [Firebase Console - Configurações do App](https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo)
- [Firebase App Check](https://console.firebase.google.com/project/task-go-ee85f/appcheck)
- [Google Play Console - App Signing](https://play.google.com/console/developers/1093466748007/app/4973841882000000000/setup/app-signing)
- [Play Integrity API](https://console.cloud.google.com/apis/library/playintegrity.googleapis.com?project=task-go-ee85f)

## 📝 Notas Técnicas

- O Play Integrity **só funciona** com apps instalados via Play Store
- Apps instalados via APK local **não funcionam** com Play Integrity
- O SHA-256 deve ser cadastrado **antes** de fazer upload do AAB
- Se o app já está na Play Store, você precisa do SHA-256 do App Signing Key atual





















