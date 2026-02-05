# Como Obter as Chaves do Stripe

## 🔍 Onde Estão as Chaves?

As chaves do Stripe estão configuradas como **Firebase Secrets** (não estão em arquivos locais por segurança).

---

## 📋 Opção 1: Obter do Firebase Secrets

### Via Terminal:

```powershell
# Ver STRIPE_SECRET_KEY
firebase functions:secrets:access STRIPE_SECRET_KEY

# Ver STRIPE_WEBHOOK_SECRET
firebase functions:secrets:access STRIPE_WEBHOOK_SECRET
```

### Via Script:

```powershell
.\scripts\obter-chaves-stripe.ps1
```

---

## 🌐 Opção 2: Obter do Stripe Dashboard

### STRIPE_SECRET_KEY:

1. Acesse: https://dashboard.stripe.com/apikeys
2. Faça login na sua conta Stripe
3. Na seção **"Secret keys"**, você verá:
   - **Test mode key** (sk_test_...) - para desenvolvimento
   - **Live mode key** (sk_live_...) - para produção
4. Clique em **"Reveal test key"** ou **"Reveal live key"**
5. Copie a chave (começa com `sk_test_` ou `sk_live_`)

### STRIPE_WEBHOOK_SECRET:

1. Acesse: https://dashboard.stripe.com/webhooks
2. Clique no webhook que você configurou (ou crie um novo)
3. Na seção **"Signing secret"**, clique em **"Reveal"**
4. Copie o secret (começa com `whsec_`)

**Nota:** Se você ainda não criou o webhook, você precisará:
1. Criar o webhook apontando para: `https://sua-url-railway.app/api/stripe/webhook`
2. Selecionar os eventos que deseja escutar
3. Copiar o signing secret gerado

---

## 📝 Variáveis para Railway

Depois de obter as chaves, adicione no Railway:

```
STRIPE_SECRET_KEY=sk_live_SUA_CHAVE_AQUI
STRIPE_WEBHOOK_SECRET=whsec_SEU_SECRET_AQUI
```

**Importante:**
- Use `sk_live_` para produção
- Use `sk_test_` apenas para testes
- O webhook secret só existe após criar o webhook no Stripe

---

## 🔐 Segurança

⚠️ **NUNCA** commite essas chaves no Git!

- ✅ Use variáveis de ambiente (Railway)
- ✅ Use Firebase Secrets (Firebase Functions)
- ❌ NÃO coloque em arquivos .env que vão para o Git
- ❌ NÃO coloque no código fonte

---

## 🆘 Se Não Encontrar as Chaves

### Criar Novas Chaves no Stripe:

1. Acesse: https://dashboard.stripe.com/apikeys
2. Clique em **"Create secret key"**
3. Dê um nome (ex: "TaskGo Production")
4. Copie a chave gerada

### Criar Novo Webhook:

1. Acesse: https://dashboard.stripe.com/webhooks
2. Clique em **"Add endpoint"**
3. URL do endpoint: `https://sua-url-railway.app/api/stripe/webhook`
4. Selecione os eventos:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `account.updated`
5. Clique em **"Add endpoint"**
6. Copie o **"Signing secret"** gerado

---

## ✅ Checklist

- [ ] STRIPE_SECRET_KEY obtida (sk_live_... ou sk_test_...)
- [ ] STRIPE_WEBHOOK_SECRET obtido (whsec_...)
- [ ] Chaves adicionadas no Railway
- [ ] Webhook configurado no Stripe Dashboard
- [ ] URL do webhook aponta para Railway

---

## 📞 Ajuda

- Stripe Dashboard: https://dashboard.stripe.com
- Documentação Stripe: https://stripe.com/docs
- Suporte Stripe: https://support.stripe.com
