# 🚀 Sistema de Cache Local com Sincronização Assíncrona

## 📋 Visão Geral

Implementado um sistema de cache local com sincronização assíncrona para melhorar drasticamente o desempenho do app, eliminando problemas de carregamento lento causados por conexões em tempo real com Firebase.

## 🎯 Lógica Implementada

### 1. **Salvamento Local Primeiro (Instantâneo)**
- Todos os dados são salvos **localmente primeiro** usando Room Database
- O usuário vê as mudanças **imediatamente**, sem esperar pela rede
- Experiência fluida e responsiva

### 2. **Sincronização Assíncrona (Após 1 Minuto)**
- Após salvar localmente, a operação é **agendada** para sincronização
- A sincronização com Firebase acontece **após 1 minuto** de delay
- Múltiplas operações são agrupadas e sincronizadas juntas

### 3. **Dados Permanecem Locais**
- Dados salvos localmente **permanecem no cache**
- Melhora o carregamento em acessos subsequentes
- App funciona mesmo sem conexão (com dados em cache)

### 4. **Dados Só São Apagados Quando Reescritos**
- Dados locais só são **substituídos** quando o usuário faz novas alterações
- Não há perda de dados durante sincronização
- Garante consistência entre local e remoto

## 🏗️ Arquitetura

### Componentes Criados

#### 1. **SyncQueueEntity** (`app/src/main/java/com/taskgoapp/taskgo/data/local/entity/SyncQueueEntity.kt`)
- Entidade Room para armazenar pendências de sincronização
- Campos:
  - `syncType`: Tipo de entidade (product, service, user_profile, etc.)
  - `entityId`: ID da entidade
  - `operation`: Tipo de operação (create, update, delete)
  - `data`: Dados em JSON para sincronização
  - `syncAt`: Timestamp de quando deve ser sincronizada
  - `status`: Status (pending, syncing, completed, failed)
  - `retryCount`: Número de tentativas

#### 2. **SyncQueueDao** (`app/src/main/java/com/taskgoapp/taskgo/data/local/dao/SyncQueueDao.kt`)
- DAO para gerenciar fila de sincronização
- Funções principais:
  - `getPendingSyncs()`: Busca pendências prontas para sincronização
  - `upsert()`: Insere ou atualiza pendência
  - `markAsCompleted()`: Marca como concluída
  - `markAsFailed()`: Marca como falha
  - `reschedule()`: Reagenda para nova tentativa

#### 3. **SyncManager** (`app/src/main/java/com/taskgoapp/taskgo/core/sync/SyncManager.kt`)
- Gerenciador principal de sincronização
- Funcionalidades:
  - `scheduleSync()`: Agenda sincronização após 1 minuto
  - `startSync()`: Inicia processo contínuo de sincronização
  - `syncPendingItems()`: Sincroniza itens pendentes
  - `performSync()`: Executa sincronização real com Firebase
  - Suporte para múltiplos tipos de entidades (product, service, user_profile, settings, order, address, card)

#### 4. **Repositórios Modificados**
- **FirestoreProductsRepositoryImpl**: Modificado para usar cache-first
  - `observeProducts()`: Retorna cache local primeiro, sincroniza em background
  - `getProduct()`: Busca do cache primeiro, depois Firebase
  - `upsertProduct()`: Salva localmente primeiro, agenda sincronização
  - `deleteProduct()`: Remove localmente primeiro, agenda sincronização

## 🔄 Fluxo de Funcionamento

### **Leitura (Read)**
```
1. Usuário solicita dados
   ↓
2. Busca no cache local (Room) → Retorna IMEDIATAMENTE
   ↓
3. Sincroniza com Firebase em background (sem bloquear UI)
   ↓
4. Atualiza cache local com dados do Firebase
```

### **Escrita (Write)**
```
1. Usuário salva/atualiza dados
   ↓
2. Salva no cache local (Room) → Retorna IMEDIATAMENTE
   ↓
3. Agenda sincronização com Firebase (após 1 minuto)
   ↓
4. SyncManager sincroniza automaticamente após delay
   ↓
5. Dados permanecem no cache local
```

## 📊 Benefícios

