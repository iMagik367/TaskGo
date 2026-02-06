# Guia de Atualização: Substituir FirebaseAuth por TokenManager

## Telas que precisam ser atualizadas

### 1. AccountScreen.kt
**Localização:** `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/AccountScreen.kt`

**Substituir:**
```kotlin
import com.google.firebase.auth.FirebaseAuth

val currentUser = FirebaseAuth.getInstance().currentUser
```

**Por:**
```kotlin
import com.taskgoapp.taskgo.core.auth.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context

@Inject lateinit var tokenManager: TokenManager

val currentUser = tokenManager.getCurrentUser()
val isAuthenticated = tokenManager.isAuthenticated()
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
private val auth = FirebaseAuth.getInstance()
val currentUser = auth.currentUser
```

**Por:**
```kotlin
import com.taskgoapp.taskgo.data.repository.AuthRepository

@Inject constructor(
    private val authRepository: AuthRepository
)

val currentUser = authRepository.getCurrentUser()
val isAuthenticated = authRepository.isAuthenticated()
```

## Padrão de substituição

### Verificar autenticação
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

### Obter ID do usuário
```kotlin
// Antes
val userId = FirebaseAuth.getInstance().currentUser?.uid

// Depois
val userId = tokenManager.getCurrentUserId()
```

### Obter email do usuário
```kotlin
// Antes
val email = FirebaseAuth.getInstance().currentUser?.email

// Depois
val email = tokenManager.getCurrentUser()?.email
```

## Injeção de dependência

Adicione TokenManager como dependência injetada:

```kotlin
@Inject
lateinit var tokenManager: TokenManager

// Ou no construtor
@Inject constructor(
    private val tokenManager: TokenManager
)
```

## Nota importante

- TokenManager já está configurado no NetworkModule
- Não é necessário criar nova instância, apenas injetar
- TokenManager gerencia automaticamente a renovação de tokens
