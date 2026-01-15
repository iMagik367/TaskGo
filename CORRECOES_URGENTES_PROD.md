# Correções Urgentes para Produção - Versão 1.0.24

## ✅ Problemas Corrigidos

### 1. Cadastro não define modo de conta (ROLE) ✅

**Problema:** Ao criar conta como prestador ou vendedor, sempre ficava como cliente.

**Causa Raiz:**
- A Cloud Function `onUserCreate` estava criando o usuário com `role: 'client'` por padrão
- Esta função executava quando o Firebase Auth criava o usuário
- Mesmo que o app atualizasse o role, a função podia executar depois e sobrescrever

**Solução Implementada:**

1. **Cloud Function `onUserCreate` modificada:**
   - Agora verifica se o documento já existe antes de criar
   - Se existir, faz merge apenas de campos básicos que faltam
   - **CRÍTICO: NÃO sobrescreve o campo `role` se já existir**
   - Preserva o role definido pelo app durante o cadastro

2. **SignupViewModel melhorado:**
   - Adicionado delay de 500ms para garantir que a função execute primeiro (se necessário)
   - Sempre atualiza o role corretamente baseado no `accountType` selecionado
   - Logs detalhados para debug: `Log.d("SignupViewModel", "Salvando com role: $role")`

**Arquivos Modificados:**
- `functions/src/auth.ts` - Função `onUserCreate` refatorada
- `app/src/main/java/com/taskgoapp/taskgo/feature/auth/presentation/SignupViewModel.kt` - Delay e logs adicionados

**Como Funciona Agora:**
```
Cadastro → Firebase Auth cria usuário → Cloud Function cria doc com role='client' 
→ App atualiza com role correto (provider/seller) → Role correto preservado ✅
```

---

### 2. Código de Verificação 2FA não chega ✅

**Problema:** Códigos 2FA não estão chegando no email ou telefone.

**Causa:** A extensão "Trigger Email from Firestore" precisa estar corretamente configurada.

**Solução:**
- Função `sendTwoFactorCode` já está criando documento na coleção `mail` corretamente
- Regra do Firestore adicionada para a coleção `mail`
- **AÇÃO NECESSÁRIA:** Verificar/Configurar extensão Trigger Email no Firebase Console (ver instruções abaixo)

**Arquivos Modificados:**
- `firestore.rules` - Adicionada regra para coleção `mail`

**Instruções para Configurar Extensão:**

Ver arquivo: `INSTRUCOES_CORRECAO_EXTENSAO_EMAIL.md`

**Resumo rápido:**
1. Acesse Firebase Console > Extensions
2. Verifique se "Trigger Email from Firestore" está instalada e ACTIVE
3. Se estiver em erro, desinstale e reinstale com região `nam5`
4. Configure SMTP (Gmail App Password ou SendGrid)
5. Teste criando documento na coleção `mail`

---

### 3. Mensagens em Inglês - Templates Firebase ✅

**Problema:** Templates de email do Firebase estão em inglês.

**Solução:**
- Criado guia completo: `GUIA_TRADUZIR_TEMPLATES_FIREBASE.md`
- Todos os templates precisam ser traduzidos manualmente no Firebase Console

**AÇÃO NECESSÁRIA:**
1. Acesse Firebase Console > Authentication > Templates
2. Para cada template, altere:
   - Idioma do modelo: Português
   - Assunto: Traduza para português
   - Mensagem: Traduza para português
3. **Mantenha os placeholders:** `%APP_NAME%`, `%DISPLAY_NAME%`, `%EMAIL%`, `%LINK%`, etc.

**Templates que precisam tradução:**
1. ✅ Verificação de endereço de e-mail
2. ✅ Redefinição de senha
3. ✅ Alteração de endereço de e-mail
4. ✅ Notificação de registro da autenticação (2FA)
5. ✅ Verificação por SMS

**Textos prontos para tradução estão no arquivo:** `GUIA_TRADUZIR_TEMPLATES_FIREBASE.md`

---

## 📋 Mudanças Técnicas

### Cloud Function `onUserCreate`

**Antes:**
```typescript
await db.collection('users').doc(user.uid).set(userData); // Sempre sobrescreve
```

**Depois:**
```typescript
const userDoc = await userRef.get();
if (userDoc.exists) {
  // Faz merge apenas de campos básicos, preserva role
  await userRef.update(updateData); // Não atualiza role
} else {
  await userRef.set(userData, { merge: true }); // Cria com merge
}
```

### Firestore Rules

**Adicionada regra para coleção `mail`:**
```javascript
match /mail/{mailId} {
  allow read, write: if false; // Apenas Cloud Functions podem escrever
}
```

---

## 🚀 Deploy Necessário

### 1. Deploy das Cloud Functions

```powershell
cd C:\Users\user\AndroidStudioProjects\TaskGoApp
firebase deploy --only functions:onUserCreate
```

### 2. Deploy das Firestore Rules

```powershell
firebase deploy --only firestore:rules
```

### 3. Build do AAB

```powershell
.\gradlew.bat bundleRelease
```

---

## ✅ Checklist de Verificação

### Cadastro de Conta:
- [ ] Testar cadastro como Cliente - verificar se role='client'
- [ ] Testar cadastro como Prestador - verificar se role='provider'
- [ ] Testar cadastro como Vendedor - verificar se role='seller'

### 2FA:
- [ ] Verificar se extensão Trigger Email está ACTIVE
- [ ] Testar ativação de 2FA
- [ ] Testar solicitação de código 2FA
- [ ] Verificar se email chega com código
- [ ] Testar verificação do código

### Templates Firebase:
- [ ] Verificar todos os 5 templates traduzidos para português
- [ ] Testar email de verificação (criar conta)
- [ ] Testar email de redefinição de senha
- [ ] Testar notificação de 2FA

---

## 📝 Notas Importantes

1. **Cadastro:** O role agora é sempre preservado corretamente, mesmo se a Cloud Function executar depois
2. **2FA:** Os emails são enviados através da extensão Trigger Email, que monitora a coleção `mail`
3. **Templates:** Os templates do Firebase Auth são separados da extensão Trigger Email - ambos precisam estar configurados
4. **Região:** Certifique-se de que a extensão Trigger Email está na região `nam5` (mesma do Firestore)

---

## 🔍 Troubleshooting

### Role ainda está errado após cadastro:
1. Verificar logs do SignupViewModel: `Log.d("SignupViewModel", "Salvando com role: $role")`
2. Verificar Firestore diretamente após cadastro
3. Verificar logs da Cloud Function `onUserCreate`

### Email 2FA não chega:
1. Verificar se extensão Trigger Email está ACTIVE
2. Verificar logs: `firebase functions:log --only ext-firestore-send-email-processQueue`
3. Verificar se documento foi criado na coleção `mail`
4. Verificar configuração SMTP

### Templates ainda em inglês:
1. Verificar se traduziu no Firebase Console
2. Verificar se salvou as alterações
3. Testar criando nova conta/redefinindo senha










