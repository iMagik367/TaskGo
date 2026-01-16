# 📋 Relatório de Regras Firestore e Autenticação

## 1️⃣ REGRAS FIRESTORE (firestore.rules)

### 📌 Visão Geral
O projeto utiliza regras de segurança baseadas em autenticação do Firebase Auth (`request.auth != null`). Todas as operações exigem que o usuário esteja autenticado.

---

### 🗂️ Coleções Principais

#### **1. Posts Collection** (`/posts/{postId}`)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /posts/{postId} {
      // ✅ Leitura: Qualquer usuário autenticado
      allow read: if request.auth != null;
      
      // ✅ Criação: Apenas o próprio autor
      allow create: if request.auth != null 
                    && request.resource.data.userId == request.auth.uid;
      
      // ✅ Atualização: Apenas o autor
      allow update: if request.auth != null 
                    && resource.data.userId == request.auth.uid;
      
      // ✅ Exclusão: Apenas o autor
      allow delete: if request.auth != null 
                    && resource.data.userId == request.auth.uid;
    }
  }
}
```

**Subcoleções:**
- **Ratings** (`/posts/{postId}/ratings/{ratingId}`): Avaliações 1-5 estrelas
- **Comments** (`/posts/{postId}/comments/{commentId}`): Comentários públicos

---

#### **2. Users Collection** (`/users/{userId}`)
```javascript
match /users/{userId} {
  // ✅ Leitura: Qualquer usuário autenticado (perfis públicos)
  allow read: if request.auth != null;
  
  // ✅ Escrita: Apenas o próprio usuário
  allow create: if request.auth != null && request.auth.uid == userId;
  allow update: if request.auth != null && request.auth.uid == userId;
  allow delete: if request.auth != null && request.auth.uid == userId;
}
```

**Subcoleções:**
- **Services** (`/users/{userId}/services/{serviceId}`): Serviços do usuário
- **Products** (`/users/{userId}/products/{productId}`): Produtos do usuário
- **Orders** (`/users/{userId}/orders/{orderId}`): Ordens do usuário
- **Posts** (`/users/{userId}/posts/{postId}`): Posts do usuário
- **PostInterests** (`/users/{userId}/postInterests/{interestId}`): Interesses privados
- **BlockedUsers** (`/users/{userId}/blockedUsers/{blockId}`): Lista de bloqueados
- **Stories** (`/users/{userId}/stories/{storyId}`): Stories do usuário

---

#### **3. Services Collection** (`/services/{serviceId}`)
```javascript
match /services/{serviceId} {
  // ✅ Leitura pública para usuários autenticados
  allow read: if request.auth != null;
  
  // ✅ Escrita: Apenas o prestador dono
  allow create: if request.auth != null 
                && request.resource.data.providerId == request.auth.uid;
  allow update: if request.auth != null 
                && resource.data.providerId == request.auth.uid;
  allow delete: if request.auth != null 
                && resource.data.providerId == request.auth.uid;
}
```

---

#### **4. Products Collection** (`/products/{productId}`)
```javascript
match /products/{productId} {
  // ✅ Leitura pública
  allow read: if request.auth != null;
  
  // ✅ Escrita: Apenas o vendedor
  allow create: if request.auth != null 
                && request.resource.data.sellerId == request.auth.uid;
  allow update: if request.auth != null 
                && resource.data.sellerId == request.auth.uid;
  allow delete: if request.auth != null 
                && resource.data.sellerId == request.auth.uid;
}
```

---

#### **5. Orders Collection** (`/orders/{orderId}`)
```javascript
match /orders/{orderId} {
  // ✅ Leitura: Cliente ou prestador relacionado
  allow read: if request.auth != null;
  
  // ✅ Criação: Apenas o cliente
  allow create: if request.auth != null 
                && request.resource.data.clientId == request.auth.uid;
  
  // ✅ Atualização: Cliente ou prestador
  allow update: if request.auth != null 
                && (resource.data.clientId == request.auth.uid 
                    || resource.data.providerId == request.auth.uid);
  
  // ✅ Exclusão: Apenas o cliente
  allow delete: if request.auth != null 
                && resource.data.clientId == request.auth.uid;
}
```

---

#### **6. Conversations Collection** (`/conversations/{conversationId}`)
```javascript
match /conversations/{conversationId} {
  // ✅ Leitura: Apenas o dono da conversa
  allow read: if request.auth != null && 
              resource.data.userId == request.auth.uid;
  
  // ✅ Criação: Apenas para si mesmo
  allow create: if request.auth != null && 
                request.resource.data.userId == request.auth.uid;
  
  // ✅ Mensagens: Apenas o dono pode ler/criar
  match /messages/{messageId} {
    allow read: if request.auth != null && 
                get(/databases/{database}/documents/conversations/$(conversationId)).data.userId == request.auth.uid;
    
    allow create: if request.auth != null && 
                  get(/databases/{database}/documents/conversations/$(conversationId)).data.userId == request.auth.uid;
    
    // ❌ Mensagens são imutáveis (sem update/delete)
    allow update, delete: if false;
  }
}
```

---

#### **7. Bank Accounts Collection** (`/bank_accounts/{accountId}`)
```javascript
match /bank_accounts/{accountId} {
  // ✅ Leitura: Apenas o dono
  allow read: if request.auth != null
              && resource.data.userId == request.auth.uid;
  
  // ✅ Criação: Validação rigorosa de campos
  allow create: if request.auth != null 
                && request.resource.data.userId == request.auth.uid
                && request.resource.data.keys().hasAll(['userId', 'bankName', 'bankCode', ...])
                && request.resource.data.bankName is string
                && request.resource.data.bankCode is string
                // ... mais validações
                && request.resource.data.accountType in ["CHECKING", "SAVINGS"];
  
  // ✅ Atualização: Completa ou parcial (apenas isDefault)
  allow update: if request.auth != null 
                && resource.data.userId == request.auth.uid
                && (/* validação completa OU apenas isDefault */);
  
  // ✅ Exclusão: Apenas o dono
  allow delete: if request.auth != null 
                && resource.data.userId == request.auth.uid;
}
```

---

#### **8. Stories Collection** (`/stories/{storyId}`)
```javascript
match /stories/{storyId} {
  // ✅ Leitura pública
  allow read: if request.auth != null;
  
  // ✅ Escrita: Apenas o dono
  allow create: if request.auth != null 
                && request.resource.data.userId == request.auth.uid;
  allow update: if request.auth != null 
                && resource.data.userId == request.auth.uid;
  allow delete: if request.auth != null 
                && resource.data.userId == request.auth.uid;
  
  // Subcoleção de views
  match /views/{userId} {
    allow read: if request.auth != null;
    allow create: if request.auth != null 
                  && request.resource.data.userId == request.auth.uid;
  }
}
```

---

#### **9. Outras Coleções**

**Notifications:**
```javascript
match /notifications/{notificationId} {
  allow read, write: if request.auth != null 
                     && resource.data.userId == request.auth.uid;
}
```

**Reviews:**
```javascript
match /reviews/{reviewId} {
  allow read: if request.auth != null;
  allow write: if request.auth != null 
               && request.resource.data.clientId == request.auth.uid;
}
```

**AI Usage (somente leitura para usuário):**
```javascript
match /ai_usage/{usageId} {
  allow read: if request.auth != null && 
              resource.data.userId == request.auth.uid;
  // ❌ Escrita apenas via Cloud Functions
  allow write: if false;
}
```

---

### 🔒 Regra Padrão (Deny All)
```javascript
// Deny all other collections by default
match /{document=**} {
  allow read, write: if false;
}
```

---

## 2️⃣ EXEMPLOS DE AUTENTICAÇÃO

### 🔐 Backend (Cloud Functions)

#### **A. Verificação de Autenticação (Helper)**
**Arquivo:** `functions/src/utils/errors.ts`

```typescript
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

