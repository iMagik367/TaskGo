# Correções de Segurança - Realtime Database Rules

## Problemas Identificados e Corrigidos

### 1. **Regra Raiz Muito Permissiva (CRÍTICO)**
**Problema**: `.read": "auth != null"` e `.write": "auth != null"` permitiam que qualquer usuário autenticado acessasse qualquer dado.

**Correção**: Regra raiz bloqueada por padrão (default deny). Cada coleção agora tem regras explícitas.

### 2. **Escrita Direta em Coleções Públicas**
**Problema**: Usuários podiam escrever diretamente em `services`, `orders`, `products`, etc., sem validação adequada.

**Correção**: Todas as coleções públicas agora bloqueiam escrita direta. Escrita deve ser feita via Cloud Functions que usam Admin SDK.

### 3. **Falta de Validação de Dados**
**Problema**: Muitas coleções não tinham validação (`.validate`), permitindo dados inválidos.

**Correção**: Validações adicionadas onde necessário (presence, typing, users).

### 4. **Uso de `data` em Criações**
**Problema**: Algumas validações usavam `data` que não existe em criações.

**Correção**: Uso de `!data.exists()` para verificar se é criação ou atualização.

### 5. **Isolamento por Usuário Incompleto**
**Problema**: Algumas coleções permitiam leitura muito ampla.

**Correção**: Isolamento reforçado - cada usuário só acessa seus próprios dados.

## Coleções Bloqueadas para Escrita Direta

Todas as seguintes coleções agora bloqueiam escrita direta e devem usar Cloud Functions:

- **services** → `createService`, `updateService`, `deleteService`
- **orders** → `createOrder`, `updateOrderStatus`
- **products** → `createProduct`, `updateProduct`, `deleteProduct`
- **purchase_orders** → Cloud Functions específicas
- **messages** → Cloud Functions específicas
- **notifications** → `sendPushNotification`
- **reviews** → `submitReview`
- **identity_verifications** → `startIdentityVerification`

## Coleções que Permitem Escrita Direta

Apenas as seguintes coleções permitem escrita direta (necessárias para funcionalidades em tempo real):

- **presence**: Status online/offline (apenas o próprio usuário)
- **typing**: Indicador de digitação (apenas o próprio usuário)
- **users**: Perfil do usuário (apenas o próprio usuário)

## Como Fazer Deploy

```bash
# Deploy apenas das regras do Realtime Database
firebase deploy --only database

# Ou deploy completo (incluindo outras regras)
firebase deploy
```

## Verificação Pós-Deploy

1. **Testar Leitura**: Verificar se usuários conseguem ler apenas seus próprios dados
2. **Testar Escrita Bloqueada**: Tentar escrever diretamente em coleções públicas (deve falhar)
3. **Testar Cloud Functions**: Verificar se Cloud Functions ainda funcionam (usam Admin SDK, não são afetadas)
4. **Testar Presence/Typing**: Verificar se chat em tempo real ainda funciona

## Notas Importantes

- **Cloud Functions não são afetadas**: Cloud Functions usam Admin SDK que ignora regras de segurança
- **Presence e Typing mantidos**: Essas coleções precisam de escrita direta para funcionar em tempo real
- **Isolamento por usuário**: Cada usuário só acessa seus próprios dados, exceto dados públicos (services, products, reviews) que são apenas leitura

## Estrutura de Segurança

```
Realtime Database
├── Regra Raiz: BLOQUEADA (default deny)
├── Coleções Públicas (leitura apenas):
│   ├── services (leitura: autenticados, escrita: Cloud Functions)
│   ├── products (leitura: autenticados, escrita: Cloud Functions)
│   └── reviews (leitura: autenticados, escrita: Cloud Functions)
├── Coleções Isoladas (leitura/escrita: apenas dono):
│   ├── users (leitura/escrita: próprio usuário ou admin)
│   ├── orders (leitura: cliente/prestador, escrita: Cloud Functions)
│   ├── purchase_orders (leitura: cliente/vendedor, escrita: Cloud Functions)
│   ├── messages (leitura: participantes, escrita: Cloud Functions)
│   ├── notifications (leitura: dono, escrita: Cloud Functions)
│   └── identity_verifications (leitura: dono/admin, escrita: Cloud Functions)
└── Coleções em Tempo Real (escrita direta permitida):
    ├── presence (escrita: próprio usuário)
    └── typing (escrita: próprio usuário)
```
