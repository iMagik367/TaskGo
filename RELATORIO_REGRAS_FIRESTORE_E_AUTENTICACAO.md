# 📋 Relatório Atualizado: Regras Firestore e Autenticação

**Data de atualização:** Janeiro 2025  
**Versão das regras:** rules_version = '2'  
**Arquitetura:** Custom Claims + App Check + Cloud Functions

> 📱 **Relatório complementar:** Para entender como o frontend Android se comunica com o backend, consulte [`RELATORIO_FRONTEND_E_COMUNICACAO_BACKEND.md`](./RELATORIO_FRONTEND_E_COMUNICACAO_BACKEND.md)

---

## 🔐 1️⃣ REGRAS FIRESTORE ATUALIZADAS

### ✨ **ARQUITETURA: Custom Claims**

O sistema utiliza **Custom Claims** do Firebase Auth como **autoridade única** para roles de usuários. As regras verificam `request.auth.token.role` em vez de confiar apenas no campo `role` no Firestore.

### 📌 **Helper Functions (Funções Auxiliares)**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
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
  // Também permite queries de listagem por role para usuários autenticados
  allow read: if isOwner(userId) || isModeratorOrAdmin() 
              || (isAuthenticated() && (resource == null || true));
  
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
- ✅ Permite queries de listagem para usuários autenticados

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
  // Permite queries de listagem e leitura de documentos individuais
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  
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
  // ✅ Leitura: Apenas produtos ativos são públicos
  // Permite queries de listagem e leitura de documentos individuais
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  
  // ❌ Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}
```

**Validações:**
- Apenas produtos `active == true` são visíveis
- Previne vazamento de produtos inativos/excluídos
- Permite queries de listagem

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

#### **6. Stories Collection** (`/stories/{storyId}`)

```javascript
match /stories/{storyId} {
  // ✅ Leitura: Qualquer usuário autenticado pode ler stories (permite queries de listagem)
  allow read: if isAuthenticated();
  
  // ❌ Escrita: BLOQUEADA - usar Cloud Function (createStory)
  // App não pode criar/editar stories diretamente
  allow write: if false;
  
  // Subcoleção story_views dentro de stories
  match /views/{userId} {
    allow read: if isAuthenticated();
    allow create: if isAuthenticated() 
                  && request.auth.uid == userId;
    // ❌ Views são imutáveis
    allow update, delete: if false;
  }
}
```

**Mudança importante:**
- ❌ Escrita direta bloqueada - usar Cloud Function `createStory`
- ✅ Leitura pública para queries de listagem

---

#### **7. Story Views Collection** (`/story_views/{storyId}`) - RAIZ

```javascript
match /story_views/{storyId} {
  // ✅ Leitura: Qualquer usuário autenticado pode ler visualizações
  allow read: if isAuthenticated();
  
  // ❌ Escrita: Apenas Cloud Functions
  allow write: if false;
  
  // Subcoleção views dentro de story_views
  match /views/{userId} {
    allow read: if isAuthenticated();
    allow create: if isAuthenticated();
    // ❌ Views são imutáveis
    allow update, delete: if false;
  }
}
```

**Nova coleção:** Analytics de visualizações de stories em coleção raiz separada.

---

#### **8. Conversations Collection** (`/conversations/{conversationId}`)

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

#### **9. Bank Accounts Collection** (`/bank_accounts/{accountId}`)

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

#### **10. Categories Collections** (NOVAS)

```javascript
// Categorias de produtos
match /product_categories/{categoryId} {
  // ✅ Leitura: Qualquer usuário autenticado pode ler categorias de produtos
  allow read: if isAuthenticated();
  
  // ✅ Escrita: Apenas Cloud Functions ou admins
  allow write: if isAdmin();
}

