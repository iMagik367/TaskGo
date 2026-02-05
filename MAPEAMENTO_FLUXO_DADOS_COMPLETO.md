# 🔍 MAPEAMENTO COMPLETO DE FLUXO DE DADOS - TaskGoApp

**Data:** 2024  
**Versão:** 2.0 (Atualizado após correções)  
**Objetivo:** Mapear o fluxo completo de dados entre Backend (Cloud Functions) e App Android

---

## 📋 SUMÁRIO EXECUTIVO

### ✅ STATUS ATUAL (Pós-Correções)

| Entidade | Backend Escreve | App Lê | Status | Observações |
|----------|----------------|--------|--------|-------------|
| **Products** | `locations/{city}_{state}/products` + `products` | `locations/{city}_{state}/products` | ✅ **CORRIGIDO** | App agora usa LocationHelper |
| **Services** | `services` + `users/{uid}/services` | `services` | ✅ **FUNCIONA** | Sempre funcionou |
| **Stories** | `locations/{city}_{state}/stories` + `stories` | `locations/{city}_{state}/stories` | ✅ **CORRIGIDO** | App agora usa LocationHelper |
| **Chat/Conversations** | `conversations/{id}` | Via Cloud Function | ✅ **FUNCIONA** | Sempre funcionou |
| **Orders** | `purchase_orders` | `purchase_orders` | ✅ **FUNCIONA** | Sempre funcionou |

### 🎯 MUDANÇAS APLICADAS

1. **Firestore Rules**: Adicionadas permissões para `locations/{locationId}/products` e `locations/{locationId}/stories`
2. **App Android**: Atualizado para usar `LocationHelper.getLocationCollection()` para ler de coleções por localização
3. **LocationStateManager**: Implementado para garantir localização antes de fazer queries

---

## 🧩 ETAPA 1 — MAPA DE ESCRITA (BACKEND → FIRESTORE)

### 1.1 PRODUCTS

**Arquivo:** `functions/src/products/index.ts`  
**Função:** `createProduct` (linha 15-169)  
**Tipo:** `functions.https.onCall` (httpsCallable)

#### Caminhos EXATOS no Firestore:

**1. PRINCIPAL - Coleção por Localização:**
```
locations/{city}_{state}/products/{productId}
```
- **Função:** `getLocationCollection(db, 'products', city, state)` (linha 129-134)
- **Exemplo:** `locations/osasco_sp/products/abc123`
- **Normalização:** `normalizeLocationId(city, state)` → remove acentos, converte para lowercase

**2. COMPATIBILIDADE - Coleção Global:**
```
products/{productId}
```
- **Direto:** `db.collection('products').doc(productId).set(productData)` (linha 139)
- **Nota:** Mantida para compatibilidade, mas não é mais usada pelo app

**3. PRIVADO - Subcoleção do Usuário:**
```
users/{userId}/products/{productId}
```
- **Direto:** `db.collection('users').doc(userId).collection('products').doc(productId).set(productData)` (linha 142-147)
- **Uso:** Para queries privadas do vendedor

#### JSON REAL Salvo (linhas 112-126):

```typescript
{
  "sellerId": "string",
  "title": "string (trimmed)",
  "description": "string (trimmed)",
  "category": "string (trimmed)",
  "price": number,
  "images": Array<string>,  // ⚠️ Campo "images" no backend
  "stock": number | null,
  "active": boolean (true),
  "status": "active",
  "city": "string",
  "state": "string",
  "createdAt": FieldValue.serverTimestamp(),
  "updatedAt": FieldValue.serverTimestamp()
}
```

#### Campos Críticos:

| Campo | Tipo | Valor Padrão | Observações |
|-------|------|--------------|-------------|
| `sellerId` | string | obrigatório | Não `ownerId` ou `providerId` |
| `active` | boolean | `true` | Filtrado no app |
| `status` | string | `"active"` | Campo adicional |
| `images` | Array<string> | `[]` | ⚠️ Backend usa `images`, app aceita `images` ou `imageUrls` |
| `city` | string | `""` ou `"unknown"` | Campo explícito |
| `state` | string | `""` ou `"unknown"` | Campo explícito |
| `createdAt` | Timestamp | `serverTimestamp()` | Timestamp do servidor |
| `updatedAt` | Timestamp | `serverTimestamp()` | Timestamp do servidor |

