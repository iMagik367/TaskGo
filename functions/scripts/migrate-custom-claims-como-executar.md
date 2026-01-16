# 🔄 Como Executar Migração de Custom Claims

## ⚠️ PROBLEMA: Credenciais Necessárias

Para executar o script localmente, você precisa de credenciais do Firebase Admin SDK.

---

## ✅ SOLUÇÃO 1: Usar Cloud Function Deployada (Recomendado)

A função `migrateExistingUsersToCustomClaims` já está deployada. Execute via HTTP:

### Opção A: Usando curl (se tiver token)

```bash
# 1. Obter token de autenticação (requer firebase-admin configurado)
# OU usar um token de service account

# 2. Chamar função via HTTP POST
curl -X POST \
  https://us-central1-task-go-ee85f.cloudfunctions.net/migrateExistingUsersToCustomClaims \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -H "Content-Type: application/json" \
  -d '{"dryRun": false}'
```

### Opção B: Via Firebase Console

1. Firebase Console → Functions
2. Encontrar função `migrateExistingUsersToCustomClaims`
3. Testar função no console (se disponível)
4. OU criar script Node.js usando Admin SDK

---

## ✅ SOLUÇÃO 2: Configurar Credenciais Locais

### 2.1 Baixar Service Account Key

1. Firebase Console → Project Settings → Service Accounts
2. Clicar em "Generate new private key"
3. Salvar o arquivo JSON (ex: `service-account-key.json`)

### 2.2 Configurar Variável de Ambiente

**Windows PowerShell:**
```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\caminho\para\service-account-key.json"
```

**Linux/Mac:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/caminho/para/service-account-key.json"
```

### 2.3 Executar Script

```bash
cd functions
node scripts/migrate-custom-claims.js
```

---

## ✅ SOLUÇÃO 3: Usar Script Node.js com Credenciais Explícitas

Criar arquivo `functions/scripts/migrate-with-credentials.js`:

```javascript
const admin = require('firebase-admin');
const serviceAccount = require('../service-account-key.json'); // Ajustar caminho

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Resto do código igual ao migrate-custom-claims.js
```

---

## ✅ SOLUÇÃO 4: Usar Firebase Emulator (Para Testes)

```bash
cd functions
firebase emulators:start --only auth
# Em outro terminal:
node scripts/migrate-custom-claims.js
```

---

## 🎯 RECOMENDAÇÃO FINAL

**Para produção, usar a Cloud Function deployada via HTTP** ou configurar service account key e executar o script local.

---

**Nota:** O script JavaScript está pronto em `functions/scripts/migrate-custom-claims.js` - só precisa de credenciais configuradas.