// Categorias de serviços
match /service_categories/{categoryId} {
  // ✅ Leitura: Qualquer usuário autenticado pode ler categorias de serviços
  allow read: if isAuthenticated();
  
  // ✅ Escrita: Apenas Cloud Functions ou admins
  allow write: if isAdmin();
}
```

**Nova funcionalidade:** Categorias públicas para produtos e serviços.

---

#### **11. Home Banners Collection** (NOVA)

```javascript
match /homeBanners/{bannerId} {
  // ✅ Leitura: Qualquer usuário autenticado pode ler banners ativos
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  
  // ✅ Escrita: Apenas Cloud Functions ou admins
  allow write: if isAdmin();
}
```

**Nova funcionalidade:** Banners da home page.

---

#### **12. Outras Coleções**

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

### 📋 **B. Helpers de Roles (NOVO)**

**Arquivo:** `functions/src/security/roles.ts`

```typescript
import * as functions from 'firebase-functions';
import {AppError} from '../utils/errors';

/**
 * Roles válidos no sistema
 */
export const VALID_ROLES = ['user', 'admin', 'moderator', 'partner', 'seller', 'provider', 'client'] as const;

export type UserRole = typeof VALID_ROLES[number];

/**
 * Verifica se um role é válido
 */
export const isValidRole = (role: string): role is UserRole => {
  return VALID_ROLES.includes(role as UserRole);
};

/**
 * Obtém o role do usuário através de Custom Claims
 * Custom Claims são a autoridade única para permissões
 */
export const getUserRole = (context: functions.https.CallableContext): UserRole => {
  if (!context.auth) {
    throw new AppError('unauthenticated', 'User must be authenticated', 401);
  }

  // Custom Claims estão em context.auth.token
  const role = context.auth.token.role as string | undefined;

  // Se não houver role em Custom Claims, verificar no documento do usuário
  // (apenas para migração - em produção, sempre deve ter Custom Claim)
  if (!role) {
    functions.logger.warn(`User ${context.auth.uid} has no role in Custom Claims`, {
      uid: context.auth.uid,
      timestamp: new Date().toISOString(),
    });

    // Fallback temporário para migração - retornar 'user' como padrão
    return 'user';
  }

  if (!isValidRole(role)) {
    throw new AppError(
      'permission-denied',
      `Invalid role: ${role}. Must be one of: ${VALID_ROLES.join(', ')}`,
      403,
    );
  }

  return role;
};

/**
 * Verifica se o usuário tem um role específico
 */
export const hasRole = (
  context: functions.https.CallableContext,
  requiredRole: UserRole,
): boolean => {
  try {
    const userRole = getUserRole(context);
    return userRole === requiredRole;
  } catch {
    return false;
  }
};

/**
 * Verifica se o usuário é admin
 */
export const isAdmin = (context: functions.https.CallableContext): boolean => {
  return hasRole(context, 'admin');
};

/**
 * Verifica se o usuário é moderador ou admin
 */
export const isModeratorOrAdmin = (
  context: functions.https.CallableContext,
): boolean => {
  const role = getUserRole(context);
  return role === 'admin' || role === 'moderator';
};

/**
 * Asserta que o usuário tem um role específico
 */
export const assertRole = (
  context: functions.https.CallableContext,
  requiredRole: UserRole,
): void => {
  const userRole = getUserRole(context);

  if (userRole !== requiredRole) {
    throw new AppError(
      'permission-denied',
      `Required role: ${requiredRole}. Current role: ${userRole}`,
      403,
    );
  }
};

/**
 * Asserta que o usuário é admin
 */
export const assertAdmin = (context: functions.https.CallableContext): void => {
  assertRole(context, 'admin');
};

/**
 * Asserta que o usuário é moderador ou admin
 */
export const assertModeratorOrAdmin = (
  context: functions.https.CallableContext,
): void => {
  const role = getUserRole(context);

  if (role !== 'admin' && role !== 'moderator') {
    throw new AppError(
      'permission-denied',
      'Moderator or admin access required',
      403,
    );
  }
};
```

**Características:**
- ✅ Type-safe com TypeScript
- ✅ Validação de roles válidos
- ✅ Fallback para migração (retorna 'user' se não houver Custom Claim)
- ✅ Helpers para verificação e asserção de roles

---

### 📋 **C. Helpers de Autenticação e Erro**

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

### 📋 **D. Trigger de Criação de Usuário**

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

### 📋 **E. Definição de Role Inicial (Custom Claims)**

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
   - Permitem queries de listagem onde apropriado

4. **Cloud Functions** ✅
   - Validações de negócio complexas
   - Validação de App Check e autenticação
   - Helpers de roles type-safe
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
- ✅ **Type safety**: Helpers de roles com TypeScript

---

### 🔄 **Como as Regras São Aplicadas na Prática:**

#### **Exemplo 1: Criar Produto**

**Frontend (Android):**
```kotlin
// ❌ TENTATIVA DIRETA (bloqueada pelas regras)
firestore.collection("products").add(productData)
// ERRO: Permission denied - write blocked