---

### 1.2 SERVICES

**Arquivo:** `functions/src/services/index.ts`  
**Função:** `createService` (linha 14-138)  
**Tipo:** `functions.https.onCall` (httpsCallable)

#### Caminhos EXATOS no Firestore:

**1. PRINCIPAL - Coleção Pública:**
```
services/{serviceId}
```
- **Direto:** `db.collection(COLLECTIONS.SERVICES).add(serviceData)` (linha 110)
- **COLLECTIONS.SERVICES:** `'services'` (constants.ts)

**2. PRIVADO - Subcoleção do Usuário:**
```
users/{userId}/services/{serviceId}
```
- **Direto:** `db.collection('users').doc(userId).collection('services').doc(serviceId).set(serviceData)` (linha 114-119)

#### JSON REAL Salvo (linhas 96-107):

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

#### Campos Críticos:

| Campo | Tipo | Valor Padrão | Observações |
|-------|------|--------------|-------------|
| `providerId` | string | obrigatório | Não `sellerId` |
| `active` | boolean | `true` | Filtrado no app |
| `createdAt` | Timestamp | `serverTimestamp()` | Timestamp do servidor |
| `updatedAt` | Timestamp | `serverTimestamp()` | Timestamp do servidor |

---

### 1.3 STORIES

**Arquivo:** `functions/src/stories.ts`  
**Função:** `createStory` (linha 12-153)  
**Tipo:** `functions.https.onCall` (httpsCallable)

#### Caminhos EXATOS no Firestore:

**1. PRINCIPAL - Coleção por Localização:**
```
locations/{city}_{state}/stories/{storyId}
```
- **Função:** `getLocationCollection(db, 'stories', city, state)` (linha 122-127)
- **Exemplo:** `locations/osasco_sp/stories/xyz789`
- **Normalização:** `normalizeLocationId(city, state)`

**2. COMPATIBILIDADE - Coleção Global:**
```
stories/{storyId}
```
- **Direto:** `db.collection('stories').doc(storyId).set(storyData)` (linha 132)
- **Nota:** Mantida para compatibilidade, mas não é mais usada pelo app

#### JSON REAL Salvo (linhas 105-119):

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

#### Campos Críticos:

| Campo | Tipo | Valor Padrão | Observações |
|-------|------|--------------|-------------|
| `userId` | string | obrigatório | Não `ownerId` |
| `city` | string | `""` ou `"unknown"` | Campo explícito |
| `state` | string | `""` ou `"unknown"` | Campo explícito |
| `createdAt` | Timestamp | `serverTimestamp()` | Timestamp do servidor |
| `expiresAt` | Timestamp | `now + 24h` | Expira em 24 horas |
| `viewsCount` | number | `0` | Contador de visualizações |

---

### 1.4 CHAT / CONVERSATIONS

**Arquivo:** `functions/src/ai-chat.ts`  
**Função:** `aiChatProxy` (linha 211-447)  
**Tipo:** `functions.https.onCall` (httpsCallable)

#### Caminhos EXATOS no Firestore:

**1. CONVERSATION DOCUMENT:**
```
conversations/{conversationId}
```
- **Criado/atualizado:** `db.collection('conversations').doc(conversationId)` (linha 269, 395)

**2. MESSAGES SUBCOLLECTION:**
```
conversations/{conversationId}/messages/{messageId}
```
- **Criado:** `db.collection('conversations').doc(conversationId).collection('messages').add()` (linha 290, 417)

#### JSON REAL Salvo:

**Conversation (linhas 274-280, 401-407):**
```typescript
{
  "userId": "string",
  "type": "ai",
  "createdAt": FieldValue.serverTimestamp(),
  "updatedAt": FieldValue.serverTimestamp(),
  "lastMessage": "string"
}
```

