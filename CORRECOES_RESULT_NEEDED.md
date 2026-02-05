# CORREÇÕES NECESSÁRIAS - RESULT TYPE

## Problema
O código está usando métodos do `kotlin.Result` (standard library) em objetos do tipo `com.taskgoapp.taskgo.core.model.Result` (customizado).

## Solução
1. Criar funções de extensão para o Result customizado (JÁ FEITO: ResultExtensions.kt)
2. Adicionar imports corretos em todos os arquivos
3. Corrigir todos os usos de `fold`, `onSuccess`, `onFailure`, `map`, `getOrElse`, `getOrThrow`

## Arquivos que precisam de correção:

### Já corrigidos:
- ✅ PaymentGateway.kt
- ✅ FirestoreServicesRepository.kt
- ✅ FirestoreStoriesRepository.kt
- ✅ ServiceRepositoryImpl.kt
- ✅ CadastrarEnderecoScreen.kt (EntryPointAccessors)
- ✅ HomeScreen.kt (EntryPointAccessors)
- ✅ MyDataScreen.kt (EntryPointAccessors)
- ✅ PrivacyScreen.kt (EntryPointAccessors)
- ✅ SecuritySettingsScreen.kt (EntryPointAccessors)

### Ainda precisam correção:
- ⚠️ SettingsUseCase.kt
- ⚠️ LoginViewModel.kt
- ⚠️ SignupViewModel.kt
- ⚠️ TwoFactorAuthViewModel.kt
- ⚠️ IdentityVerificationViewModel.kt
- ⚠️ ChatAIViewModel.kt
- ⚠️ ChatListViewModel.kt
- ⚠️ ProductFormViewModel.kt
- ⚠️ ServiceFormViewModel.kt
- ⚠️ ProposalUseCase.kt
- ⚠️ PixPaymentViewModel.kt
- ⚠️ StripePaymentHelperViewModel.kt
- ⚠️ ShipmentScreen.kt
- ⚠️ CreateWorkOrderScreen.kt

## Padrão de correção:

### 1. Adicionar imports:
```kotlin
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.fold
import com.taskgoapp.taskgo.core.model.onSuccess
import com.taskgoapp.taskgo.core.model.onFailure
import com.taskgoapp.taskgo.core.model.map
import com.taskgoapp.taskgo.core.model.getOrElse
import com.taskgoapp.taskgo.core.model.getOrThrow
```

### 2. Corrigir usos:
- `Result.success(x)` → `Result.Success(x)`
- `Result.failure(e)` → `Result.Error(e)`
- `result.fold(onSuccess = { ... }, onFailure = { ... })` → Adicionar tipos explícitos: `{ data: Type -> ... }` e `{ error: Throwable -> ... }`
- `result.onSuccess { ... }` → `result.onSuccess { data: Type -> ... }`
- `result.onFailure { ... }` → `result.onFailure { error: Throwable -> ... }`
- `result.map { ... }` → `result.map { data: Type -> ... }`
- `result.getOrElse { ... }` → `result.getOrElse { error: Throwable -> ... }`

### 3. Corrigir EntryPointAccessors:
- `dagger.hilt.EntryPointAccessors` → `dagger.hilt.android.EntryPointAccessors`
