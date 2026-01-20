# 🔍 MAPEAMENTO COMPLETO DE FLUXO DE DADOS - TaskGoApp

**Data:** 2024  
**Objetivo:** Identificar onde os dados se perdem entre Backend (Firestore) e App Android

---

## 📋 SUMÁRIO EXECUTIVO

### ✅ O QUE FUNCIONA
- **Services**: Escrita e leitura consistentes
- **Chat/Conversations**: Escrita e leitura via Cloud Functions (funcional)

### ❌ O QUE QUEBRA
- **Products**: Backend escreve em `locations/{city}_{state}/products`, app lê de `products`
- **Stories**: Backend escreve em `locations/{city}_{state}/stories`, app lê de `stories`

### 🎯 CAUSA RAIZ REAL
**Incompatibilidade de Collections**: Backend salva dados em coleções organizadas por localização (`locations/{city}_{state}/products`), mas o app Android lê da coleção global (`products`). O backend **também** salva na coleção global, mas isso pode não estar acontecendo corretamente ou a query do app pode não encontrar os dados.

---

## 🧩 ETAPA 1 — MAPA DE ESCRITA (BACKEND → FIRESTORE)

### 1.1 PRODUCTS

**Arquivo:** `functions/src/products/index.ts`  
**Função:** `createProduct` (linha 15-169)  
**Tipo:** `functions.https.onCall` (httpsCallable)

**Caminhos EXATOS no Firestore:**

1. **PRINCIPAL - Coleção por Localização:**
   ```
   locations/{city}_{state}/products/{productId}
   ```
   - Função: `getLocationCollection(db, 'products', city, state)` (linha 129-134)
   - Exemplo: `locations/osasco_sp/products/abc123`

2. **COMPATIBILIDADE - Coleção Global:**
   ```
   products/{productId}
   ```
   - Direto: `db.collection('products').doc(productId).set(productData)` (linha 139)

3. **PRIVADO - Subcoleção do Usuário:**
   ```
   users/{userId}/products/{productId}
   ```
   - Direto: `db.collection('users').doc(userId).collection('products').doc(productId).set(productData)` (linha 142-147)

**JSON REAL Salvo (linhas 112-126):**

```typescript
{
  "sellerId": "string",
  "title": "string (trimmed)",
  "description": "string (trimmed)",
  "category": "string (trimmed)",
  "price": number,
  "images": Array<string>,
  "stock": number | null,
  "active": boolean (true),
  "status": "active",
  "city": "string",
  "state": "string",
  "createdAt": FieldValue.serverTimestamp(),
  "updatedAt": FieldValue.serverTimestamp()
}
```

**Campos críticos:**
- `sellerId` (não `ownerId` ou `providerId`)
- `active: true` (padrão)
- `status: "active"` (campo adicional)
- `city` e `state` (campos explícitos)
- `createdAt` e `updatedAt` são `Timestamp`

---

### 1.2 SERVICES

**Arquivo:** `functions/src/services/index.ts`  
**Função:** `createService` (linha 14-138)  
**Tipo:** `functions.https.onCall` (httpsCallable)

**Caminhos EXATOS no Firestore:**

1. **PRINCIPAL - Coleção Pública:**
   ```
   services/{serviceId}
   ```
   - Direto: `db.collection(COLLECTIONS.SERVICES).add(serviceData)` (linha 110)
   - `COLLECTIONS.SERVICES = 'services'` (constants.ts)

2. **PRIVADO - Subcoleção do Usuário:**
   ```
   users/{userId}/services/{serviceId}
   ```
   - Direto: `db.collection('users').doc(userId).collection('services').doc(serviceId).set(serviceData)` (linha 114-119)

**JSON REAL Salvo (linhas 96-107):**

```typescript
{
  "providerId": "string",
  "title": "string (trimmed)",
  "description": "string (trimmed)",
  "category": "string (trimmed)",
  "price": number | null,
  "latitude": number | null,
  "longitude": number | null,
  "active": boolean (true),
  "createdAt": FieldValue.serverTimestamp(),
  "updatedAt": FieldValue.serverTimestamp()
}
```

**Campos críticos:**
- `providerId` (não `sellerId`)
- `active: true` (padrão)
- `createdAt` e `updatedAt` são `Timestamp`

---

