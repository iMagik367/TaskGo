# 📧 Valores SMTP Recomendados para TaskGo

## 🎯 Recomendação Principal

Para o TaskGo App, recomendo começar com **Gmail** (mais fácil) e depois migrar para **SendGrid** quando estiver em produção.

---

## ✅ Opção 1: Gmail (Para Começar - Recomendado)

**Use esta opção se:**
- Você tem uma conta Gmail
- Quer começar rapidamente
- Volume inicial de emails < 500/dia

### Valores para Configurar:

```
SMTP Host: smtp.gmail.com
SMTP Port: 465
SMTP User: seu-email@gmail.com
SMTP Password: [SENHA DE APP - veja como obter abaixo]
Email remetente padrão: noreply@taskgo.com (ou seu-email@gmail.com)
Email para resposta padrão: suporte@taskgo.com
```

### Como Obter Senha de App do Gmail:

1. Acesse: https://myaccount.google.com/apppasswords
2. Faça login na sua conta Google
3. Se não aparecer a opção, ative a verificação em 2 etapas primeiro
4. Selecione:
   - **App**: Mail
   - **Device**: Other (Custom name)
   - Digite: "TaskGo Firebase"
5. Clique em **Generate**
6. Copie a senha gerada (16 caracteres, sem espaços)
7. Use essa senha no campo **SMTP Password**

**Exemplo:**
- Seu email: `joao.silva@gmail.com`
- Senha de app gerada: `abcd efgh ijkl mnop` → Use: `abcdefghijklmnop`

---

## ⭐ Opção 2: SendGrid (Para Produção - Recomendado)

**Use esta opção se:**
- Quer uma solução mais profissional
- Precisa de analytics de emails
- Planeja enviar muitos emails
- Quer melhor deliverability

### Valores para Configurar:

```
SMTP Host: smtp.sendgrid.net
SMTP Port: 587
SMTP User: apikey
SMTP Password: [SUA API KEY DO SENDGRID]
Email remetente padrão: noreply@taskgo.com (deve estar verificado no SendGrid)
Email para resposta padrão: suporte@taskgo.com
```

### Como Obter API Key do SendGrid:

1. Crie conta gratuita em: https://signup.sendgrid.com
2. Verifique seu email
3. Vá em **Settings** → **API Keys**
4. Clique em **Create API Key**
5. Dê um nome: "TaskGo Firebase Function"
6. Selecione permissão: **Restricted Access**
7. Em **Mail Send**, marque **Full Access**
8. Clique em **Create & View**
9. **COPIE A API KEY** (ela só aparece uma vez!)
10. Use essa API Key no campo **SMTP Password**

**Importante:**
- O **SMTP User** deve ser literalmente: `apikey` (não seu email!)
- O **SMTP Password** é a API Key que você copiou

---

## 📋 Exemplos Práticos

### Exemplo com Gmail:

```powershell
.\configurar-smtp-rapido.ps1 `
  -Host "smtp.gmail.com" `
  -Port "465" `
  -User "meuemail@gmail.com" `
  -Password "abcdefghijklmnop" `
  -From "noreply@taskgo.com" `
  -ReplyTo "suporte@taskgo.com"
```

### Exemplo com SendGrid:

```powershell
.\configurar-smtp-rapido.ps1 `
  -Host "smtp.sendgrid.net" `
  -Port "587" `
  -User "apikey" `
  -Password "SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" `
  -From "noreply@taskgo.com" `
  -ReplyTo "suporte@taskgo.com"
```

---

## 🔍 Qual Escolher?

### Comece com Gmail se:
- ✅ Quer configurar rapidamente
- ✅ Tem conta Gmail
- ✅ Volume inicial baixo (< 500 emails/dia)
- ✅ É para testes/desenvolvimento

### Use SendGrid se:
- ✅ App está em produção
- ✅ Precisa de analytics
- ✅ Quer melhor deliverability
- ✅ Planeja escalar
- ✅ Quer parecer mais profissional

---

## ⚠️ Importante

1. **Gmail**: 
   - Use SEMPRE senha de app (não senha normal)
   - Limite: ~500 emails/dia
   - Pode ir para spam se enviar muitos

2. **SendGrid**:
   - Use `apikey` como usuário
   - Limite: 100 emails/dia no plano gratuito
   - Melhor deliverability
   - Precisa verificar domínio para emails customizados

3. **Email remetente**:
   - Deve ser um email válido
   - Para Gmail: pode usar seu próprio email
   - Para SendGrid: deve estar verificado no SendGrid

---

## 🚀 Próximo Passo

Escolha uma opção acima e execute:

```powershell
.\configurar-smtp-email.ps1
```

Ou forneça os valores e eu configuro para você!

















