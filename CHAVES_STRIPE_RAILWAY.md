# Chaves do Stripe para Railway

## 🔍 Como Obter as Chaves

### Opção 1: Do Firebase Secrets (Se já configuradas)

Execute no terminal:

```powershell
# Ver STRIPE_SECRET_KEY
firebase functions:secrets:access STRIPE_SECRET_KEY

# Ver STRIPE_WEBHOOK_SECRET  
firebase functions:secrets:access STRIPE_WEBHOOK_SECRET
```

### Opção 2: Do Stripe Dashboard (Recomendado)

#### STRIPE_SECRET_KEY:
1. Acesse: **https://dashboard.stripe.com/apikeys**
2. Faça login
3. Na seção **"Secret keys"**:
   - **Test mode**: `sk_test_...` (para desenvolvimento)
   - **Live mode**: `sk_live_...` (para produção)
4. Clique em **"Reveal test key"** ou **"Reveal live key"**
5. **Copie a chave completa**

#### STRIPE_WEBHOOK_SECRET:
1. Acesse: **https://dashboard.stripe.com/webhooks**
2. Clique no webhook configurado (ou crie um novo)
3. Na seção **"Signing secret"**, clique em **"Reveal"**
4. **Copie o secret** (começa com `whsec_`)

**Se ainda não tem webhook:**
- Crie um novo webhook apontando para: `https://sua-url-railway.app/api/stripe/webhook`
- Selecione os eventos necessários
- Copie o signing secret gerado

---

## 📋 Variáveis para Adicionar no Railway

Depois de obter as chaves, adicione no serviço do **backend** no Railway:

```
STRIPE_SECRET_KEY=sk_live_COLE_SUA_CHAVE_AQUI
STRIPE_WEBHOOK_SECRET=whsec_COLE_SEU_SECRET_AQUI
```

**Substitua:**
- `sk_live_COLE_SUA_CHAVE_AQUI` pela sua chave real do Stripe
- `whsec_COLE_SEU_SECRET_AQUI` pelo seu webhook secret real

---

## ⚠️ Importante

- Use `sk_live_` para **produção**
- Use `sk_test_` apenas para **testes/desenvolvimento**
- O webhook secret só existe após criar o webhook no Stripe
- **NUNCA** commite essas chaves no Git

---

## ✅ Checklist

- [ ] Acessei o Stripe Dashboard
- [ ] Copiei a STRIPE_SECRET_KEY (sk_live_... ou sk_test_...)
- [ ] Criei/configurei o webhook no Stripe
- [ ] Copiei o STRIPE_WEBHOOK_SECRET (whsec_...)
- [ ] Adicionei ambas as variáveis no Railway (serviço backend)

---

## 🆘 Precisa de Ajuda?

- **Stripe Dashboard:** https://dashboard.stripe.com
- **Documentação Stripe:** https://stripe.com/docs
- **Guia completo:** Veja `COMO_OBTER_CHAVES_STRIPE.md`
