# 🔐 Guia Passo a Passo: Configurar Secrets do Stripe

## ✅ Status Atual
- ✅ Deploy das Functions concluído
- ✅ Build do App concluída
- ✅ Script de configuração atualizado e unificado
- ⏳ **Próximo passo:** Executar o script para configurar os Secrets do Stripe

## 📋 O que são Secrets?

Secrets são variáveis de ambiente seguras que armazenam informações sensíveis (como chaves de API) no Firebase. Eles são criptografados e não aparecem no código.

## 🔑 Secrets Necessários

O sistema precisa de **5 secrets** para funcionar completamente:

1. **STRIPE_SECRET_KEY** (Obrigatório) - Chave privada do Stripe
2. **STRIPE_PUBLISHABLE_KEY** (Obrigatório) - Chave pública do Stripe  
3. **STRIPE_WEBHOOK_SECRET** (Opcional) - Secret do webhook (pode configurar depois)
4. **STRIPE_REFRESH_URL** (Obrigatório) - URL de retorno após onboarding
5. **STRIPE_RETURN_URL** (Obrigatório) - URL de retorno após onboarding

## 🚀 Método 1: Via Firebase Console (Mais Fácil)

### Passo 1: Acessar o Console
1. Abra seu navegador e acesse: https://console.firebase.google.com
2. Selecione o projeto: **task-go-ee85f**

### Passo 2: Navegar até Secrets
1. No menu lateral esquerdo, clique em **"Functions"**
2. Clique na aba **"Secrets"** (ou "Variáveis de ambiente")

### Passo 3: Adicionar Secret 1 - STRIPE_SECRET_KEY
1. Clique no botão **"Add secret"** ou **"Adicionar secret"**
2. No campo **"Secret name"**, digite: `STRIPE_SECRET_KEY`
3. No campo **"Secret value"**, cole a chave privada (sk_live_... ou sk_test_...)
4. Clique em **"Add secret"** ou **"Adicionar"**

### Passo 4: Adicionar Secret 2 - STRIPE_PUBLISHABLE_KEY
1. Clique novamente em **"Add secret"**
2. Nome: `STRIPE_PUBLISHABLE_KEY`
3. Valor: Cole a chave pública (pk_live_... ou pk_test_...)
4. Clique em **"Add secret"**

### Passo 5: Adicionar Secret 3 - STRIPE_WEBHOOK_SECRET (Opcional)
1. Clique novamente em **"Add secret"**
2. Nome: `STRIPE_WEBHOOK_SECRET`
3. Valor: Cole o secret do webhook (whsec_...)
   - Você obterá isso após configurar o webhook no Stripe Dashboard
   - Pode pular este passo e configurar depois
4. Clique em **"Add secret"**

### Passo 6: Adicionar Secret 4 - STRIPE_REFRESH_URL
1. Clique novamente em **"Add secret"**
2. Nome: `STRIPE_REFRESH_URL`
3. Valor: `https://taskgo.app/settings`
   (ou seu domínio, se diferente)
4. Clique em **"Add secret"**

### Passo 7: Adicionar Secret 5 - STRIPE_RETURN_URL
1. Clique novamente em **"Add secret"**
2. Nome: `STRIPE_RETURN_URL`
3. Valor: `https://taskgo.app/settings`
   (ou seu domínio, se diferente)
4. Clique em **"Add secret"**

### Passo 8: Verificar
Você deve ver 5 secrets listados (ou 4 se pulou o webhook):
- ✅ STRIPE_SECRET_KEY
- ✅ STRIPE_PUBLISHABLE_KEY
- ✅ STRIPE_WEBHOOK_SECRET (opcional)
- ✅ STRIPE_REFRESH_URL
- ✅ STRIPE_RETURN_URL

## 💻 Método 2: Via Firebase CLI (Terminal)

### Passo 1: Abrir Terminal/PowerShell
Abra o PowerShell na pasta do projeto:
```powershell
cd C:\Users\user\AndroidStudioProjects\TaskGoApp
```

### Passo 2: Configurar STRIPE_SECRET_KEY
Execute o comando:
```powershell
firebase functions:secrets:set STRIPE_SECRET_KEY
```

Quando solicitado, cole a chave privada (sk_live_... ou sk_test_...)

**Nota:** A chave não aparecerá na tela por segurança. Apenas cole e pressione Enter.

### Passo 3: Configurar STRIPE_PUBLISHABLE_KEY
Execute:
```powershell
firebase functions:secrets:set STRIPE_PUBLISHABLE_KEY
```

Quando solicitado, cole a chave pública (pk_live_... ou pk_test_...)

### Passo 4: Configurar STRIPE_WEBHOOK_SECRET (Opcional)
Execute:
```powershell
firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
```

Quando solicitado, cole o secret do webhook (whsec_...)
- Você obterá isso após configurar o webhook no Stripe Dashboard
- Pode pular este passo e configurar depois