### ✅ **Performance**
- **Carregamento instantâneo**: Dados do cache local são retornados imediatamente
- **Sem bloqueio de UI**: Sincronização acontece em background
- **Menos requisições**: Agrupamento de operações reduz chamadas à rede

### ✅ **Experiência do Usuário**
- **Resposta imediata**: Usuário vê mudanças instantaneamente
- **Funciona offline**: App funciona com dados em cache
- **Sem perda de dados**: Dados locais são preservados

### ✅ **Eficiência**
- **Menor uso de dados**: Sincronização agrupada reduz tráfego
- **Menor carga no servidor**: Menos requisições simultâneas
- **Bateria**: Menos operações de rede economizam bateria

## 🔧 Configuração

### **Delay de Sincronização**
- Padrão: **1 minuto** (60.000ms)
- Configurável em `SyncManager.SYNC_DELAY_MS`

### **Tentativas de Sincronização**
- Máximo de tentativas: **3**
- Configurável em `SyncManager.MAX_RETRIES`

### **Tipos de Entidades Suportadas**
- `product`: Produtos
- `service`: Serviços
- `user_profile`: Perfil de usuário
- `settings`: Configurações
- `order`: Pedidos
- `address`: Endereços
- `card`: Cartões

## 📝 Exemplo de Uso

### **Salvar Produto**
```kotlin
// No repositório
override suspend fun upsertProduct(product: Product) {
    // 1. Salva localmente primeiro (instantâneo)
    productDao.upsert(product.toEntity())
    
    // 2. Agenda sincronização com Firebase após 1 minuto
    syncManager.scheduleSync(
        syncType = "product",
        entityId = product.id,
        operation = "update",
        data = firestoreProduct
    )
}
```

### **Buscar Produtos**
```kotlin
// No repositório
override fun observeProducts(): Flow<List<Product>> = flow {
    // 1. Retorna dados do cache local (instantâneo)
    val cachedProducts = productDao.getAll().map { ... }
    emit(cachedProducts)
    
    // 2. Sincroniza com Firebase em background
    syncScope.launch {
        val firestoreProducts = productsCollection.get().await()
        // Atualiza cache local
    }
}
```

## 🚨 Tratamento de Erros

### **Falhas de Sincronização**
- Se a sincronização falhar, é **reagendada** automaticamente
- Após 3 tentativas, a pendência é marcada como `failed`
- Pendências falhas podem ser retentadas manualmente

### **Conflitos**
- Se o mesmo item for modificado múltiplas vezes, apenas a **última versão** é sincronizada
- Sistema usa `upsert` para evitar duplicatas

## 🔄 Próximos Passos

### **Melhorias Futuras**
1. **Sincronização incremental**: Sincronizar apenas mudanças desde última sync
2. **Resolução de conflitos**: Detectar e resolver conflitos entre local e remoto
3. **Sincronização manual**: Permitir usuário forçar sincronização
4. **Indicador de sincronização**: Mostrar status de sincronização na UI
5. **Sincronização em background**: Usar WorkManager para sincronização mesmo com app fechado

## 📚 Arquivos Modificados/Criados

### **Novos Arquivos**
- `app/src/main/java/com/taskgoapp/taskgo/data/local/entity/SyncQueueEntity.kt`
- `app/src/main/java/com/taskgoapp/taskgo/data/local/dao/SyncQueueDao.kt`
- `app/src/main/java/com/taskgoapp/taskgo/core/sync/SyncManager.kt`

### **Arquivos Modificados**
- `app/src/main/java/com/taskgoapp/taskgo/data/local/TaskGoDatabase.kt` (versão 3)
- `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreProductsRepositoryImpl.kt`
- `app/src/main/java/com/taskgoapp/taskgo/di/AppModule.kt`

## ✅ Status

- ✅ Entidade de sincronização criada
- ✅ DAO de sincronização criado
- ✅ SyncManager implementado
- ✅ Repositório de produtos modificado para cache-first
- ✅ Injeção de dependências configurada
- ⏳ Outros repositórios podem ser modificados seguindo o mesmo padrão

## 🎉 Resultado

O app agora tem **carregamento instantâneo** de dados, **sincronização eficiente** em background, e **funciona offline** com dados em cache. A experiência do usuário é muito mais fluida e responsiva!

