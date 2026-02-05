# Guia de Deploy e Hospedagem - TaskGo Backend

## ⚠️ Problema com Netlify

O **Netlify não suporta WebSocket nativamente**, o que é crítico para nosso sistema de notificações em tempo real. Precisamos de uma plataforma que suporte:

- ✅ WebSocket (Socket.io)
- ✅ PostgreSQL hospedado
- ✅ Backend Node.js/Express
- ✅ Custo baixo
- ✅ Fácil deploy

---

## 🏆 Melhores Opções (Ranking)

### 1. 🥇 **Railway** (RECOMENDADO)

**Por que é a melhor opção:**
- ✅ Suporta WebSocket nativamente
- ✅ PostgreSQL gerenciado incluído
- ✅ Deploy automático via Git
- ✅ Plano gratuito generoso ($5 crédito/mês)
- ✅ Muito fácil de usar
- ✅ Suporta variáveis de ambiente
- ✅ Logs em tempo real
- ✅ SSL automático

**Preços:**
- Plano Hobby: $5/mês (créditos)
- PostgreSQL: Incluído no plano
- WebSocket: Suportado nativamente

**Limitações:**
- Timeout de 5 minutos (suficiente para nosso caso)
- 500 horas/mês no plano gratuito

**Link:** https://railway.app

---

### 2. 🥈 **Render**

**Por que é boa:**
- ✅ Suporta WebSocket
- ✅ PostgreSQL gerenciado
- ✅ Deploy automático via Git
- ✅ Plano gratuito disponível
- ✅ SSL automático
- ✅ Muito estável

**Preços:**
- Plano Free: Gratuito (com limitações)
- PostgreSQL: $7/mês (ou gratuito com limitações)
- WebSocket: Suportado

**Limitações:**
- Free tier: App "dorme" após 15min de inatividade
- Free tier: Sem WebSocket persistente (precisa upgrade)

**Link:** https://render.com

---

### 3. 🥉 **Fly.io**

**Por que é interessante:**
- ✅ Suporta WebSocket
- ✅ Edge computing (baixa latência)
- ✅ PostgreSQL disponível
- ✅ Plano gratuito generoso
- ✅ Deploy global

**Preços:**
- Plano Hacker: Gratuito (3 VMs pequenas)
- PostgreSQL: $1.94/mês (mínimo)
- WebSocket: Suportado

**Limitações:**
- Mais complexo de configurar
- Requer Dockerfile

**Link:** https://fly.io

---

### 4. **DigitalOcean App Platform**

**Características:**
- ✅ Suporta WebSocket
- ✅ PostgreSQL disponível
- ✅ Deploy via Git
- ✅ SSL automático

**Preços:**
- $5/mês (mínimo)
- PostgreSQL: $15/mês adicional

**Link:** https://www.digitalocean.com/products/app-platform

---

### 5. **Heroku**

**Características:**
- ✅ Suporta WebSocket
- ✅ PostgreSQL disponível (Heroku Postgres)
- ✅ Muito fácil de usar
- ✅ Ecossistema maduro

**Preços:**
- $7/mês (Eco Dyno)
- PostgreSQL: $5/mês (Mini)

**Limitações:**
- Mais caro que alternativas
- Removeram plano gratuito

**Link:** https://www.heroku.com

---

## 🎯 Recomendação Final: **Railway**

### Por que Railway é a melhor escolha:

1. **Custo-benefício excelente**
   - $5/mês cobre backend + PostgreSQL
   - Créditos mensais generosos

2. **Suporta tudo que precisamos**
   - ✅ WebSocket nativo
   - ✅ PostgreSQL gerenciado
   - ✅ Deploy automático
   - ✅ Variáveis de ambiente
   - ✅ Logs em tempo real

3. **Fácil de usar**
   - Interface simples
   - Deploy em minutos
   - Documentação clara

4. **Escalável**
   - Fácil upgrade quando necessário
   - Suporta múltiplos serviços

---

## 📋 Checklist de Deploy no Railway

