# 📱 Relatório Frontend: Arquitetura e Comunicação com Backend

**Data:** Janeiro 2025  
**Plataforma:** Android (Kotlin)  
**Framework UI:** Jetpack Compose  
**Injeção de Dependências:** Hilt (Dagger)  
**Arquitetura:** MVVM (Model-View-ViewModel) + Clean Architecture

---

## 📋 ÍNDICE

1. [Arquitetura Geral](#1-arquitetura-geral)
2. [Comunicação com Backend](#2-comunicação-com-backend)
3. [Camadas da Aplicação](#3-camadas-da-aplicação)
4. [Firebase Services](#4-firebase-services)
5. [Repositórios](#5-repositórios)
6. [ViewModels](#6-viewmodels)
7. [Cache Local](#7-cache-local)
8. [Fluxos de Dados](#8-fluxos-de-dados)

---

## 1️⃣ ARQUITETURA GERAL

### 🏗️ **Estrutura do Projeto**

```
app/src/main/java/com/taskgoapp/taskgo/
├── data/                          # Camada de Dados
│   ├── firebase/                  # Serviços Firebase
│   │   └── FirebaseFunctionsService.kt
│   ├── firestore/                 # Models Firestore
│   ├── local/                     # Cache Local (Room + DataStore)
│   │   ├── TaskGoDatabase.kt
│   │   └── datastore/PreferencesManager.kt
│   └── repository/                # Implementações de Repositórios
│       ├── FirestoreUserRepository.kt
│       ├── FirestoreServicesRepository.kt
│       ├── FirestoreProductsRepository.kt
│       └── ...
├── domain/                        # Camada de Domínio
│   ├── repository/                # Interfaces de Repositórios
│   └── usecase/                   # Casos de Uso
├── feature/                       # Features (UI + ViewModels)
│   ├── auth/                      # Autenticação
│   ├── products/                  # Produtos
│   ├── services/                  # Serviços
│   └── ...
├── di/                            # Injeção de Dependências
│   ├── AppModule.kt
│   └── FirebaseModule.kt
└── core/                          # Componentes Core
    ├── ai/                        # Serviços de IA
    ├── design/                    # Componentes UI
    ├── network/                   # Diagnóstico de rede
    └── sync/                      # Sincronização
```

### 🎯 **Padrões Arquiteturais**

- **MVVM**: ViewModels separam lógica de negócio da UI
- **Repository Pattern**: Abstrai fontes de dados (Firestore, Local, APIs)
- **Clean Architecture**: Separação em camadas (Data, Domain, Presentation)
- **Dependency Injection**: Hilt para injeção de dependências
- **Reactive Programming**: Kotlin Flows para observação de dados

---

## 2️⃣ COMUNICAÇÃO COM BACKEND

### 🔐 **Autenticação Firebase**

#### **A. Inicialização do Firebase**

**Arquivo:** `TaskGoApp.kt`

```kotlin
@HiltAndroidApp
class TaskGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // ✅ Firebase inicializado automaticamente via google-services.json
        val firebaseApp = FirebaseApp.getInstance()
        
        // ✅ Firebase App Check (produção/desenvolvimento)
        if (BuildConfig.FIREBASE_APP_CHECK_ENABLED) {
            val appCheck = FirebaseAppCheck.getInstance()
            
            if (BuildConfig.DEBUG) {
                // Debug: usar DebugAppCheckProviderFactory
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                // Release: usar PlayIntegrityAppCheckProviderFactory
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        }
    }
}
```

**Características:**
- ✅ App Check habilitado em produção (Play Integrity)
- ✅ Debug mode para desenvolvimento (Debug Token)
- ✅ Validação automática em Cloud Functions

---

#### **B. Repository de Autenticação**

**Arquivo:** `data/repository/FirebaseAuthRepository.kt`

```kotlin
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            // ✅ Autenticação direta com Firebase Auth
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            // ✅ Google Sign-In via token
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User is null")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("User not logged in")
            // ✅ forceRefresh = true garante Custom Claims atualizados
            val token = user.getIdToken(forceRefresh).await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
}
```

**Fluxo de Autenticação:**
1. ✅ Cliente → `signInWithEmail()` → Firebase Auth
2. ✅ Firebase Auth → Retorna `FirebaseUser` com UID
3. ✅ Cliente → Cria/atualiza documento em `/users/{uid}` (se necessário)
4. ✅ Cloud Function `onUserCreate` → Define Custom Claim `role: 'user'`
5. ✅ Cliente → Chama `setInitialUserRole()` (se necessário)
6. ✅ Custom Claims incluídos automaticamente no token JWT

---

### 🌐 **Cloud Functions**

#### **A. Serviço de Cloud Functions**

**Arquivo:** `data/firebase/FirebaseFunctionsService.kt`

```kotlin
@Singleton
class FirebaseFunctionsService @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    // Helper function genérico para chamar Cloud Functions
    private suspend fun executeFunction(
        functionName: String,
        data: Map<String, Any>?
    ): Result<Map<String, Any>> {
        return try {
            Log.d("FirebaseFunctionsService", "Chamando função: $functionName com dados: $data")
            val callable = functions.getHttpsCallable(functionName)
            
            // ✅ Chamada assíncrona com await
            val result: Any? = if (data != null) {
                callable.call(data).await()
            } else {
                callable.call().await()
            }
            
            // Extrair dados do resultado
            val dataField = result?.javaClass?.getDeclaredField("data")?.apply {
                isAccessible = true
            }
            val resultData = dataField?.get(result) as? Map<String, Any>
            
            Result.success(resultData ?: emptyMap())
        } catch (e: FirebaseFunctionsException) {
            // ✅ Tratamento específico de erros do Firebase Functions
            val code = e.code
            val message = e.message ?: "Erro desconhecido"
            
            val errorMessage = when (code) {
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> {
                    "Permissão negada: $message"
                }
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                    "Não autenticado: Faça login novamente"
                }
                FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                    "Dados inválidos: $message"
                }
                FirebaseFunctionsException.Code.NOT_FOUND -> {
                    "Recurso não encontrado: $message"
                }
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> {
                    "Pré-condição falhou: $message"
                }
                else -> {
                    "Erro ao executar $functionName: $message"
                }
            }
            
            Result.failure(Exception(errorMessage, e))
        } catch (e: Exception) {
            Log.e("FirebaseFunctionsService", "Erro inesperado: ${e.message}", e)
            Result.failure(e)
        }
    }
}
```

#### **B. Funções Disponíveis**

**1. Autenticação:**
```kotlin
suspend fun promoteToProvider(): Result<Map<String, Any>>
suspend fun approveProviderDocuments(providerId: String, documents: Map<String, Any>): Result<Map<String, Any>>
suspend fun sendTwoFactorCode(): Result<Map<String, Any>>
suspend fun verifyTwoFactorCode(code: String): Result<Map<String, Any>>
suspend fun startIdentityVerification(...): Result<Map<String, Any>>
```

**2. Ordens:**
```kotlin
suspend fun createOrder(
    serviceId: String? = null,
    category: String? = null,
    details: String,
    location: String,
    budget: Double? = null,
    dueDate: String? = null
): Result<Map<String, Any>>

suspend fun updateOrderStatus(
    orderId: String,
    status: String,
    proposalDetails: ProposalDetails? = null
): Result<Map<String, Any>>

suspend fun getMyOrders(role: String? = null, status: String? = null): Result<Map<String, Any>>
```

**3. Serviços:**
```kotlin
suspend fun createService(...): Result<Map<String, Any>>
suspend fun updateService(serviceId: String, updates: Map<String, Any>): Result<Map<String, Any>>
suspend fun deleteService(serviceId: String): Result<Map<String, Any>>
```

**4. Produtos:**
```kotlin
suspend fun createProduct(...): Result<Map<String, Any>>
suspend fun updateProduct(productId: String, updates: Map<String, Any>): Result<Map<String, Any>>
suspend fun deleteProduct(productId: String): Result<Map<String, Any>>
```

**5. Stories:**
```kotlin
suspend fun createStory(...): Result<Map<String, Any>>
```

**6. Pagamentos:**
```kotlin
suspend fun createPaymentIntent(orderId: String): Result<Map<String, Any>>
suspend fun confirmPayment(paymentIntentId: String): Result<Map<String, Any>>
suspend fun createPixPayment(orderId: String): Result<Map<String, Any>>
suspend fun verifyPixPayment(paymentId: String): Result<Map<String, Any>>
```

**7. Notificações:**
```kotlin
suspend fun getMyNotifications(limit: Int = 50, unreadOnly: Boolean = false): Result<Map<String, Any>>
suspend fun markNotificationRead(notificationId: String): Result<Map<String, Any>>
suspend fun markAllNotificationsRead(): Result<Map<String, Any>>
```

**8. Preferências:**
```kotlin
suspend fun updateNotificationSettings(settings: Map<String, Boolean>): Result<Map<String, Any>>
suspend fun updatePrivacySettings(settings: Map<String, Boolean>): Result<Map<String, Any>>
suspend fun getUserPreferences(): Result<Map<String, Any>>
```

**9. AI Chat:**
```kotlin
suspend fun aiChatProxy(message: String, conversationId: String? = null): Result<Map<String, Any>>
suspend fun createConversation(): Result<Map<String, Any>>
suspend fun getConversationHistory(conversationId: String): Result<Map<String, Any>>
```

**Características:**
- ✅ Todas as funções são suspensas (corrotinas)
- ✅ Retornam `Result<Map<String, Any>>` para tratamento de erros
- ✅ Logs detalhados para debugging
- ✅ Tratamento específico de erros do Firebase Functions

---

### 📊 **Firestore (Leitura Direta)**

#### **A. Configuração do Firestore**

**Arquivo:** `di/FirebaseModule.kt`

```kotlin
@Provides
@Singleton
fun provideFirebaseFirestore(): FirebaseFirestore {
    val firestore = if (BuildConfig.USE_EMULATOR) {
        // Emulator configuration
        FirebaseFirestore.getInstance().apply {
            useEmulator("10.0.2.2", 8080)
        }
    } else {
        // Production Firestore
        FirebaseFirestore.getInstance()
    }
    
    // ✅ Configurar cache offline e performance
    val settings = FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(true) // Habilitar cache offline
        .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Cache ilimitado
        .build()
    firestore.firestoreSettings = settings
    
    return firestore
}
```

**Características:**
- ✅ Cache offline habilitado
- ✅ Cache ilimitado para melhor performance
- ✅ Suporte a emulador local

---

#### **B. Padrão de Leitura do Firestore**

**Exemplo:** `FirestoreUserRepository.kt`

```kotlin
@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    // ✅ Leitura única
    suspend fun getUser(uid: String): UserFirestore? {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                mapUser(document.id, document.data!!)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirestoreUserRepository", "Erro ao buscar usuário: ${e.message}", e)
            null
        }
    }
    
    // ✅ Observação em tempo real (Flow)
    fun observeUser(uid: String): Flow<UserFirestore?> = callbackFlow {
        val listenerRegistration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreUserRepository", "Erro no listener: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.data?.let { mapUser(snapshot.id, it) }
                    trySend(user)
                } else {
                    trySend(null)
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    // ✅ Atualização (apenas campos permitidos)
    suspend fun updateUser(user: UserFirestore): Result<Unit> {
        return try {
            usersCollection.document(user.uid)
                .set(user.toFirestoreMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Características:**
- ✅ Leitura única com `.get().await()`
- ✅ Observação em tempo real com `addSnapshotListener` → Flow
- ✅ Atualizações respeitam regras do Firestore
- ✅ Tratamento de erros robusto

---

#### **C. Coleções e Subcoleções**

**Padrão usado no app:**

1. **Coleções Públicas** (para queries):
   - `/services` - Serviços ativos
   - `/products` - Produtos ativos
   - `/orders` - Ordens (com filtro por cliente/prestador)
   - `/posts` - Posts públicos
   - `/stories` - Stories públicas

2. **Subcoleções de Usuário** (para dados privados):
   - `/users/{userId}/services` - Serviços do usuário
   - `/users/{userId}/products` - Produtos do usuário
   - `/users/{userId}/orders` - Ordens do usuário
   - `/users/{userId}/posts` - Posts do usuário
   - `/users/{userId}/stories` - Stories do usuário

**Exemplo:** `FirestoreServicesRepository.kt`

```kotlin
@Singleton
class FirestoreServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functionsService: FirebaseFunctionsService
) {
    // Coleção pública para queries
    private val publicServicesCollection = firestore.collection("services")
    
    // Subcoleção do usuário
    private fun getUserServicesCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("services")

    // ✅ Observar serviços públicos (para visualização)
    fun observePublicServices(
        category: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null
    ): Flow<List<ServiceFirestore>> = callbackFlow {
        var query: Query = publicServicesCollection
            .whereEqualTo("active", true)
        
        if (category != null) {
            query = query.whereEqualTo("category", category)
        }
        if (minPrice != null) {
            query = query.whereGreaterThanOrEqualTo("price", minPrice)
        }
        if (maxPrice != null) {
            query = query.whereLessThanOrEqualTo("price", maxPrice)
        }
        
        val listener = query
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { mapService(doc.id, it) }
                } ?: emptyList()
                
                trySend(services)
            }
        
        awaitClose { listener.remove() }
    }
    
    // ✅ Observar serviços do prestador
    fun observeProviderServices(providerId: String): Flow<List<ServiceFirestore>> = callbackFlow {
        val listener = getUserServicesCollection(providerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { mapService(doc.id, it) }
                } ?: emptyList()
                
                trySend(services)
            }
        
        awaitClose { listener.remove() }
    }
    
    // ✅ Criar serviço (via Cloud Function - escrita bloqueada)
    suspend fun createService(service: ServiceFirestore): Result<String> {
        // Não pode criar diretamente - usar Cloud Function
        return functionsService.createService(
            title = service.title,
            description = service.description,
            category = service.category,
            price = service.price,
            latitude = service.latitude,
            longitude = service.longitude,
            active = service.active
        ).fold(
            onSuccess = { result ->
                val serviceId = result["serviceId"] as? String
                Result.success(serviceId ?: "")
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }
}
```

**Regras aplicadas:**
- ✅ **Leitura**: Direta do Firestore (coleções públicas ou subcoleções)
- ❌ **Escrita**: Bloqueada - usar Cloud Functions
- ✅ **Queries**: Filtros e ordenação no Firestore
- ✅ **Observação**: SnapshotListeners → Flows para atualizações em tempo real

---

## 3️⃣ CAMADAS DA APLICAÇÃO

### 📁 **Camada de Dados (Data Layer)**

#### **A. Repositórios Firestore**

Repositórios que acessam diretamente o Firestore:

- `FirestoreUserRepository` - Usuários
- `FirestoreServicesRepository` - Serviços
- `FirestoreProductsRepository` - Produtos
- `FirestoreOrderRepository` - Ordens
- `FirestoreFeedRepository` - Feed (Posts)
- `FirestoreStoriesRepository` - Stories
- `FirestoreNotificationRepository` - Notificações
- `FirestoreReviewsRepository` - Avaliações

**Padrão comum:**
```kotlin
@Singleton
class FirestoreXXXRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository,
    private val functionsService: FirebaseFunctionsService? // Para escrita
) {
    // Coleções
    private val publicCollection = firestore.collection("xxx")
    private fun getUserCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("xxx")
    
    // Leitura: Direta do Firestore
    suspend fun getXXX(id: String): XXXFirestore?
    fun observeXXX(...): Flow<List<XXXFirestore>>
    
    // Escrita: Via Cloud Functions
    suspend fun createXXX(xxx: XXXFirestore): Result<String> {
        return functionsService?.createXXX(...) ?: Result.failure(Exception("Functions service not available"))
    }
}
```

---

#### **B. Cache Local (Room Database)**

**Arquivo:** `data/local/TaskGoDatabase.kt`

```kotlin
@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        UserProfileEntity::class,
        PurchaseOrderEntity::class,
        ServiceOrderEntity::class,
        ProposalEntity::class,
        MessageEntity::class,
        AddressEntity::class,
        CardEntity::class,
        TrackingEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TaskGoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun serviceOrderDao(): ServiceOrderDao
    abstract fun proposalDao(): ProposalDao
    abstract fun messageDao(): MessageDao
    abstract fun addressDao(): AddressDao
    abstract fun cardDao(): CardDao
    abstract fun trackingDao(): TrackingDao
    abstract fun syncQueueDao(): SyncQueueDao
}
```

**Uso do Cache:**
- ✅ Armazena dados para acesso offline
- ✅ Sincronização inicial no primeiro login
- ✅ Sincronização incremental via `SyncManager`
- ✅ WorkManager para sincronização em background

---

#### **C. Sincronização Inicial**

**Arquivo:** `core/sync/InitialDataSyncManager.kt`

```kotlin
@Singleton
class InitialDataSyncManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val productsRepository: ProductsRepository,
    private val ordersRepository: OrdersRepository,
    private val addressRepository: AddressRepository,
    private val cardRepository: CardRepository,
    private val userRepository: UserRepository,
    // ...
) {
    suspend fun syncAllUserData(): Boolean = withContext(Dispatchers.IO) {
        val userId = firebaseAuth.currentUser?.uid ?: return@withContext false
        
        try {
            // ✅ Executar todas as sincronizações em paralelo
            val results = awaitAll(
                async { syncUserProfile(userId) },
                async { syncProducts(userId) },
                async { syncOrders(userId) },
                async { syncAddresses(userId) },
                async { syncCards(userId) }
            )
            
            results.all { it }
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante sincronização: ${e.message}", e)
            false
        }
    }
}
```

**Quando é chamado:**
- ✅ Primeiro login do usuário (após autenticação)
- ✅ No `SplashViewModel` ao verificar estado de autenticação
- ✅ Após login bem-sucedido (se necessário)

---

### 🎨 **Camada de Apresentação (Presentation Layer)**

#### **A. ViewModels**

**Padrão MVVM:**

```kotlin
@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val functionsService: FirebaseFunctionsService,
    private val authRepository: FirebaseAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    fun createProduct(product: Product) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // ✅ Chamar Cloud Function para criar produto
            val result = functionsService.createProduct(
                title = product.title,
                description = product.description,
                category = product.category,
                price = product.price,
                images = product.images,
                active = product.active
            )
            
            result.fold(
                onSuccess = { data ->
                    val productId = data["productId"] as? String
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        productId = productId
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
            )
        }
    }
}
```

**Fluxo típico:**
1. ✅ ViewModel recebe ação do usuário
2. ✅ Valida dados localmente
3. ✅ Chama repositório ou Cloud Function
4. ✅ Atualiza UI State
5. ✅ Observa mudanças em repositórios (via Flows)

---

#### **B. Screens (Jetpack Compose)**

```kotlin
@Composable
fun ProductFormScreen(
    viewModel: ProductFormViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI Components...
    
    Button(
        onClick = {
            viewModel.createProduct(product)
        },
        enabled = !uiState.isLoading
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Text("Criar Produto")
        }
    }
    
    // Tratamento de erros
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Mostrar snackbar de erro
        }
    }
    
    // Navegação após sucesso
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }
}
```

---

## 4️⃣ FIREBASE SERVICES

### 🔧 **Configuração (DI)**

**Arquivo:** `di/FirebaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = if (BuildConfig.USE_EMULATOR) {
            FirebaseFirestore.getInstance().apply {
                useEmulator("10.0.2.2", 8080)
            }
        } else {
            FirebaseFirestore.getInstance()
        }
        
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
        
        return firestore
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return if (BuildConfig.USE_EMULATOR) {
            FirebaseFunctions.getInstance().apply {
                useEmulator("10.0.2.2", 5001)
            }
        } else {
            FirebaseFunctions.getInstance(
                FirebaseApp.getInstance(),
                BuildConfig.FIREBASE_FUNCTIONS_REGION
            )
        }
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return if (BuildConfig.USE_EMULATOR) {
            FirebaseStorage.getInstance().apply {
                useEmulator("10.0.2.2", 9199)
            }
        } else {
            FirebaseStorage.getInstance()
        }
    }
}
```

**Características:**
- ✅ Suporte a emulador local (desenvolvimento)
- ✅ Configuração de produção com região
- ✅ Cache offline habilitado no Firestore
- ✅ Singleton para todos os serviços

---

## 5️⃣ REPOSITÓRIOS

### 📚 **Hierarquia de Repositórios**

```
Domain Layer (Interfaces)
    ↓
