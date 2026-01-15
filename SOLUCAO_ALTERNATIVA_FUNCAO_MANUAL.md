# 🔧 Solução Alternativa: Cloud Function Manual para Envio de Email

## 🔍 Problema Identificado

O Firestore está em multi-região `nam5`, e a extensão "Trigger Email from Firestore" **não suporta** triggers em multi-regiões. Isso é uma limitação conhecida do Firebase.

**Solução:** Criar uma Cloud Function manual que funciona com `nam5`.

## ✅ Solução: Cloud Function Manual

Vamos criar uma Cloud Function que escuta mudanças no Firestore e envia emails manualmente.

### Passo 1: Verificar Estrutura do Projeto

Já temos o diretório `functions` no projeto. Vamos criar uma função de email.

### Passo 2: Instalar Dependências

No diretório `functions`, instale as dependências necessárias:

```bash
cd functions
npm install nodemailer firebase-functions@latest firebase-admin@latest
```

### Passo 3: Criar Função de Email

Crie um arquivo `functions/src/sendEmail.ts`:

```typescript
import * as functions from 'firebase-functions/v1';
import * as admin from 'firebase-admin';
import * as nodemailer from 'nodemailer';

admin.initializeApp();

// Configurar transporte SMTP (ajuste com suas credenciais)
const transporter = nodemailer.createTransport({
  host: functions.config().smtp.host || 'smtp.gmail.com',
  port: parseInt(functions.config().smtp.port || '465'),
  secure: true,
  auth: {
    user: functions.config().smtp.user,
    pass: functions.config().smtp.password,
  },
});

// Função que escuta mudanças na coleção 'mail'
export const sendEmail = functions.firestore
  .document('mail/{mailId}')
  .onCreate(async (snap, context) => {
    const mailData = snap.data();
    
    try {
      // Preparar opções do email
      const mailOptions: nodemailer.SendMailOptions = {
        from: mailData.from || functions.config().email.default_from,
        to: Array.isArray(mailData.to) ? mailData.to.join(', ') : mailData.to,
        cc: mailData.cc ? (Array.isArray(mailData.cc) ? mailData.cc.join(', ') : mailData.cc) : undefined,
        bcc: mailData.bcc ? (Array.isArray(mailData.bcc) ? mailData.bcc.join(', ') : mailData.bcc) : undefined,
        subject: mailData.message?.subject || 'Sem assunto',
        text: mailData.message?.text,
        html: mailData.message?.html,
        replyTo: mailData.replyTo || functions.config().email.default_reply_to,
      };

      // Enviar email
      const info = await transporter.sendMail(mailOptions);
      
      // Atualizar documento com status de sucesso
      await snap.ref.update({
        status: 'sent',
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        messageId: info.messageId,
      });
      
      console.log('Email sent successfully:', info.messageId);
      return null;
    } catch (error: any) {
      console.error('Error sending email:', error);
      
      // Atualizar documento com status de erro
      await snap.ref.update({
        status: 'error',
        error: error.message,
        failedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      
      throw new functions.https.HttpsError('internal', 'Failed to send email', error);
    }
  });
```

### Passo 4: Configurar Variáveis de Ambiente

Configure as variáveis de ambiente do Firebase:

```bash
# Configurar SMTP
firebase functions:config:set smtp.host="smtp.gmail.com"
firebase functions:config:set smtp.port="465"
firebase functions:config:set smtp.user="seu-email@gmail.com"
firebase functions:config:set smtp.password="sua-senha-app"

# Configurar emails padrão
firebase functions:config:set email.default_from="noreply@exemplo.com"
firebase functions:config:set email.default_reply_to="suporte@exemplo.com"
```

### Passo 5: Atualizar package.json

Verifique se `functions/package.json` tem as dependências:

```json
{
  "dependencies": {
    "firebase-admin": "^12.0.0",
    "firebase-functions": "^5.0.0",
    "nodemailer": "^6.9.0"
  }
}
```

### Passo 6: Deploy da Função

```bash
# Fazer build
cd functions
npm run build

# Deploy (do diretório raiz)
cd ..
firebase deploy --only functions:sendEmail
```

**IMPORTANTE:** Durante o deploy, quando perguntado sobre a região, selecione **"Iowa (us-central1)"**. A função pode estar em `us-central1` enquanto acessa o Firestore em `nam5`.

## 📝 Como Usar

Para enviar um email, crie um documento na coleção `mail`:

```typescript
// Exemplo de documento na coleção 'mail'
{
  "to": "destinatario@exemplo.com",
  "from": "remetente@exemplo.com", // opcional
  "message": {
    "subject": "Assunto do Email",
    "text": "Texto plano do email",
    "html": "<h1>HTML do email</h1>"
  },
  "replyTo": "resposta@exemplo.com" // opcional
}
```

A função será disparada automaticamente quando o documento for criado.

## 🔧 Alternativa: Usar v2 Functions (Mais Moderno)

Se preferir usar Cloud Functions v2 (recomendado), aqui está a versão:

```typescript
import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import { defineString } from 'firebase-functions/params';
import * as admin from 'firebase-admin';
import * as nodemailer from 'nodemailer';

admin.initializeApp();

// Parâmetros configuráveis
const smtpHost = defineString('SMTP_HOST', { default: 'smtp.gmail.com' });
const smtpPort = defineString('SMTP_PORT', { default: '465' });
const smtpUser = defineString('SMTP_USER');
const smtpPassword = defineString('SMTP_PASSWORD');
const defaultFrom = defineString('DEFAULT_FROM');
const defaultReplyTo = defineString('DEFAULT_REPLY_TO');

const transporter = nodemailer.createTransport({
  host: smtpHost.value(),
  port: parseInt(smtpPort.value()),
  secure: true,
  auth: {
    user: smtpUser.value(),
    pass: smtpPassword.value(),
  },
});

export const sendEmail = onDocumentCreated(
  {
    document: 'mail/{mailId}',
    region: 'us-central1', // Região da função (compatível com nam5)
  },
  async (event) => {
    const mailData = event.data?.data();
    
    if (!mailData) {
      console.error('No data in document');
      return;
    }

    try {
      const mailOptions: nodemailer.SendMailOptions = {
        from: mailData.from || defaultFrom.value(),
        to: Array.isArray(mailData.to) ? mailData.to.join(', ') : mailData.to,
        subject: mailData.message?.subject || 'Sem assunto',
        text: mailData.message?.text,
        html: mailData.message?.html,
        replyTo: mailData.replyTo || defaultReplyTo.value(),
      };

      const info = await transporter.sendMail(mailOptions);
      
      await event.data?.ref.update({
        status: 'sent',
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        messageId: info.messageId,
      });
      
      console.log('Email sent:', info.messageId);
    } catch (error: any) {
      console.error('Error:', error);
      await event.data?.ref.update({
        status: 'error',
        error: error.message,
        failedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      throw error;
    }
  }
);
```

Para configurar os parâmetros:

```bash
firebase functions:config:set smtp.host="smtp.gmail.com"
# etc...
```

## ✅ Vantagens desta Solução

1. ✅ Funciona com Firestore em `nam5`
2. ✅ Controle total sobre a configuração
3. ✅ Não depende de extensões
4. ✅ Pode ser customizada conforme necessário
5. ✅ Função pode estar em `us-central1` e acessar Firestore em `nam5`

## 🔗 Próximos Passos

1. Criar a função de email
2. Configurar credenciais SMTP
3. Fazer deploy
4. Testar criando um documento na coleção `mail`

















