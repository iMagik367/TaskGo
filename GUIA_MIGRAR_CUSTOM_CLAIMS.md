# 🔄 Guia: Migrar Custom Claims para Usuários Existentes

Este guia mostra como executar a migração de Custom Claims para todos os usuários existentes.

---

## 📋 PRÉ-REQUISITOS

1. ✅ Deploy da função `migrateExistingUsersToCustomClaims` concluído
2. ✅ Acesso ao projeto Firebase (credenciais configuradas)
3. ✅ Permissões de admin no Firebase

---

## 🚀 OPÇÕES DE MIGRAÇÃO

### Opção 1: Via Script Local (Recomendado) ✅

Esta é a forma mais simples e controlável:

```bash
# 1. Compilar
cd functions
npm run build

# 2. Executar migração (simulação primeiro)
node lib/scripts/migrateExistingUsers.js

# OU criar arquivo de execução direto
node -e "require('./lib/scripts/migrateExistingUsers').migrateLocal()"
```

**Vantagens:**
- Controle total sobre o processo
- Logs detalhados em tempo real
- Pode ser interrompido com Ctrl+C se necessário

---

### Opção 2: Via Cloud Function (Callable)

A função `migrateExistingUsersToCustomClaims` está deployada, mas precisa ser chamada via HTTP ou Admin SDK.

#### Usando Admin SDK (Node.js):

```typescript
// Criar arquivo: migrate-via-admin.ts
import * as admin from 'firebase-admin';
import {initializeApp} from 'firebase-admin/app';

initializeApp();

async function runMigration() {
  const functions = admin.functions();
  const migrateFunction = functions.httpsCallable('migrateExistingUsersToCustomClaims');
  
  // Teste com dry-run
  console.log('Executando dry-run...');
  const dryRunResult = await migrateFunction({dryRun: true});
  console.log('Dry-run result:', dryRunResult.data);
  
  // Se tudo OK, executar de verdade
  console.log('Executando migração real...');
  const realResult = await migrateFunction({dryRun: false});
  console.log('Migration result:', realResult.data);
}

runMigration().catch(console.error);
```

```bash
# Compilar e executar
cd functions
npm run build
node lib/migrate-via-admin.js
```

---

### Opção 3: Via HTTP Request (curl/Postman)

Se você tem o token de autenticação:

```bash
# Obter token de autenticação (requer firebase-admin)
TOKEN="seu-firebase-admin-token"

# Chamar função via HTTP
curl -X POST \
  https://us-central1-task-go-ee85f.cloudfunctions.net/migrateExistingUsersToCustomClaims \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"dryRun": true}'
```

---

## 🔧 IMPLEMENTAÇÃO RECOMENDADA: Script Local

### 1. Criar Script Executável

Criar arquivo `functions/scripts/run-migration.js` (já criado acima).

### 2. Adicionar ao package.json

```json
{
  "scripts": {
    "migrate:users": "npm run build && node lib/scripts/migrateExistingUsers.js"
  }
}
```

### 3. Executar

```bash
cd functions
npm run migrate:users
```

---

## ✅ VERIFICAÇÃO PÓS-MIGRAÇÃO

### Verificar Custom Claims

Após a migração, verificar se os usuários têm Custom Claims:

#### Opção A: Firebase Console
1. Firebase Console → Authentication → Users
2. Abrir um usuário
3. Verificar "Custom claims" na seção de detalhes

#### Opção B: Script de Verificação

```typescript
// verify-custom-claims.ts
import * as admin from 'firebase-admin';

admin.initializeApp();

async function verifyCustomClaims() {
  const listUsersResult = await admin.auth().listUsers(100);
  let withClaims = 0;
  let withoutClaims = 0;
  
  for (const user of listUsersResult.users) {
    const claims = user.customClaims || {};
    if (claims.role) {
      withClaims++;
      console.log(`✓ ${user.email || user.uid}: role=${claims.role}`);
    } else {
      withoutClaims++;
      console.log(`✗ ${user.email || user.uid}: sem Custom Claims`);
    }
  }
  
  console.log(`\nTotal: ${listUsersResult.users.length}`);
  console.log(`Com Custom Claims: ${withClaims}`);
  console.log(`Sem Custom Claims: ${withoutClaims}`);
}

verifyCustomClaims().catch(console.error);
```

---

## 📊 O QUE A MIGRAÇÃO FAZ

A migração:
1. Lista todos os usuários do Firebase Auth
2. Para cada usuário:
   - Verifica se já tem Custom Claims com role válido (pula se tiver)
   - Busca role no documento Firestore (`/users/{uid}`)
   - Mapeia role do Firestore para Custom Claims:
     - `client` → `user`
     - `provider`, `seller`, `partner` → mantém (compatibilidade)
     - `admin`, `moderator` → mantém
   - Define Custom Claims no Firebase Auth
   - Sincroniza role no documento Firestore (se necessário)

---

## ⚠️ IMPORTANTE

1. **Backup:** Recomendado fazer backup dos dados antes da migração
2. **Teste Primeiro:** Sempre executar dry-run primeiro
3. **Interrupção:** A migração pode ser interrompida (Ctrl+C) - será retomada na próxima execução para usuários não migrados
4. **Quota:** Firebase Auth tem limites de rate - o script já processa em batches

---

## 🆘 TROUBLESHOOTING

### Erro: "Permission denied"
- Verificar se você tem permissões de admin no Firebase
- Verificar se o service account tem as permissões necessárias

### Erro: "Quota exceeded"
- A migração processa em batches - aguardar e retomar
- Verificar quotas no Firebase Console

### Custom Claims não aparecem
- Verificar se o token foi atualizado: `getIdToken(true)`
- Custom Claims são incluídas no token JWT na próxima renovação

---

## 📝 COMANDO RÁPIDO

```bash
# Migração completa em um comando
cd functions && npm run build && node -e "require('./lib/scripts/migrateExistingUsers').migrateLocal()"
```

---

**Última atualização:** 2024