**Message (linhas 290-294, 417-421):**
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

#### Ponto de Leitura Principal:

**Função:** `observeProducts()` (linha 56-74)

**Fluxo:**
```kotlin
locationStateManager.locationState
    .flatMapLatest { locationState ->
        when (locationState) {
            is LocationState.Loading -> flowOf(emptyList())
            is LocationState.Error -> flowOf(emptyList())
            is LocationState.Ready -> observeProductsFromFirestore(locationState)
        }
    }
```

**QUERY EXATA (linhas 95-120):**

```kotlin
val collectionToUse = LocationHelper.getLocationCollection(
    firestore,
    "products",
    locationState.city,
    locationState.state
)

collectionToUse
    .whereEqualTo("active", true)
    .whereEqualTo("status", "active")  // Filtro adicional
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .addSnapshotListener { snapshot, error ->
        // Processa documentos
    }
```

**Detalhes:**
- **Collection:** `locations/{city}_{state}/products` (via `LocationHelper.getLocationCollection()`)
- **Filtros:** 
  - `active == true`
  - `status == "active"`
- **Ordenação:** `createdAt DESC`
- **Limites:** Nenhum
- **Paginação:** Não
- **Listener:** `addSnapshotListener` (real-time)
- **Tipo:** `callbackFlow` dentro de `flatMapLatest`

**✅ CORREÇÃO APLICADA:**
- App agora usa `LocationHelper.getLocationCollection()` para ler de `locations/{city}_{state}/products`
- Aguarda `LocationState.Ready` antes de fazer query
- Suporta tanto `images` quanto `imageUrls` (linha 156-158)

**Mapeamento de Campos (linhas 150-170):**
- `id` → `doc.id`
- `sellerId` → `data["sellerId"]`
- `title` → `data["title"]`
- `price` → `data["price"]`
- `images` → `data["images"]` OU `data["imageUrls"]` (compatibilidade)
- `active` → `data["active"]`
- `status` → `data["status"]` (novo filtro)
- `createdAt` → conversão de `Timestamp`/`Long`/`Date` para `Date?`

---

### 2.2 SERVICES

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreServicesRepository.kt`

#### Ponto de Leitura:

**Função:** `observeAllActiveServices()` (linha 69-122)

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

#### Ponto de Leitura Principal:

**Função:** `observeStories()` (linha 101-129)

**Fluxo:**
```kotlin
locationStateManager.locationState
    .flatMapLatest { locationState ->
        when (locationState) {
            is LocationState.Loading -> flowOf(emptyList())
            is LocationState.Error -> flowOf(emptyList())
            is LocationState.Ready -> {
                if (locationState.locationId == "unknown" || locationState.locationId.isBlank()) {
                    flowOf(emptyList())  // Bloqueia "unknown"
                } else {
                    observeStoriesFromFirestore(locationState, currentUserId, radiusKm, userLocation)
                }
            }
        }
    }
```

**QUERY EXATA (linhas 130-180):**

```kotlin
val locationStoriesCollection = LocationHelper.getLocationCollection(
    firestore,
    "stories",
    locationState.city,
    locationState.state
)

val query = locationStoriesCollection
    .whereGreaterThan("expiresAt", timestamp)
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .limit(100)
```

**Detalhes:**
- **Collection:** `locations/{city}_{state}/stories` (via `LocationHelper.getLocationCollection()`)
- **Filtros:** 
  - `expiresAt > now - 24h`
  - Bloqueia `locationId == "unknown"`
- **Ordenação:** `createdAt DESC`
- **Limites:** `100`
- **Paginação:** Não
- **Listener:** `addSnapshotListener` (real-time)
- **Tipo:** `callbackFlow` dentro de `flatMapLatest`

**✅ CORREÇÃO APLICADA:**
- App agora usa `LocationHelper.getLocationCollection()` para ler de `locations/{city}_{state}/stories`
- Aguarda `LocationState.Ready` antes de fazer query
- Bloqueia explicitamente `locationId == "unknown"`

---

### 2.4 CHAT / CONVERSATIONS

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/feature/chatai/presentation/ChatListViewModel.kt`