### 1.3 STORIES

**Arquivo:** `functions/src/stories.ts`  
**Função:** `createStory` (linha 12-153)  
**Tipo:** `functions.https.onCall` (httpsCallable)

**Caminhos EXATOS no Firestore:**

1. **PRINCIPAL - Coleção por Localização:**
   ```
   locations/{city}_{state}/stories/{storyId}
   ```
   - Função: `getLocationCollection(db, 'stories', city, state)` (linha 122-127)
   - Exemplo: `locations/osasco_sp/stories/xyz789`

2. **COMPATIBILIDADE - Coleção Global:**
   ```
   stories/{storyId}
   ```
   - Direto: `db.collection('stories').doc(storyId).set(storyData)` (linha 132)

**JSON REAL Salvo (linhas 105-119):**

```typescript
{
  "userId": "string",
  "userName": "string",
  "userAvatarUrl": "string | null",
  "mediaUrl": "string (trimmed)",
  "mediaType": "string (trimmed)",
  "caption": "string (trimmed)",
  "thumbnailUrl": "string | null",
  "location": {
    "city": "string",
    "state": "string",
    "latitude": number,
    "longitude": number
  } | null,
  "city": "string",  // Campo explícito
  "state": "string", // Campo explícito
  "createdAt": FieldValue.serverTimestamp(),
  "expiresAt": Timestamp (24 horas a partir de agora),
  "viewsCount": 0
}
```

**Campos críticos:**
- `userId` (não `ownerId`)
- `city` e `state` (campos explícitos, além do objeto `location`)
- `createdAt` é `Timestamp`
- `expiresAt` é `Timestamp` (24h)

---

### 1.4 CHAT / CONVERSATIONS

**Arquivo:** `functions/src/ai-chat.ts`  
**Função:** `aiChatProxy` (linha 211-447)  
**Tipo:** `functions.https.onCall` (httpsCallable)

**Caminhos EXATOS no Firestore:**

1. **CONVERSATION DOCUMENT:**
   ```
   conversations/{conversationId}
   ```
   - Criado/atualizado: `db.collection('conversations').doc(conversationId)` (linha 269, 395)

2. **MESSAGES SUBCOLLECTION:**
   ```
   conversations/{conversationId}/messages/{messageId}
   ```
   - Criado: `db.collection('conversations').doc(conversationId).collection('messages').add()` (linha 290, 417)

**JSON REAL Salvo (Conversation - linhas 274-280, 401-407):**

```typescript
{
  "userId": "string",
  "type": "ai",
  "createdAt": FieldValue.serverTimestamp(),
  "updatedAt": FieldValue.serverTimestamp(),
  "lastMessage": "string"
}
```

**JSON REAL Salvo (Message - linhas 290-294, 417-421):**

```typescript
{
  "role": "user" | "assistant",
  "content": "string",
  "timestamp": FieldValue.serverTimestamp()
}
```

---

## 🧩 ETAPA 2 — MAPA DE LEITURA (APP → FIRESTORE)

### 2.1 PRODUCTS

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreProductsRepositoryImpl.kt`

**Ponto de Leitura:** Linha 40-102 (`observeProducts()`)

**QUERY EXATA (linhas 42-44):**

```kotlin
productsCollection
    .whereEqualTo("active", true)
    .orderBy("createdAt", Query.Direction.DESCENDING)
```

**Detalhes:**
- **Collection:** `products` (coleção global - linha 36)
- **Filtros:** `active == true`
- **Ordenação:** `createdAt DESC`
- **Limites:** Nenhum
- **Paginação:** Não
- **Listener:** `addSnapshotListener` (real-time)
- **Tipo:** `callbackFlow`

**⚠️ PROBLEMA IDENTIFICADO:**
- O app **NÃO** usa `observeAllProducts(city, state)` que existe em `FirestoreProductsRepository.kt`
- O repositório injetado é `FirestoreProductsRepositoryImpl` que lê de `products` (global)
- Backend salva PRINCIPALMENTE em `locations/{city}_{state}/products`

**Mapeamento de Campos (linhas 55-88):**
- `id` → `doc.id`
- `sellerId` → `data["sellerId"]`
- `title` → `data["title"]`
- `price` → `data["price"]`
- `images` → `data["imageUrls"]` (⚠️ backend usa `images`, app espera `imageUrls`)
- `active` → `data["active"]`
- `createdAt` → conversão de `Timestamp`/`Long`/`Date` para `Date?`

---

### 2.2 SERVICES

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreServicesRepository.kt`

