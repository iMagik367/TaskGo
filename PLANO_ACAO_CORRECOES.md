# 🔧 Plano de Ação - Correções para Distribuição

Este documento contém instruções passo a passo para corrigir todos os problemas identificados.

---

## 🔴 FASE 1: CORREÇÕES CRÍTICAS

### 1. Implementar Firebase App Check

**Por quê:** Protege contra tráfego abusivo e bot attacks.

**Passos:**

1. **Adicionar dependência no `app/build.gradle.kts`:**
```kotlin
implementation("com.google.firebase:firebase-appcheck-ktx:18.1.1")
implementation("com.google.firebase:firebase-appcheck-playintegrity:18.1.1")
```

2. **Inicializar App Check no `TaskGoApp.kt`:**
```kotlin
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

override fun onCreate() {
    super.onCreate()
    
    // Initialize App Check
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        PlayIntegrityAppCheckProviderFactory.getInstance()
    )
}
```

3. **Configurar no Firebase Console:**
   - Acesse Firebase Console > App Check
   - Habilite Play Integrity para Android
   - Configure debug tokens para desenvolvimento

---

### 2. Configurar Facebook App ID ou Remover

**Opção A: Se usar Facebook Login:**

1. **Obter App ID e Client Token do Facebook Developers:**
   - Acesse https://developers.facebook.com
   - Crie/configure um app
   - Obtenha App ID e Client Token

2. **Atualizar `app/src/main/res/values/auth_config.xml`:**
```xml
<string name="facebook_app_id">SEU_APP_ID_REAL</string>
<string name="facebook_client_token">SEU_CLIENT_TOKEN_REAL</string>
```

**Opção B: Se NÃO usar Facebook Login:**

1. **Remover meta-data do AndroidManifest.xml:**
   - Remover linhas 31-36 do `AndroidManifest.xml`

2. **Remover arquivo `auth_config.xml`** (se não for usado)

3. **Remover dependência do Facebook SDK** (se existir)

---

### 3. Desabilitar Cleartext Traffic

**Passos:**

1. **Criar `app/src/main/res/xml/network_security_config.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Permitir apenas HTTPS em produção -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Se precisar de HTTP para desenvolvimento local -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

2. **Atualizar `AndroidManifest.xml`:**
```xml
<application
    ...
    android:usesCleartextTraffic="false"
    android:networkSecurityConfig="@xml/network_security_config">
```

---

### 4. Alterar Application ID

**Passos:**

1. **Definir novo ID no `app/build.gradle.kts`:**
```kotlin
defaultConfig {
    applicationId = "br.com.taskgo.app"  // OU "com.taskgo.app"
    // ... resto da config
}
```

2. **Atualizar package no código:**
   - Refatorar todos os packages de `com.example.taskgoapp` para `br.com.taskgo.app`
   - Atualizar imports em todos os arquivos
   - Atualizar AndroidManifest.xml

3. **Atualizar Firebase Project:**
   - Adicionar novo app Android no Firebase Console com o novo package name
   - Baixar novo `google-services.json`
   - Substituir o arquivo antigo

**⚠️ ATENÇÃO:** Isso requer criar um novo app no Firebase ou migrar o existente.

---

### 5. Configurar Signing Configs

**Passos:**

1. **Criar keystore:**
```bash
keytool -genkeypair -v -storetype PKCS12 -keystore taskgo-release.jks -alias taskgo -keyalg RSA -keysize 2048 -validity 10000
```

2. **Adicionar ao `gradle.properties` (não commitar no git):**
```properties
TASKGO_RELEASE_STORE_FILE=taskgo-release.jks
TASKGO_RELEASE_KEY_ALIAS=taskgo
TASKGO_RELEASE_STORE_PASSWORD=sua_senha_aqui
TASKGO_RELEASE_KEY_PASSWORD=sua_senha_aqui
```

3. **Adicionar ao `.gitignore`:**
```
*.jks
*.keystore
```

4. **Configurar no `app/build.gradle.kts`:**
```kotlin
android {
    // ... outras configs
    
    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("TASKGO_RELEASE_STORE_FILE") as String ?: "release.jks")
            storePassword = project.findProperty("TASKGO_RELEASE_STORE_PASSWORD") as String ?: ""
            keyAlias = project.findProperty("TASKGO_RELEASE_KEY_ALIAS") as String ?: ""
            keyPassword = project.findProperty("TASKGO_RELEASE_KEY_PASSWORD") as String ?: ""
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## ⚠️ FASE 2: OTIMIZAÇÕES E SEGURANÇA

### 6. Habilitar Minify e Configurar ProGuard

**Passos:**

