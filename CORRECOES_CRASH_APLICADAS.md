# 🔧 Correções Aplicadas para Crash no Início do App

## ✅ Problemas Identificados e Corrigidos

### 1. **Erro do Room Database - Schema Mudou**
**Erro:**
```
Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number.
Expected identity hash: 6a4e5a11df4599b7a1a04806c0cf5225, found: 6002ae27329e1d2c4d9c65d23c53569b
```

**Causa:** Adicionamos o campo `rating` ao `ProductEntity`, mas não atualizamos a versão do banco de dados.

**Correção:**
- ✅ Atualizado `TaskGoDatabase.kt`: versão de `4` para `5`
- ✅ O Room agora reconhece a mudança no schema

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/data/local/TaskGoDatabase.kt`

---

### 2. **Erro de Deserialização do Firestore - createdAt**
**Erro:**
```
Could not deserialize object. Failed to convert value of type java.lang.Long to Date (found in field 'createdAt')
```

**Causa:** O Firestore retorna `createdAt` como `Long` (timestamp), mas o modelo `ProductFirestore` espera `Date`.

**Correção:**
- ✅ Implementada conversão manual que aceita `Long`, `Date` ou `Timestamp`
- ✅ Tratamento de erro para documentos inválidos

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreProductsRepositoryImpl.kt`

**Código:**
```kotlin
val createdAt = when (val createdAtValue = data["createdAt"]) {
    is Long -> java.util.Date(createdAtValue)
    is java.util.Date -> createdAtValue
    is com.google.firebase.Timestamp -> createdAtValue.toDate()
    else -> null
}
```

---

### 3. **Erro do SyncWorker - Construtor Incorreto**
**Erro:**
```
Could not instantiate com.taskgoapp.taskgo.core.sync.SyncWorker
java.lang.NoSuchMethodException: com.taskgoapp.taskgo.core.sync.SyncWorker.<init> [class android.content.Context, class androidx.work.WorkerParameters]
```

**Causa:** O `SyncWorker` estava tentando injetar `syncQueueDao` diretamente, mas o Hilt WorkManager já fornece `Context` e `WorkerParameters` via `@AssistedInject`.

**Correção:**
- ✅ Removido `syncQueueDao` do construtor (não é necessário, já está no `SyncManager`)
- ✅ Mantido apenas `syncManager` como dependência injetada

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/core/sync/SyncWorker.kt`

---

### 4. **Permissões do Firestore - Coleções Faltando**
**Erro:**
```
PERMISSION_DENIED, description=Missing or insufficient permissions.
```

**Coleções afetadas:**
- `service_categories`
- `product_categories`
- `homeBanners`

**Causa:** Essas coleções não tinham regras definidas no `firestore.rules`.

**Correção:**
- ✅ Adicionadas regras para `service_categories` (leitura para autenticados, escrita apenas para admins)
- ✅ Adicionadas regras para `product_categories` (leitura para autenticados, escrita apenas para admins)
- ✅ Adicionadas regras para `homeBanners` (leitura de banners ativos para autenticados, escrita apenas para admins)
- ✅ Deploy das regras realizado com sucesso

**Arquivo:** `firestore.rules`

**Regras adicionadas:**
```javascript
// Service Categories collection
match /service_categories/{categoryId} {
  allow read: if isAuthenticated();
  allow write: if isAdmin();
}

// Product Categories collection
match /product_categories/{categoryId} {
  allow read: if isAuthenticated();
  allow write: if isAdmin();
}

// Home Banners collection
match /homeBanners/{bannerId} {
  allow read: if isAuthenticated() && 
    (!resource.exists || resource.data.active == true);
  allow write: if isAdmin();
}
```

---

## 📋 Resumo das Alterações

### Arquivos Modificados:
1. ✅ `app/src/main/java/com/taskgoapp/taskgo/data/local/TaskGoDatabase.kt`
   - Versão do banco: 4 → 5

2. ✅ `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreProductsRepositoryImpl.kt`
   - Conversão manual de `createdAt`/`updatedAt` (Long/Date/Timestamp)

3. ✅ `app/src/main/java/com/taskgoapp/taskgo/core/sync/SyncWorker.kt`
   - Removido `syncQueueDao` do construtor

4. ✅ `firestore.rules`
   - Adicionadas regras para `service_categories`, `product_categories` e `homeBanners`

### Deploys Realizados:
- ✅ Firestore Rules: Deploy realizado com sucesso

### Build:
- ✅ Build debug concluída com sucesso
- ✅ APK gerado: `app\build\outputs\apk\debug\app-debug.apk`

---

## 🧪 Próximos Passos para Teste

1. **Desinstalar o app anterior** (para limpar o banco de dados antigo)
2. **Instalar o novo APK** gerado
3. **Testar o app** e verificar se:
   - ✅ O app inicia sem crash
   - ✅ Os produtos são carregados corretamente
   - ✅ As categorias são exibidas
   - ✅ Os banners são exibidos
   - ✅ A sincronização funciona

---

## ⚠️ Nota Importante

O erro do Room Database foi resolvido aumentando a versão. Como o banco usa `.fallbackToDestructiveMigration()`, **os dados locais serão apagados** na primeira execução após a atualização. Isso é esperado e os dados serão sincronizados novamente do Firestore.

---

**Data das Correções:** 23/11/2025  
**Status:** ✅ **TODAS AS CORREÇÕES APLICADAS E BUILD GERADA**