Data Layer (Implementações)
    ├── FirestoreXXXRepository (leitura direta)
    ├── XXXRepositoryImpl (combina Firestore + Cache + Functions)
    └── Local Cache (Room Database)
```

**Exemplo:** `ProductsRepository`

```kotlin
// Domain Interface
interface ProductsRepository {
    fun observeProducts(): Flow<List<Product>>
    suspend fun getProduct(id: String): Product?
    suspend fun createProduct(product: Product): Result<String>
}

// Data Implementation
class FirestoreProductsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cartDao: CartDao
) : ProductsRepository {
    
    private val productsCollection = firestore.collection("products")
    
    override fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("active", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { mapProduct(doc.id, it) }
                } ?: emptyList()
                
                trySend(products)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun createProduct(product: Product): Result<String> {
        // ✅ Usar Cloud Function (escrita bloqueada no Firestore)
        return functionsService.createProduct(...)
    }
}
```

---

## 6️⃣ VIEWMODELS

### 🎯 **Padrão de Uso**

**Exemplo:** `ProductFormViewModel`

```kotlin
@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val functionsService: FirebaseFunctionsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    // Observar produtos existentes
    val products: Flow<List<Product>> = productsRepository.observeProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createProduct(product: Product) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // ✅ Chamar Cloud Function
            val result = functionsService.createProduct(
                title = product.title,
                description = product.description,
                category = product.category,
                price = product.price,
                images = product.images,
                active = product.active
            )
            
            result.fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
            )
        }
    }
}
```

**Características:**
- ✅ Injeção de dependências via Hilt
- ✅ StateFlow para UI State
- ✅ Flows para observação de dados
- ✅ Corrotinas para operações assíncronas
- ✅ Tratamento de erros robusto

---

## 7️⃣ CACHE LOCAL

### 💾 **Armazenamento Local**

#### **A. Room Database**

```kotlin
@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        UserProfileEntity::class,
        PurchaseOrderEntity::class,
        // ...
    ],
    version = 1
)
abstract class TaskGoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    // ...
}
```

#### **B. DataStore (Preferences)**

**Arquivo:** `data/local/datastore/PreferencesManager.kt`

```kotlin
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE_KEY] ?: "pt" }
    val theme: Flow<String> = dataStore.data.map { it[THEME_KEY] ?: "system" }
    val promosEnabled: Flow<Boolean> = dataStore.data.map { it[PROMOS_ENABLED_KEY] ?: true }
    // ...
    
    suspend fun updateLanguage(language: String) {
        dataStore.edit { it[LANGUAGE_KEY] = language }
    }
    // ...
}
```

**Uso:**
- ✅ Preferências do usuário
- ✅ Configurações de notificação
- ✅ Privacidade
- ✅ Biometria
- ✅ Flags de sincronização

---

## 8️⃣ FLUXOS DE DADOS

### 🔄 **Fluxo Completo: Criar Produto**

```
1. Usuário preenche formulário
   ↓
