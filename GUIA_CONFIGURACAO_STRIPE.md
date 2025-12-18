# Guia de Configuração do Stripe para TaskGo

## 📋 Resumo Rápido

1. **Criar conta Stripe:** https://dashboard.stripe.com/register
2. **Obter chaves API:** https://dashboard.stripe.com/apikeys
3. **Ativar Stripe Connect:** https://dashboard.stripe.com/settings/connect
4. **Configurar Secrets no Firebase:**
   - Console: Functions → Secrets → Add secret
   - CLI: `firebase functions:secrets:set STRIPE_SECRET_KEY`
5. **Fazer deploy:** `firebase deploy --only functions`

**Variáveis necessárias:**
- `STRIPE_SECRET_KEY` (sk_test_... ou sk_live_...)
- `STRIPE_REFRESH_URL` (https://taskgo.app/settings)
- `STRIPE_RETURN_URL` (https://taskgo.app/settings)

---

## Passo a Passo para Configurar a Conta da Plataforma

### 1. Criar Conta no Stripe

1. Acesse https://dashboard.stripe.com/register
2. Crie uma conta com:
   - Email da TaskGo
   - Nome da empresa: TaskGo
   - País: Brasil
   - Tipo de conta: **Business/Plataforma**
3. Complete o cadastro com seus dados pessoais/empresariais

### 2. Verificar e Ativar a Conta

1. Após criar a conta, você precisará:
   - Verificar seu email
   - Adicionar informações da empresa
   - Adicionar informações bancárias (para receber a comissão de 2%)
   - Verificar identidade (documentos)

2. **IMPORTANTE:** Complete todo o processo de verificação antes de usar em produção

### 3. Obter as Chaves de API

1. Acesse https://dashboard.stripe.com/apikeys
2. Você verá duas chaves:
   - **Publishable key** (chave pública) - começa com `pk_test_` ou `pk_live_`
   - **Secret key** (chave secreta) - começa com `sk_test_` ou `sk_live_`

3. **Para testes (desenvolvimento):**
   - Use as chaves de **Test mode** (modo de teste)
   - `pk_test_...` e `sk_test_...`

4. **Para produção:**
   - Ative o **Live mode** no dashboard
   - Use as chaves de **Live mode**
   - `pk_live_...` e `sk_live_...`

### 4. Ativar Stripe Connect

1. Acesse https://dashboard.stripe.com/settings/connect
2. Clique em **"Get started"** ou **"Activate Connect"**
3. Escolha o tipo: **Express accounts** (recomendado para marketplace)
4. Configure:
   - **Application name:** TaskGo
   - **Application website:** https://taskgo.app (ou seu domínio)
   - **Support email:** suporte@taskgo.app (ou seu email)
5. Salve as configurações

### 5. Configurar no Firebase Functions

#### Opção A: Via Firebase Console - Secrets (Recomendado para Produção)

**Firebase Functions agora usa "Secrets" em vez de variáveis de ambiente config.**

1. Acesse https://console.firebase.google.com
2. Selecione seu projeto TaskGo
3. Vá em **Functions** → **Secrets** (menu lateral)
4. Clique em **"Add secret"** e adicione cada uma:

   **Secret 1:**
   - Nome: `STRIPE_SECRET_KEY`
   - Valor: `sk_test_...` (para testes) ou `sk_live_...` (para produção)
   - Clique em **"Add secret"**

   **Secret 2:**
   - Nome: `STRIPE_REFRESH_URL`
   - Valor: `https://taskgo.app/settings` (ou seu domínio)
   - Clique em **"Add secret"**

   **Secret 3:**
   - Nome: `STRIPE_RETURN_URL`
   - Valor: `https://taskgo.app/settings` (ou seu domínio)
   - Clique em **"Add secret"**

5. **IMPORTANTE:** Após adicionar os secrets, você precisa atualizar as functions para usá-los:
   - Edite cada function que usa Stripe e adicione `.runWith({ secrets: ['STRIPE_SECRET_KEY', 'STRIPE_REFRESH_URL', 'STRIPE_RETURN_URL'] })`
   - Ou faça o deploy novamente (o Firebase pode detectar automaticamente)

**Exemplo de atualização do código (se necessário):**
```typescript
export const createProductPaymentIntent = functions
  .runWith({ secrets: ['STRIPE_SECRET_KEY'] })
  .https.onCall(async (data, context) => {
    // ... código
  });
```

**Nota:** Se você já está usando `process.env.STRIPE_SECRET_KEY` no código, o Firebase Functions v2+ automaticamente injeta os secrets como variáveis de ambiente. Você só precisa adicionar os secrets no console.

#### Opção B: Via Firebase CLI - Secrets (Recomendado)

1. No terminal, navegue até a pasta raiz do projeto:
```bash
cd C:\Users\user\AndroidStudioProjects\TaskGoApp
```

2. Configure os secrets (para testes):
```bash
firebase functions:secrets:set STRIPE_SECRET_KEY
# Quando solicitado, cole: sk_test_...

firebase functions:secrets:set STRIPE_REFRESH_URL
# Quando solicitado, cole: https://taskgo.app/settings

firebase functions:secrets:set STRIPE_RETURN_URL
# Quando solicitado, cole: https://taskgo.app/settings
```

3. Para produção, use as chaves `sk_live_...`:
```bash
firebase functions:secrets:set STRIPE_SECRET_KEY
# Quando solicitado, cole: sk_live_...
```

**Nota:** O Firebase CLI pedirá para você colar o valor do secret de forma segura (não aparece na tela).

#### Opção C: Via arquivo .env (Apenas para Desenvolvimento Local com Emuladores)

**Use esta opção APENAS para testar localmente com Firebase Emulators.**

1. Crie um arquivo `.env` na pasta `functions`:
```bash
cd functions
# No Windows PowerShell:
New-Item .env -ItemType File
# Ou crie manualmente o arquivo .env
```

2. Adicione ao arquivo `.env`:
```env
STRIPE_SECRET_KEY=sk_test_...
STRIPE_REFRESH_URL=https://taskgo.app/settings
STRIPE_RETURN_URL=https://taskgo.app/settings
```

3. **IMPORTANTE:** 
   - O arquivo `.env` já está no `.gitignore` (não será commitado)
   - **NÃO use .env em produção** - sempre use Secrets do Firebase
   - Para carregar o .env nos emuladores, você pode precisar instalar e configurar `dotenv` no código

4. Para usar dotenv nos emuladores, adicione no início de `functions/src/index.ts`:
```typescript
import * as dotenv from 'dotenv';
dotenv.config();
```

**Nota:** O Firebase Emulators pode não carregar automaticamente o .env. Verifique a documentação dos emuladores para mais detalhes.

### 6. Atualizar o Código das Functions (se necessário)

Verifique se o arquivo `functions/src/product-payments.ts` está usando as variáveis corretamente:

```typescript
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
  apiVersion: '2023-10-16',
});
```

E no `functions/src/stripe-connect.ts`:

```typescript
refresh_url: process.env.STRIPE_REFRESH_URL || 'https://taskgo.app/settings',
return_url: process.env.STRIPE_RETURN_URL || 'https://taskgo.app/settings',
```

### 7. Fazer Deploy das Functions

1. Após configurar as variáveis, faça o deploy:
```bash
firebase deploy --only functions
```

2. Verifique se as functions foram deployadas corretamente:
```bash
firebase functions:list
```

### 8. Testar a Configuração

1. **Teste de criação de Payment Intent:**
   - Faça um pedido de teste no app
   - Verifique os logs no Firebase Console → Functions → Logs
   - Deve aparecer: "Product payment intent created for order..."

2. **Teste de Stripe Connect:**
   - Acesse a tela de configurações no app (como vendedor)
   - Tente criar um link de onboarding
   - Deve redirecionar para o Stripe para completar o cadastro

### 9. Configurações Adicionais Recomendadas

#### Webhooks (Opcional, mas recomendado)

1. Acesse https://dashboard.stripe.com/webhooks
2. Clique em **"Add endpoint"**
3. Configure:
   - **Endpoint URL:** `https://us-central1-[SEU-PROJECT-ID].cloudfunctions.net/stripeWebhook`
   - **Events to send:** Selecione eventos relevantes (ex: `payment_intent.succeeded`)
4. Copie o **Signing secret** e adicione como variável de ambiente:
   ```
   STRIPE_WEBHOOK_SECRET = whsec_...
   ```

#### Taxas e Comissões

1. Acesse https://dashboard.stripe.com/settings/billing/overview
2. Configure as taxas padrão (opcional)
3. **Nota:** A comissão de 2% já está configurada no código (`application_fee_amount`)

### 10. Checklist de Produção

Antes de ir para produção, certifique-se de:

- [ ] Conta Stripe verificada e ativada
- [ ] Stripe Connect ativado
- [ ] Chaves de **Live mode** configuradas no Firebase
- [ ] Informações bancárias adicionadas no Stripe
- [ ] Webhooks configurados (se necessário)
- [ ] Testes realizados com sucesso
- [ ] Documentação atualizada

### 11. Segurança

**NUNCA:**
- ❌ Commite as chaves secretas no Git
- ❌ Compartilhe as chaves secretas publicamente
- ❌ Use chaves de produção em ambiente de desenvolvimento

**SEMPRE:**
- ✅ Use variáveis de ambiente
- ✅ Adicione `.env` ao `.gitignore`
- ✅ Use chaves de teste durante desenvolvimento
- ✅ Rotacione as chaves se suspeitar de comprometimento

### 12. Suporte e Recursos

- **Documentação Stripe Connect:** https://stripe.com/docs/connect
- **Dashboard Stripe:** https://dashboard.stripe.com
- **Suporte Stripe:** https://support.stripe.com

### 13. Estrutura de Custos

**Stripe cobra:**
- Taxa por transação: ~3,99% + R$ 0,40 (cartão de crédito)
- Taxa por transferência Connect: 0% (você define a comissão)

**TaskGo recebe:**
- 2% de comissão (configurado no código)

**Vendedor recebe:**
- 98% do valor do produto (menos taxas do Stripe)

**Exemplo:**
- Produto: R$ 100,00
- Taxa Stripe: R$ 4,39 (3,99% + R$ 0,40)
- Comissão TaskGo: R$ 2,00 (2%)
- Vendedor recebe: R$ 93,61 (98% - taxas Stripe)

### 14. Troubleshooting

#### Erro: "Invalid API Key"
- Verifique se a chave está correta
- Verifique se está usando a chave do modo correto (test/live)
- Verifique se a chave está configurada no Firebase

#### Erro: "Stripe Connect not activated"
- Acesse o dashboard e ative o Stripe Connect
- Verifique se completou todo o processo de verificação

#### Erro: "Account not found"
- Verifique se o vendedor completou o onboarding
- Verifique se o `stripeAccountId` está salvo no Firestore

#### Pagamentos não estão sendo processados
- Verifique os logs do Firebase Functions
- Verifique se as variáveis de ambiente estão configuradas
- Verifique se o Stripe Connect está ativado

