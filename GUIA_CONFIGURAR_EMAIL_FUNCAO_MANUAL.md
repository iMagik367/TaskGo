# 📧 Guia: Configurar Função Manual de Envio de Email

## ✅ Solução Implementada

Como a extensão "Trigger Email from Firestore" não funciona com Firestore em multi-região `nam5`, criamos uma **Cloud Function manual** que funciona perfeitamente.

## 📋 Passos para Configurar

### 1. Instalar Dependências

```bash
cd functions
npm install
```

Isso instalará o `nodemailer` que foi adicionado ao `package.json`.

### 2. Configurar Credenciais SMTP

Configure as variáveis de ambiente do Firebase Functions:

```bash
# SMTP Configuration
firebase functions:config:set smtp.host="smtp.gmail.com"
firebase functions:config:set smtp.port="465"
firebase functions:config:set smtp.user="seu-email@gmail.com"
firebase functions:config:set smtp.password="sua-senha-app"

# Email defaults
firebase functions:config:set email.default_from="noreply@taskgo.com"
firebase functions:config:set email.default_reply_to="suporte@taskgo.com"
```

**Para Gmail:**
- Você precisará gerar uma "Senha de app" em: https://myaccount.google.com/apppasswords
- Não use sua senha normal do Gmail

**Para outros provedores SMTP:**
- Ajuste `smtp.host` e `smtp.port` conforme seu provedor
- Exemplo SendGrid: `smtp.sendgrid.net`, porta `587`

### 3. Fazer Build

```bash
cd functions
npm run build
```

### 4. Deploy da Função

```bash
# Do diretório raiz do projeto
firebase deploy --only functions:sendEmail
```

**Durante o deploy:**
- Quando perguntado sobre a região, selecione **"Iowa (us-central1)"**
- A função pode estar em `us-central1` e acessar o Firestore em `nam5` sem problemas

### 5. Verificar Deploy

```bash
firebase functions:list
```

Você deve ver `sendEmail` na lista.

## 📝 Como Usar

### Enviar um Email

Para enviar um email, crie um documento na coleção `mail` do Firestore:

**Exemplo 1: Email Simples**
```javascript
// No Firestore, coleção 'mail'
{
  "to": "destinatario@exemplo.com",
  "message": {
    "subject": "Bem-vindo!",
    "text": "Olá! Este é um email de teste.",
    "html": "<h1>Olá!</h1><p>Este é um email de teste.</p>"
  }
}
```

**Exemplo 2: Email Completo**
```javascript
{
  "to": ["pessoa1@exemplo.com", "pessoa2@exemplo.com"],
  "cc": "copia@exemplo.com",
  "from": "remetente@exemplo.com", // Opcional, usa default_from se não especificado
  "replyTo": "resposta@exemplo.com", // Opcional
  "message": {
    "subject": "Assunto do Email",
    "text": "Versão texto do email",
    "html": "<html><body><h1>Título</h1><p>Conteúdo HTML</p></body></html>"
  },
  "headers": { // Opcional
    "X-Custom-Header": "Valor"
  }
}
```

### Status do Email

A função atualiza o documento com o status:

- `processing` - Email sendo processado
- `sent` - Email enviado com sucesso (inclui `messageId` e `sentAt`)
- `error` - Erro ao enviar (inclui `error` e `failedAt`)

## 🔍 Verificar Logs

Para ver os logs da função:

```bash
firebase functions:log --only sendEmail
```

Ou no console:
- https://console.firebase.google.com/project/task-go-ee85f/functions

## ⚙️ Configuração Avançada

### Alterar Configurações SMTP

```bash
firebase functions:config:set smtp.host="novo-host"
firebase functions:config:set smtp.port="587"
firebase deploy --only functions:sendEmail
```

### Ver Configurações Atuais

```bash
firebase functions:config:get
```

## ✅ Vantagens desta Solução

1. ✅ **Funciona com Firestore em `nam5`** - Não tem problema com multi-regiões
2. ✅ **Controle total** - Você pode customizar conforme necessário
3. ✅ **Sem dependência de extensões** - Não depende de extensões do Firebase
4. ✅ **Mesma funcionalidade** - Envia email quando documento é criado
5. ✅ **Fácil de manter** - Código no seu projeto, fácil de debugar

## 🐛 Troubleshooting

### Erro: "SMTP credentials not configured"
- Execute os comandos `firebase functions:config:set` acima
- Faça redeploy: `firebase deploy --only functions:sendEmail`

### Erro: "Authentication failed"
- Para Gmail: Use uma "Senha de app", não a senha normal
- Verifique se o usuário e senha estão corretos

### Email não está sendo enviado
- Verifique os logs: `firebase functions:log --only sendEmail`
- Verifique se o documento na coleção `mail` tem os campos corretos
- Verifique o campo `status` no documento para ver se há erro

### Verificar se a função está ativa
```bash
firebase functions:list
```

## 📚 Documentação Adicional

- Função criada em: `functions/src/sendEmail.ts`
- Exportada em: `functions/src/index.ts`
- Usa a biblioteca: [nodemailer](https://nodemailer.com/about/)

## 🎯 Resumo Rápido

1. ✅ Função criada: `functions/src/sendEmail.ts`
2. ⏳ Execute: `cd functions && npm install`
3. ⏳ Configure: `firebase functions:config:set smtp.*`
4. ⏳ Deploy: `firebase deploy --only functions:sendEmail`
5. ✅ Use: Criar documento na coleção `mail`

















