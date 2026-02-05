# Guia de Configuração - PostgreSQL TaskGo

## 📋 Pré-requisitos

1. **PostgreSQL 12+** instalado e rodando
2. **Node.js 20+** instalado
3. **npm** ou **yarn** instalado
4. Credenciais do Firebase (para migração de dados)

## 🚀 Passo 1: Configurar PostgreSQL

### Windows (PowerShell)

```powershell
# Navegar para o diretório database
cd database

# Executar script de setup
.\setup.ps1
```

### Linux/Mac (Bash)

```bash
# Navegar para o diretório database
cd database

# Dar permissão de execução
chmod +x setup.sh

# Executar script de setup
./setup.sh
```

### Manual

Se preferir configurar manualmente:

```bash
# Criar banco de dados
createdb taskgo

# Executar schema
psql -d taskgo -f database/schema.sql

# Executar migrations
psql -d taskgo -f database/migrations/002_seed_states_cities.sql
psql -d taskgo -f database/migrations/003_seed_categories.sql
```

## 🔧 Passo 2: Configurar Backend

### 2.1. Instalar dependências

```bash
cd backend
npm install
```

### 2.2. Configurar variáveis de ambiente

Crie o arquivo `backend/.env`:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=taskgo
DB_USER=postgres
DB_PASSWORD=sua_senha

# Firebase (para migração)
FIREBASE_PROJECT_ID=seu_project_id
FIREBASE_CLIENT_EMAIL=seu_client_email
FIREBASE_PRIVATE_KEY=sua_private_key

# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Server
PORT=3000
CORS_ORIGIN=http://localhost:3000
```

### 2.3. Popular cidades (opcional)

Para popular mais cidades além das principais:

```bash
npm run populate:cities
```

## 📦 Passo 3: Migrar Dados do Firestore

### 3.1. Preparar credenciais do Firebase

1. Acesse o Firebase Console
2. Vá em Project Settings > Service Accounts
3. Gere uma nova chave privada
4. Configure no `.env`:
   - `FIREBASE_PROJECT_ID`
   - `FIREBASE_CLIENT_EMAIL`
   - `FIREBASE_PRIVATE_KEY`

### 3.2. Executar migração

```bash
npm run migrate:firestore
```

A migração irá:
- ✅ Migrar todos os usuários
- ✅ Migrar produtos
- ✅ Criar localizações iniciais
- ✅ Migrar categorias preferidas

### 3.3. Validar migração

```bash
npm run validate:migration
```

Isso irá:
- ✅ Comparar contagens entre Firestore e PostgreSQL
- ✅ Validar dados de alguns registros
- ✅ Verificar estrutura do banco

## 🧪 Passo 4: Testar

### 4.1. Iniciar servidor

```bash
npm run dev
```

O servidor estará disponível em `http://localhost:3000`

### 4.2. Testar endpoints

#### Health Check
```bash
curl http://localhost:3000/health
```

#### Atualizar localização
```bash
curl -X POST http://localhost:3000/api/location/update \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-id",
    "latitude": -23.5505,
    "longitude": -46.6333
  }'
```

#### Criar ordem de serviço
```bash
curl -X POST http://localhost:3000/api/orders/service \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "client-id",
    "created_in_city_id": 1,
    "category": "Pintura",
    "details": "Preciso pintar minha casa"
  }'
```

### 4.3. Testar WebSocket

Conecte-se ao WebSocket server:

```javascript
const io = require('socket.io-client');
const socket = io('http://localhost:3000');

socket.on('connect', () => {
  console.log('Conectado!');
  
  // Autenticar
  socket.emit('authenticate', { userId: 'user-id' });
  
  // Entrar em sala de cidade/categoria (parceiros)
  socket.emit('join_city_category', { cityId: 1, categoryId: 1 });
  
  // Escutar novas ordens
  socket.on('new_service_order', (data) => {
    console.log('Nova ordem:', data);
  });
});
```

## 🐛 Troubleshooting

### Erro: "Não foi possível conectar ao PostgreSQL"

**Solução:**
- Verifique se o PostgreSQL está rodando: `pg_isready`
- Verifique as credenciais no `.env`
- Verifique se a porta está correta (padrão: 5432)

### Erro: "database does not exist"

**Solução:**
- Execute o script de setup: `./database/setup.sh` ou `.\database\setup.ps1`
- Ou crie manualmente: `createdb taskgo`

### Erro: "relation does not exist"

**Solução:**
- Execute o schema: `psql -d taskgo -f database/schema.sql`

### Erro na migração do Firestore

**Solução:**
- Verifique as credenciais do Firebase no `.env`
- Verifique se o Firestore está acessível
- Verifique os logs para identificar qual registro está falhando

### WebSocket não conecta

**Solução:**
- Verifique se o servidor está rodando
- Verifique a URL de conexão
- Verifique CORS no `app.ts`

## 📊 Verificar Status

### Contar registros

```sql
-- Usuários
SELECT COUNT(*) FROM users;

-- Produtos
SELECT COUNT(*) FROM products;

-- Cidades
SELECT COUNT(*) FROM cities;

-- Estados
SELECT COUNT(*) FROM states;
```

### Verificar triggers

```sql
-- Ver triggers criados
SELECT * FROM pg_trigger WHERE tgname LIKE '%service_order%';
```

### Verificar conexões WebSocket

O servidor WebSocket escuta automaticamente os NOTIFY do PostgreSQL. Verifique os logs do servidor para confirmar.

## 🚀 Próximos Passos

1. ✅ Configurar ambiente de produção
2. ✅ Configurar SSL/TLS para PostgreSQL
3. ✅ Configurar backup automático
4. ✅ Configurar monitoramento
5. ✅ Integrar com app mobile

## 📝 Notas Importantes

- **Localização Dinâmica**: A localização do usuário é atualizada automaticamente via GPS
- **Dados Históricos**: Produtos/posts/stories permanecem na cidade onde foram criados
- **Notificações**: O sistema usa PostgreSQL LISTEN/NOTIFY + WebSocket para notificações em tempo real
- **Segurança**: Configure firewall e use SSL em produção

## 🔗 Links Úteis

- [Documentação PostgreSQL](https://www.postgresql.org/docs/)
- [Documentação Socket.io](https://socket.io/docs/)
- [Documentação Stripe](https://stripe.com/docs)
