# Guia de Correção de Extensões Firebase

## 🔴 Problema: Erro na Extensão Trigger Email

**Erro identificado:**
A extensão "Trigger Email from Firestore" está tentando criar um trigger na região `eur3`, mas o banco de dados Firestore padrão não existe nessa região. O sistema sugere usar a região `nam5` ou verificar a região correta.

**Causa:**
A extensão foi instalada com configuração de região incorreta. O Firestore pode estar em outra região (provavelmente `nam5` ou `us-central1`).

---

## ✅ Solução: Reconfigurar Extensão Trigger Email

### Opção 1: Desinstalar e Reinstalar (Recomendado)

1. **No Firebase Console:**
   - Vá para: Extensions > Trigger Email from Firestore
   - Clique em "Mais detalhes" no erro
   - Clique em "Desinstalar" (botão vermelho)

2. **Verificar região do Firestore:**
   - Vá para: Firestore Database > Settings (Configurações)
   - Verifique a região do banco de dados padrão `(default)`
   - Anote a região (provavelmente `nam5` ou `us-central1`)

3. **Reinstalar a extensão:**
   - Vá para: Extensions > Browse Catalog
   - Procure: "Trigger Email"
   - Clique em "Install" (Instalar)

4. **Configuração durante instalação:**
   ```
   Collection path: mail
   Location: [Use a mesma região do seu Firestore - provavelmente nam5 ou us-central1]
   SMTP connection URI: [Configure seu servidor SMTP]
   ```
   
   **Importante:** Na configuração de "Location", escolha a mesma região do seu banco Firestore.

### Opção 2: Configurar via Firebase CLI

Se você preferir configurar via CLI:

```bash
# Listar extensões instaladas
firebase ext:list

# Desinstalar a extensão
firebase ext:uninstall firestore-send-email

# Reinstalar com configuração correta
firebase ext:install firebase/firestore-send-email \
  --params=location=nam5 \
  --params=SMTP_CONNECTION_URI="smtps://smtp.gmail.com:465" \
  --params=SMTP_USERNAME="seu-email@gmail.com" \
  --params=SMTP_PASSWORD="sua-senha-app"
```

**Nota:** Ajuste `location` para a região correta do seu Firestore (verifique no console).

---

## 🔧 Configuração das Extensões (Print 2)

### 1. Trigger Email from Firestore
**Status:** Precisa ser corrigido (erro de região)

**Configuração necessária:**
- **Collection path:** `mail`
- **Location:** Mesma região do Firestore (verificar no console)
- **SMTP Connection URI:** Configure seu servidor SMTP
  - Exemplo Gmail: `smtps://smtp.gmail.com:465`
  - Exemplo SendGrid: `smtps://smtp.sendgrid.net:465`
- **SMTP Username:** Seu email SMTP
- **SMTP Password:** Senha ou App Password do email

**Para Gmail:**
1. Ative "Acesso de apps menos seguros" ou use "App Password"
2. Gere uma App Password: https://myaccount.google.com/apppasswords
3. Use como SMTP_PASSWORD

---

### 2. Run Payments with Stripe
**Status:** Instalada, precisa verificar configuração

**Verificar:**
- Vá para: Extensions > Run Payments with Stripe > Configurar
- Verifique se as seguintes configurações estão corretas:
  - **Stripe API Key:** Deve estar configurada
  - **Stripe Webhook Secret:** Deve estar configurado
  - **Locations:** Verifique se está usando a região correta

**Configurações importantes:**
```
Stripe API Key: sk_live_... ou sk_test_... (conforme ambiente)
Stripe Webhook Secret: whsec_... (do webhook configurado)
```

---

### 3. Export User Data
**Status:** Instalada

**Verificar configuração:**
- Vá para: Extensions > Export User Data > Configurar
- Verifique se o Storage bucket está configurado
- Verifique se as permissões estão corretas

**Configurações:**
- **Storage bucket:** `task-go-ee85f.appspot.com` (ou o bucket do seu projeto)
- **Collection path:** `users` (ou a coleção que armazena dados do usuário)

