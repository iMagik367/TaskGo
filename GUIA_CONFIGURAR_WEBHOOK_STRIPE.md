# 🔗 Guia Completo: Configurar Webhook no Stripe Dashboard

## 📋 O que é um Webhook?

Um webhook é uma forma do Stripe notificar seu servidor quando eventos importantes acontecem, como quando um pagamento é confirmado ou falha. Isso permite que seu sistema atualize automaticamente o status dos pedidos.

---

## 🚀 Passo a Passo: Configurar Webhook no Stripe

### **Passo 1: Acessar o Stripe Dashboard**

1. Abra seu navegador e acesse: **https://dashboard.stripe.com**
2. Faça login na sua conta Stripe
3. Certifique-se de estar no modo **"Live"** (canto superior direito) se estiver em produção

### **Passo 2: Navegar até Webhooks**

1. No menu lateral esquerdo, clique em **"Developers"** (Desenvolvedores)
2. Clique em **"Webhooks"** no submenu
3. Você verá uma lista de webhooks existentes (se houver)

### **Passo 3: Adicionar Novo Webhook**

1. Clique no botão **"+ Add endpoint"** ou **"Adicionar endpoint"** (canto superior direito)

### **Passo 4: Configurar o Endpoint**

Preencha os seguintes campos:

#### **Endpoint URL:**
```
https://us-central1-task-go-ee85f.cloudfunctions.net/stripeWebhook
```

**⚠️ IMPORTANTE:** 
- Substitua `task-go-ee85f` pelo ID do seu projeto Firebase se for diferente
- A URL deve ser **HTTPS** (não HTTP)
- Não adicione barra `/` no final

#### **Description (Opcional):**
```
TaskGo Payment Webhook - Confirma pagamentos automaticamente
```

### **Passo 5: Selecionar Eventos**

Na seção **"Events to send"**, selecione **"Select events"** e marque os seguintes eventos:

#### **Eventos Obrigatórios:**
- ✅ `payment_intent.succeeded` - Quando um pagamento é bem-sucedido
- ✅ `payment_intent.payment_failed` - Quando um pagamento falha

#### **Eventos Recomendados:**
- ✅ `account.updated` - Quando uma conta Stripe Connect é atualizada
- ✅ `transfer.created` - Quando uma transferência é criada

#### **Como Selecionar:**
1. Clique em **"Select events"**
2. Na busca, digite `payment_intent`
3. Marque `payment_intent.succeeded` e `payment_intent.payment_failed`
4. Digite `account` e marque `account.updated`
5. Digite `transfer` e marque `transfer.created`
6. Clique em **"Add events"**

### **Passo 6: Criar o Webhook**

1. Clique no botão **"Add endpoint"** ou **"Adicionar endpoint"**
2. Aguarde alguns segundos enquanto o Stripe cria o webhook

### **Passo 7: Copiar o Signing Secret**

Após criar o webhook:

1. Clique no webhook que você acabou de criar
2. Na página de detalhes, procure por **"Signing secret"**
3. Clique no botão **"Reveal"** ou **"Revelar"** ao lado do secret
4. **COPIE** o secret (começa com `whsec_...`)
5. **GUARDE** este secret em local seguro - você precisará dele!

**Exemplo de Signing Secret:**
```
whsec_1234567890abcdefghijklmnopqrstuvwxyz
```

### **Passo 8: Configurar o Secret no Firebase**

Agora você precisa adicionar este secret como uma variável de ambiente no Firebase Functions:

#### **Opção A: Via Firebase CLI**

```powershell
firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
```

Quando solicitado, cole o secret que você copiou (`whsec_...`)

#### **Opção B: Via Script PowerShell**

Execute o script que criamos:
```powershell
.\configurar_secrets_stripe.ps1
```

Quando solicitado pelo secret do webhook, cole o `whsec_...`

#### **Opção C: Via Firebase Console**

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/functions/config
2. Vá na aba **"Secrets"**
3. Clique em **"Add secret"**
4. Nome: `STRIPE_WEBHOOK_SECRET`
5. Valor: Cole o `whsec_...`
6. Clique em **"Add secret"**

### **Passo 9: Fazer Redeploy das Functions**

Após configurar o secret, faça o deploy novamente:

```powershell
firebase deploy --only functions
```

---

## ✅ Verificar se o Webhook Está Funcionando

### **Método 1: Testar um Pagamento**

1. Faça um pedido no app usando cartão de crédito
2. Use um cartão de teste do Stripe: `4242 4242 4242 4242`
3. Verifique se o pedido é atualizado automaticamente para `PAID`

### **Método 2: Verificar Logs no Stripe**

1. No Stripe Dashboard, vá em **"Developers" > "Webhooks"**
2. Clique no seu webhook
3. Vá na aba **"Logs"**
4. Você verá todos os eventos enviados e as respostas do seu servidor

### **Método 3: Verificar Logs no Firebase**

```powershell
firebase functions:log
```

Ou no Console:
https://console.firebase.google.com/project/task-go-ee85f/functions/logs

---

## 🔍 Troubleshooting

### **Problema: Webhook não está recebendo eventos**

**Soluções:**
1. Verifique se a URL do webhook está correta
2. Certifique-se de que as functions estão deployadas
3. Verifique se o webhook está no modo correto (Live vs Test)
4. Verifique os logs do Firebase Functions

### **Problema: Erro "Webhook signature verification failed"**

**Solução:**
- Verifique se o `STRIPE_WEBHOOK_SECRET` está configurado corretamente
- Certifique-se de que está usando o secret correto (Live vs Test)
- Faça um novo deploy das functions após configurar o secret

### **Problema: Pagamentos não estão sendo confirmados**

**Soluções:**
1. Verifique se o evento `payment_intent.succeeded` está selecionado
2. Verifique os logs do webhook no Stripe Dashboard
3. Verifique os logs do Firebase Functions
4. Certifique-se de que o webhook está retornando status 200

---

## 📝 Resumo dos Valores

| Item | Valor |
|------|-------|
| **Webhook URL** | `https://us-central1-task-go-ee85f.cloudfunctions.net/stripeWebhook` |
| **Eventos** | `payment_intent.succeeded`, `payment_intent.payment_failed`, `account.updated`, `transfer.created` |
| **Secret Name** | `STRIPE_WEBHOOK_SECRET` |
| **Secret Value** | `whsec_...` (copie do Stripe Dashboard) |

---

## 🎯 Checklist Final

- [ ] Webhook criado no Stripe Dashboard
- [ ] URL do webhook configurada corretamente
- [ ] Eventos selecionados (`payment_intent.succeeded`, etc.)
- [ ] Signing secret copiado do Stripe
- [ ] Secret `STRIPE_WEBHOOK_SECRET` configurado no Firebase
- [ ] Deploy das functions realizado após configurar secret
- [ ] Teste de pagamento realizado com sucesso
- [ ] Logs verificados para confirmar funcionamento

---

## 🎉 Pronto!

Após seguir todos os passos, seu webhook estará configurado e funcionando. Os pagamentos serão confirmados automaticamente quando processados pelo Stripe!

---

## 📚 Referências

- **Documentação Stripe Webhooks**: https://stripe.com/docs/webhooks
- **Stripe Dashboard**: https://dashboard.stripe.com/webhooks
- **Firebase Functions**: https://console.firebase.google.com/project/task-go-ee85f/functions

