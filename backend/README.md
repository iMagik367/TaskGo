# TaskGo Backend - PostgreSQL

Backend API para o TaskGo usando PostgreSQL, migrado do Firebase Firestore.

## Características

- ✅ Localização dinâmica baseada em GPS
- ✅ Notificações em tempo real via WebSocket (PostgreSQL LISTEN/NOTIFY + Socket.io)
- ✅ Gateway de pagamento Stripe completo
- ✅ Rastreio de pedidos (iFood-like)
- ✅ Sistema de conversas e chat
- ✅ Contas bancárias privadas para parceiros

## Instalação

```bash
npm install
```

## Configuração

1. Copie `.env.example` para `.env` e configure as variáveis:

```bash
cp .env.example .env
```

2. Configure o banco de dados PostgreSQL:

```bash
# Criar banco de dados
createdb taskgo

# Executar migrations
psql -d taskgo -f database/schema.sql
psql -d taskgo -f database/migrations/002_seed_states_cities.sql
psql -d taskgo -f database/migrations/003_seed_categories.sql
```

## Executar

```bash
# Desenvolvimento
npm run dev

# Produção
npm run build
npm start
```

## Migração de Dados

Para migrar dados do Firestore para PostgreSQL:

```bash
npm run migrate:firestore
```

## Estrutura

```
backend/
├── src/
│   ├── models/          # Models TypeScript
│   ├── repositories/    # Repositories para acesso aos dados
│   ├── services/        # Services de negócio
│   ├── routes/          # Rotas da API
│   ├── websocket/       # Servidor WebSocket
│   ├── database/        # Conexão com PostgreSQL
│   └── scripts/          # Scripts de migração
├── database/
│   ├── schema.sql       # Schema completo
│   └── migrations/      # Migrations versionadas
└── package.json
```

## API Endpoints

### Localização
- `POST /api/location/update` - Atualiza localização do usuário via GPS
- `GET /api/location/cities/:stateId` - Obtém cidades de um estado
- `GET /api/location/nearest` - Busca cidade mais próxima

### Usuários
- `GET /api/users/:id` - Obtém usuário por ID
- `POST /api/users` - Cria novo usuário
- `PUT /api/users/:id` - Atualiza usuário

### Produtos
- `GET /api/products/city/:cityId` - Obtém produtos de uma cidade
- `POST /api/products` - Cria novo produto

### Ordens
- `POST /api/orders/service` - Cria ordem de serviço
- `POST /api/orders/purchase` - Cria pedido de produto

### Notificações
- `GET /api/notifications/:userId` - Obtém notificações do usuário
- `PUT /api/notifications/:id/read` - Marca como lida

### Stripe
- `POST /api/stripe/webhook` - Webhook do Stripe
- `POST /api/stripe/payment-intents` - Cria Payment Intent

### Rastreio
- `POST /api/tracking/:orderId/event` - Adiciona evento de rastreamento
- `GET /api/tracking/:orderId` - Obtém histórico de rastreamento

## WebSocket

O servidor WebSocket escuta eventos do PostgreSQL via LISTEN/NOTIFY e emite para clientes conectados via Socket.io.

### Eventos

- `authenticate` - Autentica usuário
- `join_city_category` - Entra em sala de cidade/categoria (parceiros)
- `new_service_order` - Nova ordem de serviço disponível
