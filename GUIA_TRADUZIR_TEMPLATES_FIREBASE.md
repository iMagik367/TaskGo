# Guia para Traduzir Templates Firebase para Português

## 📋 Templates que Precisam ser Traduzidos

### 1. Verificação de Endereço de E-mail
**Localização:** Firebase Console > Authentication > Templates > Email address verification

**Assunto (Subject):**
```
Verifique seu email para %APP_NAME%
```

**Mensagem (Message):**
```
Olá %DISPLAY_NAME%,

Siga este link para verificar seu endereço de email:
%LINK%

Se você não solicitou verificar este endereço, pode ignorar este email.

Obrigado,
Equipe %APP_NAME%
```

---

### 2. Redefinição de Senha
**Localização:** Firebase Console > Authentication > Templates > Password reset

**Assunto (Subject):**
```
Redefina sua senha para %APP_NAME%
```

**Mensagem (Message):**
```
Olá,

Siga este link para redefinir sua senha do %APP_NAME% para sua conta %EMAIL%:
%LINK%

Se você não solicitou redefinir sua senha, pode ignorar este email.

Obrigado,
Equipe %APP_NAME%
```

---

### 3. Alteração de Endereço de E-mail
**Localização:** Firebase Console > Authentication > Templates > Email address change

**Assunto (Subject):**
```
Seu email de login foi alterado para %APP_NAME%
```

**Mensagem (Message):**
```
Olá %DISPLAY_NAME%,

Seu email de login para %APP_NAME% foi alterado para %NEW_EMAIL%.

Se você não solicitou alterar seu email, siga este link para redefinir seu email de login:
%LINK%

Obrigado,
Equipe %APP_NAME%
```

---

### 4. Notificação de Registro da Autenticação (2FA)
**Localização:** Firebase Console > Authentication > Templates > Authentication registration notification

**Assunto (Subject):**
```
Você adicionou verificação de duas etapas à sua conta %APP_NAME%
```

**Mensagem (Message):**
```
Olá %DISPLAY_NAME%,

Sua conta no %APP_NAME% foi atualizada com %SECOND_FACTOR% para verificação de duas etapas.

Se você não adicionou esta verificação de duas etapas, clique no link abaixo para removê-la:
%LINK%

Obrigado,
Equipe %APP_NAME%
```

---

### 5. Verificação por SMS
**Localização:** Firebase Console > Authentication > Templates > SMS Verification

**Mensagem (Message):**
```
%LOGIN_CODE% é seu código de verificação para %APP_NAME%
```

---

## 🔧 Passos para Traduzir

1. **Acesse o Firebase Console:**
   - Vá para: https://console.firebase.google.com/
   - Selecione o projeto: `task-go-ee85f`

2. **Navegue até Authentication:**
   - Menu lateral > Authentication
   - Aba "Templates"

3. **Para cada template:**
   - Clique no template desejado (ex: "Verificação de endereço de e-mail")
   - Clique no ícone de edição (lápis) ao lado de "Idioma do modelo"
   - Selecione "Português (Brasil)" ou "Português"
   - **OU** mantenha em "inglês" e edite manualmente o assunto e mensagem usando os textos acima

4. **Editar Assunto:**
   - Clique no ícone de edição ao lado do campo "Assunto"
   - Substitua pelo texto em português fornecido acima

5. **Editar Mensagem:**
   - Clique no ícone de edição ao lado do campo "Mensagem"
   - Substitua pelo texto em português fornecido acima
   - **IMPORTANTE:** Mantenha os placeholders como estão:
     - `%APP_NAME%` - Nome do app
     - `%DISPLAY_NAME%` - Nome do usuário
     - `%EMAIL%` - Email do usuário
     - `%NEW_EMAIL%` - Novo email
     - `%LINK%` - Link de ação
     - `%LOGIN_CODE%` - Código de verificação
     - `%SECOND_FACTOR%` - Método de 2FA

6. **Ativar Templates:**
   - Certifique-se de que o switch "Enviar email" está ATIVADO
   - Para SMS, certifique-se de que está configurado e ativo

7. **Salvar:**
   - Clique em "Salvar" após cada edição

---

## ✅ Verificação

Após traduzir todos os templates:

1. **Teste de Email de Verificação:**
   - Crie uma conta de teste
   - Verifique se o email chega em português

2. **Teste de Redefinição de Senha:**
   - Solicite redefinição de senha
   - Verifique se o email chega em português

3. **Teste de 2FA:**
   - Ative 2FA em uma conta
   - Solicite código de verificação
   - Verifique se o email/código chega em português

---

## 📝 Notas Importantes

- Os templates do Firebase Auth são enviados automaticamente quando as ações correspondentes são acionadas
- A extensão "Trigger Email from Firestore" é usada para emails customizados (como códigos 2FA)
- Para SMS, é necessário ter configurado um provedor de SMS (Twilio, etc.) no Firebase
- Os placeholders (%APP_NAME%, etc.) são substituídos automaticamente pelo Firebase










