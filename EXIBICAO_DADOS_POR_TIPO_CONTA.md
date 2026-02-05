# 📱 Exibição de Dados por Tipo de Conta - TaskGo

## 🎯 Visão Geral

Com a nova arquitetura baseada em `locations/{locationId}`, **TODOS os dados são exibidos corretamente** porque:

1. ✅ **Filtragem por Localização**: Todos os dados são buscados de `locations/{locationId}` baseado no `city`/`state` do usuário logado
2. ✅ **Filtragem por Role**: Cada tipo de conta vê apenas os dados relevantes para seu papel
3. ✅ **Filtragem por Status**: Ordens são filtradas por status (ativa, cancelada, concluída)
4. ✅ **Filtragem por Relacionamento**: Ordens mostram apenas as que pertencem ao usuário (como cliente ou parceiro)

---

## 👤 CONTA CLIENTE

### **📋 Menu Principal (ProfileScreen)**

```
┌─────────────────────────────┐
│ Minhas Ordens de Serviço    │ ← Criar e gerenciar ordens
│ Meus Pedidos                 │ ← Pedidos de produtos
│ Configurações                │
└─────────────────────────────┘
```

---

### **1. Tela: "Minhas Ordens de Serviço" (MyServiceOrdersScreen)**

**Path**: `ProfileScreen` → "Minhas Ordens de Serviço"

**Fonte de Dados**:
```kotlin
// Busca em locations/{locationId}/orders
// Filtro: clientId == currentUserId && deleted == false
observeOrders(userId, role = "client")
```

**Abas**:
- ✅ **Ativas**: Ordens com `status != "cancelled" && status != "completed"`
  - Mostra sinalização: "Aguardando Aceitação do Prestador" ou "Aceita pelo Prestador"
  - Baseado em `acceptedByProvider` e `providerId != null`
- ✅ **Canceladas**: Ordens com `status == "cancelled"`
- ✅ **Concluídas**: Ordens com `status == "completed"`

**Ações Disponíveis**:
- ✅ Criar nova ordem (FAB na aba Ativas)
- ✅ Editar ordem (se ainda não aceita)
- ✅ Cancelar ordem (soft delete)

**O que é Exibido**:
- ✅ Apenas ordens criadas pelo próprio cliente
- ✅ Todas as ordens da região do cliente (`locations/{locationId}/orders`)
- ✅ Filtradas por `clientId == userId`

---

### **2. Tela: "Serviços" (ServicesScreen)**

**Path**: Bottom Navigation → "Serviços"

**Fonte de Dados**:
```kotlin
// Para CLIENTE: Mostra prestadores (parceiros) disponíveis
// Busca em locations/{locationId}/users
// Filtro: role == "partner" && preferredCategories contém categoria selecionada
filteredProviders
```

**O que é Exibido**:
1. **Categorias de Serviços** (grid):
   - Todas as categorias disponíveis
   - Ao clicar, mostra prestadores que têm essa categoria em `preferredCategories`

2. **Prestadores por Categoria**:
   - Lista de parceiros que oferecem a categoria selecionada
   - Filtrados por `preferredCategories.contains(categoria)`
   - Todos da mesma região (`locations/{locationId}/users`)

3. **Botão "Criar Ordem de Serviço"**:
   - Visível apenas para CLIENTE
   - Navega para `CreateWorkOrderScreen`

**Filtros Disponíveis**:
- ✅ Busca por nome
- ✅ Filtro por categoria
- ✅ Ordenação: Melhor Avaliado, Mais Recente

---

### **3. Tela: "Loja" (ProductsScreen)**

**Path**: Bottom Navigation → "Loja"

**Fonte de Dados**:
```kotlin
// Busca em locations/{locationId}/products
// Filtro: active == true && sellerId != currentUserId
observeProducts()
```

**O que é Exibido**:
- ✅ Produtos de **outros vendedores** (não próprios)
- ✅ Todos os produtos da região (`locations/{locationId}/products`)
- ✅ Filtrados por `active == true`
- ✅ Exclui produtos do próprio usuário (`sellerId != currentUserId`)

**Filtros Disponíveis**:
- ✅ Busca por texto
- ✅ Filtro por categoria
- ✅ Filtro por preço (Até R$ 50, R$ 50-100, etc.)
- ✅ Filtro por promoção
- ✅ Ordenação: Melhor Avaliado, Mais Recente, Mais Vendidos

---

### **4. Tela: "Meus Pedidos" (Purchase Orders)**

**Path**: `ProfileScreen` → "Meus Pedidos"

**Fonte de Dados**:
```kotlin
// Busca em locations/{locationId}/orders
// Filtro: clientId == currentUserId && type == "purchase"
observePurchaseOrders(userId)
```

