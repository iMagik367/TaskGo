# Sistema de Pagamento com Stripe Connect

## Como Funciona o Sistema de Pagamento

### 1. Arquitetura do Stripe Connect

O Stripe Connect permite que a plataforma (TaskGo) receba pagamentos e faça split payments automaticamente:

#### Conta da Plataforma (TaskGo)
- A TaskGo possui uma **conta Stripe principal** configurada no Firebase Functions
- Esta conta é identificada pela **chave secreta** (`STRIPE_SECRET_KEY`) armazenada nas variáveis de ambiente do Firebase
- **TODOS os pagamentos** são inicialmente recebidos nesta conta da plataforma
- A comissão de 2% fica automaticamente na conta da plataforma
- O restante (98%) é transferido automaticamente para a conta do vendedor

#### Contas dos Vendedores (Stripe Connect)
- Cada vendedor precisa criar uma **conta Stripe Connect** através do processo de onboarding
- O vendedor acessa o onboarding via `createOnboardingLink` (função Firebase)
- Após completar o onboarding, o vendedor recebe um `stripeAccountId` que é salvo no Firestore (`users/{userId}.stripeAccountId`)
- Esta conta permite que o vendedor receba pagamentos diretamente

### 2. Fluxo de Pagamento

```
1. Cliente finaliza compra
   ↓
2. CheckoutUseCase cria o pedido (PurchaseOrder) com status PENDING_PAYMENT
   ↓
3. PaymentGateway.process() é chamado
   ↓
4. createProductPaymentIntent (Firebase Function) é chamado:
   - Busca o pedido no Firestore
   - Busca o vendedor e seu stripeAccountId
   - Calcula: totalAmount (100%), platformFee (2%), sellerAmount (98%)
   - Cria PaymentIntent no Stripe com:
     * amount: totalAmount (valor total cobrado do cliente)
     * application_fee_amount: platformFee (2% que fica na conta da plataforma)
     * transfer_data.destination: stripeAccountId (conta do vendedor)
   ↓
5. Cliente confirma pagamento (via Stripe SDK no app)
   ↓
6. confirmProductPayment (Firebase Function) é chamado:
   - Verifica se o pagamento foi bem-sucedido
   - Atualiza o status do pedido para PAID
   - Cria notificações para cliente e vendedor
   - O Stripe automaticamente:
     * Recebe o pagamento na conta da plataforma
     * Deduz 2% (application_fee) e mantém na conta da plataforma
     * Transfere 98% para a conta do vendedor (stripeAccountId)
```

### 3. Configuração Necessária

#### No Firebase Functions (.env ou variáveis de ambiente):
```bash
STRIPE_SECRET_KEY=sk_live_... # Chave secreta da conta da plataforma
STRIPE_REFRESH_URL=https://taskgo.app/settings
STRIPE_RETURN_URL=https://taskgo.app/settings
```

#### No Stripe Dashboard:
1. **Conta da Plataforma:**
   - Acesse https://dashboard.stripe.com
   - Configure a conta como "Platform" (conta principal)
   - Ative o Stripe Connect
   - Configure as taxas de comissão (opcional, mas já configurado no código como 2%)

2. **Contas dos Vendedores:**
   - São criadas automaticamente via `createOnboardingLink`
   - Cada vendedor completa o onboarding e fornece:
     * Dados pessoais/empresariais
     * Informações bancárias (para receber pagamentos)
     * Documentos de verificação (se necessário)

### 4. Onde o Dinheiro Fica?

#### Durante o Pagamento:
- **100% do valor** é cobrado do cliente
- O pagamento é recebido na **conta Stripe da plataforma (TaskGo)**

#### Após Confirmação do Pagamento:
- **2% (application_fee)** → Fica na **conta da plataforma (TaskGo)**
- **98% (sellerAmount)** → É transferido automaticamente para a **conta Stripe Connect do vendedor**

#### Saque do Dinheiro:
- **Plataforma (TaskGo):** Pode sacar o dinheiro da comissão (2%) através do Stripe Dashboard
- **Vendedor:** Pode sacar o dinheiro (98%) através do Stripe Dashboard ou configurar saques automáticos

### 5. Código Relevante

#### Firebase Function: `createProductPaymentIntent`
```typescript
// Calcula valores
const totalAmount = Math.round(order.total * 100); // em centavos
const platformFeePercent = 0.02; // 2%
const platformFeeAmount = Math.round(totalAmount * platformFeePercent);

// Cria PaymentIntent com split payment
const paymentIntent = await stripe.paymentIntents.create({
  amount: totalAmount, // Valor total cobrado
  currency: 'brl',
  application_fee_amount: platformFeeAmount, // 2% para plataforma
  transfer_data: {
    destination: seller.stripeAccountId, // 98% para vendedor
  },
});
```

### 6. Importante

1. **A conta da plataforma NÃO precisa ser configurada manualmente pelos vendedores** - ela já existe e é gerenciada pela TaskGo
2. **Cada vendedor precisa apenas completar o onboarding do Stripe Connect** para receber pagamentos
3. **O split payment é automático** - o Stripe faz tudo quando o pagamento é confirmado
4. **A comissão de 2% é fixa** no código, mas pode ser ajustada se necessário
5. **O vendedor só recebe pagamentos se tiver completado o onboarding** - caso contrário, o pagamento falha

### 7. Troubleshooting

#### Erro: "Seller has not completed Stripe Connect onboarding"
- **Causa:** O vendedor não tem `stripeAccountId` ou não completou o onboarding
- **Solução:** Vendedor deve acessar a tela de configurações e completar o onboarding do Stripe

#### Erro: "Payment not succeeded"
- **Causa:** O pagamento não foi confirmado no Stripe
- **Solução:** Verificar logs do Stripe Dashboard e status do PaymentIntent

#### Dinheiro não chegou na conta do vendedor
- **Causa:** Pode haver delay na transferência ou problemas com a conta do vendedor
- **Solução:** Verificar no Stripe Dashboard se a transferência foi criada e seu status

