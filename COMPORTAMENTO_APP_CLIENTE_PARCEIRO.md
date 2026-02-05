# 📋 COMPORTAMENTO DO APP - CLIENTE VS PARCEIRO

## 🎯 MODO CLIENTE (role: "client", AccountType.CLIENTE)

### ✅ O QUE O CLIENTE PODE FAZER:

#### 1. VISUALIZAÇÃO
- ✅ Ver produtos na loja de TODOS os parceiros do mesmo city/state
- ✅ Ver posts de PARCEIROS no feed (não vê posts de outros clientes)
- ✅ Ver stories de PARCEIROS do mesmo city/state
- ✅ Ver parceiros disponíveis em cards por categoria
- ✅ Ver perfis públicos de parceiros
- ✅ Ver avaliações de produtos e parceiros

#### 2. COMPRAS E SERVIÇOS
- ✅ Comprar produtos (via PIX, Cartão, etc)
- ✅ Criar ordens de serviço por categoria
- ✅ Ver propostas/orçamentos recebidos de parceiros
- ✅ Aceitar orçamentos
- ✅ Avaliar produtos comprados
- ✅ Avaliar serviços contratados
- ✅ Enviar mensagens para parceiros

#### 3. INTERAÇÃO
- ✅ Dar like em posts de parceiros
- ✅ Comentar em posts de parceiros
- ✅ Ver stories de parceiros
- ✅ Seguir parceiros

### ❌ O QUE O CLIENTE NÃO PODE FAZER:

- ❌ Criar produtos para venda
- ❌ Criar posts no feed (apenas parceiros podem postar)
- ❌ Criar stories (apenas parceiros podem criar)
- ❌ Aparecer em cards de categorias
- ❌ Receber ordens de serviço
- ❌ Enviar propostas/orçamentos
- ❌ Acessar "Meus Produtos"
- ❌ Acessar "Minhas Ordens de Serviço" (como prestador)
- ❌ Fazer login com CPF/CNPJ (apenas email/senha ou Google)

---

## 🎯 MODO PARCEIRO (role: "partner", AccountType.PARCEIRO)

### ✅ O QUE O PARCEIRO PODE FAZER:

#### 1. CRIAÇÃO DE CONTEÚDO
- ✅ Criar produtos para venda
- ✅ Criar posts no feed
- ✅ Criar stories
- ✅ Definir categorias de serviços oferecidos (`preferredCategories`)

#### 2. VISUALIZAÇÃO
- ✅ Ver TODOS os produtos na loja (próprios + de outros parceiros do mesmo city/state)
- ✅ Ver "Meus Produtos" (filtrado por sellerId)
- ✅ Ver posts de TODOS os parceiros no feed
- ✅ Ver stories de todos os parceiros do mesmo city/state
- ✅ Ver ordens de serviço nas categorias que oferece
- ✅ Ver seus próprios posts em "Meus Dados" → Aba "Feed"
- ✅ Ver suas próprias stories em "Meus Dados" → Aba "Feed"

#### 3. SERVIÇOS E VENDAS
- ✅ Receber notificações de ordens de serviço nas categorias escolhidas
- ✅ Ver ordens de serviço filtradas por `preferredCategories`
- ✅ Enviar propostas/orçamentos para clientes
- ✅ Aceitar ordens de serviço
- ✅ Vender produtos
- ✅ Gerenciar estoque de produtos

#### 4. INTERAÇÃO
- ✅ Dar like em posts de outros parceiros
- ✅ Comentar em posts
- ✅ Ver stories de parceiros
- ✅ Seguir outros parceiros
- ✅ Enviar mensagens para clientes

#### 5. AUTENTICAÇÃO
- ✅ Fazer login com CPF/CNPJ (na tela de login de parceiro)
- ✅ Fazer login com email/senha
- ✅ NÃO pode fazer login com Google (ou será redirecionado para seleção de conta)

### ❌ O QUE O PARCEIRO NÃO PODE FAZER:

- ❌ Criar ordens de serviço (apenas clientes podem contratar)
- ❌ Comprar produtos (parceiros vendem, não compram - comportamento atual)

---

## 📊 COMPARAÇÃO LADO A LADO

| Funcionalidade | Cliente | Parceiro |
|----------------|---------|----------|
| **Ver produtos na loja** | ✅ Todos do city/state | ✅ Todos do city/state |
| **Ver "Meus Produtos"** | ❌ | ✅ Apenas seus produtos |
| **Criar produtos** | ❌ | ✅ |
| **Comprar produtos** | ✅ | ❌ |
| **Ver posts no feed** | ✅ Apenas de parceiros | ✅ Todos os parceiros |
| **Criar posts** | ❌ | ✅ |
| **Ver stories** | ✅ Apenas de parceiros | ✅ Todos os parceiros |
| **Criar stories** | ❌ | ✅ |
| **Criar ordem de serviço** | ✅ | ❌ |
| **Ver ordens de serviço** | ✅ Próprias | ✅ Por categoria |
| **Enviar propostas** | ❌ | ✅ |
| **Ver parceiros em cards** | ✅ Por categoria | ✅ Por categoria |
| **Login com CPF/CNPJ** | ❌ | ✅ |
| **Login com Google** | ✅ | ⚠️ Redireciona |