**O que é Exibido**:
- ✅ Pedidos de produtos feitos pelo cliente
- ✅ Status: Pendente, Confirmado, Em Trânsito, Entregue, Cancelado

---

### **5. Tela: "Home" (HomeScreen)**

**Fonte de Dados**:
- ✅ Produtos em destaque (`featured == true`)
- ✅ Prestadores em destaque (baseado em rating)
- ✅ Categorias de serviços
- ✅ Banners promocionais

**O que é Exibido**:
- ✅ Todos os dados da região do cliente (`locations/{locationId}/`)
- ✅ Produtos e prestadores filtrados por localização

---

## 🛠️ CONTA PARCEIRO

### **📋 Menu Principal (ProfileScreen)**

```
┌─────────────────────────────┐
│ Meus Serviços               │ ← Ordens aceitas pelo parceiro
│ Meus Produtos               │ ← Produtos cadastrados
│ Meus Pedidos                │ ← Pedidos recebidos (como vendedor)
│ Configurações               │
└─────────────────────────────┘
```

---

### **1. Tela: "Meus Serviços" (MeusServicosScreen)**

**Path**: `ProfileScreen` → "Meus Serviços"

**Fonte de Dados**:
```kotlin
// Busca em locations/{locationId}/orders
// Filtro: providerId == currentUserId && deleted == false
observeOrders(userId, role = "provider")
```

**Abas**:
- ✅ **Ativas**: Ordens com `status != "cancelled" && status != "completed"`
  - Apenas ordens onde `providerId == userId` (ordens que o parceiro aceitou)
  - **SEM** sinalização de aceitação (já foram aceitas)
- ✅ **Canceladas**: Ordens com `status == "cancelled"`
- ✅ **Concluídas**: Ordens com `status == "completed"`

**O que é Exibido**:
- ✅ **APENAS** ordens que o parceiro **aceitou e fechou** com o cliente
- ✅ Filtradas por `providerId == userId`
- ✅ Todas da mesma região (`locations/{locationId}/orders`)

**Ações Disponíveis**:
- ✅ Visualizar detalhes da ordem
- ✅ Completar ordem (quando finalizado o serviço)
- ✅ Cancelar ordem (se necessário)

---

### **2. Tela: "Serviços" (ServicesScreen)**

**Path**: Bottom Navigation → "Serviços"

**Fonte de Dados**:
```kotlin
// Para PARCEIRO: Mostra ordens de serviço disponíveis (não aceitas)
// Busca em locations/{locationId}/orders
// Filtro: status == "pending" && providerId == null && deleted == false
// + Filtro por preferredCategories do parceiro
observeLocalServiceOrders(category = null)
```

**O que é Exibido**:
1. **Ordens de Serviço Disponíveis**:
   - Ordens com `status == "pending"` (abertas)
   - Ordens com `providerId == null` (ainda não aceitas)
   - Filtradas por `preferredCategories` do parceiro
   - Todas da mesma região (`locations/{locationId}/orders`)

2. **Sem Botão "Criar Ordem"**:
   - Parceiros **NÃO** podem criar ordens
   - Apenas podem aceitar ordens existentes

**Filtros Automáticos**:
- ✅ Apenas ordens da região do parceiro
- ✅ Apenas categorias em `preferredCategories`
- ✅ Apenas ordens não aceitas (`providerId == null`)

**Ações Disponíveis**:
- ✅ Aceitar ordem (define `providerId = userId`)
- ✅ Visualizar detalhes da ordem

---

### **3. Tela: "Loja" (ProductsScreen)**

**Path**: Bottom Navigation → "Loja"

**Fonte de Dados**:
```kotlin
// Busca em locations/{locationId}/products
// Filtro: active == true && sellerId != currentUserId
observeProducts()
```

**O que é Exibido**:
- ✅ Produtos de **outros vendedores** (para comprar)
- ✅ Todos os produtos da região (`locations/{locationId}/products`)
- ✅ Filtrados por `active == true`
- ✅ Exclui produtos próprios (`sellerId != currentUserId`)

**Filtros Disponíveis**:
- ✅ Mesmos filtros do CLIENTE (busca, categoria, preço, etc.)

---

### **4. Tela: "Meus Produtos" (ManageProductsScreen)**

**Path**: `ProfileScreen` → "Meus Produtos"

**Fonte de Dados**:
```kotlin
// Busca em locations/{locationId}/products
// Filtro: sellerId == currentUserId
observeProductsBySeller(sellerId)
```

**O que é Exibido**:
- ✅ **APENAS** produtos cadastrados pelo próprio parceiro
- ✅ Filtrados por `sellerId == userId`
- ✅ Todos da mesma região (`locations/{locationId}/products`)