### Pré-requisitos:
- [ ] Conta no Railway (https://railway.app)
- [ ] Código no GitHub/GitLab
- [ ] PostgreSQL configurado localmente (para testes)

### Passos:

1. **Criar Projeto no Railway**
   - Conectar repositório Git
   - Criar novo projeto

2. **Adicionar PostgreSQL**
   - Clicar em "New" → "Database" → "Add PostgreSQL"
   - Railway cria automaticamente

3. **Adicionar Backend**
   - Clicar em "New" → "GitHub Repo"
   - Selecionar repositório
   - Railway detecta Node.js automaticamente

4. **Configurar Variáveis de Ambiente**
   - No serviço do backend, ir em "Variables"
   - Adicionar:
     ```
     DB_HOST=${{Postgres.PGHOST}}
     DB_PORT=${{Postgres.PGPORT}}
     DB_NAME=${{Postgres.PGDATABASE}}
     DB_USER=${{Postgres.PGUSER}}
     DB_PASSWORD=${{Postgres.PGPASSWORD}}
     STRIPE_SECRET_KEY=sk_live_...
     PORT=3000
     ```

5. **Configurar Deploy**
   - Railway detecta automaticamente
   - Ou configurar `railway.json` se necessário

6. **Configurar Domínio**
   - Railway fornece domínio automático
   - Ou adicionar domínio customizado

---

## 🔧 Arquivos Necessários para Railway

### 1. `railway.json` (Opcional)
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS"
  },
  "deploy": {
    "startCommand": "cd backend && npm start",
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

### 2. `Procfile` (Alternativa)
```
web: cd backend && npm start
```

### 3. Atualizar `backend/package.json`
```json
{
  "scripts": {
    "start": "node dist/app.js",
    "build": "tsc"
  }
}
```

---

## 🌐 Alternativa: Render (Se preferir)

### Configuração no Render:

1. **Criar Web Service**
   - Conectar repositório Git
   - Build Command: `cd backend && npm install && npm run build`
   - Start Command: `cd backend && npm start`

2. **Criar PostgreSQL**
   - Criar novo PostgreSQL database
   - Copiar connection string

3. **Variáveis de Ambiente**
   - Adicionar no Web Service:
     ```
     DB_HOST=...
     DB_PORT=5432
     DB_NAME=...
     DB_USER=...
     DB_PASSWORD=...
     ```

4. **WebSocket**
   - Render suporta WebSocket automaticamente
   - Apenas garantir que a porta está configurada

---

## ⚡ WebSocket em Produção

### Railway:
- ✅ Suporta WebSocket nativamente
- ✅ Não precisa configuração especial
- ✅ Funciona automaticamente

### Render:
- ✅ Suporta WebSocket
- ⚠️ Free tier: WebSocket pode desconectar após inatividade
- ✅ Paid tier: WebSocket persistente

### Fly.io:
- ✅ Suporta WebSocket
- ✅ Edge computing (baixa latência)
- ✅ Funciona globalmente

---

## 💰 Comparação de Custos (Estimado)

| Plataforma | Backend | PostgreSQL | Total/Mês | WebSocket |
|------------|---------|------------|-----------|-----------|
| **Railway** | $5 | Incluído | **$5** | ✅ Nativo |
| **Render** | Free* | $7 | **$7** | ✅ (paid) |
| **Fly.io** | Free | $1.94 | **$1.94** | ✅ |
| **DigitalOcean** | $5 | $15 | **$20** | ✅ |
| **Heroku** | $7 | $5 | **$12** | ✅ |

*Render Free: App dorme após inatividade

---

## 🎯 Recomendação Final

### Para Produção: **Railway**
- Melhor custo-benefício
- Mais fácil de usar
- Suporta tudo que precisamos
- Escalável

### Para Desenvolvimento/Testes: **Fly.io**
- Gratuito generoso
- Bom para testes
- Edge computing

---

## 📝 Próximos Passos

1. **Escolher plataforma** (recomendado: Railway)
2. **Criar conta e projeto**
3. **Configurar PostgreSQL**
4. **Fazer deploy do backend**
5. **Configurar variáveis de ambiente**
6. **Testar WebSocket**
7. **Configurar domínio customizado** (opcional)

---

## 🔗 Links Úteis

- Railway: https://railway.app
- Render: https://render.com
- Fly.io: https://fly.io
- Documentação Railway: https://docs.railway.app
- Documentação Render: https://render.com/docs

---

## ❓ FAQ

**P: Posso usar Netlify para frontend e Railway para backend?**
R: Sim! É uma arquitetura comum. Netlify para frontend estático, Railway para API + WebSocket.

**P: Railway suporta PostgreSQL com LISTEN/NOTIFY?**
R: Sim! O PostgreSQL do Railway suporta todas as funcionalidades, incluindo LISTEN/NOTIFY.

**P: E se eu quiser usar Supabase para PostgreSQL?**
R: Pode! Supabase oferece PostgreSQL gerenciado. Você pode usar Supabase + Railway (ou outra plataforma) para o backend.

**P: Qual é a melhor opção gratuita?**
R: Fly.io tem o plano gratuito mais generoso, mas Railway oferece melhor experiência geral por $5/mês.