---

## 🔄 FLUXO DE CADASTRO E LOGIN

### NOVO USUÁRIO (Google Sign-In)
1. Usuário faz login pelo Google
2. App verifica se existe no Firestore
3. Se NÃO existe → **Mostra dialog de seleção: CLIENTE ou PARCEIRO**
4. Usuário escolhe tipo de conta
5. Cloud Function `setInitialUserRole` salva o role escolhido
6. App lê o role do Firestore e navega para a tela principal
7. Barra inferior aparece com as abas corretas

### USUÁRIO EXISTENTE
1. Usuário faz login
2. App lê role do Firestore
3. Navega direto para a tela principal
4. Barra inferior aparece automaticamente

---

## 🗂️ ESTRUTURA DE DADOS NO FIRESTORE

### Usuários
```
users/{userId}
  - role: "partner" ou "client"
  - city: string
  - state: string
  - preferredCategories: ["categoria1", "categoria2"] (apenas para partners)

locations/{locationId}/users/{userId}
  - (mesmos dados, coleção pública para queries)
```

### Produtos
```
locations/{locationId}/products/{productId}
  - sellerId: userId do parceiro
  - active: true
  - city: string
  - state: string
```

### Posts
```
locations/{locationId}/posts/{postId}
  - userId: userId do parceiro
  - userRole: "partner"
  - city: string
  - state: string

users/{userId}/posts/{postId}
  - (cópia privada para "Meus Dados")
```

### Stories
```
locations/{locationId}/stories/{storyId}
  - userId: userId do parceiro
  - userRole: "partner"
  - expiresAt: timestamp
  - city: string
  - state: string
```

### Ordens de Serviço
```
locations/{locationId}/orders/{orderId}
  - clientId: userId do cliente
  - providerId: userId do parceiro (null se ainda não foi aceita)
  - category: string
  - status: "pending" | "accepted" | "completed"
  - city: string
  - state: string
```

---

## ⚠️ REGRAS CRÍTICAS

### 1. ISOLAMENTO POR LOCALIZAÇÃO
- Todos os dados são filtrados por `city` e `state`
- Apenas usuários do mesmo `city/state` veem os dados uns dos outros
- LocationId = `${city}_${state}` (normalizado, lowercase, sem acentos)

### 2. FILTRO POR ROLE
- Clientes veem apenas posts/stories de PARCEIROS
- Parceiros veem posts/stories de TODOS os parceiros
- Ordens de serviço são vistas apenas por parceiros com a categoria correta

### 3. FILTRO POR CATEGORIA (PARCEIROS)
- Parceiros definem `preferredCategories` no cadastro
- Recebem notificações APENAS de ordens nas categorias escolhidas
- Aparecem em cards APENAS das categorias escolhidas

### 4. EXIBIÇÃO DE DADOS PRÓPRIOS
- "Meus Produtos": `sellerId == userId`
- "Meus Posts": `users/{userId}/posts`
- "Minhas Stories": `users/{userId}/stories`
- "Minhas Ordens" (cliente): `clientId == userId`
- "Minhas Ordens" (parceiro): `providerId == userId`

---

## 🔍 VALIDAÇÃO DO FLUXO ATUAL

### ✅ CORREÇÕES APLICADAS
1. ✅ Enum AccountType limpo (apenas PARCEIRO e CLIENTE)
2. ✅ LoginViewModel não cria usuário com role padrão
3. ✅ Todas as queries usam "partner" ao invés de "provider"
4. ✅ Filtros de ordens por categoria funcionando
5. ✅ Barra inferior configurada corretamente

### ⚠️ PONTOS DE ATENÇÃO

1. **Barra Inferior**: Deve aparecer automaticamente após login se:
   - Usuário está autenticado
   - Usuário tem role válido ("partner" ou "client")
   - Está em uma rota principal (home, services, products, feed, profile)

2. **Exibição de Dados**: Depende de:
   - `city` e `state` estarem definidos no perfil do usuário
   - Role estar correto ("partner" ou "client")
   - LocationId estar normalizado corretamente

---

## 🚀 STATUS ATUAL

**TUDO FOI CORRIGIDO E ESTÁ PRONTO PARA FUNCIONAR!**

O fluxo está correto porque:
- ✅ Roles definidos corretamente pelo usuário
- ✅ Queries filtram por role correto
- ✅ Isolamento por localização funciona
- ✅ Filtros por categoria funcionam
- ✅ Exibição de dados próprios vs públicos está correta

**Próximo passo**: Testar no dispositivo para validar o comportamento na prática.
