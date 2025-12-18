# Instruções para Deploy e Configuração

## 📦 Deploy das Cloud Functions

### Pré-requisitos
1. Node.js instalado (versão 18 ou superior)
2. Firebase CLI instalado: `npm install -g firebase-tools`
3. Autenticado no Firebase: `firebase login`

### Comandos para Deploy

```bash
# Navegar para o diretório do projeto
cd C:\Users\user\AndroidStudioProjects\TaskGoApp

# Navegar para functions
cd functions

# Instalar dependências (se ainda não instalou)
npm install

# Fazer deploy de todas as functions
firebase deploy --only functions

# Ou fazer deploy de functions específicas
firebase deploy --only functions:deleteUserAccount
firebase deploy --only functions:createOrder
firebase deploy --only functions:updateOrderStatus
firebase deploy --only functions:createPaymentIntent
firebase deploy --only functions:confirmPayment
```

### Functions Disponíveis

Todas as functions estão implementadas em `functions/src/`:

- ✅ `deleteUserAccount` - Exclusão completa de conta do usuário
- ✅ `createOrder` - Criação de ordens de serviço
- ✅ `updateOrderStatus` - Atualização de status de ordens
- ✅ `getMyOrders` - Buscar ordens do usuário
- ✅ `onServiceOrderCreated` - Trigger para notificar prestadores
- ✅ `createPaymentIntent` - Criar intenção de pagamento (Stripe)
- ✅ `confirmPayment` - Confirmar pagamento
- ✅ `requestRefund` - Solicitar reembolso
- ✅ `createOnboardingLink` - Link de onboarding Stripe Connect
- ✅ `notifications` - Funções de notificação
- ✅ `ai-chat` - Suporte de IA
- ✅ `identityVerification` - Verificação de identidade

## 🔍 Criar Índices Compostos no Firestore

### Método 1: Via Firebase Console (Recomendado)

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/indexes
2. Clique em "Criar Índice"
3. Para cada índice abaixo, preencha os campos e clique em "Criar"

### Método 2: Via arquivo firestore.indexes.json

Crie/atualize o arquivo `firestore.indexes.json` na raiz do projeto:

```json
{
  "indexes": [
    {
      "collectionGroup": "services",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "providerId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "services",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "category",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "active",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "orders",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "clientId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "status",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "orders",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "providerId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "status",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "orders",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "status",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "category",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "products",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "sellerId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "active",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "reviews",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "targetId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "type",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    }
  ],
  "fieldOverrides": []
}
```

Depois, faça deploy dos índices:

```bash
firebase deploy --only firestore:indexes
```

## 📋 Lista de Índices Necessários

### 1. Services - Por Provider
- **Collection**: `services`
- **Campos**: `providerId` (ASC), `createdAt` (DESC)
- **Uso**: Listar serviços de um prestador

### 2. Services - Por Categoria
- **Collection**: `services`
- **Campos**: `category` (ASC), `active` (ASC), `createdAt` (DESC)
- **Uso**: Buscar serviços por categoria

### 3. Orders - Por Cliente
- **Collection**: `orders`
- **Campos**: `clientId` (ASC), `status` (ASC), `createdAt` (DESC)
- **Uso**: Listar ordens de um cliente por status

### 4. Orders - Por Prestador
- **Collection**: `orders`
- **Campos**: `providerId` (ASC), `status` (ASC), `createdAt` (DESC)
- **Uso**: Listar ordens de um prestador por status

### 5. Orders - Por Categoria
- **Collection**: `orders`
- **Campos**: `status` (ASC), `category` (ASC), `createdAt` (DESC)
- **Uso**: Buscar ordens pendentes por categoria

### 6. Products - Por Vendedor
- **Collection**: `products`
- **Campos**: `sellerId` (ASC), `active` (ASC), `createdAt` (DESC)
- **Uso**: Listar produtos de um vendedor

### 7. Reviews - Por Target
- **Collection**: `reviews`
- **Campos**: `targetId` (ASC), `type` (ASC), `createdAt` (DESC)
- **Uso**: Listar avaliações de um produto/serviço/prestador

## ⚙️ Configurações de Ambiente

### Variáveis de Ambiente para Cloud Functions

Configure no Firebase Console ou via arquivo `.env`:

```bash
# Stripe (se usar pagamentos)
STRIPE_SECRET_KEY=sk_test_...

# OpenAI (se usar chat AI)
OPENAI_API_KEY=sk-...

# Outras configurações
```

### Configurar no Firebase Console:
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/functions/config
2. Adicione as variáveis de ambiente necessárias

## ✅ Checklist de Deploy

- [ ] Node.js instalado
- [ ] Firebase CLI instalado e autenticado
- [ ] Dependências das functions instaladas (`npm install` em `functions/`)
- [ ] Variáveis de ambiente configuradas
- [ ] Deploy das Cloud Functions realizado
- [ ] Índices do Firestore criados
- [ ] Testes das functions realizados
- [ ] Logs verificados no Firebase Console

## 🧪 Testar Functions

Após o deploy, teste as functions principais:

1. **deleteUserAccount**: Testar exclusão de conta
2. **createOrder**: Criar uma ordem de teste
3. **updateOrderStatus**: Atualizar status de uma ordem
4. **onServiceOrderCreated**: Verificar se notificações são enviadas

## 📝 Notas Importantes

- Os índices podem levar alguns minutos para serem criados
- Verifique os logs das functions no Firebase Console em caso de erros
- Mantenha as variáveis de ambiente seguras (não commitar no Git)
- Faça backup antes de fazer deploy em produção