**Ponto de Leitura:** Linha 69-122 (`observeAllActiveServices()`)

**QUERY EXATA (linhas 72-74):**

```kotlin
publicServicesCollection
    .whereEqualTo("active", true)
    .limit(50)
```

**Detalhes:**
- **Collection:** `services` (coleção pública - linha 22)
- **Filtros:** `active == true`
- **Ordenação:** Nenhuma (depois ordena em memória por `createdAt DESC` - linha 96)
- **Limites:** `50`
- **Paginação:** Não
- **Listener:** `addSnapshotListener` (real-time)
- **Tipo:** `callbackFlow`

**✅ COMPATÍVEL:**
- Backend salva em `services` (coleção pública)
- App lê de `services` (coleção pública)
- Campos são compatíveis

---

### 2.3 STORIES

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreStoriesRepository.kt`

**Ponto de Leitura:** Linha 41-161 (`observeStories()`)

**QUERY EXATA (linhas 57-60):**

```kotlin
storiesCollection
    .whereGreaterThan("expiresAt", timestamp)
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .limit(100)
```

**Detalhes:**
- **Collection:** `stories` (coleção global - linha 34)
- **Filtros:** `expiresAt > now - 24h`
- **Ordenação:** `createdAt DESC`
- **Limites:** `100`
- **Paginação:** Não
- **Listener:** `addSnapshotListener` (real-time)
- **Tipo:** `callbackFlow`

**⚠️ PROBLEMA IDENTIFICADO:**
- O app lê de `stories` (coleção global)
- Backend salva PRINCIPALMENTE em `locations/{city}_{state}/stories`
- Comentário na linha 51-53 diz: "TODO: Implementar observação por localização quando tivermos cidade/estado do usuário"

---

### 2.4 CHAT / CONVERSATIONS

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/feature/chatai/presentation/ChatListViewModel.kt`

**Ponto de Leitura:** Linha 54-86 (`loadChats()`)

**QUERY:**
- Usa Cloud Function `listConversations` via `functionsService.listConversations(limit = 50)` (linha 56)

**Backend (functions/src/ai-chat.ts - linhas 532-557):**
```typescript
db.collection('conversations')
    .where('userId', '==', context.auth!.uid)
    .orderBy('updatedAt', 'desc')
    .limit(50)
    .get()
```

**✅ COMPATÍVEL:**
- App usa Cloud Function (correto)
- Backend filtra por `userId`

---

## 🧩 ETAPA 3 — COMPARAÇÃO DIRETA (ONDE QUEBRA)

### 3.1 PRODUCTS

| Elemento | Escrita (Backend) | Leitura (App) | Compatível |
|----------|------------------|---------------|------------|
| **Collection Principal** | `locations/{city}_{state}/products` | `products` | ❌ |
| **Collection Secundária** | `products` (compatibilidade) | `products` | ✅ |
| **Collection Privada** | `users/{userId}/products` | Não lê | ⚠️ |
| `sellerId` | `sellerId` | `sellerId` | ✅ |
| `active` | `active: true` | `where active == true` | ✅ |
| `status` | `status: "active"` | Não usado | ⚠️ |
| `images` (backend) vs `imageUrls` (app) | `images` | `imageUrls` | ❌ |
| `createdAt` | `Timestamp` | `Timestamp` → `Date?` | ✅ |
| `city` / `state` | Campos explícitos | Não usados na query | ⚠️ |

**PROBLEMA REAL:**
1. Backend salva PRINCIPALMENTE em `locations/{city}_{state}/products` (linha 129-135)
2. App lê APENAS de `products` (coleção global) (linha 36, 42)
3. Backend também salva em `products` (linha 139), mas se esta operação falhar silenciosamente, os dados não aparecem no app
4. Campo `images` (backend) não corresponde a `imageUrls` (app)

---

### 3.2 SERVICES

| Elemento | Escrita (Backend) | Leitura (App) | Compatível |
|----------|------------------|---------------|------------|
| **Collection** | `services` | `services` | ✅ |
| `providerId` | `providerId` | `providerId` | ✅ |
| `active` | `active: true` | `where active == true` | ✅ |
| `createdAt` | `Timestamp` | `Timestamp` → conversão manual | ✅ |

