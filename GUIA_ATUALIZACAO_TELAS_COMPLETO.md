# Guia Completo: Atualizar Telas para Usar TokenManager

## Arquivos que Precisam ser Atualizados

### 1. AccountScreen.kt
**Localização:** `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/AccountScreen.kt`

**Substituir:**
```kotlin
import com.google.firebase.auth.FirebaseAuth

val currentUser = FirebaseAuth.getInstance().currentUser
if (currentUser != null) {
    // código
}
```

**Por:**
```kotlin
import com.taskgoapp.taskgo.core.auth.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context

@Inject
lateinit var tokenManager: TokenManager

val currentUser = tokenManager.getCurrentUser()
if (tokenManager.isAuthenticated()) {
    // código
}
```

### 2. MyDataScreen.kt
**Localização:** `app/src/main/java/com/taskgoapp/taskgo/feature/profile/presentation/MyDataScreen.kt`

**Substituir:**
```kotlin
FirebaseAuth.getInstance().currentUser?.uid
```

**Por:**
```kotlin
tokenManager.getCurrentUserId()
```

### 3. ProductsScreen.kt
**Localização:** `app/src/main/java/com/taskgoapp/taskgo/feature/products/presentation/ProductsScreen.kt`

**Substituir:**
```kotlin
FirebaseAuth.getInstance().currentUser
```

**Por:**
```kotlin
tokenManager.getCurrentUser()
```

### 4. ServiceFormScreen.kt
**Localização:** `app/src/main/java/com/taskgoapp/taskgo/feature/services/presentation/ServiceFormScreen.kt`

**Substituir:**
```kotlin
FirebaseAuth.getInstance().currentUser?.uid
```

**Por:**
```kotlin
tokenManager.getCurrentUserId()
```

### 5. SplashViewModel.kt
**Localização:** `app/src/main/java/com/taskgoapp/taskgo/feature/splash/presentation/SplashViewModel.kt`

**Substituir:**
```kotlin
import com.google.firebase.auth.FirebaseAuth

@HiltViewModel
class SplashViewModel @Inject constructor(
    // ...
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // usuário logado
        }
    }
}
```

**Por:**
```kotlin
import com.taskgoapp.taskgo.data.repository.AuthRepository

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    init {
        if (authRepository.isAuthenticated()) {
            val currentUser = authRepository.getCurrentUser()
            // usuário logado
        }
    }
}
```

## Padrões de Substituição

### Verificar Autenticação
```kotlin
// Antes
if (FirebaseAuth.getInstance().currentUser != null) {
    // usuário autenticado
}

// Depois
if (tokenManager.isAuthenticated()) {
    // usuário autenticado
}
```

### Obter ID do Usuário
```kotlin
// Antes
val userId = FirebaseAuth.getInstance().currentUser?.uid

// Depois
val userId = tokenManager.getCurrentUserId()
```

### Obter Email do Usuário
```kotlin
// Antes
val email = FirebaseAuth.getInstance().currentUser?.email

// Depois
val email = tokenManager.getCurrentUser()?.email
```

### Obter Dados Completos do Usuário
```kotlin
// Antes
val user = FirebaseAuth.getInstance().currentUser
val uid = user?.uid
val email = user?.email
val displayName = user?.displayName

// Depois
val user = tokenManager.getCurrentUser()
val uid = user?.id
val email = user?.email
val displayName = user?.displayName
```

## Injeção de Dependência

### Em Composables (Telas)
```kotlin
@Composable
fun MyScreen(
    tokenManager: TokenManager = hiltViewModel().tokenManager
) {
    // ou
    val tokenManager: TokenManager = remember { 
        TokenManager(LocalContext.current) 
    }
}
```

### Em ViewModels
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {
    // usar tokenManager diretamente
}
```

### Em Classes Normais
```kotlin
class MyClass @Inject constructor(
    private val tokenManager: TokenManager
) {
    // usar tokenManager diretamente
}
```

## Notas Importantes

1. **TokenManager já está configurado** - Não precisa criar nova instância
2. **Mantém select boxes** - Não remover seletores de cidade/estado
3. **Biometria continua funcionando** - Usa email salvo localmente
4. **Tokens são renovados automaticamente** - TokenManager gerencia isso

## Checklist

- [ ] AccountScreen.kt atualizado
- [ ] MyDataScreen.kt atualizado
- [ ] ProductsScreen.kt atualizado
- [ ] ServiceFormScreen.kt atualizado
- [ ] SplashViewModel.kt atualizado
- [ ] Testado login
- [ ] Testado logout
- [ ] Testado verificação de autenticação