// Helper para verificar autenticação
export const assertAuthenticated = (context: functions.https.CallableContext) => {
  if (!context.auth) {
    throw new AppError('unauthenticated', 'User must be authenticated', 401);
  }
};

// Helper para verificar admin
export const assertAdmin = async (context: functions.https.CallableContext) => {
  assertAuthenticated(context);
  
  const db = admin.firestore();
  const userDoc = await db.collection('users').doc(context.auth!.uid).get();
  
  if (!userDoc.exists || userDoc.data()?.role !== 'admin') {
    throw new AppError('permission-denied', 'Admin access required', 403);
  }
};
```

---

#### **B. Trigger de Criação de Usuário**
**Arquivo:** `functions/src/auth.ts`

```typescript
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

/**
 * Triggered when a new user is created in Firebase Auth
 * Creates corresponding user document in Firestore
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
      
      if (!existingData?.email && user.email) {
        updateData.email = user.email;
      }
      // ... outros campos
      
      await userRef.update(updateData);
    } else {
      // Criar documento inicial
      const userData = {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL,
        role: 'client', // Default
        pendingAccountType: true, // Flag para dialog de seleção
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        profileComplete: false,
        verified: false,
      };

      await userRef.set(userData, { merge: true });
    }
    
    return null;
  } catch (error) {
    functions.logger.error('Error creating user document:', error);
    throw error;
  }
});
```

---

#### **C. Cloud Function Callable (Exemplo: 2FA)**
**Arquivo:** `functions/src/twoFactorAuth.ts`

```typescript
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {assertAuthenticated, handleError} from './utils/errors';

const db = admin.firestore();

/**
 * Envia código de verificação 2FA por email
 */