**Ações Disponíveis**:
- ✅ Criar novo produto
- ✅ Editar produto
- ✅ Desativar produto (soft delete: `active = false`)
- ✅ Marcar como destaque (`featured = true`)

---

### **5. Tela: "Meus Pedidos" (Purchase Orders - como Vendedor)**

**Path**: `ProfileScreen` → "Meus Pedidos"

**Fonte de Dados**:
```kotlin
// Busca em locations/{locationId}/orders
// Filtro: sellerId == currentUserId && type == "purchase"
observePurchaseOrdersBySeller(sellerId)
```

**O que é Exibido**:
- ✅ Pedidos de produtos **recebidos** pelo parceiro (como vendedor)
- ✅ Filtrados por `sellerId == userId`
- ✅ Status: Pendente, Confirmado, Em Trânsito, Entregue, Cancelado

---

### **6. Tela: "Home" (HomeScreen)**

**Fonte de Dados**:
- ✅ Produtos em destaque (`featured == true`)
- ✅ Prestadores em destaque (baseado em rating)
- ✅ Categorias de serviços
- ✅ Banners promocionais

**O que é Exibido**:
- ✅ Todos os dados da região do parceiro (`locations/{locationId}/`)
- ✅ Produtos e prestadores filtrados por localização

---

## 🔍 Como a Arquitetura Garante Exibição Correta

### **1. Filtragem por Localização**

```kotlin
// SEMPRE obtém city/state do perfil do usuário
val (city, state) = LocationHelper.getUserLocation(userRepository)

// Normaliza para locationId
val locationId = LocationHelper.normalizeLocationId(city, state)

// Busca em locations/{locationId}/{collection}
val collection = firestore.collection("locations")
    .document(locationId)
    .collection("orders") // ou "products", "users", etc.
```

**Resultado**: ✅ Usuário vê apenas dados da sua região

---

### **2. Filtragem por Role**

#### **Para CLIENTE**:
```kotlin
// Ordens: apenas as que o cliente criou
collection.whereEqualTo("clientId", userId)

// Produtos: apenas de outros vendedores
collection.whereEqualTo("active", true)
    .whereNotEqualTo("sellerId", userId)
```

#### **Para PARCEIRO**:
```kotlin
// Ordens aceitas: apenas as que o parceiro aceitou
collection.whereEqualTo("providerId", userId)

// Ordens disponíveis: apenas as não aceitas
collection.whereEqualTo("status", "pending")
    .whereEqualTo("providerId", null)

// Produtos próprios: apenas os que o parceiro cadastrou
collection.whereEqualTo("sellerId", userId)
```

**Resultado**: ✅ Cada tipo de conta vê apenas dados relevantes

---

### **3. Filtragem por Status**

```kotlin
// Ativas
.whereNotEqualTo("status", "cancelled")
.whereNotEqualTo("status", "completed")

// Canceladas
.whereEqualTo("status", "cancelled")

// Concluídas
.whereEqualTo("status", "completed")
```

**Resultado**: ✅ Ordens são exibidas nas abas corretas

---

### **4. Filtragem por PreferredCategories (Parceiro)**

```kotlin
// Para parceiros, apenas ordens nas categorias que ele oferece
val preferredCategories = user.preferredCategories ?: emptyList()

// No ServicesScreen, filtra ordens por categoria
if (preferredCategories.isNotEmpty()) {
    query = query.whereIn("category", preferredCategories)
}
```

**Resultado**: ✅ Parceiro vê apenas ordens relevantes para suas habilidades

---

## 📊 Resumo: O que Cada Tipo de Conta Vê

### **CLIENTE**

| Tela | Dados Exibidos | Filtros Aplicados |
|------|----------------|-------------------|
| **Minhas Ordens** | Ordens criadas pelo cliente | `clientId == userId`, `deleted == false` |
| **Serviços** | Prestadores disponíveis | `role == "partner"`, `preferredCategories.contains(categoria)` |
| **Loja** | Produtos de outros vendedores | `active == true`, `sellerId != userId` |
| **Meus Pedidos** | Pedidos de produtos feitos | `clientId == userId`, `type == "purchase"` |
| **Home** | Produtos/prestadores em destaque | `featured == true`, região do usuário |

---

### **PARCEIRO**

| Tela | Dados Exibidos | Filtros Aplicados |
|------|----------------|-------------------|
| **Meus Serviços** | Ordens aceitas pelo parceiro | `providerId == userId`, `deleted == false` |
| **Serviços** | Ordens disponíveis (não aceitas) | `status == "pending"`, `providerId == null`, `preferredCategories` |
| **Loja** | Produtos de outros vendedores | `active == true`, `sellerId != userId` |
| **Meus Produtos** | Produtos cadastrados pelo parceiro | `sellerId == userId` |
| **Meus Pedidos** | Pedidos recebidos (como vendedor) | `sellerId == userId`, `type == "purchase"` |
| **Home** | Produtos/prestadores em destaque | `featured == true`, região do usuário |