**✅ FUNCIONA:** Tudo compatível.

---

### 3.3 STORIES

| Elemento | Escrita (Backend) | Leitura (App) | Compatível |
|----------|------------------|---------------|------------|
| **Collection Principal** | `locations/{city}_{state}/stories` | `stories` | ❌ |
| **Collection Secundária** | `stories` (compatibilidade) | `stories` | ✅ |
| `userId` | `userId` | `userId` | ✅ |
| `expiresAt` | `Timestamp` | `Timestamp` (comparação) | ✅ |
| `createdAt` | `Timestamp` | `Timestamp` → conversão manual | ✅ |
| `city` / `state` | Campos explícitos | Não usados na query | ⚠️ |

**PROBLEMA REAL:**
1. Backend salva PRINCIPALMENTE em `locations/{city}_{state}/stories` (linha 122-128)
2. App lê APENAS de `stories` (coleção global) (linha 34, 57)
3. Backend também salva em `stories` (linha 132), mas se esta operação falhar silenciosamente, os dados não aparecem no app

---

### 3.4 CHAT / CONVERSATIONS

| Elemento | Escrita (Backend) | Leitura (App) | Compatível |
|----------|------------------|---------------|------------|
| **Collection** | `conversations/{conversationId}` | Via Cloud Function `listConversations` | ✅ |
| `userId` | `userId` | Filtrado pelo backend | ✅ |
| `lastMessage` | `lastMessage` | `lastMessage` | ✅ |
| `updatedAt` | `Timestamp` | `Timestamp` → `Date?` | ✅ |

**✅ FUNCIONA:** Tudo compatível via Cloud Functions.

---

## 🧩 ETAPA 4 — MAPEAMENTO DE MODELS E MAPPERS

### 4.1 PRODUCTS

**Model Kotlin:** `ProductFirestore` (`app/src/main/java/com/taskgoapp/taskgo/data/firestore/models/ProductFirestore.kt`)

```kotlin
data class ProductFirestore(
    val id: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val description: String? = null,
    val sellerId: String = "",
    val sellerName: String? = null,
    val imageUrls: List<String> = emptyList(),  // ⚠️ App espera imageUrls
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
    val featured: Boolean = false,
    val discountPercentage: Double? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val rating: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
```

**Parsing (FirestoreProductsRepositoryImpl.kt - linhas 55-88):**

```kotlin
val data = doc.data
val createdAt = when (val v = data["createdAt"]) {
    is Long -> java.util.Date(v)
    is java.util.Date -> v
    is com.google.firebase.Timestamp -> v.toDate()
    else -> null
}
ProductFirestore(
    id = doc.id,
    title = data["title"] as? String ?: "",
    price = (data["price"] as? Number)?.toDouble() ?: 0.0,
    description = data["description"] as? String,
    sellerId = data["sellerId"] as? String ?: "",
    imageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
    // ...
)
```

**⚠️ PROBLEMA:**
- Backend salva `images` (array)
- App lê `imageUrls` (array)
- **Se o campo `imageUrls` não existir no Firestore, retorna lista vazia** (linha 77: `?: emptyList()`)
- Campos inexistentes viram `null` ou valores padrão (não quebram, mas dados são perdidos)

**Resposta:**
- Campo inexistente (`imageUrls`) → lista vazia
- Campo inexistente (`sellerName`) → `null`
- Campo inexistente (`active`) → `true` (padrão)

---

## 🧩 ETAPA 5 — ESTADO (Repository → ViewModel → UI)

### 5.1 PRODUCTS

**ViewModel:** `ProductsViewModel` (`app/src/main/java/com/taskgoapp/taskgo/feature/products/presentation/ProductsViewModel.kt`)

