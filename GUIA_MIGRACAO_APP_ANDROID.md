# 📱 Guia de Migração - App Android para Cloud Functions

Este guia mostra como migrar o app Android para usar Cloud Functions ao invés de escrita direta no Firestore.

---

## 🔄 MUDANÇAS NECESSÁRIAS

### ⚠️ Breaking Changes

As Firestore Rules foram atualizadas e agora **bloqueiam escrita direta** para:
- Services
- Products
- Orders
- Notifications
- Reviews

**Solução:** Usar Cloud Functions para todas essas operações.

---

## 1️⃣ MIGRAÇÃO: CRIAR SERVIÇOS

### ❌ ANTES (Escrita Direta - BLOQUEADO)

```kotlin
// ❌ NÃO FUNCIONA MAIS - Bloqueado pelas Firestore Rules
suspend fun createService(service: ServiceFirestore): Result<String> {
    val serviceData = service.copy(
        createdAt = Date(),
        updatedAt = Date()
    )
    
    // BLOQUEADO: Firestore Rules bloqueiam escrita direta
    val docRef = publicServicesCollection.add(serviceData).await()
    return Result.success(docRef.id)
}
```

### ✅ DEPOIS (Cloud Function)

```kotlin
// ✅ USAR Cloud Function
suspend fun createService(service: ServiceFirestore): Result<String> {
    return try {
        val functions = FirebaseFunctions.getInstance()
        val createServiceFunction = functions.getHttpsCallable("createService")
        
        val data = hashMapOf(
            "title" to service.title,
            "description" to service.description,
            "category" to service.category,
            "price" to service.price,
            "latitude" to (service.latitude ?: null),
            "longitude" to (service.longitude ?: null),
            "active" to (service.active ?: true)
        )
        
        val result = createServiceFunction.call(data).await()
        val resultData = result.data as? Map<*, *>
        val serviceId = resultData?.get("serviceId") as? String
        
        if (serviceId != null) {
            Result.success(serviceId)
        } else {
            Result.failure(Exception("Service ID not returned"))
        }
    } catch (e: Exception) {
        Log.e("FirestoreServicesRepo", "Error creating service via Cloud Function: ${e.message}", e)
        Result.failure(e)
    }
}
```

---

## 2️⃣ MIGRAÇÃO: ATUALIZAR SERVIÇOS

### ❌ ANTES

```kotlin
// ❌ BLOQUEADO
suspend fun updateService(serviceId: String, service: ServiceFirestore): Result<Unit> {
    publicServicesCollection.document(serviceId).set(service).await()
    return Result.success(Unit)
}
```

### ✅ DEPOIS

```kotlin
// ✅ USAR Cloud Function
suspend fun updateService(serviceId: String, updates: Map<String, Any?>): Result<Unit> {
    return try {
        val functions = FirebaseFunctions.getInstance()
        val updateServiceFunction = functions.getHttpsCallable("updateService")
        
        val data = hashMapOf(
            "serviceId" to serviceId,
            "updates" to updates
        )
        
        updateServiceFunction.call(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreServicesRepo", "Error updating service via Cloud Function: ${e.message}", e)
        Result.failure(e)
    }
}
```

---

## 3️⃣ MIGRAÇÃO: DELETAR SERVIÇOS

### ❌ ANTES

```kotlin
// ❌ BLOQUEADO
suspend fun deleteService(serviceId: String): Result<Unit> {
    publicServicesCollection.document(serviceId).delete().await()
    return Result.success(Unit)
}
```

### ✅ DEPOIS

```kotlin
// ✅ USAR Cloud Function
suspend fun deleteService(serviceId: String): Result<Unit> {
    return try {
        val functions = FirebaseFunctions.getInstance()
        val deleteServiceFunction = functions.getHttpsCallable("deleteService")
        
        val data = hashMapOf("serviceId" to serviceId)
        deleteServiceFunction.call(data).await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreServicesRepo", "Error deleting service via Cloud Function: ${e.message}", e)
        Result.failure(e)
    }
}
```

---

## 4️⃣ MIGRAÇÃO: CRIAR PRODUTOS