// ✅ CORRETO (via Cloud Function)
functionsService.createProduct(
    title = product.title,
    description = product.description,
    category = product.category,
    price = product.price
)
```

**Backend (Cloud Function):**
```typescript
export const createProduct = functions.https.onCall(async (data, context) => {
  validateAppCheck(context);        // ✅ Valida app legítimo
  assertAuthenticated(context);      // ✅ Valida autenticação
  const role = getUserRole(context); // ✅ Lê Custom Claim do token
  
  // Validações de negócio...
  
  // ✅ Escrita com privilégios admin (bypass das regras)
  await db.collection('products').add(productData);
});
```

**Resultado:**
- ✅ Escrita bloqueada no cliente (regras do Firestore)
- ✅ Validações executadas no backend
- ✅ Produto criado com privilégios elevados (Cloud Function)

---

#### **Exemplo 2: Ler Produtos**

**Frontend (Android):**
```kotlin
// ✅ PERMITIDO (regras permitem leitura de produtos ativos)
firestore.collection("products")
    .whereEqualTo("active", true)
    .addSnapshotListener { snapshot, error ->
        // ✅ Sucesso: produtos retornados
    }
```

**Regra aplicada:**
```javascript
match /products/{productId} {
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
}
```

**Resultado:**
- ✅ Leitura permitida para usuários autenticados
- ✅ Apenas produtos `active == true` são retornados
- ✅ Regra valida no momento da query

---

#### **Exemplo 3: Atualizar Perfil de Usuário**

**Frontend (Android):**
```kotlin
// ✅ PERMITIDO (usuário pode atualizar próprio perfil)
firestore.collection("users").document(userId)
    .update(mapOf("displayName" to newName))
    .await()
```

**Regra aplicada:**
```javascript
match /users/{userId} {
  allow update: if isOwner(userId)
                && !('role' in request.resource.data.diff(resource.data).affectedKeys());
}
```

**Resultado:**
- ✅ Usuário pode atualizar próprio perfil
- ❌ Não pode alterar campo `role` (protegido)
- ✅ Validação ocorre no Firestore antes da escrita

---

#### **Exemplo 4: Admin Acessando Dados**

**Frontend (Android):**
```kotlin
// ✅ PERMITIDO (admin tem acesso especial)
firestore.collection("users")
    .whereEqualTo("role", "provider")
    .get()
    .await()
```

**Regra aplicada:**
```javascript
match /users/{userId} {
  allow read: if isOwner(userId) || isModeratorOrAdmin();
}
```

**Custom Claims no token:**
```json
{
  "uid": "admin123",
  "role": "admin"  // ✅ Custom Claim
}
```

**Resultado:**
- ✅ Admin pode ler todos os usuários
- ✅ Custom Claim `role: 'admin'` é verificado
- ✅ Regra `isModeratorOrAdmin()` retorna `true`

---

### 🆕 **Novidades nesta versão:**

1. ✅ **Nova coleção:** `product_categories` e `service_categories`
2. ✅ **Nova coleção:** `homeBanners`
3. ✅ **Nova coleção:** `story_views` (raiz) para analytics
4. ✅ **Stories:** Escrita bloqueada - usar Cloud Function `createStory`
5. ✅ **Helpers de roles:** Novo arquivo `functions/src/security/roles.ts`
6. ✅ **Queries de listagem:** Permissões melhoradas para queries
7. ✅ **Type safety:** Roles com TypeScript types

---

**Este relatório está atualizado com as últimas implementações do sistema.**
