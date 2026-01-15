# 📧 Guia: Como Escolher Configurações SMTP

## 🎯 Qual Provedor SMTP Você Deve Usar?

Escolha baseado no que você já tem ou prefere:

### Opção 1: Gmail (Mais Comum e Fácil) ✅

**Vantagens:**
- Gratuito
- Fácil de configurar
- Confiável
- 500 emails/dia no plano gratuito

**Configurações:**
- **SMTP Host**: `smtp.gmail.com`
- **SMTP Port**: `465` (recomendado) ou `587`
- **SMTP User**: Seu email Gmail completo (ex: `seuemail@gmail.com`)
- **SMTP Password**: **Senha de app** (NÃO sua senha normal!)
- **Email remetente padrão**: Seu email Gmail ou `noreply@taskgo.com`
- **Email para resposta padrão**: `suporte@taskgo.com` ou seu email

**Como obter Senha de App do Gmail:**
1. Acesse: https://myaccount.google.com/apppasswords
2. Faça login
3. Selecione "App" → "Mail"
4. Selecione "Device" → "Other (Custom name)"
5. Digite: "TaskGo Firebase Function"
6. Clique em "Generate"
7. Copie a senha gerada (16 caracteres sem espaços)
8. Use essa senha no campo SMTP Password

### Opção 2: SendGrid (Recomendado para Produção) ⭐

**Vantagens:**
- 100 emails/dia gratuitos
- Melhor para produção
- APIs e webhooks
- Analytics de emails

**Configurações:**
- **SMTP Host**: `smtp.sendgrid.net`
- **SMTP Port**: `587` (STARTTLS) ou `465` (SSL)
- **SMTP User**: `apikey` (literalmente essa palavra)
- **SMTP Password**: Sua API Key do SendGrid
- **Email remetente padrão**: Email verificado no SendGrid
- **Email para resposta padrão**: Email de suporte

**Como obter API Key do SendGrid:**
1. Crie conta em: https://sendgrid.com
2. Vá em Settings → API Keys
3. Crie uma nova API Key
4. Dê permissão "Mail Send"
5. Copie a API Key gerada

### Opção 3: Outlook/Hotmail

**Configurações:**
- **SMTP Host**: `smtp-mail.outlook.com`
- **SMTP Port**: `587`
- **SMTP User**: Seu email Outlook completo
- **SMTP Password**: Sua senha do Outlook
- **Email remetente padrão**: Seu email Outlook
- **Email para resposta padrão**: Seu email Outlook

### Opção 4: Outros Provedores

**Zoho Mail:**
- Host: `smtp.zoho.com`
- Port: `465` ou `587`
- User: Seu email Zoho
- Password: Senha de app do Zoho

**Mailgun:**
- Host: `smtp.mailgun.org`
- Port: `587`
- User: Seu SMTP username do Mailgun
- Password: Sua SMTP password do Mailgun

## 📋 Valores Recomendados para TaskGo

Baseado no seu projeto, recomendo:

### Se usar Gmail:
```
SMTP Host: smtp.gmail.com
SMTP Port: 465
SMTP User: [seu-email@gmail.com]
SMTP Password: [senha-app do Gmail - 16 caracteres]
Email remetente padrão: noreply@taskgo.com (ou seu email Gmail)
Email para resposta padrão: suporte@taskgo.com
```

### Se usar SendGrid (Melhor para produção):
```
SMTP Host: smtp.sendgrid.net
SMTP Port: 587
SMTP User: apikey
SMTP Password: [sua-api-key-do-sendgrid]
Email remetente padrão: noreply@taskgo.com (deve estar verificado no SendGrid)
Email para resposta padrão: suporte@taskgo.com
```

## ⚠️ Importante

1. **Gmail**: Use SEMPRE senha de app, nunca a senha normal
2. **SendGrid**: Use `apikey` como usuário e a API Key como senha
3. **Email remetente**: Deve ser um email válido e verificado no provedor
4. **Limites**: Gmail permite ~500 emails/dia, SendGrid ~100/dia no plano gratuito

## 🚀 Próximo Passo

Depois de escolher o provedor e obter as credenciais, execute:

```powershell
.\configurar-smtp-email.ps1
```

Ou se preferir com parâmetros:

```powershell
.\configurar-smtp-rapido.ps1 -Host "smtp.gmail.com" -Port "465" -User "seu-email@gmail.com" -Password "senha-app" -From "noreply@taskgo.com" -ReplyTo "suporte@taskgo.com"
```

## 💡 Recomendação Final

Para começar rapidamente: **Use Gmail** com senha de app
Para produção: **Use SendGrid** (mais profissional e confiável)

















