# 🚀 Guia Completo: Configurar Tudo no Firebase

Este guia vai te ajudar a configurar todos os secrets necessários no Firebase usando o Firebase CLI.

---

## 📋 Pré-requisitos

1. ✅ Firebase CLI instalado (`npm install -g firebase-tools`)
2. ✅ Logado no Firebase (`firebase login`)
3. ✅ Projeto Firebase configurado (`firebase use task-go-ee85f`)

---

## 🔐 Passo 1: Configurar Secrets do Stripe

### **Opção A: Usar o Script Automático (Recomendado)**

Execute o script PowerShell que criamos:

```powershell
.\configurar_secrets_stripe.ps1
```

O script vai solicitar cada secret e configurá-lo automaticamente.

### **Opção B: Configurar Manualmente**

#### **1.1. STRIPE_SECRET_KEY**

```powershell
firebase functions:secrets:set STRIPE_SECRET_KEY
```

Quando solicitado, cole sua chave secreta do Stripe (sk_live_...)
```
[INSIRA_SUA_CHAVE_SECRETA_AQUI]
```

#### **1.2. STRIPE_PUBLISHABLE_KEY**

```powershell
firebase functions:secrets:set STRIPE_PUBLISHABLE_KEY
```

Quando solicitado, cole:
```
pk_live_51SZcoYIw5Kqt55XkyogUr3cUG7RFlupPmSkI7sJfZ93fzGoGXAR7GfCnSVUsKJAsq5DG7ErNYgPFZggxMrzQOfgu008mWkFgNe
```

#### **1.3. STRIPE_WEBHOOK_SECRET**

⚠️ **IMPORTANTE:** Você precisa configurar o webhook no Stripe Dashboard primeiro para obter este secret. Veja o guia `GUIA_CONFIGURAR_WEBHOOK_STRIPE.md`.

Depois de obter o secret (`whsec_...`), execute:

```powershell
firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
```

Quando solicitado, cole o secret que você copiou do Stripe Dashboard.

---

## ✅ Passo 2: Verificar Secrets Configurados

Para listar todos os secrets configurados:

```powershell
firebase functions:secrets:access
```

Você deve ver:
- ✅ STRIPE_SECRET_KEY
- ✅ STRIPE_PUBLISHABLE_KEY
- ✅ STRIPE_WEBHOOK_SECRET (após configurar webhook)

---

## 🚀 Passo 3: Fazer Deploy das Functions

Após configurar todos os secrets, faça o deploy:

```powershell
firebase deploy --only functions
```

**⚠️ IMPORTANTE:** Você DEVE fazer o deploy após configurar os secrets para que as functions possam acessá-los.

---

## 📝 Passo 4: Verificar Configuração

### **4.1. Verificar Logs**

```powershell
firebase functions:log
```

### **4.2. Testar uma Function**

Você pode testar a function `getStripePublishableKey`:

```powershell
firebase functions:call getStripePublishableKey
```

Se retornar a chave pública, está funcionando! ✅

---

## 🔗 Passo 5: Configurar Webhook no Stripe

Siga o guia completo em: **`GUIA_CONFIGURAR_WEBHOOK_STRIPE.md`**

Resumo rápido:
1. Acesse https://dashboard.stripe.com/webhooks
2. Clique em "Add endpoint"
3. URL: `https://us-central1-task-go-ee85f.cloudfunctions.net/stripeWebhook`
4. Selecione eventos: `payment_intent.succeeded`, `payment_intent.payment_failed`
5. Copie o Signing Secret (`whsec_...`)
6. Configure no Firebase: `firebase functions:secrets:set STRIPE_WEBHOOK_SECRET`
7. Faça deploy novamente: `firebase deploy --only functions`

---

## 📋 Checklist Completo

- [ ] `STRIPE_SECRET_KEY` configurado
- [ ] `STRIPE_PUBLISHABLE_KEY` configurado
- [ ] Webhook criado no Stripe Dashboard
- [ ] `STRIPE_WEBHOOK_SECRET` configurado
- [ ] Deploy das functions realizado
- [ ] Teste de pagamento realizado
- [ ] Logs verificados

---

## 🎉 Pronto!

Após seguir todos os passos, seu sistema de pagamento estará totalmente configurado e funcionando!

---

## 📚 Documentação Adicional

- **Guia de Secrets**: `GUIA_CONFIGURAR_SECRETS.md`
- **Guia de Webhook**: `GUIA_CONFIGURAR_WEBHOOK_STRIPE.md`
- **Validação de Pagamentos**: `VALIDACAO_PAGAMENTOS_IMPLEMENTADA.md`

