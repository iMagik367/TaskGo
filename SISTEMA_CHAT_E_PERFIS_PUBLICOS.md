# 💬 Sistema de Chat e Perfis Públicos - TaskGo

## 📋 Índice

1. [Sistema de Chat](#sistema-de-chat)
2. [Páginas Públicas de Perfil](#páginas-públicas-de-perfil)
3. [Acesso às Páginas Públicas](#acesso-às-páginas-públicas)

---

## 💬 Sistema de Chat

### **Arquitetura**

O sistema de chat do TaskGo usa **Firebase Realtime Database** para mensagens em tempo real, com cache local via Room Database para funcionamento offline.

#### **Estrutura de Dados**

```
Firebase Realtime Database:
├── conversations/{threadId}
│   ├── title: String
│   ├── lastMessage: String
│   ├── lastTime: Long
│   ├── participants: Map<String, Boolean>
│   │   ├── userId1: true
│   │   └── userId2: true
│   ├── orderId: String? (opcional - se relacionado a uma ordem)
│   └── createdAt: Long
│
└── messages/{threadId}/{messageId}
    ├── senderId: String
    ├── text: String
    └── time: Long
```

**Cache Local (Room)**:
- `MessageThreadEntity`: Threads de conversa
- `ChatMessageEntity`: Mensagens individuais

---

### **Funcionalidades do Chat**

#### **1. Lista de Conversas (MessagesScreen)**

**Path**: Bottom Navigation → "Mensagens"

**O que é Exibido**:
- ✅ Lista de todas as conversas do usuário
- ✅ Última mensagem de cada conversa
- ✅ Timestamp da última mensagem
- ✅ Ordenadas por data (mais recente primeiro)

**Fonte de Dados**:
```kotlin
// Observa conversations no Firebase Realtime Database
// Filtro: participants/{currentUserId} == true
messageRepository.observeThreads()
```

**Ações Disponíveis**:
- ✅ Clicar em uma conversa → Abre `ChatScreen`
- ✅ Botão "Encontrar Prestadores" (para CLIENTE)
- ✅ Botão "Encontrar Ordens" (para PARCEIRO)

---

#### **2. Tela de Chat (ChatScreen)**

**Path**: `MessagesScreen` → Clicar em uma conversa

**Route**: `chat/{threadId}`

**O que é Exibido**:
- ✅ Lista de mensagens da conversa
- ✅ Balões de mensagem (verde para enviadas, cinza para recebidas)
- ✅ Timestamp de cada mensagem
- ✅ Campo de texto para enviar mensagem
- ✅ Botão de envio

**Fonte de Dados**:
```kotlin
// Observa messages/{threadId} no Firebase Realtime Database
// Ordenadas por time (crescente)
messageRepository.observeMessages(threadId)
```

**Ações Disponíveis**:
- ✅ Enviar mensagem de texto
- ✅ Scroll automático para última mensagem
- ✅ Atualização em tempo real

---

### **Criação de Conversas**

#### **1. Conversa a partir de Perfil Público**

**Fluxo**:
```kotlin
// 1. Usuário clica em "Mensagem" no perfil público
onMessageClick(userId)

// 2. Sistema busca ou cria thread entre os dois usuários
val threadId = messageRepository.getOrCreateThreadForProvider(providerId, userRepository)

// 3. Navega para ChatScreen
navController.navigate("chat/$threadId")
```

**Lógica**:
- ✅ Busca thread existente entre os dois usuários
- ✅ Se não existir, cria nova thread
- ✅ Título da thread = nome do outro usuário

---

#### **2. Conversa a partir de Ordem de Serviço**

**Fluxo**:
```kotlin
// 1. Usuário clica em "Chat" em uma ordem
onNavigateToChat(orderId)

// 2. Sistema busca ou cria thread para a ordem
val threadId = messageRepository.getOrCreateThreadForOrder(
    orderId = orderId,
    orderRepository = orderRepository,
    userRepository = userRepository
)

// 3. Navega para ChatScreen
navController.navigate("chat/$threadId")
```

**Lógica**:
- ✅ Busca ordem para obter `clientId` e `providerId`
- ✅ Identifica o outro usuário (se for cliente, pega provider; se for provider, pega cliente)
- ✅ Busca thread existente com `orderId` associado
- ✅ Se não existir, cria nova thread com `orderId` no metadata
- ✅ Título da thread = nome do outro usuário

---

### **Envio de Mensagens**

**Fluxo**:
```kotlin
// 1. Usuário digita mensagem e clica em enviar
viewModel.sendMessage(threadId, text)

// 2. Sistema salva mensagem
messageRepository.sendMessage(threadId, text)

// 3. Atualiza thread com última mensagem
threadsRef.child(threadId).updateChildren(
    mapOf(
        "lastMessage" to text,
        "lastTime" to timestamp
    )
)
```

**Otimizações**:
- ✅ **Cache Local Primeiro**: Mensagem é salva no Room Database imediatamente (otimista)
- ✅ **Sincronização Firebase**: Depois sincroniza com Firebase Realtime Database
- ✅ **Atualização em Tempo Real**: Outro usuário recebe mensagem instantaneamente

---

### **Sincronização Offline**

- ✅ Mensagens são salvas no cache local (Room) primeiro
- ✅ Quando online, sincroniza com Firebase
- ✅ Se offline, mensagens ficam no cache e sincronizam quando voltar online
- ✅ Threads também são cacheadas localmente

---

## 👤 Páginas Públicas de Perfil

### **Arquitetura**

A página pública de perfil (`PublicUserProfileScreen`) é **unificada** para todos os tipos de conta, mas exibe conteúdo diferente baseado no tipo de conta do usuário visualizado.

**Route**: `user_profile/{userId}`

---

### **Estrutura da Página Pública**

#### **1. Header do Perfil**

**O que é Exibido**:
- ✅ Avatar do usuário
- ✅ Nome (displayName)
- ✅ Tipo de conta (Cliente, Parceiro)
- ✅ Avaliação média (se houver)
- ✅ Número de avaliações

**Fonte de Dados**:
```kotlin
// Busca em users/{userId} (global) ou locations/{locationId}/users/{userId}
userRepository.getUser(userId)
```

---

#### **2. Abas (Tabs)** - Layout Similar ao Facebook

**Para TODOS os tipos de conta**:
```
┌─────────────────────────────────┐
│ Feed | Produtos | Avaliações    │
└─────────────────────────────────┘
```

**Padronização**: Todas as contas têm as mesmas 3 abas, mas o conteúdo varia:
- **Feed**: Posts e stories do usuário (todos)
- **Produtos**: Produtos à venda (apenas para PARCEIRO, vazio para CLIENTE)
- **Avaliações**: Avaliações recebidas (todos)

---

#### **3. Conteúdo das Abas**

##### **Aba "Feed"** (Todos os tipos)
- ✅ Posts do usuário
- ✅ Stories do usuário
- ✅ Fonte: `locations/{locationId}/posts` e `locations/{locationId}/stories`
- ✅ Filtro: `authorId == userId`

##### **Aba "Produtos"** (Todos os tipos, mas conteúdo apenas para PARCEIRO)
- ✅ Produtos cadastrados pelo parceiro
- ✅ Grid de produtos (2 colunas)
- ✅ Fonte: `locations/{locationId}/products`
- ✅ Filtro: `sellerId == userId` e `active == true`

##### **Aba "Avaliações"** (Todos os tipos)
- ✅ **Todas as avaliações recebidas** pelo usuário
- ✅ Lista de avaliações com:
  - Nome do avaliador
  - Rating (estrelas)
  - Comentário (se houver)
  - Data da avaliação
- ✅ Fonte: `locations/{locationId}/reviews`
- ✅ Filtro: `targetId == userId` e `type == "PROVIDER"`
- ✅ **LEI MÁXIMA**: Usa `city`/`state` do usuário avaliado para buscar em `locations/{locationId}/reviews`

---

#### **4. Barra de Ações (Bottom Bar)**

**Apenas se NÃO for o próprio perfil**:
```
┌─────────────────────────────────────┐
│ [Avaliar] [Postar] [Mensagem]      │
└─────────────────────────────────────┘
```

**Ações**:
- ✅ **Avaliar**: Abre tela de criação de avaliação
- ✅ **Postar**: Abre tela de criação de post (no feed do usuário)
- ✅ **Mensagem**: Cria ou abre conversa com o usuário

---

## 🚪 Acesso às Páginas Públicas

### **1. A partir de Cards de Prestadores (ServicesScreen)**

**Contexto**: CLIENTE está na tela "Serviços" e selecionou uma categoria

**Fluxo**:
```kotlin
// 1. CLIENTE vê lista de prestadores filtrados por categoria
ProviderCard(
    provider = provider,
    onProviderClick = { providerId ->
        // 2. Clica no card do prestador
        onNavigateToServiceDetail(providerId)
    }
)

// 3. Navega para perfil público
navController.navigate("user_profile/$providerId")
```

**O que o CLIENTE vê**:
- ✅ Perfil completo do prestador
- ✅ Categorias de serviços oferecidos
- ✅ Produtos à venda (se houver)
- ✅ Avaliações
- ✅ Pode enviar mensagem, avaliar, ou postar

---

### **2. A partir de Cards de Produtos**

**Contexto**: Usuário está na tela "Loja" ou "Home"

**Fluxo**:
```kotlin
// 1. Usuário vê card de produto
ProductCard(
    product = product,
    onClick = { productId ->
        // 2. Clica no produto
        onNavigateToProductDetail(productId)
    }
)

// 3. Na tela de detalhes do produto, há botão "Ver Perfil do Vendedor"
// 4. Navega para perfil público do vendedor
navController.navigate("user_profile/${product.sellerId}")
```

**O que o usuário vê**:
- ✅ Perfil completo do vendedor
- ✅ Todos os produtos à venda
- ✅ Avaliações
- ✅ Pode enviar mensagem, avaliar, ou postar

---

### **3. A partir de Ordens de Serviço**

**Contexto**: PARCEIRO está na tela "Serviços" vendo ordens disponíveis

**Fluxo**:
```kotlin
// 1. PARCEIRO vê card de ordem de serviço
ServiceOrderCardFirestore(
    order = order,
    onServiceClick = { orderId ->
        // 2. Clica na ordem
        onNavigateToServiceDetail(orderId)
    }
)

// 3. Na tela de detalhes da ordem, há informações do cliente
// 4. Pode navegar para perfil do cliente
navController.navigate("user_profile/${order.clientId}")
```

**O que o PARCEIRO vê**:
- ✅ Perfil do cliente que criou a ordem
- ✅ Feed do cliente
- ✅ Avaliações
- ✅ Pode enviar mensagem, avaliar, ou postar

---

### **4. A partir de "Meus Serviços" (Parceiro)**

**Contexto**: PARCEIRO está na tela "Meus Serviços" vendo ordens aceitas

**Fluxo**:
```kotlin
// 1. PARCEIRO vê ordem aceita
// 2. Na tela de detalhes da ordem, pode ver perfil do cliente
navController.navigate("user_profile/${order.clientId}")
```

---

### **5. A partir de "Minhas Ordens" (Cliente)**

**Contexto**: CLIENTE está na tela "Minhas Ordens" vendo suas ordens

**Fluxo**:
```kotlin
// 1. CLIENTE vê ordem com prestador aceito
// 2. Na tela de detalhes da ordem, pode ver perfil do prestador
navController.navigate("user_profile/${order.providerId}")
```

---

### **6. A partir de Feed/Posts**

**Contexto**: Usuário está no feed e vê um post

**Fluxo**:
```kotlin
// 1. Usuário vê post no feed
PostCard(
    post = post,
    onAuthorClick = { authorId ->
        // 2. Clica no nome/autor do post
        navController.navigate("user_profile/$authorId")
    }
)
```

---

### **7. A partir de Avaliações**

**Contexto**: Usuário está vendo avaliações de um prestador

**Fluxo**:
```kotlin
// 1. Usuário vê avaliação
ReviewCard(
    review = review,
    onReviewerClick = { reviewerId ->
        // 2. Clica no nome do avaliador
        navController.navigate("user_profile/$reviewerId")
    }
)
```

---

## 📊 Resumo: Pontos de Acesso

### **Para CLIENTE**

| Origem | Destino | Route |
|--------|---------|-------|
| **ServicesScreen** (Card de Prestador) | Perfil do Prestador | `user_profile/{providerId}` |
| **ProductsScreen** (Card de Produto) | Perfil do Vendedor | `user_profile/{sellerId}` |
| **MyServiceOrdersScreen** (Ordem com prestador) | Perfil do Prestador | `user_profile/{providerId}` |
| **FeedScreen** (Post) | Perfil do Autor | `user_profile/{authorId}` |
| **ReviewsScreen** (Avaliação) | Perfil do Avaliador | `user_profile/{reviewerId}` |

---

### **Para PARCEIRO**

| Origem | Destino | Route |
|--------|---------|-------|
| **ServicesScreen** (Card de Ordem) | Perfil do Cliente | `user_profile/{clientId}` |
| **MeusServicosScreen** (Ordem aceita) | Perfil do Cliente | `user_profile/{clientId}` |
| **ProductsScreen** (Card de Produto) | Perfil do Vendedor | `user_profile/{sellerId}` |
| **FeedScreen** (Post) | Perfil do Autor | `user_profile/{authorId}` |
| **ReviewsScreen** (Avaliação) | Perfil do Avaliador | `user_profile/{reviewerId}` |

---

## 🔐 Segurança e Privacidade

### **Chat**

- ✅ Apenas participantes da thread podem ver mensagens
- ✅ Threads são filtradas por `participants/{userId} == true`
- ✅ Mensagens são privadas entre os dois usuários

### **Perfil Público**

- ✅ **Dados Públicos**:
  - Nome, avatar, tipo de conta
  - Categorias de serviços (parceiro)
  - Produtos à venda (parceiro)
  - Posts e stories
  - Avaliações recebidas
  - Localização (city/state)

- ✅ **Dados Privados** (NÃO exibidos):
  - Email
  - Telefone (opcional - pode ser exibido se o usuário permitir)
  - CPF/CNPJ
  - Endereço completo
  - Dados de pagamento

---

## 🎯 Fluxo Completo: Cliente → Prestador

### **Exemplo: CLIENTE quer contratar um prestador**

1. **CLIENTE** vai em "Serviços" → Seleciona categoria (ex: "Montagem")
2. **Sistema** mostra lista de prestadores com essa categoria em `preferredCategories`
3. **CLIENTE** clica no card de um prestador
4. **Sistema** navega para `user_profile/{providerId}`
5. **CLIENTE** vê:
   - Perfil do prestador
   - Categorias oferecidas
   - Produtos à venda (se houver)
   - Avaliações
6. **CLIENTE** clica em "Mensagem"
7. **Sistema** cria ou abre thread entre cliente e prestador
8. **Sistema** navega para `chat/{threadId}`
9. **CLIENTE** e **PRESTADOR** podem conversar em tempo real

---

## 🎯 Fluxo Completo: Parceiro → Cliente (Ordem)

### **Exemplo: PARCEIRO quer aceitar uma ordem**

1. **PARCEIRO** vai em "Serviços" → Vê ordens disponíveis
2. **Sistema** mostra ordens com `status == "pending"` e `providerId == null`
3. **PARCEIRO** clica em uma ordem
4. **Sistema** navega para `service_order_detail/{orderId}`
5. **PARCEIRO** vê detalhes da ordem e pode ver perfil do cliente
6. **PARCEIRO** clica em "Chat" ou "Aceitar Ordem"
7. **Sistema** cria ou abre thread para a ordem
8. **Sistema** navega para `chat/{threadId}`
9. **PARCEIRO** e **CLIENTE** podem conversar sobre a ordem

---

## ✅ Garantias

1. ✅ **Chat em Tempo Real**: Mensagens são sincronizadas instantaneamente via Firebase Realtime Database
2. ✅ **Cache Offline**: Mensagens ficam disponíveis offline via Room Database
3. ✅ **Perfis Públicos Unificados**: Mesma tela para todos os tipos de conta, com conteúdo adaptado
4. ✅ **Navegação Intuitiva**: Acesso fácil a perfis a partir de qualquer contexto
5. ✅ **Privacidade**: Apenas dados públicos são exibidos nos perfis
6. ✅ **Threads Inteligentes**: Sistema busca thread existente antes de criar nova

---

**Fim do Documento**
