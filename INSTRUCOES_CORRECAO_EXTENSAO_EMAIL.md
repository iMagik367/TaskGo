# Instruções para Corrigir Extensão Trigger Email

## ✅ Região do Firestore Identificada
**Região correta:** `nam5` (US multi-region)

O erro ocorreu porque a extensão estava tentando usar a região `eur3`, mas seu Firestore está em `nam5`.

---

## 🔧 Como Corrigir

### Opção 1: Via Firebase Console (Recomendado)

1. **Acesse o Firebase Console:**
   - Vá para: https://console.firebase.google.com/project/task-go-ee85f/extensions

2. **Desinstalar a extensão com erro:**
   - Clique na extensão "Trigger Email from Firestore"
   - Clique em "Mais detalhes" no erro
   - Clique no botão "Desinstalar" (vermelho)
   - Confirme a desinstalação

3. **Reinstalar a extensão:**
   - Clique em "Browse Catalog" ou procure "Trigger Email"
   - Clique em "Install" na extensão "Trigger Email"
   - Configure os seguintes parâmetros:

   **Configurações obrigatórias:**
   ```
   Instance ID: firestore-send-email (ou deixe o padrão)
   
   Collection path: mail
   
   Location: nam5 ⚠️ IMPORTANTE: Use nam5 (mesma região do Firestore)
   
   SMTP connection URI: [Configure conforme instruções abaixo]
   ```

   **Para configurar SMTP:**

   **Opção A - Gmail:**
   ```
   SMTP connection URI: smtps://smtp.gmail.com:465
   SMTP username: seu-email@gmail.com
   SMTP password: [Use App Password do Gmail - veja abaixo]
   ```

   **Como criar App Password no Gmail:**
   1. Acesse: https://myaccount.google.com/security
   2. Ative "Verificação em duas etapas" (se não estiver ativa)
   3. Acesse: https://myaccount.google.com/apppasswords
   4. Selecione "Mail" e "Other (Custom name)"
   5. Digite: "TaskGo Firebase"
   6. Clique em "Generate"
   7. Copie a senha gerada (16 caracteres, sem espaços)
   8. Use essa senha no campo "SMTP password"

   **Opção B - SendGrid (Recomendado para produção):**
   ```
   SMTP connection URI: smtps://smtp.sendgrid.net:465
   SMTP username: apikey
   SMTP password: [Sua API Key do SendGrid]
   ```

4. **Finalizar instalação:**
   - Revise todas as configurações
   - Clique em "Install" ou "Install extension"
   - Aguarde a instalação completar (pode levar alguns minutos)

---

### Opção 2: Via Firebase CLI

Se preferir usar CLI, execute os seguintes comandos:

```powershell
# 1. Desinstalar (pode dar erro se não estiver no firebase.json, mas tente mesmo assim)
firebase ext:uninstall firestore-send-email --force

# 2. Aguardar alguns segundos
Start-Sleep -Seconds 5

# 3. Reinstalar com configuração correta
# IMPORTANTE: Ajuste os valores entre < > antes de executar

firebase ext:install firebase/firestore-send-email `
  --instance-id=firestore-send-email `
  --params=location=nam5 `
  --params=SMTP_CONNECTION_URI="smtps://smtp.gmail.com:465" `
  --params=SMTP_USERNAME="<seu-email@gmail.com>" `
  --params=SMTP_PASSWORD="<sua-app-password>"
```

**Nota:** Se usar SendGrid, ajuste:
```powershell
firebase ext:install firebase/firestore-send-email `
  --instance-id=firestore-send-email `
  --params=location=nam5 `
  --params=SMTP_CONNECTION_URI="smtps://smtp.sendgrid.net:465" `
  --params=SMTP_USERNAME="apikey" `
  --params=SMTP_PASSWORD="<sua-sendgrid-api-key>"
```

---

## ✅ Verificar Instalação

Após a reinstalação:

1. **Verificar status:**
   ```powershell
   firebase ext:list
   ```
   Deve mostrar `ACTIVE` para `firestore-send-email`

2. **Testar envio de email:**
   - Vá para: Firestore Database > Coleção `mail`
   - Adicione um documento de teste:
   ```json
   {
     "to": "seu-email@teste.com",
     "message": {
       "subject": "Teste",
       "text": "Email de teste",
       "html": "<p>Email de teste</p>"
     }
   }
   ```
   - O email deve ser enviado automaticamente

3. **Verificar logs:**
   ```powershell
   firebase functions:log --only ext-firestore-send-email-processQueue
   ```

---

## 🔍 Outras Extensões (Print 2)

### 1. Run Payments with Stripe ✅
**Status:** ACTIVE

**Verificar configuração:**
- Vá para: Extensions > Run Payments with Stripe > Configurar
- Verifique se as chaves do Stripe estão configuradas:
  - Stripe API Key (sk_live_... ou sk_test_...)
  - Stripe Webhook Secret (whsec_...)
  - Location: Provavelmente `us-central1` (padrão das functions)

### 2. Export User Data ✅
**Status:** ACTIVE

**Verificar configuração:**
- Vá para: Extensions > Export User Data > Configurar
- Verifique Storage bucket: `task-go-ee85f.appspot.com`
- Verifique Collection path: `users`

### 3. Stream Firestore to BigQuery ✅
**Status:** ACTIVE (Processamento concluído)

**Verificar:**
- Vá para: BigQuery Console
- Verifique se os dados estão sendo exportados
- Verifique logs para erros

### 4. Delete User Data ✅
**Status:** ACTIVE

**Verificar configuração:**
- Vá para: Extensions > Delete User Data > Gerenciar
- Verifique se está configurado para excluir dados de todas as coleções necessárias

---

## 📋 Checklist Final

- [ ] Desinstalar extensão Trigger Email com erro
- [ ] Reinstalar Trigger Email com região `nam5`
- [ ] Configurar SMTP (Gmail App Password ou SendGrid)
- [ ] Testar envio de email
- [ ] Verificar status da extensão (deve estar ACTIVE)
- [ ] Verificar outras extensões (Stripe, Export, BigQuery, Delete)
- [ ] Testar 2FA após correção

---

## 🚨 Troubleshooting

### Erro persiste após reinstalação:
1. Aguarde alguns minutos (deployment pode levar tempo)
2. Verifique logs: `firebase functions:log`
3. Verifique IAM permissions do service account
4. Tente novamente após alguns minutos

### Email não está sendo enviado:
1. Verifique se o documento foi criado na coleção `mail`
2. Verifique logs da função: `ext-firestore-send-email-processQueue`
3. Verifique configuração SMTP (senha, usuário, URI)
4. Teste SMTP manualmente (usando cliente de email)

### Problemas com Gmail:
- Use App Password, não a senha normal
- Certifique-se de que "Verificação em duas etapas" está ativa
- Verifique se não há bloqueios de segurança

---

## ✅ Após Correção

Após corrigir a extensão Trigger Email:

1. **Teste o 2FA:**
   - Ative 2FA nas configurações do app
   - Faça login
   - Verifique se recebe o código por email
   - Complete o login com o código

2. **Monitorar logs:**
   - Acompanhe logs das extensões regularmente
   - Verifique se há erros

3. **Considerar alternativas:**
   - Se problemas persistirem, considere usar SendGrid ou Mailgun
   - Mais confiável para produção
   - Melhor deliverability










