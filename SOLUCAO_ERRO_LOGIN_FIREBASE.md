# 🔧 Solução: Erro de Login no Firebase

## 📋 Problema Identificado

Os logs mostram que o login está falhando devido a dois problemas principais:

### 1. ❌ Firebase App Check API Não Habilitada
**Erro nos logs:**
```
Firebase App Check API has not been used in project 605187481719 before or it is disabled.
Enable it by visiting https://console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=605187481719
```

### 2. ❌ Firebase Installations API Não Habilitada
**Erro nos logs:**
```
Firebase Installations API has not been used in project 605187481719 before or it is disabled.
Enable it by visiting https://console.developers.google.com/apis/api/firebaseinstallations.googleapis.com/overview?project=605187481719
```

### 3. ❌ Erro de Rede no Login
**Erro final:**
```
FirebaseNetworkException: A network error (such as timeout, interrupted connection or unreachable host) has occurred.
```

Este erro ocorre porque o reCAPTCHA do Firebase Auth não consegue se comunicar corretamente quando o App Check não está funcionando.

---

## ✅ SOLUÇÃO PASSO A PASSO

### **Passo 1: Habilitar Firebase App Check API**

1. **Abra este link no navegador:**
   ```
   https://console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=605187481719
   ```

2. **Clique no botão "ENABLE" (Habilitar)**

3. **Aguarde a confirmação** (pode levar alguns segundos)

### **Passo 2: Habilitar Firebase Installations API**

1. **Abra este link no navegador:**
   ```
   https://console.developers.google.com/apis/api/firebaseinstallations.googleapis.com/overview?project=605187481719
   ```

2. **Clique no botão "ENABLE" (Habilitar)**

3. **Aguarde a confirmação** (pode levar alguns segundos)

### **Passo 3: Configurar Token de Debug do App Check**

1. **Acesse o Firebase Console:**
   ```
   https://console.firebase.google.com/project/task-go-ee85f/appcheck
   ```

2. **Clique em "Manage debug tokens"** (Gerenciar tokens de debug)

3. **Adicione o token identificado nos logs:**
   ```
   d863e2c2-ce5b-4109-b7d5-e1db6a1dceae
   ```

4. **Clique em "Add" (Adicionar)**

   **Nota:** Se você não vir esta opção, primeiro você precisa:
   - Ir em **App Check** > **Apps**
   - Selecionar seu app Android
   - Configurar o provider (Debug ou Play Integrity)

### **Passo 4: Aguardar Propagação**

⚠️ **IMPORTANTE:** Após habilitar as APIs, aguarde **5-10 minutos** para que as mudanças sejam propagadas nos sistemas do Google.

### **Passo 5: Testar Novamente**

1. **Feche completamente o app** (force stop)
2. **Abra o app novamente**
3. **Tente fazer login**

---

## 🔍 Verificação

Após seguir os passos acima, você deve ver nos logs:

✅ **Sem erros de "API has not been used"**  
✅ **Token de App Check obtido com sucesso**  
✅ **Login funcionando corretamente**

Exemplo de log esperado:
```
TaskGoApp: ✅ App Check Debug Token obtido: d863e2c2-ce5b-4109-b7d5-e1db6a1dceae
FirebaseAuthRepository: Login bem-sucedido: [user-id]
```

---

## 📝 Notas Importantes

### Sobre o Projeto ID
- O projeto ID usado nos links é `605187481719`
- Se este não for o ID correto do seu projeto, você precisa:
  1. Acessar o Firebase Console
  2. Verificar o ID do projeto nas configurações
  3. Substituir `605187481719` pelo ID correto nos links

### Permissões Necessárias
- Você precisa ter permissões de **Administrador** ou **Editor** no projeto do Google Cloud para habilitar APIs
- Se não tiver permissões, peça ao administrador do projeto para habilitar

### Billing
- Habilitar essas APIs **NÃO gera custos adicionais**
- Elas são APIs básicas do Firebase e são gratuitas
- No entanto, verifique se seu projeto tem billing habilitado caso seja necessário

---

## 🚨 Alternativa Temporária (Apenas para Testes)

Se você precisar testar o login **imediatamente** sem habilitar as APIs, pode temporariamente desabilitar o App Check:

⚠️ **ATENÇÃO:** Isso deve ser usado **APENAS para desenvolvimento**. Nunca use em produção!

1. Comente a inicialização do App Check no `TaskGoApp.kt`:
```kotlin
// Initialize Firebase App Check
// Temporariamente desabilitado para testes
/*
try {
    val appCheck = FirebaseAppCheck.getInstance()
    // ... resto do código
} catch (e: Exception) {
    // ...
}
*/
```

2. **Reconstrua o app:**
   ```bash
   ./gradlew clean assembleDebug
   ```

3. **Teste o login**

4. **Reabilite o App Check** assim que habilitar as APIs no Google Cloud Console

---

## 📞 Precisa de Ajuda?

Se após seguir todos os passos o problema persistir:

1. **Verifique os logs novamente** após aguardar 10 minutos
2. **Confirme que o projeto ID está correto**
3. **Verifique se você tem as permissões necessárias**
4. **Tente fazer logout e login novamente no Firebase Console**

---

## ✅ Checklist Final

- [ ] Firebase App Check API habilitada
- [ ] Firebase Installations API habilitada
- [ ] Token de debug adicionado no Firebase Console
- [ ] Aguardado 5-10 minutos para propagação
- [ ] App reiniciado completamente
- [ ] Login testado com sucesso

---

**Última atualização:** 2025-11-06