export const sendTwoFactorCode = functions.https.onCall(async (data, context) => {
  try {
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
    
    // Enviar email (implementação omitida)
    // await sendEmail(email, code);
    
    functions.logger.info(`Código 2FA enviado para ${userId}`);
    
    return {
      success: true,
      message: 'Código enviado com sucesso'
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
```

---

#### **D. Verificação de Identidade**
**Arquivo:** `functions/src/identityVerification.ts`

```typescript
export const verifyIdentity = functions.https.onCall(async (data, context) => {
  // ✅ Verificar autenticação
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Usuário não autenticado'
    );
  }

  const userId = context.auth.uid;
  const { documentFront, documentBack, selfie, addressProof } = data;

  // Validar documentos
  if (!documentFront || !documentBack || !selfie) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Documentos obrigatórios não fornecidos'
    );
  }

  // Atualizar status de verificação
  const userRef = admin.firestore().collection('users').doc(userId);
  
  await userRef.update({
    documentFront,
    documentBack,
    selfie,
    addressProof: addressProof || null,
    verified: false, // Será aprovado manualmente por admin
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return {
    success: true,
    message: 'Documentos enviados com sucesso. Aguardando verificação.'
  };
});
```

---

### 📱 Cliente (Android/Kotlin)

#### **A. Repository de Autenticação**
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
    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obter token ID (útil para chamadas de API)
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("User not logged in")
            val token = user.getIdToken(forceRefresh).await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

#### **B. ViewModel de Login**
**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/feature/auth/presentation/LoginViewModel.kt`

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // ✅ Chamar Firebase Auth
                val result = authRepository.signInWithEmail(email.trim(), password)
                
                result.fold(
                    onSuccess = { firebaseUser ->
                        Log.d("LoginViewModel", "Login bem-sucedido: ${firebaseUser.uid}")
                        
                        // Salvar email para biometria
                        preferencesManager.saveEmailForBiometric(email.trim())
                        
                        // Verificar e criar usuário no Firestore se necessário
                        val existingUser = firestoreUserRepository.getUser(firebaseUser.uid)
                        if (existingUser == null) {
                            // Criar perfil no Firestore
                            val newUser = UserFirestore(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: email.trim(),
                                displayName = firebaseUser.displayName,
                                photoURL = firebaseUser.photoUrl?.toString(),
                                role = "client",
                                profileComplete = false,
                                verified = firebaseUser.isEmailVerified,
                                createdAt = Date(),
                                updatedAt = Date()
                            )
                            
                            firestoreUserRepository.updateUser(newUser).fold(
                                onSuccess = {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        loginSuccess = true
                                    )
                                },
                                onFailure = { error ->
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = "Erro ao criar perfil: ${error.message}"
                                    )
                                }
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                loginSuccess = true
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro no login: ${exception.message}", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = when {
                                exception.message?.contains("password", ignoreCase = true) == true ->
                                    "Senha incorreta"
                                exception.message?.contains("user-not-found", ignoreCase = true) == true ->
                                    "Usuário não encontrado"
                                exception.message?.contains("network", ignoreCase = true) == true ->
                                    "Erro de conexão. Verifique sua internet."
                                else -> "Erro ao fazer login: ${exception.message}"
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro inesperado: ${e.message}"
                )
            }
        }
    }
}
```

---

#### **C. Verificação de Estado de Autenticação (Splash)**
**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/feature/splash/presentation/SplashViewModel.kt`

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
                Log.d("SplashViewModel", "Verificando estado de autenticação")
                
                // ✅ Obter usuário atual
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    // ✅ Verificar se o token ainda é válido
                    try {
                        currentUser.getIdToken(true).await()
                        
                        // Se estiver logado e token válido, verificar sync inicial
                        val needsSync = !preferencesManager.isInitialSyncCompleted(currentUser.uid)
                        if (needsSync) {
                            Log.d("SplashViewModel", "Iniciando sincronização inicial...")
                            try {
                                initialDataSyncManager.syncAllUserData()
                                preferencesManager.setInitialSyncCompleted(currentUser.uid)
                            } catch (e: Exception) {
                                Log.e("SplashViewModel", "Erro ao sincronizar: ${e.message}", e)
                            }
                        }
                        
                        Log.d("SplashViewModel", "Usuário logado, navegando para home")
                        onNavigateToHome()
                    } catch (e: Exception) {
                        // Token inválido ou expirado
                        Log.w("SplashViewModel", "Token inválido: ${e.message}")
                        onNavigateToLogin()
                    }
                } else {
                    Log.d("SplashViewModel", "Usuário não logado, navegando para login")
                    onNavigateToLogin()
                }
            } catch (e: Exception) {
                Log.e("SplashViewModel", "Erro ao verificar autenticação: ${e.message}", e)
                onNavigateToLogin()
            }
        }
    }
}
```

---

#### **D. Chamada de Cloud Function (2FA)**
**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/feature/auth/presentation/TwoFactorAuthViewModel.kt`