1. **Habilitar minify no `app/build.gradle.kts`:**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        // ... resto
    }
}
```

2. **Adicionar regras ProGuard completas em `app/proguard-rules.pro`:**

```proguard
# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Modelos de dados (ajustar conforme seus models)
-keep class com.example.taskgoapp.data.firestore.models.** { *; }
-keep class com.example.taskgoapp.core.model.** { *; }

# Preservar line numbers para stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

---

### 7. Validar Variáveis de Ambiente nas Functions

**Passos:**

1. **Criar `functions/src/utils/env.ts`:**
```typescript
export function getRequiredEnv(key: string): string {
  const value = process.env[key];
  if (!value) {
    throw new Error(`Missing required environment variable: ${key}`);
  }
  return value;
}

export const env = {
  OPENAI_API_KEY: getRequiredEnv('OPENAI_API_KEY'),
  STRIPE_SECRET_KEY: getRequiredEnv('STRIPE_SECRET_KEY'),
  STRIPE_WEBHOOK_SECRET: getRequiredEnv('STRIPE_WEBHOOK_SECRET'),
};
```

2. **Atualizar `functions/src/ai-chat.ts`:**
```typescript
import {env} from './utils/env';

const openai = new OpenAI({
  apiKey: env.OPENAI_API_KEY,
});
```

3. **Atualizar `functions/src/payments.ts` e `functions/src/webhooks.ts`** de forma similar.

4. **Configurar variáveis no Firebase:**
```bash
firebase functions:config:set \
  openai.api_key="sua_chave_aqui" \
  stripe.secret_key="sua_chave_aqui" \
  stripe.webhook_secret="seu_secret_aqui"
```

---

### 8. Configurar API_BASE_URL para Produção

**Passos:**

1. **Atualizar `app/build.gradle.kts`:**
```kotlin
buildTypes {
    debug {
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8091/v1/\"")
        buildConfigField("boolean", "USE_EMULATOR", "true")
    }
    release {
        buildConfigField("String", "API_BASE_URL", "\"https://api.taskgo.com/v1/\"")
        buildConfigField("boolean", "USE_EMULATOR", "false")
        // ... resto
    }
}
```

2. **Atualizar código que usa API_BASE_URL** para usar BuildConfig.

---

### 9. Incrementar Version Code/Name

**Passos:**

1. **Atualizar `app/build.gradle.kts`:**
```kotlin
defaultConfig {
    versionCode = 2  // Incrementar a cada release
    versionName = "1.0.1"  // Versão semântica
}
```

---

### 10. Implementar Lógica de Refund Completa

**Passos:**

1. **Atualizar `functions/src/payments.ts`:**
```typescript
// Substituir TODO por implementação real
export const requestRefund = functions.https.onCall(async (data, context) => {
  // ... código existente ...
  
  const paymentDoc = paymentsSnapshot.docs[0];
  const paymentData = paymentDoc.data();
  
  // Criar refund no Stripe
  try {
    const refund = await stripe.refunds.create({
      payment_intent: paymentData.stripePaymentIntentId,
      reason: reason || 'requested_by_customer',
      metadata: {
        orderId: orderId,
        requestedBy: context.auth!.uid,
      },
    });
    
    // Atualizar payment status
    await paymentDoc.ref.update({
      status: PAYMENT_STATUS.REFUNDED,
      refundId: refund.id,
      refundedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    
    // Atualizar order status
    await db.collection(COLLECTIONS.ORDERS).doc(orderId).update({
      status: 'refunded',
      refundedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    
    return {success: true, refundId: refund.id};
  } catch (error) {
    functions.logger.error('Error processing refund:', error);
    throw new functions.https.HttpsError('internal', 'Failed to process refund');
  }
});
```

---

### 11. Tornar Região do Firebase Configurável

**Passos:**

1. **Atualizar `app/build.gradle.kts`:**
```kotlin
buildConfigField("String", "FIREBASE_FUNCTIONS_REGION", "\"us-central1\"")
```

2. **Atualizar `FirebaseModule.kt`:**
```kotlin
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
```

---

## 🔴 FASE 1B: CORREÇÕES CRÍTICAS DO FRONTEND

### 14. Implementar TODOs Críticos

**Prioridade:** MUITO ALTA - Bloqueia funcionalidades essenciais

**Passos:**

1. **HomeScreen - Carregar produtos e categorias:**

```kotlin
// HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val categoriesRepository: CategoriesRepository // Criar se não existir
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadProducts()
        loadCategories()
    }
    
    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                productsRepository.observeAllProducts().collect { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoriesRepository.observeCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}
```

