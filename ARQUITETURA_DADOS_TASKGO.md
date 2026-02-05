# 📊 Arquitetura de Dados - TaskGo App

## 🎯 Lei Máxima do TaskGo: City/State como Fonte Única de Verdade

**REGRA FUNDAMENTAL**: `city` e `state` definidos no cadastro do usuário são a **fonte única de verdade** para TODAS as operações de gravação e leitura no Firestore. GPS é usado **APENAS** para coordenadas do mapa, nunca para determinar localização de dados.

---

## 🏗️ Estrutura de Dados no Firestore

### 1. **Coleção Global: `users`** (Híbrida - Legacy + Nova)

```
users/{userId}
├── uid: String
├── email: String
├── displayName: String?
├── city: String?          ← LEI MÁXIMA: Fonte única de verdade
├── state: String?         ← LEI MÁXIMA: Fonte única de verdade
├── role: String           ← "client", "partner", "admin"
├── cpf: String?
├── cnpj: String?
├── preferredCategories: List<String>?  ← Para parceiros
└── ... (outros campos)
```

**Estratégia Híbrida**:
- ✅ **Leitura**: Busca primeiro em `users/{userId}` (global), depois em `locations/{locationId}/users/{userId}` (se existir)
- ✅ **Escrita**: Salva em AMBAS as coleções:
  - `users/{userId}` (global - para autenticação/login)
  - `locations/{locationId}/users/{userId}` (regional - para buscas por localização)

**Normalização do LocationId**:
```kotlin
// Exemplo: "Osasco" + "SP" → "osasco_sp"
LocationHelper.normalizeLocationId(city, state)
```

---

### 2. **Coleção Regional: `locations/{locationId}`**

Todos os dados públicos são organizados por localização:

```
locations/{locationId}/
├── users/{userId}          ← Cópia do usuário (para buscas regionais)
├── products/{productId}     ← Produtos do marketplace
├── orders/{orderId}        ← Ordens de serviço E pedidos de produtos
├── services/{serviceId}    ← Serviços oferecidos (DEPRECATED - parceiros não criam serviços)
├── reviews/{reviewId}      ← Avaliações (PROVIDER, PRODUCT, SERVICE)
├── stories/{storyId}       ← Stories do feed
└── posts/{postId}          ← Posts do feed
```

**Onde `locationId = normalizeLocationId(city, state)`**

---

## 📝 Como os Dados são GRAVADOS

### **1. USUÁRIOS (Users)**

#### **Criação/Atualização de Perfil**
- **Frontend**: `FirestoreUserRepository.saveUser()`
- **Backend**: Cloud Function `onUserCreate` (trigger automático)
- **Paths**:
  - ✅ `users/{userId}` (global)
  - ✅ `locations/{locationId}/users/{userId}` (regional)

**Fluxo**:
```kotlin
// 1. Obter city/state do perfil do usuário (LEI MÁXIMA)
val user = userRepository.observeCurrentUser().first()
val city = user?.city ?: throw IllegalStateException("City obrigatório")
val state = user?.state ?: throw IllegalStateException("State obrigatório")

// 2. Normalizar locationId
val locationId = LocationHelper.normalizeLocationId(city, state)

// 3. Salvar em users global
firestore.collection("users").document(userId).set(userData)

// 4. Salvar em locations/{locationId}/users
firestore.collection("locations").document(locationId)
    .collection("users").document(userId).set(userData)
```

**Validação**:
- ❌ **NUNCA** usar GPS para city/state
- ❌ **NUNCA** usar fallback para "Brasília, DF"
- ✅ **SEMPRE** usar `user.city` e `user.state` do cadastro

---

### **2. PRODUTOS (Products)**

#### **Criação de Produto**
- **Frontend**: `FirestoreProductsRepositoryImpl.createProduct()`
- **Backend**: Cloud Function `createProduct`
- **Path**: `locations/{locationId}/products/{productId}`

**Fluxo**:
```kotlin
// 1. Obter city/state do usuário logado (LEI MÁXIMA)
val (city, state) = LocationHelper.getUserLocation(userRepository)

// 2. Validar localização
if (city.isBlank() || state.isBlank()) {
    throw IllegalStateException("Localização obrigatória para criar produto")
}

// 3. Normalizar locationId
val locationId = LocationHelper.normalizeLocationId(city, state)

// 4. Salvar produto
val collection = LocationHelper.getLocationCollection(
    firestore, "products", city, state
)
collection.document(productId).set(productData)
```