### Passo 5: Configurar STRIPE_REFRESH_URL
Execute:
```powershell
echo "https://taskgo.app/settings" | firebase functions:secrets:set STRIPE_REFRESH_URL
```

Ou se preferir digitar manualmente:
```powershell
firebase functions:secrets:set STRIPE_REFRESH_URL
```
E cole: `https://taskgo.app/settings`

### Passo 6: Configurar STRIPE_RETURN_URL
Execute:
```powershell
echo "https://taskgo.app/settings" | firebase functions:secrets:set STRIPE_RETURN_URL
```

Ou se preferir digitar manualmente:
```powershell
firebase functions:secrets:set STRIPE_RETURN_URL
```
E cole: `https://taskgo.app/settings`

### Passo 7: Verificar Secrets
Para listar todos os secrets configurados:
```powershell
firebase functions:secrets:access
```

## 🎯 Método 3: Via Script PowerShell (Automático)

### Passo 1: Executar o Script
Na pasta raiz do projeto, execute:
```powershell
.\configurar_stripe_secrets.ps1
```

### Passo 2: Seguir as Instruções
O script irá solicitar cada valor. Cole quando solicitado:
1. **STRIPE_SECRET_KEY:** Sua chave privada do Stripe (sk_live_... ou sk_test_...)
2. **STRIPE_PUBLISHABLE_KEY:** Sua chave pública do Stripe (pk_live_... ou pk_test_...)
3. **STRIPE_WEBHOOK_SECRET:** Secret do webhook (whsec_...) - Opcional, pode pular
4. **STRIPE_REFRESH_URL:** `https://taskgo.app/settings` (ou seu domínio)
5. **STRIPE_RETURN_URL:** `https://taskgo.app/settings` (ou seu domínio)

## ⚠️ Importante Após Configurar

### 1. Fazer Redeploy das Functions
Após adicionar os secrets, você precisa fazer um novo deploy para que as functions possam usá-los:

```powershell
firebase deploy --only functions
```

**Por quê?** As functions precisam ser atualizadas para ter acesso aos novos secrets.

### 2. Verificar se Funcionou
Após o deploy, teste fazendo um pedido no app. Se tudo estiver configurado corretamente:
- ✅ O pagamento será processado
- ✅ O dinheiro ficará na conta da plataforma
- ✅ A transferência acontecerá após confirmação de envio

## 🔍 Verificar Logs

Se algo não funcionar, verifique os logs:

```powershell
firebase functions:log
```

Ou no Console:
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/functions/logs
2. Veja os logs em tempo real

## 📝 Resumo dos Secrets

| Secret | Tipo | Onde Obter |
|--------|------|------------|
| `STRIPE_SECRET_KEY` | Obrigatório | Stripe Dashboard > API Keys > Secret key |
| `STRIPE_PUBLISHABLE_KEY` | Obrigatório | Stripe Dashboard > API Keys > Publishable key |
| `STRIPE_WEBHOOK_SECRET` | Opcional | Stripe Dashboard > Webhooks > Signing secret |
| `STRIPE_REFRESH_URL` | Obrigatório | URL do seu app (ex: `https://taskgo.app/settings`) |
| `STRIPE_RETURN_URL` | Obrigatório | URL do seu app (ex: `https://taskgo.app/settings`) |

### Valores Padrão para URLs
- **STRIPE_REFRESH_URL:** `https://taskgo.app/settings`
- **STRIPE_RETURN_URL:** `https://taskgo.app/settings`

**Nota:** Se seu domínio for diferente, use o domínio correto.

## ❓ Troubleshooting

### Erro: "Secret not found"
- Verifique se o nome do secret está correto (case-sensitive)
- Certifique-se de que fez o deploy após adicionar o secret

### Erro: "Invalid API Key"
- Verifique se copiou a chave completa (sem espaços)
- Certifique-se de que está usando a chave `sk_live_...` (não `sk_test_...`)

### Pagamentos não funcionam
- Verifique os logs das functions
- Certifique-se de que a conta Stripe está verificada
- Verifique se o Stripe Connect está ativado

## ✅ Checklist Final

- [ ] Secret `STRIPE_SECRET_KEY` configurado
- [ ] Secret `STRIPE_PUBLISHABLE_KEY` configurado
- [ ] Secret `STRIPE_WEBHOOK_SECRET` configurado (opcional, mas recomendado)
- [ ] Secret `STRIPE_REFRESH_URL` configurado
- [ ] Secret `STRIPE_RETURN_URL` configurado
- [ ] Deploy das functions realizado após configurar secrets
- [ ] Webhook configurado no Stripe Dashboard (se usar webhooks)
- [ ] Teste de pagamento realizado com sucesso

## 🎉 Pronto!

Após configurar os secrets e fazer o redeploy, seu sistema de pagamento estará totalmente funcional!