```kotlin
// HomeScreen.kt - Atualizar
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Usar uiState.products e uiState.categories
}
```

2. **CartScreen - Implementar funcionalidades do carrinho:**

```kotlin
// CartViewModel.kt
fun increaseQuantity(productId: String) {
    viewModelScope.launch {
        cartRepository.increaseQuantity(productId)
    }
}

fun decreaseQuantity(productId: String) {
    viewModelScope.launch {
        cartRepository.decreaseQuantity(productId)
    }
}

fun removeItem(productId: String) {
    viewModelScope.launch {
        cartRepository.removeItem(productId)
    }
}
```

3. **MessagesScreen - Carregar mensagens:**

```kotlin
// MessagesViewModel.kt
val conversations: StateFlow<List<Conversation>> = 
    messagesRepository.observeConversations()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
```

4. **ChatScreen - Carregar mensagens da conversa:**

```kotlin
// ChatViewModel.kt
fun loadMessages(conversationId: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            messagesRepository.observeMessages(conversationId).collect { messages ->
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message,
                isLoading = false
            )
        }
    }
}
```

5. **TaskGoNavGraph - Implementar lógicas pendentes:**

```kotlin
// Implementar exclusão de produto
onDeleteClick = { productId ->
    viewModel.deleteProduct(productId)
}

// Implementar aceitar/rejeitar proposta
onAcceptProposal = { proposalId ->
    viewModel.acceptProposal(proposalId)
}

onRejectProposal = { proposalId ->
    viewModel.rejectProposal(proposalId)
}
```

---

### 15. Mover Strings Hardcoded para Resources

**Passos:**

1. **Identificar todas as strings hardcoded:**
   - `HomeScreen.kt`: "Ações Rápidas"
   - `PaymentMethodScreen.kt`: "Método de pagamento"
   - `SupportScreen.kt`: Várias strings
   - `SobreScreen.kt`: "© 2024 TaskGo..."
   - E outras

2. **Adicionar ao `strings.xml`:**

```xml
<string name="home_quick_actions">Ações Rápidas</string>
<string name="payment_method_title">Método de pagamento</string>
<string name="about_copyright">© 2024 TaskGo. Todos os direitos reservados.</string>
<string name="form_fill_all_fields">Preencha corretamente todos os campos.</string>
```

3. **Substituir no código:**

```kotlin
// Antes
Text("Ações Rápidas")

// Depois
Text(stringResource(R.string.home_quick_actions))
```

---

### 16. Padronizar Tratamento de Erros

**Passos:**

1. **Criar `ErrorHandler.kt`:**

```kotlin
object ErrorHandler {
    fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is FirebaseException -> FirebaseErrorHandler.getErrorMessage(error)
            is NetworkException -> "Erro de conexão. Verifique sua internet."
            is TimeoutException -> "Tempo de espera excedido. Tente novamente."
            else -> error.message ?: "Erro desconhecido"
        }
    }
    
    fun handleError(error: Throwable, onError: (String) -> Unit) {
        val message = getErrorMessage(error)
        onError(message)
        Log.e("ErrorHandler", message, error)
    }
}
```

2. **Criar `UiState` base:**

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

3. **Usar em todos os ViewModels:**

```kotlin
private val _uiState = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)

fun loadData() {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        try {
            val data = repository.getData()
            _uiState.value = UiState.Success(data)
        } catch (e: Exception) {
            ErrorHandler.handleError(e) { message ->
                _uiState.value = UiState.Error(message)
            }
        }
    }
}
```

---

### 17. Adicionar Estados de Loading/Error Consistentes

**Passos:**

1. **Criar componentes reutilizáveis:**

```kotlin
// Components.kt
@Composable
fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                contentDescription(AccessibilityStrings.loadingState())
            }
        )
    }
}

@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.ui_retry))
        }
    }
}
```

2. **Usar em todas as telas:**

```kotlin
when (val state = uiState.value) {
    is UiState.Loading -> LoadingState()
    is UiState.Error -> ErrorState(
        error = state.message,
        onRetry = { viewModel.loadData() }
    )
    is UiState.Success -> SuccessContent(data = state.data)
}
```

---

### 18. Completar Acessibilidade

**Passos:**

1. **Adicionar contentDescription em todos os ícones/botões:**

```kotlin
IconButton(
    onClick = { },
    modifier = Modifier.semantics {
        contentDescription(AccessibilityStrings.NOTIFICATIONS_BUTTON)
    }
) {
    Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = null // Já está no botão pai
    )
}
```

2. **Adicionar labels semânticos em campos:**

```kotlin
TextField(
    value = email,
    onValueChange = { },
    label = { Text("Email") },
    modifier = Modifier.semantics {
        contentDescription(AccessibilityStrings.emailField())
    }
)
```