---

### 4. Stream Firestore to BigQuery
**Status:** Processamento concluído

**Ação necessária:**
- Verifique se os dados estão sendo exportados corretamente
- Vá para: BigQuery Console para verificar as tabelas
- Verifique se há erros nos logs

---

### 5. Delete User Data
**Status:** Instalada (com botão "Gerenciar")

**Verificar configuração:**
- Clique em "Gerenciar"
- Verifique se está configurado para excluir dados de todas as coleções necessárias
- Verifique permissões e triggers

---

## 📝 Checklist de Configuração

### Trigger Email (Prioridade ALTA)
- [ ] Desinstalar extensão com erro
- [ ] Verificar região do Firestore no console
- [ ] Reinstalar extensão com região correta
- [ ] Configurar SMTP (Gmail, SendGrid, etc.)
- [ ] Testar envio de email (criar documento na coleção `mail`)

### Stripe Payments
- [ ] Verificar Stripe API Key está configurada
- [ ] Verificar Webhook Secret está configurado
- [ ] Testar pagamento de teste

### Export User Data
- [ ] Verificar Storage bucket configurado
- [ ] Verificar permissões de acesso
- [ ] Testar exportação de dados

### BigQuery Export
- [ ] Verificar tabelas no BigQuery
- [ ] Verificar se dados estão sendo exportados
- [ ] Verificar logs para erros

### Delete User Data
- [ ] Verificar configuração de coleções
- [ ] Testar exclusão de usuário (teste)

---

## 🔍 Como Verificar a Região do Firestore

### Método 1: Console Firebase
1. Vá para: Firebase Console > Firestore Database
2. Clique em "Settings" (Configurações) no topo
3. Procure por "Location" ou "Region"
4. Anote a região (ex: `nam5`, `us-central1`, etc.)

### Método 2: Firebase CLI
```bash
firebase firestore:databases:list
```

### Método 3: Google Cloud Console
1. Vá para: https://console.cloud.google.com/firestore/databases
2. Selecione seu projeto
3. Veja a coluna "Location" para cada banco de dados

---

## 🚨 Solução Alternativa: Usar Serviço de Email Externo

Se a extensão Trigger Email continuar com problemas, você pode usar um serviço de email externo diretamente nas Cloud Functions:

### Opção A: SendGrid
1. Criar conta no SendGrid
2. Obter API Key
3. Instalar SDK: `npm install @sendgrid/mail`
4. Atualizar função `sendVerificationEmail` em `twoFactorAuth.ts`

### Opção B: Nodemailer
1. Instalar: `npm install nodemailer`
2. Configurar serviço SMTP
3. Atualizar função `sendVerificationEmail`

### Opção C: Mailgun
1. Criar conta no Mailgun
2. Obter API Key
3. Instalar SDK: `npm install mailgun-js`
4. Atualizar função `sendVerificationEmail`

**Exemplo com SendGrid:**
```typescript
import * as sgMail from '@sendgrid/mail';

sgMail.setApiKey(process.env.SENDGRID_API_KEY || '');

async function sendVerificationEmail(email: string, code: string): Promise<void> {
  const msg = {
    to: email,
    from: 'noreply@taskgo.app',
    subject: 'Código de Verificação - TaskGo',
    html: `...`, // HTML do email
    text: `...`, // Texto do email
  };
  
  await sgMail.send(msg);
}
```

---

## ✅ Próximos Passos

1. **Imediato:** Corrigir Trigger Email (desinstalar e reinstalar com região correta)
2. **Curto prazo:** Verificar e configurar todas as extensões
3. **Testes:** Testar envio de email 2FA após correção
4. **Monitoramento:** Verificar logs das extensões regularmente

---

## 📞 Suporte

Se continuar com problemas:
- Verifique logs: `firebase functions:log`
- Verifique logs da extensão no Firebase Console
- Consulte documentação: https://firebase.google.com/docs/extensions










