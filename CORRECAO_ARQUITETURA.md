# CORRECAO DE ARQUITETURA FRONTEND-BACKEND

## PROBLEMA IDENTIFICADO

O banco de dados 'taskgo' estava vazio porque havia código usando `FirebaseFirestore.getInstance()` diretamente, que acessa o database 'default' ao invés do 'taskgo'.

## CORRECOES APLICADAS

### 1. ShipmentScreen.kt
**Antes:**
```kotlin
val shipmentRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    .collection("shipments")
    .add(shipmentData)
    .await()
```

**Depois:**
```kotlin
// CRÍTICO: Usar FirestoreHelper para garantir que está usando database 'taskgo'
val shipmentRef = com.taskgoapp.taskgo.core.firebase.FirestoreHelper.getInstance()
    .collection("shipments")
    .add(shipmentData)
    .await()
```

### 2. FirestoreHelper.kt
**Adicionado logs de diagnóstico:**
- Log quando acessa o database 'taskgo'
- Log de sucesso/erro
- Stack trace completo em caso de erro

## VERIFICACAO COMPLETA

### Arquivos que usam Firestore corretamente (via FirestoreHelper):
- ✅ `FirebaseModule.kt` - Usa `FirestoreHelper.getInstance()` em produção
- ✅ `FirestoreUserRepository.kt` - Usa `firestore` injetado (vem do FirebaseModule)
- ✅ Todos os outros repositórios - Usam `firestore` injetado

### Arquivos corrigidos:
- ✅ `ShipmentScreen.kt` - Agora usa `FirestoreHelper.getInstance()`

## TESTE AGORA

### 1. Fazer login no app
1. Faça logout (se estiver logado)
2. Faça login com Google ou email
3. Selecione tipo de conta (PARCEIRO ou CLIENTE)

### 2. Verificar logs do Android Studio
Procure por estas mensagens:
```
FirestoreHelper: 🔍 Acessando database 'taskgo'...
FirestoreHelper: ✅ Database 'taskgo' acessado com sucesso
FirestoreUserRepository: Buscando usuário no Firestore: uid=...
FirestoreUserRepository: Usuário encontrado: ...
```

### 3. Verificar no Firebase Console
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/databases/taskgo/data
2. Verifique se a coleção `users` foi criada
3. Verifique se há um documento com o UID do usuário logado
4. Verifique se o campo `role` está correto (partner ou client)

### 4. Verificar Cloud Functions
1. Acesse: https://console.cloud.google.com/functions/list?project=task-go-ee85f
2. Verifique os logs da função `onUserCreate`
3. Verifique se não há erros relacionados ao database

## SE AINDA NÃO FUNCIONAR

### Verificar se o database 'taskgo' está ativo:
```powershell
firebase firestore:databases:list --project=task-go-ee85f
```

Deve mostrar:
- `(default)`
- `taskgo` ✅

### Verificar Firestore Rules:
```powershell
firebase firestore:rules:get --project=task-go-ee85f
```

As rules devem permitir criação de documentos em `/users/{userId}` para usuários autenticados.

### Executar teste de diagnóstico:
Adicione este código temporariamente no `LoginViewModel` ou `SignupViewModel` após o login bem-sucedido:

```kotlin
// Teste de diagnóstico
viewModelScope.launch {
    val firestore = FirestoreHelper.getInstance()
    val testDoc = firestore.collection("_test").document("connection_test")
    testDoc.set(mapOf("test" to true, "timestamp" to FieldValue.serverTimestamp())).await()
    Log.d("Diagnostico", "✅ Teste de escrita no database 'taskgo' funcionou!")
    testDoc.delete().await()
}
```

## PROXIMOS PASSOS

1. ✅ Corrigir ShipmentScreen.kt
2. ✅ Adicionar logs de diagnóstico
3. ⏳ Testar login/cadastro
4. ⏳ Verificar se dados aparecem no Firebase Console
5. ⏳ Se funcionar, remover logs de debug excessivos
