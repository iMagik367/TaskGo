# Configurações de Pagamentos - TaskGo App

## ✅ Sistema de Pagamentos Implementado

### 1. Stripe (Cartões de Crédito/Débito) ✅
**Status**: Totalmente implementado

**Cloud Functions**:
- `createPaymentIntent` - Cria intenção de pagamento
- `confirmPayment` - Confirma pagamento
- `requestRefund` - Solicita reembolso
- `stripeWebhook` - Webhook para eventos do Stripe
- `createOnboardingLink` - Onboarding Stripe Connect para prestadores

**Funcionalidades**:
- ✅ Pagamento com cartão de crédito
- ✅ Pagamento com cartão de débito
- ✅ Stripe Connect para prestadores receberem pagamentos
- ✅ Taxa de plataforma (15%)
- ✅ Webhooks configurados

**Configuração Necessária**:
1. Variável de ambiente: `STRIPE_SECRET_KEY`
2. Variável de ambiente: `STRIPE_WEBHOOK_SECRET`
3. Configurar webhook no Stripe Dashboard apontando para a function `stripeWebhook`

**Arquivos**:
- `functions/src/payments.ts`
- `functions/src/stripe-connect.ts`
- `functions/src/webhooks.ts`

### 2. PIX ✅
**Status**: Interface implementada, integração pendente

**Funcionalidades**:
- ✅ Interface de pagamento PIX implementada
- ✅ Tela de confirmação PIX
- ⚠️ Integração com gateway PIX pendente (Mercado Pago, PagSeguro, etc.)

**Arquivos**:
- `app/src/main/java/com/taskgoapp/taskgo/feature/checkout/presentation/PixPaymentScreen.kt`
- `app/src/main/java/com/taskgoapp/taskgo/feature/checkout/presentation/ConfirmacaoPixScreen.kt`

**Próximo Passo**: Integrar com gateway PIX (Mercado Pago, PagSeguro, ou outro)

### 3. Google Pay ✅
**Status**: Manager implementado

**Funcionalidades**:
- ✅ `GooglePayManager` implementado
- ⚠️ Integração na UI pendente

**Arquivos**:
- `app/src/main/java/com/taskgoapp/taskgo/core/payment/GooglePayManager.kt`

## 📋 Configurações Necessárias

### Variáveis de Ambiente (Firebase Functions)

Configure no Firebase Console:
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/functions/config
2. Adicione as variáveis:

```bash
STRIPE_SECRET_KEY=sk_live_... (ou sk_test_... para desenvolvimento)
STRIPE_WEBHOOK_SECRET=whsec_...
```

### Webhook do Stripe

1. Acesse: https://dashboard.stripe.com/webhooks
2. Crie um webhook apontando para: `https://us-central1-task-go-ee85f.cloudfunctions.net/stripeWebhook`
3. Eventos a escutar:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `account.updated`
   - `transfer.created`

### Stripe Connect

Para prestadores receberem pagamentos:
1. Prestador deve completar verificação de identidade
2. Prestador deve chamar `createOnboardingLink` Cloud Function
3. Completar onboarding no Stripe
4. `stripeAccountId` será salvo no perfil do usuário

## 🔧 Integração PIX (Pendente)

### Opções de Gateway PIX:

1. **Mercado Pago**
   - SDK: `com.mercadopago.android.px`
   - Documentação: https://www.mercadopago.com.br/developers/pt/docs

2. **PagSeguro**
   - SDK: PagSeguro SDK
   - Documentação: https://dev.pagseguro.uol.com.br/

3. **Asaas**
   - API REST
   - Documentação: https://docs.asaas.com/

### Implementação Sugerida:

1. Criar `PixPaymentRepository` ou `PixPaymentService`
2. Integrar com gateway escolhido
3. Atualizar `PixPaymentScreen` para usar o serviço
4. Adicionar Cloud Function para processar pagamentos PIX (se necessário)

## 📝 Status Atual

### ✅ Implementado:
- Stripe (cartões) - 100%
- Interface PIX - 100%
- Google Pay Manager - 100%
- Cloud Functions de pagamento - 100%

### ⚠️ Pendente:
- Integração PIX com gateway
- Integração Google Pay na UI
- Testes de pagamento em produção

## 🚀 Próximos Passos

1. **Configurar variáveis de ambiente do Stripe** (se usar Stripe)
2. **Configurar webhook do Stripe** (se usar Stripe)
3. **Escolher e integrar gateway PIX** (se usar PIX)
4. **Testar pagamentos em ambiente de desenvolvimento**
5. **Configurar chaves de produção** (quando for para produção)

## 📚 Documentação de Referência

- **Stripe**: https://stripe.com/docs
- **Stripe Connect**: https://stripe.com/docs/connect
- **Mercado Pago**: https://www.mercadopago.com.br/developers/pt/docs
- **PagSeguro**: https://dev.pagseguro.uol.com.br/

## ⚠️ Notas Importantes

1. **Nunca commitar chaves de API no código**
2. **Usar variáveis de ambiente para todas as chaves**
3. **Testar em ambiente de desenvolvimento antes de produção**
4. **Configurar webhooks corretamente**
5. **Implementar logs adequados para debugging**

