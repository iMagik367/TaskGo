# Configurar URL do Railway no App

## ⚠️ IMPORTANTE

Após fazer deploy no Railway, você precisa atualizar a URL da API no app.

## 📋 Passo a Passo

### 1. Obter URL do Railway

1. Acesse o Railway Dashboard
2. Vá no serviço do **backend**
3. Em **Settings** → **Networking**
4. Copie a URL gerada (ex: `https://taskgo-backend-production.up.railway.app`)

### 2. Configurar no App

#### Opção 1: Via local.properties (Desenvolvimento)

Adicione em `local.properties`:

```properties
railwayApiUrl=https://sua-url-railway.app/api
```

#### Opção 2: Atualizar build.gradle.kts (Produção)

Edite `app/build.gradle.kts` linha 188 e substitua:

```kotlin
val releaseApiUrl = if (railwayApiUrl.isNotEmpty()) railwayApiUrl else "https://SUA-URL-RAILWAY.app/api"
```

**Substitua** `SUA-URL-RAILWAY.app` pela URL real do seu Railway.

### 3. Rebuild do App

```bash
.\gradlew.bat bundleRelease
```

## ✅ Verificação

Após configurar, teste:

1. Abra o app
2. Verifique os logs - deve conectar ao Railway
3. Teste uma funcionalidade que usa API

## 🔄 WebSocket

O WebSocket usa a mesma URL base, mas com protocolo `wss://`:

```
wss://sua-url-railway.app
```

O Socket.io detecta automaticamente.
