# 🔗 Links Diretos - Configuração SHA-256 App Check

## 📋 Links para Configuração

### 1. 🔑 Obter SHA-256 do App Signing Key (Play Console)

**Link direto:**
https://play.google.com/console/developers/1093466748007/app/4973841882000000000/setup/app-signing

**Passos:**
1. Acesse o link acima
2. Role até a seção **"App signing certificate"**
3. Copie o **SHA-256 certificate fingerprint**
4. Formato esperado: `95:AF:63:3A:8F:CD:20:49:...` (hexadecimal com dois pontos)

**Alternativa (se o link acima não funcionar):**
1. Acesse: https://play.google.com/console
2. Selecione: **TaskGo**
3. Menu lateral: **Release** → **Setup** → **App signing**
4. Seção: **"App signing certificate"** → Copie o **SHA-256**

---

### 2. ✅ Adicionar SHA-256 no Firebase App Check

**Link direto:**
https://console.firebase.google.com/project/task-go-ee85f/appcheck

**Passos:**
1. Acesse o link acima
2. Selecione o app: **Task Go** (`com.taskgoapp.taskgo`)
3. Clique em: **Play Integrity** (já deve estar selecionado)
4. Na seção **"Impressão digital do certificado SHA-256"**:
   - Clique em: **"Adicionar outra impressão digital"**
   - Cole o SHA-256 do **App Signing Key** (obtido do Play Console)
   - Clique em: **Salvar**

**Nota:** Você pode manter o SHA-256 atual (Upload Key) e adicionar o novo (App Signing Key). O Firebase aceita múltiplos SHA-256.

---

### 3. 🔐 Adicionar SHA-256 no Firebase Console (Android App)

**Link direto:**
https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo

**Passos:**
1. Acesse o link acima
2. Role até a seção **"SHA certificate fingerprints"**
3. Clique em: **"Add fingerprint"** (ou "Adicionar impressão digital")
4. Cole o SHA-256 do **App Signing Key** (obtido do Play Console)
5. Clique em: **Save** (ou "Salvar")

**Nota:** Este passo é importante para garantir que o SHA-256 esteja cadastrado tanto no App Check quanto nas configurações gerais do app.

---

## 📋 Checklist Rápido

- [ ] **Passo 1:** Obter SHA-256 do App Signing Key do Play Console
- [ ] **Passo 2:** Adicionar SHA-256 no Firebase App Check (Play Integrity)
- [ ] **Passo 3:** Adicionar SHA-256 no Firebase Console (Android App → SHA certificates)
- [ ] **Passo 4:** Aguardar 5-10 minutos para propagação
- [ ] **Passo 5:** Testar o app novamente (deve estar instalado via Play Store)

---

## 🔍 Verificação

Após adicionar o SHA-256 correto, os logs do app devem mostrar:

```
✅ App Check token obtido com sucesso (Play Integrity)
Token (primeiros 20 chars): ...
```

**Se ainda mostrar erro 403:**
- Verifique se o SHA-256 foi adicionado corretamente
- Verifique se o app foi instalado via Play Store (não via APK local)
- Aguarde mais alguns minutos para propagação

---

## 📝 Informações do Projeto

- **Project ID:** `task-go-ee85f`
- **Project Number:** `1093466748007`
- **Package Name:** `com.taskgoapp.taskgo`
- **App ID:** `1:1093466748007:android:55d3d395716e81c4e8d0c2`

---

## ⚠️ Lembrete Importante

**SHA-256 atual no App Check (Upload Key):**
```
95:af:63:3a:8f:cd:20:49:a2:59:89:fb:86:71:d8:de:0f:11:89:cf:d7:82:7f:50:45:1c:fb:e7:98:cf:37:18
```

**SHA-256 necessário (App Signing Key):**
```
[OBTER DO PLAY CONSOLE - SERÁ DIFERENTE DO ACIMA]
```

**Você precisa adicionar o SHA-256 do App Signing Key, não substituir o atual!**





















