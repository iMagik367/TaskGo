# Configuração do Google Pay - TaskGo App

## APIs do Google que precisam ser ativadas

Para que o Google Pay funcione corretamente, você precisa ativar as seguintes APIs no Google Cloud Console:

### 1. Google Pay API
- **Nome**: Google Pay API
- **URL**: https://console.cloud.google.com/apis/library/payapi.googleapis.com
- **Descrição**: API necessária para processar pagamentos via Google Pay
- **Status**: Deve estar ativada

### 2. Google Wallet API (anteriormente Google Pay Passes API)
- **Nome**: Google Wallet API
- **URL**: https://console.cloud.google.com/apis/library/walletobjects.googleapis.com
- **Descrição**: API para gerenciar passes e cartões digitais
- **Status**: Opcional, mas recomendada para funcionalidades futuras

### 3. Google Play Services Wallet
- **Nome**: Google Play Services Wallet
- **Descrição**: SDK do Android para integração com Google Pay
- **Status**: Já incluído no projeto via `play-services-wallet:19.2.0`

## Configuração no Google Pay Business Console

1. Acesse: https://pay.google.com/business/console
2. Crie uma conta de comerciante ou faça login
3. Configure seu perfil de comerciante:
   - Nome do negócio: TaskGo
   - Informações de contato
   - Endereço comercial
4. Obtenha seu **Merchant ID** do Google Pay
5. Configure o gateway de pagamento:
   - Selecione **Stripe** como gateway
   - Configure o **Gateway Merchant ID** do Stripe
   - O Gateway Merchant ID do Stripe pode ser encontrado no Stripe Dashboard em Settings > Payment methods > Google Pay

## Configuração no Stripe

1. Acesse: https://dashboard.stripe.com/
2. Vá em **Settings > Payment methods**
3. Ative o **Google Pay**
4. Configure o Google Pay:
   - Adicione seus domínios autorizados
   - Configure o Merchant ID do Google Pay
   - Obtenha o **Gateway Merchant ID** do Stripe

## Variáveis de Ambiente Necessárias

### Firebase Functions
Configure no Firebase Console (https://console.firebase.google.com/project/[SEU_PROJECT]/functions/config):

1. `STRIPE_SECRET_KEY` - Chave secreta do Stripe
2. `STRIPE_WEBHOOK_SECRET` - Secret do webhook do Stripe
3. `GOOGLE_PAY_MERCHANT_ID` - Merchant ID do Google Pay (opcional, pode ser hardcoded no código)

### Android App
As configurações estão no código:
- `GooglePayManager.kt` - Contém o Merchant ID e Gateway Merchant ID
- Para produção, atualize os valores hardcoded com os valores reais

## Como Ativar as APIs

1. Acesse o Google Cloud Console: https://console.cloud.google.com/
2. Selecione seu projeto Firebase
3. Vá em **APIs & Services > Library**
4. Procure por "Google Pay API"
5. Clique em **Enable**
6. Repita para "Google Wallet API" se necessário

## Testando o Google Pay

### Ambiente de Teste
- Use `WalletConstants.ENVIRONMENT_TEST` no código
- Teste com cartões de teste do Google Pay
- Use o Stripe Test Mode

### Ambiente de Produção
- Altere para `WalletConstants.ENVIRONMENT_PRODUCTION`
- Use cartões reais
- Use o Stripe Live Mode
- Certifique-se de que todas as APIs estão ativadas

## Troubleshooting

### Erro: "Google Pay não está disponível"
- Verifique se o dispositivo suporta Google Pay
- Verifique se o Google Pay está instalado
- Verifique se as APIs estão ativadas

### Erro: "Gateway não configurado"
- Verifique o Gateway Merchant ID no Stripe
- Certifique-se de que o Stripe está configurado corretamente
- Verifique as credenciais do Stripe

### Erro: "Merchant ID inválido"
- Verifique o Merchant ID no Google Pay Business Console
- Certifique-se de que o Merchant ID está correto no código

## Arquivos Modificados

1. `app/src/main/java/com/taskgoapp/taskgo/core/payment/GooglePayManager.kt`
   - Configurado para usar Stripe como gateway
   - Merchant ID e Gateway Merchant ID configuráveis

2. `app/src/main/java/com/taskgoapp/taskgo/feature/checkout/presentation/PaymentMethodScreen.kt`
   - UI melhorada para o botão do Google Pay
   - Integração com o backend

3. `functions/src/payments.ts`
   - Nova função `processGooglePayPayment` para processar pagamentos do Google Pay via Stripe

## Próximos Passos

1. Ativar as APIs no Google Cloud Console
2. Configurar o perfil de comerciante no Google Pay Business Console
3. Obter o Merchant ID do Google Pay
4. Configurar o Stripe com Google Pay
5. Obter o Gateway Merchant ID do Stripe
6. Atualizar os valores no código (ou usar variáveis de ambiente)
7. Testar em ambiente de teste
8. Fazer deploy para produção

