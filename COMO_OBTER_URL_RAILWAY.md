# Como Obter a URL Pública do Railway

## 🔍 Diferença entre URLs

### URL Interna (NÃO usar no app)
```
taskgo.railway.internal
```
- ✅ Usada para comunicação entre serviços no mesmo projeto Railway
- ❌ NÃO funciona fora do Railway
- ❌ NÃO funciona no app mobile

### URL Pública (USAR no app)
```
https://taskgo-backend-production.up.railway.app
```
ou
```
https://seu-projeto.up.railway.app
```
- ✅ Funciona de qualquer lugar
- ✅ Funciona no app mobile
- ✅ Acessível via internet

---

## 📋 Como Obter a URL Pública

### Método 1: Via Dashboard Railway

1. Acesse: https://railway.app/dashboard
2. Clique no seu projeto
3. Clique no serviço do **backend**
4. Vá em **Settings**
5. Role até **Networking**
6. Você verá:
   - **Public Domain**: `https://seu-projeto.up.railway.app` ← **ESTA É A URL PÚBLICA**
   - **Private Domain**: `taskgo.railway.internal` ← Esta é interna (não usar)

### Método 2: Via Railway CLI

```bash
railway status
```

Mostrará a URL pública do serviço.

### Método 3: Verificar nos Logs

Após o deploy, os logs mostrarão algo como:
```
🚀 Servidor rodando na porta 3000
```

E você pode testar:
```bash
curl https://seu-projeto.up.railway.app/health
```

---

## ✅ Como Usar no App

### 1. Copiar a URL Pública

Exemplo:
```
https://taskgo-backend-production.up.railway.app
```

### 2. Adicionar `/api` no final

```
https://taskgo-backend-production.up.railway.app/api
```

### 3. Atualizar no build.gradle.kts

Edite `app/build.gradle.kts` linha ~189:

```kotlin
val releaseApiUrl = if (railwayApiUrl.isNotEmpty()) railwayApiUrl else "https://taskgo-backend-production.up.railway.app/api"
```

**Substitua** `taskgo-backend-production.up.railway.app` pela sua URL real.

---

## 🧪 Testar a URL

Antes de usar no app, teste no navegador ou curl:

```bash
# Health check
curl https://sua-url-railway.app/health

# Deve retornar:
# {"status":"ok","timestamp":"2024-01-01T00:00:00.000Z"}
```

Se funcionar, a URL está correta! ✅

---

## ⚠️ Importante

- **NUNCA** use `*.railway.internal` no app mobile
- **SEMPRE** use a URL pública `*.up.railway.app`
- A URL pública pode mudar se você recriar o serviço
- Para URL fixa, configure um domínio customizado no Railway
