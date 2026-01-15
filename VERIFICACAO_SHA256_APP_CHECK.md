# 🔍 Verificação SHA-256 App Check

## 📋 SHA-256 Cadastrado no App Check

**SHA-256 atual no Firebase App Check:**
```
42aekdJSFWXCwxRKNEY3ye2AkV3YHuAfGdjF6wUS7dV2NU2p26gZWCYGruYrp1HKxMDt8ZwnqxPBrGJG44jm6XnMEdtH15K
```

**Formato:** Base64 ou formato customizado do Firebase

---

## ⚠️ PROBLEMA IDENTIFICADO

O SHA-256 que você cadastrou no App Check pode não corresponder ao SHA-256 do **App Signing Key** que a Play Store usa para reassinar o app.

### Por que isso é crítico?

1. **Upload Key** (sua chave local):
   - Você usa para assinar o AAB antes do upload
   - SHA-256: `95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18`

2. **App Signing Key** (chave da Play Store):
   - A Play Store **reassina** seu app com esta chave
   - SHA-256: **DIFERENTE** - obtido do Play Console
   - **Este é o SHA-256 que o Firebase precisa!**

---

## ✅ SOLUÇÃO

### ETAPA 1: Obter SHA-256 Correto do App Signing Key

1. Acesse: [Google Play Console](https://play.google.com/console)
2. Selecione: **TaskGo** (seu app)
3. Vá em: **Release** → **Setup** → **App signing**
4. Na seção **"App signing certificate"**, copie o **SHA-256 certificate fingerprint**

   **Formato esperado:**
   ```
   SHA-256: 95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18
   ```

### ETAPA 2: Converter para Formato do Firebase (se necessário)

O Firebase App Check pode aceitar SHA-256 em diferentes formatos:

**Formato Hexadecimal (com dois pontos):**
```
95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18
```

**Formato Hexadecimal (sem dois pontos):**
```
95AF633A8FCD2049A25989FB8671D8DE0F1189CFD7827F50451CFBE798CF3718
```

**Formato Base64 (pode ser o que você tem):**
```
42aekdJSFWXCwxRKNEY3ye2AkV3YHuAfGdjF6wUS7dV2NU2p26gZWCYGruYrp1HKxMDt8ZwnqxPBrGJG44jm6XnMEdtH15K
```

### ETAPA 3: Atualizar no Firebase App Check

1. Acesse: [Firebase App Check](https://console.firebase.google.com/project/task-go-ee85f/appcheck)
2. Selecione o app: **Task Go** (`com.taskgoapp.taskgo`)
3. Clique em **Play Integrity**
4. Na seção **"Impressão digital do certificado SHA-256"**:
   - Se o SHA-256 atual estiver incorreto, **remova** o atual
   - Clique em **"Adicionar outra impressão digital"**
   - Cole o SHA-256 do **App Signing Key** (obtido do Play Console)
   - Salve

### ETAPA 4: Verificar também no Firebase Console (Android App)

1. Acesse: [Firebase Console - Android App](https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo)
2. Role até **"SHA certificate fingerprints"**
3. Verifique se o SHA-256 do **App Signing Key** está cadastrado
4. Se não estiver, adicione

---

## 🔍 Como Verificar se Está Correto

### Teste 1: Verificar Logs do App

Após atualizar o SHA-256, os logs devem mostrar:

```
✅ App Check token obtido com sucesso (Play Integrity)
Token (primeiros 20 chars): ...
```

**Se ainda mostrar erro:**
```
❌ FALHA AO OBTER APP CHECK TOKEN
Error returned from API. code: 403 body: App attestation failed.
```

**Significa que:**
- O SHA-256 ainda não corresponde
- Ou o app não foi instalado via Play Store
- Ou precisa aguardar mais tempo para propagação

### Teste 2: Verificar no Play Console

1. Acesse: [Play Console - App Signing](https://play.google.com/console)
2. Compare o SHA-256 do **"App signing certificate"** com o cadastrado no Firebase
3. **Devem ser idênticos!**

---

## 📝 Notas Importantes

1. **O SHA-256 do Upload Key NÃO é suficiente**
   - Mesmo que você tenha cadastrado o SHA-256 do seu keystore local
   - O Firebase precisa do SHA-256 do App Signing Key da Play Store

2. **Formato do SHA-256**
   - O Firebase pode aceitar diferentes formatos
   - Mas o valor deve corresponder ao SHA-256 real do App Signing Key

3. **Propagação**
   - Após cadastrar, aguarde 5-10 minutos
   - Reinicie o app completamente
   - Teste novamente

4. **App deve ser instalado via Play Store**
   - Play Integrity só funciona com apps instalados via Play Store
   - Apps instalados via APK local não funcionam

---

## 🔗 Links Diretos

- [Firebase App Check](https://console.firebase.google.com/project/task-go-ee85f/appcheck)
- [Firebase Console - Android App](https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo)
- [Google Play Console - App Signing](https://play.google.com/console)

---

## ⚠️ AÇÃO NECESSÁRIA

**Você precisa verificar se o SHA-256 cadastrado no App Check corresponde ao SHA-256 do App Signing Key da Play Store.**

Se não corresponder, atualize no Firebase App Check com o SHA-256 correto obtido do Play Console.





