2. ViewModel: ProductFormViewModel.createProduct()
   ↓
3. Validação local dos dados
   ↓
4. FirebaseFunctionsService.createProduct()
   ↓
5. Cloud Function: createProduct
   - Valida App Check ✅
   - Valida Autenticação ✅
   - Valida Custom Claims (role) ✅
   - Cria produto no Firestore
   - Retorna productId
   ↓
6. ViewModel recebe resultado
   ↓
7. UI atualiza (sucesso/erro)
   ↓
8. Firestore atualizado → SnapshotListener detecta mudança
   ↓
9. ProductsRepository.observeProducts() → Flow emite novo produto
   ↓
10. ViewModel observa Flow → UI atualiza automaticamente
```

---

### 🔄 **Fluxo Completo: Leitura de Dados**

```
1. Screen observa ViewModel
   ↓
2. ViewModel observa Repository
   ↓
3. Repository observa Firestore (addSnapshotListener)
   ↓
4. Firestore emite mudanças → Flow
   ↓
5. Repository transforma dados (mapper)
   ↓
6. ViewModel recebe dados via Flow
   ↓
7. UI atualiza automaticamente (Compose recomposition)
```

---

### 🔄 **Fluxo Completo: Autenticação**

```
1. Usuário faz login
   ↓
2. FirebaseAuthRepository.signInWithEmail()
   ↓