**Tipo de Estado:**
```kotlin
val allProducts: StateFlow<List<Product>> = productsRepository
    .observeProducts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

**Onde o valor é emitido:**
- `FirestoreProductsRepositoryImpl.observeProducts()` → `callbackFlow { trySend(products) }` (linha 94)
- Se erro: `trySend(emptyList())` (linha 48, 52)

**Onde pode estar sendo engolido:**
- Erro no listener: `trySend(emptyList())` (linha 48)
- Snapshot null: `trySend(emptyList())` (linha 52)
- Exceção no parsing: documento é ignorado (`mapNotNull` retorna `null`)

**Fluxo:**
```
FirestoreProductsRepositoryImpl.observeProducts()
  → callbackFlow { addSnapshotListener { snapshot, error ->
      if (error) trySend(emptyList())  // ❌ Engole erro
      val products = snapshot.documents.mapNotNull { doc ->
        // Se falhar, retorna null (ignorado)
      }
      trySend(products)  // ✅ Emite lista (pode estar vazia)
    }
  }
  → ProductsViewModel.allProducts (StateFlow)
  → UI
```

---

### 5.2 SERVICES

**ViewModel:** `HomeViewModel` (`app/src/main/java/com/taskgoapp/taskgo/feature/home/presentation/HomeViewModel.kt`)

**Tipo de Estado:**
```kotlin
val services: StateFlow<List<ServiceFirestore>> = servicesRepository
    .observeAllActiveServices()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

**Onde pode estar sendo engolido:**
- Erro no listener: `trySend(emptyList())` (linha 79)
- Exceção no parsing: documento é ignorado (linha 93-95)

---

## 🧩 ETAPA 6 — LOGS REAIS (PROVA)

### 6.1 PRODUCTS (Backend)

**Logs existentes (functions/src/products/index.ts - linha 149):**

```typescript
functions.logger.info(`Product created: ${productId}`, {
  productId,
  sellerId: userId,
  category,
  price,
  location: `${city || 'unknown'}, ${state || 'unknown'}`,
  locationCollection: `locations/${normalizeLocationId(city || 'unknown', state || 'unknown')}/products`,
  timestamp: new Date().toISOString(),
});
```

**✅ Log confirma:** Produto é salvo em `locations/{city}_{state}/products`

---

### 6.2 PRODUCTS (App)

**Logs existentes (FirestoreProductsRepositoryImpl.kt):**

```kotlin
android.util.Log.e("FirestoreProductsRepo", "Erro no listener de produtos: ${error.message}", error)
android.util.Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
```

**⚠️ FALTA:**
- Log do tamanho do snapshot (`snapshot.size()`)
- Log dos dados brutos (`doc.data`)
- Log da collection usada

**Logs necessários (para diagnóstico):**

```kotlin
android.util.Log.d("FirestoreProductsRepo", "📦 Snapshot size: ${snapshot.size()}")
android.util.Log.d("FirestoreProductsRepo", "📦 Doc data: ${doc.data}")
android.util.Log.d("FirestoreProductsRepo", "📦 Collection: products")
```

---

## 🧩 ETAPA 7 — FIRESTORE RULES (CRÍTICO)

### 7.1 PRODUCTS

**Rules (firestore.rules - linhas 271-278):**

```javascript
match /products/{productId} {
  // Leitura: Qualquer usuário autenticado pode ler produtos ativos
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  
  // Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}
```

**⚠️ PROBLEMA:**
- Rules permitem `read` em `products` ✅
- Rules **NÃO** cobrem `locations/{city}_{state}/products` ❌
- Regra padrão `match /{document=**}` bloqueia tudo (linha 565-567)

**Regra padrão (linhas 565-567):**

```javascript
match /{document=**} {
  allow read, write: if false;
}
```

**CONCLUSÃO:**
- `locations/{city}_{state}/products` está **BLOQUEADO** pela regra padrão
- Backend salva via Admin SDK (não é afetado por rules)
- App lê via Client SDK → **BLOQUEADO** ❌

---

### 7.2 STORIES

**Rules (firestore.rules - linhas 381-395):**

```javascript
match /stories/{storyId} {
  // Leitura: Qualquer usuário autenticado
  allow read: if isAuthenticated();
  
  // Escrita: BLOQUEADA - usar Cloud Function (createStory)
  allow write: if false;
}
```

**⚠️ PROBLEMA:**
- Rules permitem `read` em `stories` ✅
- Rules **NÃO** cobrem `locations/{city}_{state}/stories` ❌
- Regra padrão bloqueia `locations/...` ❌

---

### 7.3 SERVICES

**Rules (firestore.rules - linhas 284-291):**

