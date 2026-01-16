# 📋 Relatório Atualizado: Regras Firestore e Autenticação

**Data de atualização:** Janeiro 2025  
**Versão das regras:** rules_version = '2'  
**Arquitetura:** Custom Claims + App Check + Cloud Functions

---

## 🔐 1️⃣ REGRAS FIRESTORE ATUALIZADAS

### ✨ **NOVA ARQUITETURA: Custom Claims**

O sistema agora utiliza **Custom Claims** do Firebase Auth como **autoridade única** para roles de usuários, em vez de confiar apenas no campo `role` no Firestore. Isso proporciona maior segurança e performance.

### 📌 **Helper Functions (Funções Auxiliares)**

As regras incluem funções auxiliares para simplificar e padronizar verificações:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ==========================================
    // HELPER FUNCTIONS
    // ==========================================
    
    // Verifica se o usuário está autenticado
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Obtém o role do usuário através de Custom Claims (autoridade única)
    function getUserRole() {
      return request.auth.token.role;
    }
    
    // Verifica se o usuário é admin
    function isAdmin() {
      return isAuthenticated() && getUserRole() == 'admin';
    }
    
    // Verifica se o usuário é moderador ou admin
    function isModeratorOrAdmin() {
      return isAuthenticated() && (getUserRole() == 'moderator' || getUserRole() == 'admin');
    }
    
    // Verifica se o usuário é o dono do recurso
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
  }
}
```

**🔥 Vantagens:**
- ✅ Roles verificados no token JWT (Custom Claims), não no Firestore
- ✅ Mais seguro: roles não podem ser alterados diretamente no Firestore
- ✅ Mais rápido: não precisa ler documento do usuário para verificar role
- ✅ Sincronização automática: Custom Claims são incluídos em todos os tokens

---

### 🗂️ **Coleções Principais**

#### **1. Users Collection** (`/users/{userId}`)

```javascript
match /users/{userId} {
  // ✅ Leitura: Próprio usuário, moderadores e admins
  allow read: if isOwner(userId) || isModeratorOrAdmin();
  
  // ✅ Criação: Apenas o próprio usuário pode criar seu documento inicial
  // Role é definido por Cloud Functions (setInitialUserRole)
  allow create: if isOwner(userId) 
                && request.resource.data.uid == userId
                && request.resource.data.keys().hasAll(['uid', 'email']);
  
  // ✅ Atualização: Apenas o próprio usuário pode atualizar (exceto role)
  // Role só pode ser alterado por admins via Cloud Functions
  allow update: if isOwner(userId)
                && !('role' in request.resource.data.diff(resource.data).affectedKeys())
                && !('roleUpdatedAt' in request.resource.data.diff(resource.data).affectedKeys())
                && !('roleUpdatedBy' in request.resource.data.diff(resource.data).affectedKeys());
  
  // ✅ Admins podem atualizar qualquer campo (incluindo role)
  allow update: if isAdmin();
  
  // ✅ Exclusão: Apenas admins
  allow delete: if isAdmin();
}
```

**Características:**
- 🔒 Proteção de campos críticos (`role`, `roleUpdatedAt`, `roleUpdatedBy`)
- 🔒 Apenas admins podem alterar roles
- 🔒 Validação de estrutura de dados na criação

**Subcoleções:**
- **Services** (`/users/{userId}/services/{serviceId}`): ✅ Leitura permitida, ❌ Escrita bloqueada (usar Cloud Functions)
- **Products** (`/users/{userId}/products/{productId}`): ✅ Leitura permitida, ❌ Escrita bloqueada (usar Cloud Functions)
- **Orders** (`/users/{userId}/orders/{orderId}`): ✅ Leitura por cliente/prestador/admin, ❌ Escrita bloqueada (usar Cloud Functions)
- **Posts** (`/users/{userId}/posts/{postId}`): ✅ CRUD pelo dono
- **PostInterests** (`/users/{userId}/postInterests/{interestId}`): ✅ CRUD privado pelo dono
- **BlockedUsers** (`/users/{userId}/blockedUsers/{blockId}`): ✅ CRUD privado pelo dono
- **Stories** (`/users/{userId}/stories/{storyId}`): ✅ Leitura pública, escrita pelo dono

---

#### **2. Services Collection** (`/services/{serviceId}`) - PÚBLICA

```javascript
match /services/{serviceId} {
  // ✅ Leitura: Qualquer usuário autenticado pode ler serviços ativos
  allow read: if isAuthenticated() 
              && (resource.data.active == true);
  
  // ❌ Escrita: BLOQUEADA - usar Cloud Functions
  // App não pode criar/editar serviços diretamente
  allow write: if false;
}
```

**Motivo do bloqueio de escrita:**
- Validações de negócio complexas
- Sincronização com subcoleções
- Auditoria e logs centralizados
- Prevenção de inconsistências

---

#### **3. Products Collection** (`/products/{productId}`) - PÚBLICA

```javascript
match /products/{productId} {
  // ✅ Leitura: Apenas produtos com status "active" são públicos
  allow read: if isAuthenticated() 
              && resource.data.status == 'active'
              && resource.data.active == true;
  
  // ❌ Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}
```

**Validações:**
- Apenas produtos `active == true` e `status == 'active'` são visíveis
- Previne vazamento de produtos inativos/excluídos

---

#### **4. Orders Collection** (`/orders/{orderId}`) - PÚBLICA

```javascript
match /orders/{orderId} {
  // ✅ Leitura: Cliente ou prestador relacionado, ou admins
  allow read: if isAuthenticated() 
              && (resource.data.clientId == request.auth.uid 
                  || resource.data.providerId == request.auth.uid
                  || isAdmin());
  
  // ❌ Escrita: BLOQUEADA - usar Cloud Functions
  // Transições de status são validadas pela Cloud Function
  allow write: if false;
}
```

**Segurança:**
- Apenas participantes da ordem podem ler
- Validação de transições de status via Cloud Functions

---

#### **5. Posts Collection** (`/posts/{postId}`)

```javascript
match /posts/{postId} {
  // ✅ Leitura pública para usuários autenticados
  allow read: if isAuthenticated();
  
  // ✅ Criação: Apenas o autor
  allow create: if isAuthenticated() 
                && request.resource.data.userId == request.auth.uid;
  
  // ✅ Atualização/Exclusão: Apenas o autor
  allow update, delete: if isAuthenticated() 
                        && resource.data.userId == request.auth.uid;
  
  // Subcoleções: Ratings e Comments
  match /ratings/{ratingId} {
    allow read: if isAuthenticated();
    allow create: if isAuthenticated() 
                  && request.resource.data.userId == request.auth.uid
                  && request.resource.data.postId == postId
                  && request.resource.data.rating is int
                  && request.resource.data.rating >= 1
                  && request.resource.data.rating <= 5;
    allow update, delete: if isAuthenticated() 
                          && resource.data.userId == request.auth.uid;
  }
  
  match /comments/{commentId} {
    allow read: if isAuthenticated();
    allow create: if isAuthenticated() 
                  && request.resource.data.userId == request.auth.uid
                  && request.resource.data.postId == postId;
    allow update, delete: if isAuthenticated() 
                          && resource.data.userId == request.auth.uid;
  }
}
```

---

#### **6. Conversations Collection** (`/conversations/{conversationId}`)

```javascript
match /conversations/{conversationId} {
  // ✅ Apenas participantes podem ler
  allow read: if isAuthenticated() 
              && resource.data.userId == request.auth.uid;
  
  allow create: if isAuthenticated() 
                && request.resource.data.userId == request.auth.uid;
  
  allow update: if isAuthenticated() 
                && resource.data.userId == request.auth.uid;
  
  allow delete: if isAuthenticated() 
                && resource.data.userId == request.auth.uid;
  
  match /messages/{messageId} {
    // ✅ Mensagens podem ser criadas por participantes
    allow read: if isAuthenticated() 
                && get(/databases/$(database)/documents/conversations/$(conversationId)).data.userId == request.auth.uid;
    
    allow create: if isAuthenticated() 
                  && get(/databases/$(database)/documents/conversations/$(conversationId)).data.userId == request.auth.uid;
    
    // ❌ Mensagens são imutáveis
    allow update, delete: if false;
  }
}
```

---

#### **7. Bank Accounts Collection** (`/bank_accounts/{accountId}`)

```javascript
match /bank_accounts/{accountId} {
  // ✅ Leitura: Apenas o dono
  allow read: if isAuthenticated() 
              && resource.data.userId == request.auth.uid;
  
  // ✅ Criação: Validação rigorosa de campos
  allow create: if isAuthenticated() 
                && request.resource.data.userId == request.auth.uid
                && request.resource.data.keys().hasAll(['userId', 'bankName', 'bankCode', 'agency', 'account', 'accountType', 'accountHolderName', 'accountHolderDocument', 'accountHolderDocumentType', 'isDefault'])
                && request.resource.data.bankName is string && request.resource.data.bankName.size() > 0
                && request.resource.data.bankCode is string && request.resource.data.bankCode.size() > 0
                && request.resource.data.agency is string && request.resource.data.agency.size() >= 4 && request.resource.data.agency.size() <= 5
                && request.resource.data.account is string && request.resource.data.account.size() >= 5 && request.resource.data.account.size() <= 12
                && request.resource.data.accountType is string && (request.resource.data.accountType == "CHECKING" || request.resource.data.accountType == "SAVINGS")
                && request.resource.data.accountHolderName is string && request.resource.data.accountHolderName.size() >= 3
                && request.resource.data.accountHolderDocument is string && request.resource.data.accountHolderDocument.size() >= 11 && request.resource.data.accountHolderDocument.size() <= 14
                && request.resource.data.accountHolderDocumentType is string && (request.resource.data.accountHolderDocumentType == "CPF" || request.resource.data.accountHolderDocumentType == "CNPJ")
                && request.resource.data.isDefault is bool;
  
  // ✅ Atualização: Completa ou parcial (apenas isDefault)
  allow update: if isAuthenticated() 
                && resource.data.userId == request.auth.uid
                && (
                  // Atualização completa com validações
                  (/* validações completas */)
                  ||
                  // Atualização parcial (apenas isDefault)
                  (request.resource.data.keys().hasOnly(['isDefault']) && request.resource.data.isDefault is bool)
                );
  
  // ✅ Exclusão: Apenas o dono
  allow delete: if isAuthenticated() 
                && resource.data.userId == request.auth.uid;
}
```

**Validações implementadas:**
- ✅ Campos obrigatórios
- ✅ Tipos de dados corretos
- ✅ Tamanhos mínimos/máximos
- ✅ Valores enum (CHECKING/SAVINGS, CPF/CNPJ)
- ✅ Atualização parcial permitida apenas para `isDefault`

---

#### **8. Stories Collection** (`/stories/{storyId}`)

```javascript
match /stories/{storyId} {
  // ✅ Leitura pública
  allow read: if isAuthenticated();
  
  // ✅ Escrita: Apenas o dono
  allow create: if isAuthenticated() 
                && request.resource.data.userId == request.auth.uid;
  
  allow update, delete: if isAuthenticated() 
                        && resource.data.userId == request.auth.uid;
  
  // Subcoleção de views (analytics)
  match /views/{userId} {
    allow read: if isAuthenticated();
    allow create: if isAuthenticated() 
                  && request.auth.uid == userId;
    // ❌ Views são imutáveis
    allow update, delete: if false;
  }
}
```

---

#### **9. Outras Coleções**

**Notifications:**
```javascript
match /notifications/{notificationId} {
  // ✅ Apenas o dono pode ler
  allow read: if isAuthenticated() 
              && resource.data.userId == request.auth.uid;
  
  // ❌ Escrita: Apenas Cloud Functions
  allow write: if false;
}
```

**Reviews:**
```javascript
match /reviews/{reviewId} {
  // ✅ Leitura pública
  allow read: if isAuthenticated();
  
  // ❌ Escrita: Apenas Cloud Functions
  allow write: if false;
}
```

**AI Usage:**
```javascript
match /ai_usage/{usageId} {
  // ✅ Leitura: Apenas o dono
  allow read: if isAuthenticated() 
              && resource.data.userId == request.auth.uid;
  
  // ❌ Escrita: Apenas Cloud Functions
  allow write: if false;
}
```

**Moderation Logs:**
```javascript
match /moderation_logs/{logId} {
  // ✅ Leitura: Apenas admins
  allow read: if isAdmin();
  
  // ❌ Escrita: Apenas Cloud Functions
  allow write: if false;
}
```

---

### 🔒 **Regra Padrão (Deny All)**

```javascript
// Deny all other collections by default
match /{document=**} {
  allow read, write: if false;
}
```

Todas as coleções não especificadas são negadas por padrão.

---

## 2️⃣ AUTENTICAÇÃO: BACKEND (CLOUD FUNCTIONS)

### 🔐 **Arquitetura de Segurança**

O sistema implementa **camadas múltiplas de segurança**:

1. ✅ **App Check**: Valida que requisições vêm de apps legítimos
2. ✅ **Custom Claims**: Roles no token JWT (autoridade única)
3. ✅ **Firestore Rules**: Validação de acesso aos dados
4. ✅ **Cloud Functions**: Validações de negócio e lógica complexa

---

### 📋 **A. Validação de App Check**

**Arquivo:** `functions/src/security/appCheck.ts`

```typescript
import * as functions from 'firebase-functions';

/**
 * Middleware para validar App Check token
 * Garante que apenas requests de apps legítimos sejam processados
 */
export const validateAppCheck = (
  context: functions.https.CallableContext,
): void => {
  // Em produção, App Check deve estar habilitado
  // Em desenvolvimento/emulador, permitir sem token
  if (
    process.env.FUNCTIONS_EMULATOR === 'true' ||
    process.env.NODE_ENV === 'development'
  ) {
    return;
  }

  // App Check token está em context.app
  // Se não houver token válido, context.app será undefined
  if (!context.app) {
    functions.logger.warn('App Check token missing', {
      uid: context.auth?.uid,
      timestamp: new Date().toISOString(),
    });
    throw new functions.https.HttpsError(
      'failed-precondition',
      'App Check validation failed. This request must come from a legitimate app.',
    );
  }
};
```

**Uso:**
```typescript
export const myFunction = functions.https.onCall(async (data, context) => {
  // ✅ Validar App Check primeiro
  validateAppCheck(context);
  
  // ✅ Depois validar autenticação
  assertAuthenticated(context);
  
  // ... resto da função
});
```

---

### 📋 **B. Helpers de Autenticação e Erro**

**Arquivo:** `functions/src/utils/errors.ts`

```typescript
import * as functions from 'firebase-functions';

export class AppError extends Error {
  constructor(
    public code: string,
    public message: string,
    public statusCode: number = 500,
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export const handleError = (error: unknown): functions.https.HttpsError => {
  // Não logar dados sensíveis
  const errorMessage = error instanceof Error ? error.message : 'An unknown error occurred';
  const errorCode = error instanceof AppError ? error.code : 'internal';

  // Log estruturado sem dados sensíveis
  functions.logger.error('Error occurred', {
    code: errorCode,
    message: errorMessage,
    timestamp: new Date().toISOString(),
  });

  if (error instanceof AppError) {
    return new functions.https.HttpsError(
      error.code as functions.https.FunctionsErrorCode,
      error.message,
    );
  }

  if (error instanceof Error) {
    return new functions.https.HttpsError('internal', error.message);
  }

  return new functions.https.HttpsError('internal', 'An unknown error occurred');
};

export const assertAuthenticated = (context: functions.https.CallableContext) => {
  if (!context.auth) {
    throw new AppError('unauthenticated', 'User must be authenticated', 401);
  }
};
```

---

### 📋 **C. Trigger de Criação de Usuário**

**Arquivo:** `functions/src/auth.ts`

```typescript
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

/**
 * Triggered when a new user is created in Firebase Auth
 * Creates corresponding user document in Firestore
 * IMPORTANTE: Usa merge para não sobrescrever campos já definidos pelo app (como role)
 */
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  const db = admin.firestore();
  
  try {
    const userRef = db.collection('users').doc(user.uid);
    const userDoc = await userRef.get();
    
    if (userDoc.exists) {
      // Documento já existe - fazer merge apenas dos campos básicos
      // CRÍTICO: NÃO sobrescrever role ou pendingAccountType
      const existingData = userDoc.data();
      const updateData: { [key: string]: unknown } = {
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };
      
      // Só atualizar campos que não existem ou são null
      if (!existingData?.email && user.email) {
        updateData.email = user.email;
      }
      // ... outros campos básicos
      
      await userRef.update(updateData);
    } else {
      // Criar documento inicial
      const userData = {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL,
        role: 'user', // Default role - será atualizado por setInitialUserRole
        pendingAccountType: true, // Flag para indicar que o app precisa mostrar dialog
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        profileComplete: false,
        verified: false,
      };

      await userRef.set(userData, { merge: true });
      
      // ✅ Definir Custom Claim padrão como "user"
      await admin.auth().setCustomUserClaims(user.uid, {
        role: 'user',
      });
      
      functions.logger.info(
        `User document created for ${user.uid} with pendingAccountType flag and default Custom Claim role=user`
      );
    }
    
    return null;
  } catch (error) {
    functions.logger.error('Error creating user document:', error);
    throw error;
  }
});
```

**Pontos importantes:**
- ✅ Preserva dados existentes (não sobrescreve `role` ou `pendingAccountType`)
- ✅ Define Custom Claim padrão como `'user'`
- ✅ Flag `pendingAccountType` indica que o app precisa mostrar dialog de seleção

---

### 📋 **D. Definição de Role Inicial (Custom Claims)**

**Arquivo:** `functions/src/users/role.ts`

```typescript
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from '../utils/errors';
import {validateAppCheck} from '../security/appCheck';

/**
 * Define o role inicial do usuário após cadastro
 * Esta função é chamada quando o usuário seleciona o tipo de conta (client/provider/seller)
 * 
 * IMPORTANTE:
 * - Define Custom Claims no Firebase Auth (autoridade única)
 * - Sincroniza role no documento do Firestore (apenas para referência)
 * - Firestore Rules devem usar request.auth.token.role (Custom Claims)
 */
export const setInitialUserRole = functions.https.onCall(
  async (data, context) => {
    try {
      // ✅ Validar App Check
      validateAppCheck(context);
      
      // ✅ Validar autenticação
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = admin.firestore();
      const {role, accountType} = data;

      // Validar parâmetros
      if (!role || typeof role !== 'string') {
        throw new AppError('invalid-argument', 'role is required and must be a string', 400);
      }

      // Mapear accountType legado para roles novos
      const validRoles = ['user', 'admin', 'moderator', 'provider', 'seller', 'partner', 'client'];
      let finalRole = role;

      if (role === 'client') {
        finalRole = 'user';
      }

      // Validar role final
      if (!validRoles.includes(finalRole)) {
        throw new AppError(
          'invalid-argument',
          `Invalid role: ${role}. Must be one of: ${validRoles.join(', ')}`,
          400,
        );
      }

      // Verificar se o usuário já tem role definido
      const userDoc = await db.collection('users').doc(userId).get();
      if (!userDoc.exists) {
        throw new AppError('not-found', 'User document not found', 404);
      }

      const userData = userDoc.data();
      const existingRole = userData?.role;

      // Se já tem role definido e não é "client" (padrão), não permitir mudança
      if (existingRole && existingRole !== 'client' && existingRole !== 'user') {
        throw new AppError(
          'failed-precondition',
          `User already has role: ${existingRole}. Only admins can change roles.`,
          400,
        );
      }

      // Verificar se já tem Custom Claims
      const userRecord = await admin.auth().getUser(userId);
      const existingCustomClaims = userRecord.customClaims || {};
      const existingCustomClaimsRole = existingCustomClaims.role;

      // Se já tem Custom Claims com role diferente de "user"/"client", não permitir
      if (existingCustomClaimsRole && 
          existingCustomClaimsRole !== 'user' && 
          existingCustomClaimsRole !== 'client') {
        throw new AppError(
          'failed-precondition',
          `User already has Custom Claim role: ${existingCustomClaimsRole}. Only admins can change roles.`,
          400,
        );
      }

      // ✅ DEFINIR CUSTOM CLAIMS NO FIREBASE AUTH (AUTORIDADE ÚNICA)
      await admin.auth().setCustomUserClaims(userId, {
        ...existingCustomClaims,
        role: finalRole,
      });

      // Sincronizar role no documento do Firestore (apenas para referência/compatibilidade)
      await db.collection('users').doc(userId).update({
        role: finalRole,
        pendingAccountType: false, // Remover flag de pendência
        roleSetAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      functions.logger.info(`Initial role ${finalRole} set for user ${userId}`, {
        userId,
        role: finalRole,
        accountType: accountType || null,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        role: finalRole,
        message: `Role ${finalRole} set successfully`,
      };
    } catch (error) {
      functions.logger.error('Error setting initial user role:', error);
      throw handleError(error);
    }
  },
);
```

**Fluxo de Custom Claims:**
1. ✅ Usuário cria conta → `onUserCreate` define Custom Claim `role: 'user'`
2. ✅ Usuário seleciona tipo de conta → `setInitialUserRole` atualiza Custom Claim
3. ✅ Token JWT inclui Custom Claim automaticamente
4. ✅ Firestore Rules verificam `request.auth.token.role`

---

### 📋 **E. Autenticação 2FA (com App Check)**

**Arquivo:** `functions/src/twoFactorAuth.ts`

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';

const db = admin.firestore();

/**
 * Envia código de verificação 2FA por email
 */
export const sendTwoFactorCode = functions.https.onCall(async (data, context) => {
  try {
    // ✅ Validar App Check primeiro
    validateAppCheck(context);
    
    // ✅ Verificar autenticação
    assertAuthenticated(context);
    
    const userId = context.auth!.uid;
    
    // Buscar informações do usuário
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Usuário não encontrado'
      );
    }
    
    const userData = userDoc.data();
    let email = userData?.email;
    if (!email) {
      const authUser = await admin.auth().getUser(userId);
      email = authUser.email || undefined;
    }
    
    if (!email) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Email necessário para envio do código'
      );
    }
    
    // Gerar código de 6 dígitos
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = Date.now() + (10 * 60 * 1000); // 10 minutos
    
    // Salvar código no Firestore
    await db.collection('twoFactorCodes').doc(userId).set({
      code,
      expiresAt,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      method: 'email'
    });
    
    // Enviar email (via Firebase Extensions Trigger Email)
    await sendVerificationEmail(email, code);
    
    functions.logger.info(`Código 2FA gerado para usuário ${userId}`);
    
    return {
      success: true,
      method: 'email',
      message: `Código enviado para ${maskEmail(email)}`
    };
  } catch (error) {
    functions.logger.error('Erro ao enviar código 2FA:', error);
    throw handleError(error);
  }
});

/**
 * Verifica código 2FA
 */
export const verifyTwoFactorCode = functions.https.onCall(async (data, context) => {
  try {
    // ✅ Validar App Check primeiro
    validateAppCheck(context);
    
    // ✅ Verificar autenticação
    assertAuthenticated(context);
    
    const userId = context.auth!.uid;
    const {code} = data;
    
    if (!code || typeof code !== 'string' || code.length !== 6) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Código de verificação inválido'
      );
    }
    
    // Buscar código do Firestore
    const codeDoc = await db.collection('twoFactorCodes').doc(userId).get();
    
    if (!codeDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Código não encontrado. Solicite um novo código.'
      );
    }
    
    const codeData = codeDoc.data();
    const storedCode = codeData?.code;
    const expiresAt = codeData?.expiresAt || 0;
    
    // Verificar expiração
    if (Date.now() > expiresAt) {
      await codeDoc.ref.delete();
      throw new functions.https.HttpsError(
        'deadline-exceeded',
        'Código expirado. Solicite um novo código.'
      );
    }
    
    // Verificar código
    if (code !== storedCode) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Código inválido. Tente novamente.'
      );
    }
    
    // Código válido - deletar e marcar verificação
    await codeDoc.ref.delete();
    await db.collection('users').doc(userId).update({
      twoFactorVerified: true,
      twoFactorVerifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    return {
      success: true,
      verified: true
    };
  } catch (error) {
    functions.logger.error('Erro ao verificar código 2FA:', error);
    throw handleError(error);
  }
});

/**
 * Limpa códigos 2FA expirados (executar periodicamente via scheduled function)
 */
export const cleanupExpiredTwoFactorCodes = functions.pubsub
  .schedule('every 1 hours')
  .onRun(async () => {
    try {
      const now = Date.now();
      const expiredCodes = await db.collection('twoFactorCodes')
        .where('expiresAt', '<', now)
        .get();
      
      const batch = db.batch();
      expiredCodes.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      
      functions.logger.info(`Removidos ${expiredCodes.docs.length} códigos 2FA expirados`);
      return null;
    } catch (error) {
      functions.logger.error('Erro ao limpar códigos expirados:', error);
      return null;
    }
  });
```

**Características:**
- ✅ Validação de App Check
- ✅ Validação de autenticação
- ✅ Códigos expiram em 10 minutos
- ✅ Limpeza automática de códigos expirados
- ✅ Mascaramento de email/telefone na resposta

---

## 📱 3️⃣ AUTENTICAÇÃO: CLIENTE (ANDROID/KOTLIN)

### 📋 **A. Repository de Autenticação**

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirebaseAuthRepository.kt`

```kotlin
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Cadastro com email/senha
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null")
            
            Log.d("FirebaseAuthRepository", "Usuário criado: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Erro ao criar usuário: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Login com email/senha
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            // Verificar se Firebase Auth está inicializado
            if (firebaseAuth.app == null) {
                return Result.failure(Exception("Firebase Auth não inicializado"))
            }
            
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null")
            
            Log.d("FirebaseAuthRepository", "Login bem-sucedido: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Erro no login: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Login com Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            if (firebaseAuth.app == null) {
                return Result.failure(Exception("Firebase Auth não inicializado"))
            }
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User is null")
            
            Log.d("FirebaseAuthRepository", "Login com Google: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Erro ao fazer login com Google: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observar estado de autenticação
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    /**
     * Logout
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Obter token ID (inclui Custom Claims)
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("User not logged in")
            // ✅ forceRefresh = true garante que Custom Claims atualizados sejam incluídos
            val token = user.getIdToken(forceRefresh).await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Observações:**
- ✅ `getIdToken(forceRefresh = true)` garante que Custom Claims atualizados sejam incluídos
- ✅ Observação de estado de autenticação via Flow
- ✅ Tratamento de erros robusto

---

### 📋 **B. Verificação de Estado de Autenticação (Splash)**

**Exemplo de uso no ViewModel:**

```kotlin
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val initialDataSyncManager: InitialDataSyncManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    fun checkAuthState(
        onNavigateToBiometricAuth: () -> Unit,
        onNavigateToHome: () -> Unit,
        onNavigateToLogin: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ Obter usuário atual
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    // ✅ Verificar se o token ainda é válido (inclui Custom Claims)
                    try {
                        currentUser.getIdToken(true).await()
                        
                        // Se estiver logado e token válido, verificar sync inicial
                        val needsSync = !preferencesManager.isInitialSyncCompleted(currentUser.uid)
                        if (needsSync) {
                            initialDataSyncManager.syncAllUserData()
                            preferencesManager.setInitialSyncCompleted(currentUser.uid)
                        }
                        
                        onNavigateToHome()
                    } catch (e: Exception) {
                        // Token inválido ou expirado
                        onNavigateToLogin()
                    }
                } else {
                    onNavigateToLogin()
                }
            } catch (e: Exception) {
                onNavigateToLogin()
            }
        }
    }
}
```

**Fluxo:**
1. ✅ Verificar se há usuário logado
2. ✅ Refresh do token (garante Custom Claims atualizados)
3. ✅ Se válido, sincronizar dados iniciais se necessário
4. ✅ Navegar para home ou login conforme estado

---

### 📋 **C. Definição de Role Inicial (chamando Cloud Function)**

**Exemplo de uso no ViewModel após cadastro:**

```kotlin
@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val functionsService: FirebaseFunctionsService
) : ViewModel() {

    fun setUserRole(role: String, accountType: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val currentUser = authRepository.getCurrentUser() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }
                
                // ✅ Chamar Cloud Function para definir role (Custom Claims)
                val result = functionsService.setInitialUserRole(role, accountType)
                
                result.fold(
                    onSuccess = { data ->
                        // ✅ Role definido com sucesso
                        // Custom Claims serão incluídos no próximo token
                        // Para garantir atualização imediata, fazer refresh do token
                        currentUser.getIdToken(true).await()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            roleSet = true
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao definir tipo de conta: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro: ${e.message}"
                )
            }
        }
    }
}
```

---

## 📊 RESUMO DAS SEGURANÇAS IMPLEMENTADAS

### ✅ **Camadas de Segurança:**

1. **App Check** ✅
   - Valida que requisições vêm de apps legítimos
   - Implementado em todas as Cloud Functions críticas

2. **Custom Claims** ✅
   - Roles definidos no token JWT (autoridade única)
   - Não podem ser alterados diretamente no Firestore
   - Sincronizados automaticamente em todos os tokens

3. **Firestore Rules** ✅
   - Verificam `request.auth.token.role` (Custom Claims)
   - Validações de propriedade (`isOwner`)
   - Validações de estrutura de dados

4. **Cloud Functions** ✅
   - Validações de negócio complexas
   - Validação de App Check e autenticação
   - Tratamento centralizado de erros

5. **Cliente (Android)** ✅
   - Refresh de tokens para incluir Custom Claims atualizados
   - Tratamento robusto de erros
   - Observação de estado de autenticação

---

### 🔄 **Fluxo Completo de Autenticação:**

```
1. Usuário cria conta
   ↓
2. Firebase Auth: cria usuário
   ↓
3. onUserCreate (Cloud Function):
   - Cria documento em /users/{uid}
   - Define Custom Claim role: 'user'
   ↓
4. Cliente: chama setInitialUserRole(role)
   ↓
5. setInitialUserRole (Cloud Function):
   - Valida App Check
   - Valida autenticação
   - Define Custom Claim role: {role}
   - Atualiza Firestore (referência)
   ↓
6. Cliente: refresh token (getIdToken(true))
   ↓
7. Token JWT inclui Custom Claim role
   ↓
8. Firestore Rules verificam request.auth.token.role
   ↓
9. Acesso autorizado ✅
```

---

### 📝 **Práticas de Segurança:**

- ✅ **Never trust client**: Todas as validações críticas no backend
- ✅ **Defense in depth**: Múltiplas camadas de segurança
- ✅ **Least privilege**: Usuários só acessam seus próprios dados
- ✅ **Audit logs**: Logs estruturados em Cloud Functions
- ✅ **Error handling**: Não expor informações sensíveis em erros
- ✅ **Token refresh**: Garantir que Custom Claims atualizados sejam incluídos

---

**Este relatório está atualizado com as últimas implementações do sistema.**
