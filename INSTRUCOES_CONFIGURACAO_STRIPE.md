# Instruções para Configurar Stripe - TaskGo

## ✅ Implementações Realizadas

### 1. Fluxo de Pagamento Modificado
- ✅ Pagamento NÃO é transferido imediatamente após confirmação
- ✅ Pagamento fica "em espera" na conta da plataforma
- ✅ Transferência só acontece APÓS confirmação de envio pelo vendedor
- ✅ Sistema de reembolso implementado (se pedido cancelado antes do envio)

### 2. Funções Firebase Criadas/Modificadas

#### `createProductPaymentIntent`
- Cria PaymentIntent SEM transferência automática
- Dinheiro fica na conta da plataforma até confirmação de envio

#### `confirmProductPayment`
- Confirma pagamento mas NÃO transfere
- Marca status como `PENDING_SHIPMENT`

#### `transferPaymentToSeller` (NOVA)
- Transfere 98% do valor para o vendedor
- Mantém 2% de comissão na conta da plataforma
- Só pode ser chamada após confirmação de envio

#### `refundProductPayment` (NOVA)
- Reembolsa pagamento se pedido cancelado antes do envio
- Protege compradores contra fraudes

### 3. Integração no App
- ✅ `ShipmentScreen` chama `transferPaymentToSeller` automaticamente
- ✅ Funciona para envios entre cidades e entregas locais
- ✅ Notificações criadas para vendedor e cliente

## 🔑 Configuração das Chaves Stripe

### Opção 1: Via Script PowerShell (Recomendado)

1. Execute o script na pasta raiz do projeto:
```powershell
.\configurar_stripe_secrets.ps1
```

2. Quando solicitado, cole os valores:
   - **STRIPE_SECRET_KEY:** `[INSIRA_SUA_CHAVE_SECRETA_AQUI]`
   - **STRIPE_REFRESH_URL:** `https://taskgo.app/settings` (ou seu domínio)
   - **STRIPE_RETURN_URL:** `https://taskgo.app/settings` (ou seu domínio)

### Opção 2: Via Firebase CLI (Manual)

Execute os seguintes comandos um por vez:

```powershell
# 1. Configurar chave secreta
firebase functions:secrets:set STRIPE_SECRET_KEY
# Cole quando solicitado: [INSIRA_SUA_CHAVE_SECRETA_AQUI]

# 2. Configurar URL de refresh
firebase functions:secrets:set STRIPE_REFRESH_URL
# Cole quando solicitado: https://taskgo.app/settings

# 3. Configurar URL de return
firebase functions:secrets:set STRIPE_RETURN_URL
# Cole quando solicitado: https://taskgo.app/settings
```

### Opção 3: Via Firebase Console

1. Acesse https://console.firebase.google.com
2. Selecione seu projeto TaskGo
3. Vá em **Functions** → **Secrets**
4. Clique em **"Add secret"** e adicione:
   - `STRIPE_SECRET_KEY` = `[INSIRA_SUA_CHAVE_SECRETA_AQUI]`
   - `STRIPE_REFRESH_URL` = `https://taskgo.app/settings`
   - `STRIPE_RETURN_URL` = `https://taskgo.app/settings`

## 🚀 Deploy das Functions

Após configurar os secrets, faça o deploy:

```powershell
firebase deploy --only functions
```

## 📋 Fluxo Completo de Pagamento

### 1. Cliente Finaliza Compra
```
Cliente → Checkout → createProductPaymentIntent
→ PaymentIntent criado (sem transferência)
→ Status: PENDING_PAYMENT
```

### 2. Cliente Confirma Pagamento
```
Cliente → confirmProductPayment
→ Pagamento confirmado no Stripe
→ Dinheiro recebido na conta da plataforma
→ Status: PAID (mas transferStatus: PENDING_SHIPMENT)
→ Vendedor recebe notificação: "Aguardando envio"
```

### 3. Vendedor Confirma Envio
```
Vendedor → ShipmentScreen → confirmShipment
→ Documento de envio criado no Firestore
→ transferPaymentToSeller chamado automaticamente
→ 98% transferido para vendedor
→ 2% mantido na conta da plataforma
→ Status: SHIPPED
→ transferStatus: TRANSFERRED
```

### 4. Proteção contra Fraudes
```
Se pedido cancelado ANTES do envio:
→ refundProductPayment pode ser chamado
→ Reembolso total para o cliente
→ transferStatus: REFUNDED
```

## 🔒 Segurança

- ✅ Pagamento só é transferido após confirmação de envio
- ✅ Vendedor não pode receber dinheiro sem enviar produto
- ✅ Cliente pode solicitar reembolso antes do envio
- ✅ Comissão de 2% garantida para a plataforma

## ⚠️ Importante

1. **Chaves Live:** As chaves fornecidas são de **produção (live)**. Use com cuidado!
2. **Testes:** Para testar, considere criar chaves de teste primeiro
3. **Onboarding:** Cada vendedor precisa completar o onboarding do Stripe Connect
4. **Verificação:** Certifique-se de que a conta Stripe está verificada

## 🧪 Testando

1. Faça um pedido de teste no app
2. Complete o pagamento
3. Como vendedor, confirme o envio
4. Verifique no Stripe Dashboard se a transferência foi feita
5. Verifique no Firestore se o `transferStatus` foi atualizado para `TRANSFERRED`

## 📞 Suporte

- **Stripe Dashboard:** https://dashboard.stripe.com
- **Firebase Console:** https://console.firebase.google.com
- **Logs das Functions:** `firebase functions:log`