```javascript
match /services/{serviceId} {
  // Leitura: Qualquer usuário autenticado pode ler serviços ativos
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  
  // Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}
```

**✅ CORRETO:**
- Rules permitem `read` em `services`
- Collection `services` está coberta

---

## 📊 RELATÓRIO FINAL

### ✅ O QUE FUNCIONA

1. **Services**: Escrita e leitura consistentes (`services` collection)
2. **Chat/Conversations**: Escrita e leitura via Cloud Functions
3. **Firestore Rules**: Cobertura para `services` e `stories` (coleção global)

---

### ❌ O QUE QUEBRA

1. **Products**:
   - Backend escreve em `locations/{city}_{state}/products` (PRINCIPAL)
   - App lê de `products` (coleção global)
   - **Firestore Rules bloqueiam `locations/...`** (regra padrão)
   - Campo `images` (backend) vs `imageUrls` (app)

2. **Stories**:
   - Backend escreve em `locations/{city}_{state}/stories` (PRINCIPAL)
   - App lê de `stories` (coleção global)
   - **Firestore Rules bloqueiam `locations/...`** (regra padrão)

---

### 🎯 CAUSA RAIZ REAL

**DUAS CAUSAS IDENTIFICADAS:**

1. **Incompatibilidade de Collections (PRINCIPAL):**
   - Backend usa coleções por localização (`locations/{city}_{state}/...`)
   - App usa coleções globais (`products`, `stories`)
   - Backend **também** salva na coleção global, mas isso pode falhar silenciosamente

2. **Firestore Rules bloqueando `locations/...` (SECUNDÁRIA):**
   - Rules não têm permissões para `locations/{city}_{state}/products`
   - Rules não têm permissões para `locations/{city}_{state}/stories`
   - Regra padrão `match /{document=**}` bloqueia tudo não especificado

---

### 🛠 CORREÇÃO MÍNIMA NECESSÁRIA

#### OPÇÃO 1: Corrigir Firestore Rules (RECOMENDADO - Rápido)

Adicionar rules para `locations/...` em `firestore.rules`:

```javascript
// Adicionar ANTES da regra padrão (linha ~540)

// ==========================================
// LOCATIONS COLLECTIONS - Por localização
// ==========================================

match /locations/{locationId}/products/{productId} {
  // Leitura: Qualquer usuário autenticado pode ler produtos ativos
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  
  // Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}

match /locations/{locationId}/stories/{storyId} {
  // Leitura: Qualquer usuário autenticado
  allow read: if isAuthenticated();
  
  // Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}
```

#### OPÇÃO 2: Corrigir App para ler de `locations/...` (Alternativa)

Modificar `FirestoreProductsRepositoryImpl` e `FirestoreStoriesRepository` para:
1. Obter `city` e `state` do usuário
2. Usar `LocationHelper.getLocationCollection()` para ler de `locations/{city}_{state}/...`
3. Fallback para coleção global se localização não disponível

#### OPÇÃO 3: Garantir que backend salva na coleção global (Temporária)

Garantir que a operação na linha 139 (`db.collection('products').doc(productId).set(productData)`) sempre execute com sucesso e adicione logs de erro.

---

### 📝 NOTAS ADICIONAIS

1. **Campo `images` vs `imageUrls`:**
   - Backend salva `images`
   - App lê `imageUrls`
   - Se o backend não salvar `imageUrls`, o app recebe lista vazia (não quebra, mas perde dados)

2. **Logs de diagnóstico:**
   - Adicionar logs no app para confirmar se snapshot está vazio
   - Adicionar logs no backend para confirmar salvamento na coleção global

3. **Verificação imediata:**
   - Verificar Firestore Console: existem dados em `products` e `stories` (coleção global)?
   - Se SIM: problema é de rules ou query
   - Se NÃO: backend não está salvando na coleção global corretamente

---

## 🔧 PRÓXIMOS PASSOS

1. ✅ Adicionar Firestore Rules para `locations/...`
2. ✅ Verificar logs do backend (produtos salvos em `products` global?)
3. ✅ Adicionar logs no app (tamanho do snapshot, collection usada)
4. ⚠️ Decidir estratégia: manter coleções por localização OU migrar para globais
5. ⚠️ Corrigir campo `images` vs `imageUrls` (padronizar)

---

**FIM DO RELATÓRIO**