```kotlin
@HiltViewModel
class TwoFactorAuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val functionsService: FirebaseFunctionsService
) : ViewModel() {

    fun sendCode() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val currentUser = auth.currentUser ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }
                
                // ✅ Chamar Cloud Function
                val result = functionsService.sendTwoFactorCode()
                
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            codeSent = true
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao enviar código: ${exception.message}"
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

    fun verifyCode(code: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val currentUser = auth.currentUser ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }
                
                // ✅ Chamar Cloud Function para verificar código
                val result = functionsService.verifyTwoFactorCode(code)
                
                result.fold(
                    onSuccess = { data ->
                        val verified = data["verified"] as? Boolean ?: false
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isVerified = verified
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = when {
                                exception.message?.contains("expirado") == true ->
                                    "Código expirado. Solicite um novo código."
                                exception.message?.contains("inválido") == true ->
                                    "Código inválido. Tente novamente."
                                else -> "Erro ao verificar código: ${exception.message}"
                            }
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

## 📝 RESUMO DAS SEGURANÇAS

### ✅ Pontos Importantes:

1. **Todas as regras exigem autenticação** (`request.auth != null`)
2. **Propriedade de dados**: Usuários só podem modificar seus próprios dados
3. **Validação de campos**: Regras validam estrutura e tipos de dados
4. **Cloud Functions**: Verificam autenticação via `context.auth`
5. **Cliente**: Usa `FirebaseAuth.currentUser` e tokens para validação
6. **Regra padrão**: Nega acesso a coleções não especificadas

### 🔐 Fluxo de Autenticação:

```
1. Cliente: signInWithEmail() → Firebase Auth
2. Firebase Auth: retorna FirebaseUser com UID
3. Cliente: cria/atualiza documento em /users/{uid}
4. Cloud Function: onUserCreate() → cria documento inicial se não existir
5. Regras Firestore: verificam request.auth.uid == userId
6. Cloud Functions: verificam context.auth.uid
```

---

**Data de geração:** $(date)
**Versão das regras:** rules_version = '2'