### ❌ ANTES

```kotlin
// ❌ BLOQUEADO
suspend fun createProduct(product: ProductFirestore): Result<String> {
    val docRef = publicProductsCollection.add(product).await()
    return Result.success(docRef.id)
}
```

### ✅ DEPOIS

```kotlin
// ✅ USAR Cloud Function
suspend fun createProduct(product: ProductFirestore): Result<String> {
    return try {
        val functions = FirebaseFunctions.getInstance()
        val createProductFunction = functions.getHttpsCallable("createProduct")
        
        val data = hashMapOf(
            "title" to product.title,
            "description" to product.description,
            "category" to product.category,
            "price" to product.price,
            "images" to (product.images ?: emptyList()),
            "stock" to product.stock,
            "active" to (product.active ?: true)
        )
        
        val result = createProductFunction.call(data).await()
        val resultData = result.data as? Map<*, *>
        val productId = resultData?.get("productId") as? String
        
        if (productId != null) {
            Result.success(productId)
        } else {
            Result.failure(Exception("Product ID not returned"))
        }
    } catch (e: Exception) {
        Log.e("FirestoreProductsRepo", "Error creating product via Cloud Function: ${e.message}", e)
        Result.failure(e)
    }
}
```

---

## 5️⃣ MIGRAÇÃO: CRIAR ORDENS

### ❌ ANTES

```kotlin
// ❌ BLOQUEADO
suspend fun createOrder(order: OrderFirestore): Result<String> {
    val docRef = ordersCollection.add(order).await()
    return Result.success(docRef.id)
}
```

### ✅ DEPOIS

```kotlin
// ✅ USAR Cloud Function (já existe!)
suspend fun createOrder(serviceId: String?, category: String?, details: Map<String, Any>, location: String, budget: Double?, dueDate: String?): Result<String> {
    return try {
        val functions = FirebaseFunctions.getInstance()
        val createOrderFunction = functions.getHttpsCallable("createOrder")
        
        val data = hashMapOf(
            "serviceId" to serviceId,
            "category" to category,
            "details" to details,
            "location" to location,
            "budget" to budget,
            "dueDate" to dueDate
        )
        
        val result = createOrderFunction.call(data).await()
        val resultData = result.data as? Map<*, *>
        val orderId = resultData?.get("orderId") as? String
        
        if (orderId != null) {
            Result.success(orderId)
        } else {
            Result.failure(Exception("Order ID not returned"))
        }
    } catch (e: Exception) {
        Log.e("FirestoreOrdersRepo", "Error creating order via Cloud Function: ${e.message}", e)
        Result.failure(e)
    }
}
```

---

## 6️⃣ MIGRAÇÃO: ATUALIZAR STATUS DE ORDEM

### ❌ ANTES

```kotlin
// ❌ BLOQUEADO
suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
    ordersCollection.document(orderId).update("status", status).await()
    return Result.success(Unit)
}
```

### ✅ DEPOIS

```kotlin
// ✅ USAR Cloud Function (já existe!)
suspend fun updateOrderStatus(orderId: String, status: String, proposalDetails: Map<String, Any>? = null): Result<Unit> {
    return try {
        val functions = FirebaseFunctions.getInstance()
        val updateOrderStatusFunction = functions.getHttpsCallable("updateOrderStatus")
        
        val data = hashMapOf(
            "orderId" to orderId,
            "status" to status,
            "proposalDetails" to proposalDetails
        )
        
        updateOrderStatusFunction.call(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreOrdersRepo", "Error updating order status via Cloud Function: ${e.message}", e)
        Result.failure(e)
    }
}
```

---

## 7️⃣ MIGRAÇÃO: DEFINIR ROLE INICIAL

### ❌ ANTES

```kotlin
// ❌ Direto no Firestore
suspend fun setUserRole(role: String): Result<Unit> {
    firestore.collection("users")
        .document(currentUserId)
        .update("role", role)
        .await()
    return Result.success(Unit)
}
```

### ✅ DEPOIS