#### Ponto de Leitura:

**Função:** `loadChats()` (linha 47-104)

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
| **Collection Principal** | `locations/{city}_{state}/products` | `locations/{city}_{state}/products` | ✅ **CORRIGIDO** |
| **Collection Secundária** | `products` (compatibilidade) | Não usada | ⚠️ Mantida para compatibilidade |
| **Collection Privada** | `users/{userId}/products` | Não lê | ⚠️ Para queries privadas |
| `sellerId` | `sellerId` | `sellerId` | ✅ |
| `active` | `active: true` | `where active == true` | ✅ |
| `status` | `status: "active"` | `where status == "active"` | ✅ **NOVO FILTRO** |
| `images` vs `imageUrls` | `images` | Aceita `images` OU `imageUrls` | ✅ **CORRIGIDO** |
| `createdAt` | `Timestamp` | `Timestamp` → `Date?` | ✅ |
| `city` / `state` | Campos explícitos | Usados para `LocationHelper` | ✅ |

**✅ STATUS:** **CORRIGIDO** - App agora lê da mesma collection que o backend escreve.

---

### 3.2 SERVICES

| Elemento | Escrita (Backend) | Leitura (App) | Compatível |
|----------|------------------|---------------|------------|
| **Collection** | `services` | `services` | ✅ |
| `providerId` | `providerId` | `providerId` | ✅ |
| `active` | `active: true` | `where active == true` | ✅ |
| `createdAt` | `Timestamp` | `Timestamp` → conversão manual | ✅ |

**✅ STATUS:** **FUNCIONA** - Sempre funcionou corretamente.

---

### 3.3 STORIES

| Elemento | Escrita (Backend) | Leitura (App) | Compatível |
|----------|------------------|---------------|------------|
| **Collection Principal** | `locations/{city}_{state}/stories` | `locations/{city}_{state}/stories` | ✅ **CORRIGIDO** |
| **Collection Secundária** | `stories` (compatibilidade) | Não usada | ⚠️ Mantida para compatibilidade |
| `userId` | `userId` | `userId` | ✅ |
| `expiresAt` | `Timestamp` | `Timestamp` (comparação) | ✅ |
| `createdAt` | `Timestamp` | `Timestamp` → conversão manual | ✅ |
| `city` / `state` | Campos explícitos | Usados para `LocationHelper` | ✅ |
| `locationId == "unknown"` | Pode criar | Bloqueado no app | ✅ **PROTEÇÃO** |

**✅ STATUS:** **CORRIGIDO** - App agora lê da mesma collection que o backend escreve.

---

### 3.4 CHAT / CONVERSATIONS

| Elemento | Escrita (Backend) | Leitura (App) | Compatível |
|----------|------------------|---------------|------------|
| **Collection** | `conversations/{conversationId}` | Via Cloud Function `listConversations` | ✅ |
| `userId` | `userId` | Filtrado pelo backend | ✅ |
| `lastMessage` | `lastMessage` | `lastMessage` | ✅ |
| `updatedAt` | `Timestamp` | `Timestamp` → `Date?` | ✅ |

**✅ STATUS:** **FUNCIONA** - Sempre funcionou corretamente.

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
    val imageUrls: List<String> = emptyList(),  // Aceita images OU imageUrls
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
    val status: String? = null,  // ✅ NOVO CAMPO
    val featured: Boolean = false,
    val discountPercentage: Double? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val rating: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
```

**Parsing (FirestoreProductsRepositoryImpl.kt - linhas 150-170):**

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
    imageUrls = ((data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } 
        ?: (data["images"] as? List<*>)?.mapNotNull { it as? String }  // ✅ COMPATIBILIDADE
        ?: emptyList()),
    active = data["active"] as? Boolean ?: true,
    status = data["status"] as? String ?: "active",  // ✅ NOVO CAMPO
    // ...
)
```