**Campos do Produto**:
```kotlin
data class ProductFirestore(
    val id: String,
    val title: String,
    val price: Double,
    val sellerId: String,        ← ID do vendedor
    val sellerName: String?,
    val category: String?,
    val active: Boolean = true,   ← Soft delete
    val featured: Boolean = false,
    val latitude: Double? = null, ← GPS (apenas para mapa)
    val longitude: Double? = null ← GPS (apenas para mapa)
)
```

**Observação**: `latitude`/`longitude` são usados **APENAS** para exibição no mapa, **NUNCA** para determinar onde salvar o produto.

---

### **3. ORDENS DE SERVIÇO (Service Orders)**

#### **Criação de Ordem** (APENAS por Clientes)
- **Frontend**: `CreateWorkOrderViewModel.createOrder()`
- **Backend**: Cloud Function `createOrder` (validação de role)
- **Path**: `locations/{locationId}/orders/{orderId}`

**Fluxo**:
```kotlin
// 1. VALIDAÇÃO: Apenas clientes podem criar ordens
if (userRole != "client" && accountType != "CLIENTE") {
    throw PermissionDeniedException("Apenas clientes podem criar ordens")
}

// 2. Obter city/state do usuário (LEI MÁXIMA)
val (city, state) = LocationHelper.getUserLocation(userRepository)

// 3. Normalizar locationId
val locationId = LocationHelper.normalizeLocationId(city, state)

// 4. Salvar ordem
val collection = LocationHelper.getLocationCollection(
    firestore, "orders", city, state
)
collection.document(orderId).set(orderData)
```

**Campos da Ordem**:
```kotlin
data class OrderFirestore(
    val id: String,
    val clientId: String,           ← ID do cliente que criou
    val providerId: String? = null, ← ID do parceiro que aceitou
    val category: String,
    val description: String,
    val status: String,             ← "pending", "in_progress", "completed", "cancelled"
    val acceptedByProvider: Boolean = false,
    val acceptedByClient: Boolean = false,
    val budget: Double?,
    val dueDate: Date?,
    val createdAt: Date,
    val deleted: Boolean = false    ← Soft delete
)
```

**Regras de Negócio**:
- ✅ Apenas **CLIENTES** podem criar ordens
- ✅ Ordens são salvas em `locations/{locationId}/orders`
- ✅ `city`/`state` vêm **EXCLUSIVAMENTE** do perfil do cliente

---

### **4. PEDIDOS DE PRODUTOS (Purchase Orders)**

#### **Criação de Pedido**
- **Frontend**: `FirestoreOrdersRepositoryImpl.createPurchaseOrder()`
- **Backend**: Cloud Function `createPurchaseOrder`
- **Path**: `locations/{locationId}/orders/{orderId}` (mesma coleção que service orders)

**Fluxo**: Similar às ordens de serviço, usando `city`/`state` do cliente.

---

### **5. AVALIAÇÕES (Reviews)**

#### **Criação de Avaliação**
- **Frontend**: `FirestoreReviewsRepository`
- **Backend**: Cloud Function `createReview`
- **Path**: `locations/{locationId}/reviews/{reviewId}`

**Fluxo**:
```kotlin
// 1. Obter city/state do usuário avaliado (targetId) - LEI MÁXIMA
val targetUser = userRepository.getUser(targetId)
val city = targetUser?.city ?: throw IllegalStateException("City obrigatório")
val state = targetUser?.state ?: throw IllegalStateException("State obrigatório")

// 2. Normalizar locationId
val locationId = LocationHelper.normalizeLocationId(city, state)

// 3. Salvar avaliação
val collection = LocationHelper.getLocationCollection(
    firestore, "reviews", city, state
)
collection.document(reviewId).set(reviewData)
```

**Campos da Avaliação**:
```kotlin
data class ReviewFirestore(
    val id: String,
    val targetId: String,        ← ID do usuário/produto avaliado
    val reviewerId: String,      ← ID do usuário que avaliou
    val reviewerName: String,
    val type: String,            ← "PROVIDER", "PRODUCT", "SERVICE"
    val rating: Int,             ← 1-5 estrelas
    val comment: String?,
    val createdAt: Date
)
```

**Regras de Negócio**:
- ✅ **TODAS** as avaliações são salvas em `locations/{locationId}/reviews` (PROVIDER, PRODUCT, SERVICE)
- ✅ Para PROVIDER: `city`/`state` vêm **EXCLUSIVAMENTE** do perfil do provider avaliado (`targetId`)
- ✅ Para PRODUCT/SERVICE: `city`/`state` vêm do perfil do usuário que está criando a review (reviewer)
- ✅ **NUNCA** usar coleção global `reviews` - tudo deve estar em `locations/{locationId}/reviews`

