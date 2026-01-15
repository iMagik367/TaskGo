# 🔗 Links e Caminhos Corretos - SHA-256 App Signing Key

## 📋 Caminho Correto no Play Console

Baseado na estrutura atual do Play Console, o caminho é:

### 🔑 Obter SHA-256 do App Signing Key

**Caminho no Play Console:**
1. Acesse: https://play.google.com/console
2. Selecione: **TaskGo**
3. No menu lateral, clique em: **"Configuração"** (ou "Setup" em inglês)
4. Dentro de Configuração, clique em: **"Integridade do aplicativo"** (ou "App integrity")
5. Na seção **"Chaves de assinatura"** (ou "Signing keys"):
   - Procure por: **"Chave de assinatura do aplicativo"** (App signing key)
   - Copie o **SHA-256 certificate fingerprint**

**Link direto (pode variar):**
https://play.google.com/console/developers/1093466748007/app/4973841882000000000/setup/app-integrity

---

## ✅ Adicionar SHA-256 no Firebase App Check

**Link direto:**
https://console.firebase.google.com/project/task-go-ee85f/appcheck

**Passos:**
1. Acesse o link acima
2. Selecione: **Task Go** (`com.taskgoapp.taskgo`)
3. Clique em: **Play Integrity**
4. Na seção **"Impressão digital do certificado SHA-256"**:
   - Clique em: **"Adicionar outra impressão digital"**
   - Cole o SHA-256 do **App Signing Key** (obtido do Play Console)
   - Clique em: **Salvar**

---

## 🔐 Adicionar SHA-256 no Firebase Console (Android App)

**Link direto:**
https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo

**Passos:**
1. Acesse o link acima
2. Role até: **"SHA certificate fingerprints"**
3. Clique em: **"Add fingerprint"**
4. Cole o SHA-256 do **App Signing Key**
5. Clique em: **Save**

---

## 🔍 Se Não Encontrar "Integridade do aplicativo"

Se o caminho acima não funcionar, tente:

### Alternativa 1: Buscar no Console
1. Acesse: https://play.google.com/console
2. Selecione: **TaskGo**
3. Use a barra de busca no topo
4. Procure por: **"App signing"** ou **"SHA-256"** ou **"Integridade"**

### Alternativa 2: Via "Testar e lançar"
1. Acesse: https://play.google.com/console
2. Selecione: **TaskGo**
3. Menu: **"Testar e lançar"** (Test and release)
4. Procure por: **"Configuração"** ou **"Setup"**
5. Dentro, procure por: **"App signing"** ou **"Assinatura"**

### Alternativa 3: Verificar após Upload
Se você já fez upload de um AAB:
1. O App Signing Key deve estar disponível
2. Procure na seção de **"Versões"** ou **"Releases"**
3. Ou na seção de **"Configuração"** do app

---

## ⚠️ Importante

**Se o app ainda não foi publicado:**
- A Play Store só gera o App Signing Key após o primeiro upload
- Se você ainda não fez upload de nenhuma versão, pode não existir App Signing Key ainda
- Neste caso, você precisa fazer upload de um AAB primeiro

**Se já fez upload:**
- O App Signing Key deve estar disponível
- Procure em "Configuração" > "Integridade do aplicativo"
- Ou use a busca do console

---

## 📝 Checklist

- [ ] Acessar Play Console → TaskGo
- [ ] Ir em "Configuração" → "Integridade do aplicativo"
- [ ] Copiar SHA-256 do "App Signing Key"
- [ ] Adicionar no Firebase App Check (link acima)
- [ ] Adicionar no Firebase Console - Android App (link acima)
- [ ] Aguardar 5-10 minutos
- [ ] Testar o app

---

## 🔗 Links Resumidos

1. **Play Console - App Integrity:**
   - https://play.google.com/console
   - Caminho: Configuração → Integridade do aplicativo

2. **Firebase App Check:**
   - https://console.firebase.google.com/project/task-go-ee85f/appcheck

3. **Firebase Console - Android App:**
   - https://console.firebase.google.com/project/task-go-ee85f/settings/general/android:com.taskgoapp.taskgo





