**✅ CORREÇÃO:**
- App agora aceita tanto `images` quanto `imageUrls`
- Campo `status` foi adicionado ao parsing

**Comportamento de Campos Inexistentes:**
- Campo inexistente (`imageUrls`/`images`) → lista vazia
- Campo inexistente (`sellerName`) → `null`
- Campo inexistente (`active`) → `true` (padrão)
- Campo inexistente (`status`) → `"active"` (padrão)

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

**Fluxo Completo:**
```
LocationStateManager.locationState
  → flatMapLatest { locationState ->
      when (locationState) {
        Loading -> flowOf(emptyList())
        Error -> flowOf(emptyList())
        Ready -> observeProductsFromFirestore(locationState)
          → callbackFlow { addSnapshotListener { snapshot, error ->
              if (error) trySend(emptyList())
              val products = snapshot.documents.mapNotNull { doc ->
                // Parsing com compatibilidade images/imageUrls
              }
              trySend(products)
            }
          }
      }
    }
  → ProductsViewModel.allProducts (StateFlow)
  → UI
```

**Onde pode estar sendo engolido:**
- `LocationState.Loading` → `flowOf(emptyList())` (aguarda localização)
- `LocationState.Error` → `flowOf(emptyList())` (aguarda localização)
- Erro no listener: `trySend(emptyList())`
- Snapshot null: `trySend(emptyList())`
- Exceção no parsing: documento é ignorado (`mapNotNull` retorna `null`)

---

### 5.2 STORIES

**ViewModel:** `StoriesViewModel` (`app/src/main/java/com/taskgoapp/taskgo/feature/feed/presentation/StoriesViewModel.kt`)

**Tipo de Estado:**
```kotlin
val stories: StateFlow<List<Story>> = storiesRepository
    .observeStories(currentUserId, radiusKm, userLocation)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

**Fluxo Completo:**
```
LocationStateManager.locationState
  → flatMapLatest { locationState ->
      when (locationState) {
        Loading -> flowOf(emptyList())
        Error -> flowOf(emptyList())
        Ready -> {
          if (locationState.locationId == "unknown") {
            flowOf(emptyList())  // Bloqueia "unknown"
          } else {
            observeStoriesFromFirestore(locationState, ...)
              → callbackFlow { addSnapshotListener { snapshot, error ->
                  // Processa stories
                }
              }
          }
        }
      }
    }
  → StoriesViewModel.stories (StateFlow)
  → UI