---

## 👤 PÁGINA PÚBLICA DE PERFIL (PublicUserProfileScreen)

### **Estrutura Unificada**

**Route**: `user_profile/{userId}`

**Abas Padronizadas** (Layout Similar ao Facebook):
```
┌─────────────────────────────────┐
│ Feed | Produtos | Avaliações    │
└─────────────────────────────────┘
```

### **Aba "Feed"** (Todos os tipos)
- ✅ Posts do usuário
- ✅ Stories do usuário
- ✅ Fonte: `locations/{locationId}/posts` e `locations/{locationId}/stories`
- ✅ Filtro: `authorId == userId`

### **Aba "Produtos"** (Todos os tipos, conteúdo apenas para PARCEIRO)
- ✅ **Para PARCEIRO**: Produtos cadastrados pelo parceiro
  - Fonte: `locations/{locationId}/products`
  - Filtro: `sellerId == userId` e `active == true`
- ✅ **Para CLIENTE**: Mensagem "Este usuário não vende produtos"

### **Aba "Avaliações"** (Todos os tipos)
- ✅ Todas as avaliações recebidas pelo usuário
- ✅ Fonte: `locations/{locationId}/reviews`
- ✅ Filtro: `targetId == userId` e `type == "PROVIDER"`
- ✅ **LEI MÁXIMA**: Usa `city`/`state` do usuário avaliado para buscar em `locations/{locationId}/reviews`
- ✅ Ordenadas por data (mais recente primeiro)

### **Fonte de Dados**

**Produtos**:
```kotlin
// Busca produtos do vendedor usando city/state do vendedor
firestoreProductsRepository.observeProductsBySeller(userId)
// Internamente usa: locations/{locationId}/products onde sellerId == userId
```

**Avaliações**:
```kotlin
// Busca avaliações usando city/state do usuário avaliado
reviewsRepository.observeProviderReviews(userId)
// Internamente:
// 1. Busca user para obter city/state
// 2. Usa locations/{locationId}/reviews onde targetId == userId
```

**Feed**:
```kotlin
// Busca posts e stories usando city/state do autor
// UserFeedScreen usa locations/{locationId}/posts e locations/{locationId}/stories
```

### **Garantias**

1. ✅ **Todos os dados filtrados por city/state**: Produtos, avaliações e feed usam `locations/{locationId}`
2. ✅ **Layout unificado**: Todas as contas têm as mesmas 3 abas
3. ✅ **Conteúdo adaptado**: Aba Produtos só mostra conteúdo para PARCEIRO
4. ✅ **Dados sempre atualizados**: Todas as queries usam listeners em tempo real

---

## ✅ Garantias da Arquitetura

1. ✅ **Dados Corretos**: Todos os dados vêm de `locations/{locationId}` baseado no `city`/`state` do usuário
2. ✅ **Isolamento por Role**: CLIENTE e PARCEIRO veem dados diferentes e relevantes
3. ✅ **Isolamento por Localização**: Usuários veem apenas dados da sua região
4. ✅ **Filtragem Automática**: Filtros são aplicados automaticamente baseados no tipo de conta
5. ✅ **Tempo Real**: Todas as mudanças são refletidas em tempo real via Firestore listeners
6. ✅ **Soft Delete**: Dados deletados são marcados como `deleted = true` ou `active = false`, não removidos fisicamente
7. ✅ **Padronização de Dados**: **TODOS** os dados (produtos, avaliações, posts, ordens) são filtrados por `city`/`state` usando `locations/{locationId}`
8. ✅ **Página Pública Unificada**: Layout similar ao Facebook com abas: Feed, Produtos, Avaliações

---

## 🎯 Conclusão

**SIM, com essa nova arquitetura, TUDO será exibido em seu devido lugar!**

- ✅ **CLIENTE** vê apenas suas ordens, produtos de outros, e prestadores disponíveis
- ✅ **PARCEIRO** vê apenas ordens aceitas, ordens disponíveis para aceitar, e seus próprios produtos
- ✅ **Todos** veem apenas dados da sua região (`locations/{locationId}`)
- ✅ **Filtros automáticos** garantem que cada tipo de conta veja apenas dados relevantes
- ✅ **Abas corretas** separam ordens por status (Ativas, Canceladas, Concluídas)

A arquitetura garante que **não há vazamento de dados** entre tipos de conta e que **todos os dados são filtrados corretamente** por localização, role e status.

---

**Fim do Documento**
