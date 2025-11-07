# 📚 Coleções do Firestore - TaskGo App

## O que são Coleções?

As **coleções** no Firestore são como "tabelas" em um banco de dados tradicional. Elas armazenam documentos (registros) que contêm os dados do seu aplicativo.

**Importante:** As coleções são criadas automaticamente quando você adiciona o primeiro documento a elas. Você não precisa criar manualmente no console do Firebase.

## 📋 Coleções do TaskGo App

### 1. **`users`** - Usuários do Sistema

**Para que serve:**
- Armazenar perfis de usuários (clientes, prestadores, admins)
- Informações pessoais, configurações, documentos
- Status de verificação e perfil completo

**Campos principais:**
- `uid`: ID único do usuário (mesmo do Firebase Auth)
- `email`: Email do usuário
- `displayName`: Nome completo
- `phone`: Telefone
- `role`: "client", "provider" ou "admin"
- `profileComplete`: Se o perfil está completo
- `verified`: Se o email foi verificado
- `stripeAccountId`: ID da conta Stripe (para pagamentos)
- `createdAt`: Data de criação
- `updatedAt`: Data da última atualização

**Como inserir dados:**
O app Android cria automaticamente quando o usuário se cadastra:
```kotlin
// Isso acontece automaticamente no SignupViewModel
firestoreUserRepository.updateUser(userFirestore)
```

---

### 2. **`products`** - Produtos à Venda

**Para que serve:**
- Catálogo de produtos disponíveis para compra
- Informações de produtos (nome, preço, descrição, imagens)
- Controle de estoque e status ativo/inativo

**Campos principais:**
- `id`: ID único do produto
- `title`: Nome do produto
- `price`: Preço
- `description`: Descrição detalhada
- `sellerId`: ID do vendedor (usuário)
- `sellerName`: Nome do vendedor
- `imageUrls`: Lista de URLs das imagens
- `category`: Categoria do produto
- `tags`: Tags para busca
- `active`: Se o produto está ativo (true/false)
- `createdAt`: Data de criação
- `updatedAt`: Data da última atualização

**Como inserir dados:**
O app Android cria quando um vendedor cadastra um produto:
```kotlin
// Isso acontece quando o usuário cria um produto
productsRepository.upsertProduct(product)
```

---

### 3. **`orders`** - Pedidos de Serviços

**Para que serve:**
- Pedidos de serviços (ex: "Preciso de um encanador")
- Rastreamento de status do pedido
- Comunicação entre cliente e prestador

**Campos principais:**
- `id`: ID único do pedido
- `clientId`: ID do cliente
- `providerId`: ID do prestador (pode ser null inicialmente)
- `status`: Status do pedido (pending, accepted, in_progress, completed, cancelled)
- `description`: Descrição do serviço solicitado
- `proposalDetails`: Detalhes da proposta do prestador
- `createdAt`: Data de criação
- `updatedAt`: Data da última atualização

**Como inserir dados:**
O app Android cria quando um cliente solicita um serviço:
```kotlin
// Isso acontece quando o cliente cria um pedido de serviço
firestoreOrderRepository.createOrder(...)
```

---

### 4. **`purchase_orders`** - Pedidos de Produtos

**Para que serve:**
- Pedidos de compra de produtos
- Rastreamento de entrega
- Histórico de compras

**Campos principais:**
- `id`: ID único do pedido
- `orderNumber`: Número do pedido (ex: "TG1234567890")
- `clientId`: ID do cliente
- `status`: Status do pedido (EM_ANDAMENTO, CONCLUIDO, CANCELADO)
- `items`: Lista de itens do pedido
- `total`: Valor total
- `subtotal`: Subtotal
- `deliveryFee`: Taxa de entrega
- `paymentMethod`: Método de pagamento
- `deliveryAddress`: Endereço de entrega
- `createdAt`: Data de criação
- `updatedAt`: Data da última atualização

**Como inserir dados:**
O app Android cria quando um cliente finaliza uma compra:
```kotlin
// Isso acontece no checkout quando o cliente finaliza a compra
ordersRepository.createOrder(cart, total, paymentMethod, addressId)
```

---

### 5. **`services`** - Serviços Oferecidos

**Para que serve:**
- Serviços oferecidos pelos prestadores
- Catálogo de serviços disponíveis
- Informações sobre serviços específicos

**Campos principais:**
- `id`: ID único do serviço
- `providerId`: ID do prestador
- `title`: Título do serviço
- `description`: Descrição
- `price`: Preço base
- `category`: Categoria
- `active`: Se está ativo
- `createdAt`: Data de criação
- `updatedAt`: Data da última atualização

**Como inserir dados:**
O app Android cria quando um prestador cadastra um serviço.

---

### 6. **`reviews`** - Avaliações

**Para que serve:**
- Avaliações de serviços prestados
- Sistema de classificação (estrelas)
- Comentários sobre serviços

**Campos principais:**
- `id`: ID único da avaliação
- `serviceId`: ID do serviço avaliado
- `clientId`: ID do cliente que avaliou
- `rating`: Nota (1-5 estrelas)
- `comment`: Comentário
- `createdAt`: Data da avaliação

**Como inserir dados:**
O app Android cria quando um cliente avalia um serviço.

---

## 🔐 Regras de Segurança

As regras do Firestore garantem que:
- ✅ Apenas usuários autenticados podem criar/ler/atualizar dados
- ✅ Usuários só podem modificar seus próprios dados
- ✅ Admins podem fazer tudo
- ✅ Produtos só podem ser criados/atualizados por seus donos

## 📝 Como Inserir Dados Manualmente (via Console)

Se você quiser inserir dados manualmente no console do Firebase:

1. Acesse: https://console.firebase.google.com
2. Selecione o projeto: `task-go-ee85f`
3. Vá em **Firestore Database** > **Dados**
4. Clique em **"+ Adicionar coleção"**
5. Digite o nome da coleção (ex: `users`)
6. Clique em **"Próximo"**
7. Adicione o primeiro documento:
   - **ID do documento**: Pode deixar automático ou usar o `uid` do usuário
   - **Campos**: Adicione os campos um por um (ex: `email`, `displayName`, etc.)
8. Clique em **"Salvar"**

## ⚠️ Importante

**Não é necessário inserir dados manualmente!** O app Android já faz isso automaticamente quando:
- Usuário se cadastra → cria documento em `users`
- Vendedor cadastra produto → cria documento em `products`
- Cliente faz pedido → cria documento em `orders` ou `purchase_orders`

## 🔗 Conexão MongoDB (Opcional)

Você pode conectar ferramentas MongoDB ao Firestore usando:
```
mongodb://taskgomaster:WInoNWyvp0XLru_Jal-z1yjZpIQ316yqmjSrCRMX-b0E3b2l@df7f20f8-abda-484c-bb47-3b309f569d09.nam5.firestore.goog:443/taskgo?loadBalanced=true&tls=true&authMechanism=SCRAM-SHA-256&retryWrites=false
```

Isso permite usar ferramentas como MongoDB Compass para visualizar e gerenciar os dados do Firestore.