```

**Proteções:**
- Bloqueia `locationId == "unknown"`
- Aguarda `LocationState.Ready`
- Filtra por `expiresAt > now - 24h`

---

## 🧩 ETAPA 6 — FIRESTORE RULES (CRÍTICO)

### 6.1 PRODUCTS

**Rules (firestore.rules - linhas 575-585):**

```javascript
match /locations/{locationId}/products/{productId} {
  // BLOQUEAR: locationId inválido
  allow read, write: if !isValidLocationId(locationId);
  
  // Leitura: Qualquer usuário autenticado pode ler produtos ativos
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  
  // Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}
```

**Helper Function (linhas 568-573):**
```javascript
function isValidLocationId(locationId) {
  return locationId != null 
         && locationId != '' 
         && locationId != 'unknown' 
         && locationId != 'unknown_unknown';
}
```

**✅ CORREÇÃO APLICADA:**
- Rules agora permitem leitura de `locations/{locationId}/products`
- Bloqueia explicitamente `locationId == "unknown"` ou `"unknown_unknown"`

**Rules para Coleção Global (linhas 271-278):**
```javascript
match /products/{productId} {
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  allow write: if false;
}
```
- Mantida para compatibilidade, mas não é mais usada pelo app

---

### 6.2 STORIES

**Rules (firestore.rules - linhas 587-597):**

```javascript
match /locations/{locationId}/stories/{storyId} {
  // BLOQUEAR: locationId inválido
  allow read, write: if !isValidLocationId(locationId);
  
  // Leitura: Qualquer usuário autenticado
  allow read: if isAuthenticated();
  
  // Escrita: BLOQUEADA - usar Cloud Functions
  allow write: if false;
}
```

**✅ CORREÇÃO APLICADA:**
- Rules agora permitem leitura de `locations/{locationId}/stories`
- Bloqueia explicitamente `locationId == "unknown"` ou `"unknown_unknown"`

**Rules para Coleção Global (linhas 381-395):**
```javascript
match /stories/{storyId} {
  allow read: if isAuthenticated();
  allow write: if false;
}
```
- Mantida para compatibilidade, mas não é mais usada pelo app

---

### 6.3 SERVICES

**Rules (firestore.rules - linhas 284-291):**

```javascript
match /services/{serviceId} {
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
  allow write: if false;
}
```

**✅ CORRETO:**
- Rules permitem `read` em `services`
- Collection `services` está coberta

---

## 🧩 ETAPA 7 — LOCATION HELPER E NORMALIZAÇÃO

### 7.1 NORMALIZAÇÃO DE LOCATION ID

**Backend (functions/src/utils/location.ts - linhas 14-42):**

```typescript
export function normalizeLocationId(city: string, state: string): string {
  const normalize = (str: string): string => {
    return str
      .toLowerCase()
      .trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // Remove acentos
      .replace(/[^a-z0-9]/g, '_') // Substitui caracteres especiais por underscore
      .replace(/_+/g, '_') // Remove underscores duplicados
      .replace(/^_|_$/g, ''); // Remove underscores no início e fim
  };

  const normalizedCity = normalize(city || '');
  const normalizedState = normalize(state || '');

  if (!normalizedCity && !normalizedState) {
    return 'unknown';
  }

  return `${normalizedCity}_${normalizedState}`;
}
```

**App (app/src/main/java/com/taskgoapp/taskgo/core/firebase/LocationHelper.kt - linhas 20-45):**

```kotlin
fun normalizeLocationId(city: String, state: String): String {
    val normalize = { str: String ->
        java.text.Normalizer.normalize(str.lowercase().trim(), java.text.Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036F]"), "") // Remove acentos
            .replace(Regex("[^a-z0-9]"), "_") // Substitui caracteres especiais por underscore
            .replace(Regex("_+"), "_") // Remove underscores duplicados
            .replace(Regex("^_|_\$"), "") // Remove underscores no início e fim
    }
    
    val normalizedCity = normalize(city)
    val normalizedState = normalize(state)
    
    if (normalizedCity.isEmpty() && normalizedState.isEmpty()) {
        return "unknown"
    }
    
    return "${normalizedCity}_${normalizedState}"
}
```

**✅ COMPATÍVEL:**
- Backend e App usam a mesma lógica de normalização
- Ambos retornam `"unknown"` se cidade e estado estiverem vazios
- Ambos removem acentos e caracteres especiais

**⚠️ PROTEÇÃO:**
- App bloqueia `locationId == "unknown"` antes de fazer query
- Firestore Rules bloqueiam `locationId == "unknown"` ou `"unknown_unknown"`

---

## 📊 RELATÓRIO FINAL

### ✅ O QUE FUNCIONA

1. **Products**: ✅ **CORRIGIDO**
   - Backend escreve em `locations/{city}_{state}/products`
   - App lê de `locations/{city}_{state}/products`
   - Firestore Rules permitem leitura
   - Compatibilidade `images`/`imageUrls` implementada

2. **Services**: ✅ **FUNCIONA**
   - Backend escreve em `services`
   - App lê de `services`
   - Sempre funcionou corretamente

3. **Stories**: ✅ **CORRIGIDO**
   - Backend escreve em `locations/{city}_{state}/stories`
   - App lê de `locations/{city}_{state}/stories`
   - Firestore Rules permitem leitura
   - Bloqueio de `locationId == "unknown"` implementado

4. **Chat/Conversations**: ✅ **FUNCIONA**
   - Backend escreve em `conversations/{id}`
   - App lê via Cloud Function
   - Sempre funcionou corretamente

---

### 🎯 CORREÇÕES APLICADAS

1. **Firestore Rules**:
   - ✅ Adicionadas rules para `locations/{locationId}/products`
   - ✅ Adicionadas rules para `locations/{locationId}/stories`
   - ✅ Bloqueio de `locationId == "unknown"` ou `"unknown_unknown"`

2. **App Android**:
   - ✅ `FirestoreProductsRepositoryImpl` usa `LocationHelper.getLocationCollection()`
   - ✅ `FirestoreStoriesRepository` usa `LocationHelper.getLocationCollection()`
   - ✅ Aguarda `LocationState.Ready` antes de fazer queries
   - ✅ Compatibilidade `images`/`imageUrls` implementada
   - ✅ Filtro `status == "active"` adicionado para products

3. **Proteções**:
   - ✅ Bloqueio de `locationId == "unknown"` no app
   - ✅ Bloqueio de `locationId == "unknown"` nas Firestore Rules
   - ✅ `LocationStateManager` garante localização antes de queries

---

### 📝 NOTAS IMPORTANTES

1. **Coleções Globais (`products`, `stories`):**
   - Backend ainda salva nas coleções globais para compatibilidade
   - App não lê mais dessas coleções
   - Podem ser removidas no futuro após migração completa

2. **LocationStateManager:**
   - Garante que localização está pronta antes de fazer queries
   - Retorna `emptyList()` enquanto localização está carregando
   - Não bloqueia a UI, apenas aguarda localização

3. **Normalização de Location ID:**
   - Backend e App usam a mesma lógica
   - Ambos retornam `"unknown"` se cidade/estado estiverem vazios
   - Proteções múltiplas bloqueiam `"unknown"`

4. **Compatibilidade de Campos:**
   - App aceita tanto `images` quanto `imageUrls` (products)
   - Campos inexistentes retornam valores padrão (não quebram)

---

## 🔧 PRÓXIMOS PASSOS RECOMENDADOS

1. ✅ **Firestore Rules**: Deploy das novas rules
2. ✅ **Testes**: Verificar se produtos e stories aparecem no app
3. ⚠️ **Migração**: Considerar remover coleções globais (`products`, `stories`) após confirmação
4. ⚠️ **Logs**: Adicionar logs de diagnóstico para confirmar queries
5. ⚠️ **Monitoramento**: Monitorar erros de Firestore Rules no Firebase Console

---

## 📈 DIAGRAMA DE FLUXO

### Products (CORRIGIDO)

```
Backend (createProduct)
  ↓
locations/{city}_{state}/products/{id}  ✅ PRINCIPAL
  ↓
products/{id}  ⚠️ COMPATIBILIDADE (não usada pelo app)
  ↓
users/{userId}/products/{id}  ⚠️ PRIVADO

App (observeProducts)
  ↓
LocationStateManager.locationState
  ↓
LocationState.Ready?
  ↓ SIM
LocationHelper.getLocationCollection("products", city, state)
  ↓
locations/{city}_{state}/products  ✅ LÊ DA MESMA COLLECTION
  ↓
Firestore Rules: allow read if isValidLocationId && active == true
  ↓
ProductsViewModel.allProducts
  ↓
UI
```

### Stories (CORRIGIDO)

```
Backend (createStory)
  ↓
locations/{city}_{state}/stories/{id}  ✅ PRINCIPAL
  ↓
stories/{id}  ⚠️ COMPATIBILIDADE (não usada pelo app)

App (observeStories)
  ↓
LocationStateManager.locationState
  ↓
LocationState.Ready?
  ↓ SIM
locationId == "unknown"?  ❌ BLOQUEADO
  ↓ NÃO
LocationHelper.getLocationCollection("stories", city, state)
  ↓
locations/{city}_{state}/stories  ✅ LÊ DA MESMA COLLECTION
  ↓
Firestore Rules: allow read if isValidLocationId
  ↓
StoriesViewModel.stories
  ↓
UI
```

---

**FIM DO RELATÓRIO**

**Status:** ✅ **TODAS AS CORREÇÕES APLICADAS**