### **6. STORIES e POSTS (Feed)**

#### **Criação de Story/Post**
- **Frontend**: `FirestoreStoriesRepository` / `FirestoreFeedRepository`
- **Path**: `locations/{locationId}/stories/{storyId}` ou `locations/{locationId}/posts/{postId}`

**Fluxo**: Sempre usa `city`/`state` do usuário logado.

---

## 📖 Como os Dados são LIDOS

### **1. USUÁRIOS**

#### **Buscar Usuário por ID**
```kotlin
// Estratégia Híbrida: Busca em ambas as coleções
fun getUser(uid: String): UserFirestore? {
    // 1. Buscar em users global (legacy)
    val globalUser = firestore.collection("users").document(uid).get().await()
    
    // 2. Se encontrou e tem city/state, buscar também em locations
    if (globalUser.exists && globalUser.city != null && globalUser.state != null) {
        val locationId = LocationHelper.normalizeLocationId(globalUser.city, globalUser.state)
        val locationUser = firestore.collection("locations").document(locationId)
            .collection("users").document(uid).get().await()
        
        // Priorizar dados de locations (mais atualizado)
        return locationUser.takeIf { it.exists } ?: globalUser
    }
    
    return globalUser
}
```

#### **Observar Mudanças do Usuário**
```kotlin
fun observeUser(uid: String): Flow<UserFirestore?> {
    // Observa AMBAS as coleções simultaneamente
    // Prioriza dados de locations/{locationId}/users se existir
}
```

---

### **2. PRODUTOS**

#### **Listar Produtos da Região**
```kotlin
fun observeProducts(): Flow<List<Product>> {
    // 1. Aguardar localização do usuário estar pronta
    locationStateManager.locationState
        .flatMapLatest { locationState ->
            when (locationState) {
                is LocationState.Ready -> {
                    // 2. Obter city/state do usuário
                    val (city, state) = locationState.location
                    
                    // 3. Buscar produtos em locations/{locationId}/products
                    val collection = LocationHelper.getLocationCollection(
                        firestore, "products", city, state
                    )
                    
                    // 4. Filtrar por active == true
                    collection.whereEqualTo("active", true)
                        .addSnapshotListener { ... }
                }
                else -> flowOf(emptyList())
            }
        }
}
```

**Filtros**:
- ✅ `active == true` (soft delete)
- ✅ `featured == true` (produtos em destaque)
- ✅ Por categoria
- ✅ Por vendedor (`sellerId`)

---

### **3. ORDENS DE SERVIÇO**

#### **Para CLIENTES: "Minhas Ordens"**
```kotlin
fun observeOrders(userId: String, role: String): Flow<List<OrderFirestore>> {
    // 1. Obter city/state do usuário
    val (city, state) = LocationHelper.getUserLocation(userRepository)
    val locationId = LocationHelper.normalizeLocationId(city, state)
    
    // 2. Buscar em locations/{locationId}/orders
    val collection = firestore.collection("locations").document(locationId)
        .collection("orders")
    
    // 3. Filtrar por clientId
    collection.whereEqualTo("clientId", userId)
        .whereEqualTo("deleted", false)
        .addSnapshotListener { ... }
}
```

**Abas para Clientes**:
- **Ativas**: `status != "cancelled" && status != "completed"`
  - Mostra `acceptedByProvider` (sinalização de aceitação)
- **Canceladas**: `status == "cancelled"`
- **Concluídas**: `status == "completed"`

#### **Para PARCEIROS: "Meus Serviços"**
```kotlin
fun observeOrders(userId: String, role: String): Flow<List<OrderFirestore>> {
    // 1. Obter city/state do parceiro
    val (city, state) = LocationHelper.getUserLocation(userRepository)
    val locationId = LocationHelper.normalizeLocationId(city, state)
    
    // 2. Buscar em locations/{locationId}/orders
    val collection = firestore.collection("locations").document(locationId)
        .collection("orders")
    
    // 3. Filtrar por providerId (apenas ordens que o parceiro aceitou)
    collection.whereEqualTo("providerId", userId)
        .whereEqualTo("deleted", false)
        .addSnapshotListener { ... }
}
```

**Abas para Parceiros**:
- **Ativas**: `status == "in_progress"`
- **Canceladas**: `status == "cancelled"`
- **Concluídas**: `status == "completed"`