3. Firebase Auth valida credenciais
   ↓
4. Retorna FirebaseUser com UID
   ↓
5. LoginViewModel verifica se usuário existe no Firestore
   ↓
6. Se não existe: cria documento em /users/{uid}
   ↓
7. Cloud Function onUserCreate:
   - Define Custom Claim role: 'user'
   - Atualiza documento do usuário
   ↓
8. Se pendingAccountType == true:
   - Mostrar dialog de seleção de tipo de conta
   ↓
9. Usuário seleciona tipo:
   - Chamar Cloud Function setInitialUserRole(role)
   - Atualizar Custom Claim
   ↓
10. Refresh token (getIdToken(true))
   ↓
11. Token JWT inclui Custom Claims atualizados
   ↓
12. Firestore Rules verificam request.auth.token.role
   ↓
13. Acesso autorizado ✅
```

---

## 📊 RESUMO

### ✅ **Pontos Principais:**

1. **Autenticação:**
   - ✅ Firebase Auth direto (email/senha, Google)
   - ✅ App Check habilitado em produção
   - ✅ Custom Claims para roles
   - ✅ Refresh de tokens para incluir Custom Claims

2. **Comunicação com Backend:**
   - ✅ Cloud Functions para escrita (validações de negócio)
   - ✅ Firestore direto para leitura (observação em tempo real)
   - ✅ Tratamento robusto de erros
   - ✅ Logs detalhados

3. **Cache Local:**
   - ✅ Room Database para cache de dados
   - ✅ DataStore para preferências
   - ✅ Sincronização inicial no primeiro login
   - ✅ Sincronização incremental em background

4. **Arquitetura:**
   - ✅ MVVM + Clean Architecture
   - ✅ Repository Pattern
   - ✅ Dependency Injection (Hilt)
   - ✅ Reactive Programming (Kotlin Flows)

5. **Segurança:**
   - ✅ App Check em todas as Cloud Functions
   - ✅ Custom Claims verificados no backend
   - ✅ Firestore Rules como última linha de defesa
   - ✅ Validações no cliente e servidor

---

**Este relatório documenta como o frontend Android se comunica com o backend Firebase.**