3. **Testar com TalkBack ativado**

---

## ⚠️ FASE 2B: MELHORIAS DO FRONTEND

### 19. Implementar Validação de Formulários

**Passos:**

1. **Criar `Validators.kt` (já existe, expandir):**

```kotlin
object Validators {
    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult.Error("Email é obrigatório")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult.Error("Email inválido")
        }
        return ValidationResult.Success
    }
    
    fun validatePassword(password: String): ValidationResult {
        if (password.length < 6) {
            return ValidationResult.Error("Senha deve ter pelo menos 6 caracteres")
        }
        return ValidationResult.Success
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

2. **Usar nos formulários:**

```kotlin
var email by remember { mutableStateOf("") }
var emailError by remember { mutableStateOf<String?>(null) }

TextField(
    value = email,
    onValueChange = { 
        email = it
        emailError = null
    },
    isError = emailError != null,
    supportingText = emailError?.let { { Text(it) } }
)

// Validar ao submeter
val emailValidation = Validators.validateEmail(email)
if (emailValidation is ValidationResult.Error) {
    emailError = emailValidation.message
    return
}
```

---

### 20. Implementar Suporte Offline

**Passos:**

1. **Usar Room para cache local:**

```kotlin
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Double,
    // ... outros campos
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun observeAll(): Flow<List<ProductEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)
}
```

2. **Sincronizar com Firestore:**

```kotlin
class ProductsRepository @Inject constructor(
    private val firestore: FirestoreProductsRepository,
    private val local: ProductDao
) {
    fun observeProducts(): Flow<List<Product>> = flow {
        // Emitir dados locais primeiro
        local.observeAll().collect { emit(it.toDomain()) }
        
        // Sincronizar com Firestore quando online
        try {
            firestore.observeAllProducts().collect { remote ->
                local.insertAll(remote.toEntity())
            }
        } catch (e: Exception) {
            // Offline - usar apenas dados locais
        }
    }
}
```

---

## 📝 FASE 3: MELHORIAS E POLIMENTO

### 12. Revisar Permissões do AndroidManifest

**Verificar se todas as permissões são necessárias:**
- `POST_NOTIFICATIONS` - ✅ Necessário
- `READ_MEDIA_IMAGES` - ✅ Necessário
- `CAMERA` - ✅ Necessário (se usar câmera)
- `ACCESS_COARSE_LOCATION` - ⚠️ Verificar se realmente necessário
- `ACCESS_FINE_LOCATION` - ⚠️ Verificar se realmente necessário

**Adicionar justificativa no Google Play Console** para cada permissão.

---

### 13. Suportar Múltiplas Moedas

**Passos:**

1. **Adicionar campo de moeda no modelo de usuário**
2. **Atualizar `functions/src/payments.ts`** para usar moeda do usuário
3. **Configurar suporte a BRL no Stripe**

---

## ✅ CHECKLIST FINAL

Antes de publicar, verificar:

### Configuração e Build
- [ ] Todos os problemas críticos corrigidos
- [ ] App compila sem erros
- [ ] App Check configurado e testado
- [ ] Signing config funcionando
- [ ] Build de release gerado com sucesso
- [ ] Application ID alterado
- [ ] Version code/name incrementados

### Funcionalidades
- [ ] Todos os TODOs críticos implementados
- [ ] HomeScreen carrega dados reais
- [ ] Carrinho funciona completamente
- [ ] Mensagens carregam do backend
- [ ] Todas as navegações funcionam
- [ ] Formulários validados

### Qualidade do Código
- [ ] Todas as strings em resources
- [ ] Tratamento de erros padronizado
- [ ] Estados de loading/error consistentes
- [ ] Acessibilidade completa
- [ ] Sem strings hardcoded

### Testes
- [ ] Testado em dispositivos reais
- [ ] Testado com diferentes tamanhos de tela
- [ ] Testado offline
- [ ] Testado com TalkBack
- [ ] Sem crashes conhecidos

### Backend e Segurança
- [ ] Variáveis de ambiente configuradas
- [ ] Firebase Functions deployadas
- [ ] Firestore rules testadas
- [ ] Storage rules testadas
- [ ] ProGuard não quebra funcionalidades

### Performance
- [ ] Performance aceitável
- [ ] Imagens otimizadas
- [ ] Listas com lazy loading
- [ ] Cache implementado

### Google Play
- [ ] Screenshots preparados
- [ ] Descrição completa
- [ ] Política de privacidade
- [ ] Termos de serviço
- [ ] Permissões justificadas

---

**Última atualização:** 2024