#### **Para PARCEIROS: "Serviços" (Buscar Novas Ordens)**
```kotlin
fun observeLocalServiceOrders(category: String?): Flow<List<OrderFirestore>> {
    // 1. Obter city/state do parceiro
    val (city, state) = LocationHelper.getUserLocation(userRepository)
    val locationId = LocationHelper.normalizeLocationId(city, state)
    
    // 2. Buscar em locations/{locationId}/orders
    val collection = firestore.collection("locations").document(locationId)
        .collection("orders")
    
    // 3. Filtrar ordens disponíveis (não aceitas ainda)
    var query = collection
        .whereEqualTo("status", "pending")
        .whereEqualTo("providerId", null)  // ← Ainda não aceita por ninguém
        .whereEqualTo("deleted", false)
    
    // 4. Filtrar por categoria se fornecida
    if (category != null) {
        query = query.whereEqualTo("category", category)
    }
    
    query.addSnapshotListener { ... }
}
```

**Filtros para Parceiros**:
- ✅ `status == "pending"` (ordens abertas)
- ✅ `providerId == null` (ainda não aceitas)
- ✅ Por `preferredCategories` do parceiro
- ✅ Por categoria selecionada

---

### **4. PEDIDOS DE PRODUTOS**

#### **Listar Pedidos do Cliente/Vendedor**
```kotlin
// Similar às ordens de serviço, usando locations/{locationId}/orders
// Filtra por clientId (cliente) ou sellerId (vendedor)
```

### **5. AVALIAÇÕES (Reviews)**

#### **Listar Avaliações de um Usuário**
```kotlin
fun observeProviderReviews(providerId: String): Flow<List<ReviewFirestore>> {
    // 1. Buscar provider para obter city/state (LEI MÁXIMA)
    val provider = userRepository.getUser(providerId)
    val city = provider?.city ?: return flowOf(emptyList())
    val state = provider?.state ?: return flowOf(emptyList())
    
    // 2. Buscar em locations/{locationId}/reviews
    val collection = LocationHelper.getLocationCollection(
        firestore, "reviews", city, state
    )
    
    // 3. Filtrar por targetId e type
    collection.whereEqualTo("targetId", providerId)
        .whereEqualTo("type", "PROVIDER")
        .addSnapshotListener { ... }
}
```

**Filtros**:
- ✅ `targetId == userId` (avaliações recebidas)
- ✅ `type == "PROVIDER"` (avaliações de prestador)
- ✅ Ordenadas por `createdAt` (mais recente primeiro)

**Métodos Corrigidos**:
- ✅ `createReview`: Salva em `locations/{locationId}/reviews` para TODOS os tipos
- ✅ `updateReview`: Busca em todas as locations conhecidas
- ✅ `deleteReview`: Busca em todas as locations conhecidas
- ✅ `getReview`: Busca em todas as locations conhecidas
- ✅ `observeProviderReviews`: Usa `city`/`state` do provider
- ✅ `observeUserReviewsAsTarget`: Usa `city`/`state` do usuário
- ✅ `getReviewSummary`: Usa `city`/`state` do target
- ✅ `canUserReview`: Usa `city`/`state` do target
- ✅ `markReviewAsHelpful`: Busca em todas as locations conhecidas
- ✅ `getUserReviewSummaryAsTarget`: Usa `city`/`state` do usuário

---

## 🔄 Sincronização Offline

### **SyncManager**
```kotlin
class SyncManager {
    fun syncOrder(order: OrderFirestore) {
        // 1. Obter city/state do usuário
        val (city, state) = LocationHelper.getUserLocation(userRepository)
        val locationId = LocationHelper.normalizeLocationId(city, state)
        
        // 2. Salvar em locations/{locationId}/orders
        firestore.collection("locations").document(locationId)
            .collection("orders").document(order.id).set(order)
    }
}
```

---

## 🛡️ Validações e Regras de Segurança

### **1. Cloud Functions (Backend)**

#### **createOrder**
```typescript
// VALIDAÇÃO 1: Apenas clientes podem criar ordens
if (userRole === 'provider' || userRole === 'partner' || accountType === 'PARCEIRO') {
    throw new functions.https.HttpsError(
        'permission-denied',
        'Apenas clientes podem criar ordens de serviço'
    );
}

// VALIDAÇÃO 2: city/state obrigatórios
if (!city || !state) {
    throw new functions.https.HttpsError(
        'invalid-argument',
        'City e state são obrigatórios'
    );
}

// VALIDAÇÃO 3: Obter city/state do perfil do usuário (LEI MÁXIMA)
const userDoc = await db.collection('users').doc(userId).get();
const userCity = userDoc.data()?.city;
const userState = userDoc.data()?.state;

if (!userCity || !userState) {
    throw new functions.https.HttpsError(
        'failed-precondition',
        'Usuário deve ter city e state definidos no perfil'
    );
}

// VALIDAÇÃO 4: Usar city/state do perfil, não do request
const locationId = normalizeLocationId(userCity, userState);
```