```kotlin
// ✅ USAR Cloud Function (NOVO!)
suspend fun setInitialUserRole(role: String, accountType: String? = null): Result<String> {
    return try {
        val functions = FirebaseFunctions.getInstance()
        val setInitialUserRoleFunction = functions.getHttpsCallable("setInitialUserRole")
        
        val data = hashMapOf(
            "role" to role,
            "accountType" to accountType
        )
        
        val result = setInitialUserRoleFunction.call(data).await()
        val resultData = result.data as? Map<*, *>
        val finalRole = resultData?.get("role") as? String ?: role
        
        // IMPORTANTE: Atualizar token para incluir Custom Claims
        firebaseAuth.currentUser?.getIdToken(true)?.await()
        
        Result.success(finalRole)
    } catch (e: Exception) {
        Log.e("AuthRepo", "Error setting initial user role via Cloud Function: ${e.message}", e)
        Result.failure(e)
    }
}
```

**⚠️ IMPORTANTE:** Após chamar `setInitialUserRole`, o app deve atualizar o token para incluir as Custom Claims:
```kotlin
firebaseAuth.currentUser?.getIdToken(true)?.await()
```

---

## 8️⃣ HELPER: FirebaseFunctionsService

Criar ou atualizar `FirebaseFunctionsService` para centralizar chamadas:

```kotlin
@Singleton
class FirebaseFunctionsService @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    suspend fun createService(data: Map<String, Any?>): Result<HttpsCallableResult> {
        return try {
            val result = functions.getHttpsCallable("createService")
                .call(data)
                .await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirebaseFunctionsService", "Error calling createService: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateService(serviceId: String, updates: Map<String, Any?>): Result<HttpsCallableResult> {
        return try {
            val result = functions.getHttpsCallable("updateService")
                .call(mapOf("serviceId" to serviceId, "updates" to updates))
                .await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirebaseFunctionsService", "Error calling updateService: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteService(serviceId: String): Result<HttpsCallableResult> {
        return try {
            val result = functions.getHttpsCallable("deleteService")
                .call(mapOf("serviceId" to serviceId))
                .await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirebaseFunctionsService", "Error calling deleteService: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Similar para products, orders, etc.
}
```

---

## 9️⃣ TRATAMENTO DE ERROS

### Erros Comuns

```kotlin
when (val error = e as? FirebaseFunctionsException) {
    is FirebaseFunctionsException.HttpsException -> {
        when (error.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                // Usuário não autenticado
                showError("Você precisa estar logado")
            }
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> {
                // Permissão negada (role inválido, etc)
                showError("Você não tem permissão para esta ação")
            }
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                // Dados inválidos
                showError("Dados inválidos: ${error.message}")
            }
            FirebaseFunctionsException.Code.NOT_FOUND -> {
                // Recurso não encontrado
                showError("Recurso não encontrado")
            }
            else -> {
                showError("Erro: ${error.message}")
            }
        }
    }
    else -> {
        showError("Erro desconhecido: ${e.message}")
    }
}
```

---

## ✅ CHECKLIST DE MIGRAÇÃO

- [ ] Atualizar `FirestoreServicesRepository` para usar Cloud Functions
- [ ] Atualizar `FirestoreProductsRepository` para usar Cloud Functions
- [ ] Atualizar `FirestoreOrdersRepository` para usar Cloud Functions (se necessário)
- [ ] Adicionar `setInitialUserRole` após cadastro
- [ ] Atualizar `FirebaseFunctionsService` com novas funções
- [ ] Atualizar tratamento de erros
- [ ] Testar criação de services via Cloud Function
- [ ] Testar atualização de services via Cloud Function
- [ ] Testar deleção de services via Cloud Function
- [ ] Testar criação de products via Cloud Function
- [ ] Testar criação de orders via Cloud Function
- [ ] Testar atualização de status de orders via Cloud Function
- [ ] Verificar App Check configurado no app

---

## 📚 DOCUMENTAÇÃO ADICIONAL

- [Firebase Cloud Functions - Callable](https://firebase.google.com/docs/functions/callable)
- [App Check - Android](https://firebase.google.com/docs/app-check/android/play-integrity-provider)

---

**Data de Criação:** 2024
**Versão:** 1.0.0