### **2. Firestore Security Rules**

```javascript
// Regras para locations/{locationId}/orders
match /locations/{locationId}/orders/{orderId} {
    // Leitura: Apenas cliente ou parceiro associado
    allow read: if request.auth != null 
        && (resource.data.clientId == request.auth.uid 
            || resource.data.providerId == request.auth.uid);
    
    // Criação: Apenas clientes (validado também no Cloud Function)
    allow create: if request.auth != null 
        && request.resource.data.clientId == request.auth.uid
        && getUserRole() == 'client';
    
    // Atualização: Cliente (cancelar) ou parceiro (aceitar/completar)
    allow update: if request.auth != null 
        && (resource.data.clientId == request.auth.uid 
            || resource.data.providerId == request.auth.uid);
}
```

---

## 📍 LocationHelper: Centralizador de Localização

### **Funções Principais**

```kotlin
object LocationHelper {
    // 1. Normaliza city + state → locationId
    fun normalizeLocationId(city: String, state: String): String {
        // Valida city/state antes de normalizar
        // Remove acentos, caracteres especiais
        // Retorna "osasco_sp" para "Osasco, SP"
    }
    
    // 2. Obtém referência da coleção por localização
    fun getLocationCollection(
        firestore: FirebaseFirestore,
        collection: String,
        city: String,
        state: String
    ): CollectionReference {
        val locationId = normalizeLocationId(city, state)
        return firestore.collection("locations")
            .document(locationId)
            .collection(collection)
    }
    
    // 3. Obtém city/state do usuário logado
    suspend fun getUserLocation(
        userRepository: UserRepository
    ): Pair<String, String> {
        val user = userRepository.observeCurrentUser().first()
        val city = user?.city?.takeIf { it.isNotBlank() } ?: ""
        val state = user?.state?.takeIf { it.isNotBlank() } ?: ""
        return city to state
    }
}
```

---

## 🎯 Resumo: Fluxo Completo de Dados

### **GRAVAÇÃO**
1. ✅ Usuário faz ação (criar produto, ordem, etc.)
2. ✅ App obtém `city`/`state` do perfil do usuário (LEI MÁXIMA)
3. ✅ Valida `city`/`state` (não pode ser vazio)
4. ✅ Normaliza `locationId = normalizeLocationId(city, state)`
5. ✅ Salva em `locations/{locationId}/{collection}/{documentId}`
6. ✅ (Opcional) Salva também em coleção global se necessário (ex: `users`)

### **LEITURA**
1. ✅ App obtém `city`/`state` do perfil do usuário
2. ✅ Normaliza `locationId = normalizeLocationId(city, state)`
3. ✅ Busca em `locations/{locationId}/{collection}`
4. ✅ Aplica filtros (role, status, categoria, etc.)
5. ✅ Retorna dados filtrados para a UI

---

## ⚠️ Regras Críticas (NUNCA Violar)

1. ❌ **NUNCA** usar GPS para determinar `city`/`state` de gravação
2. ❌ **NUNCA** usar fallback para "Brasília, DF"
3. ❌ **NUNCA** salvar dados sem `city`/`state` válidos
4. ✅ **SEMPRE** usar `user.city` e `user.state` do cadastro
5. ✅ **SEMPRE** validar `city`/`state` antes de normalizar
6. ✅ **SEMPRE** usar `LocationHelper.getLocationCollection()` para obter referências
7. ✅ **SEMPRE** salvar dados públicos em `locations/{locationId}/{collection}`
8. ❌ **NUNCA** usar coleção global `reviews` - **TODAS** as avaliações devem estar em `locations/{locationId}/reviews`
9. ✅ **SEMPRE** buscar `city`/`state` do target (para PROVIDER) ou do reviewer (para PRODUCT/SERVICE) antes de salvar/ler reviews

---

## 🔍 Logs de Rastreamento

Todas as operações de localização geram logs com tag `LocationTrace`:

```
📍 FRONTEND LOCATION TRACE
Function: normalizeLocationId
RawCity: Osasco
RawState: SP
ValidatedCity: Osasco
ValidatedState: SP
NormalizedCity: osasco
NormalizedState: sp
LocationId: osasco_sp
Timestamp: 2024-01-15 10:30:00
```

Isso permite rastrear exatamente onde os dados estão sendo salvos e lidos.

---

**Fim do Documento**
