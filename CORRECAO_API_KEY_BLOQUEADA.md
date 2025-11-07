# 🔴 CORREÇÃO URGENTE: API Key Bloqueada

## ⚠️ PROBLEMA IDENTIFICADO

Os logs mostram que a API Key está **BLOQUEADA** ou com **restrições incorretas**:

```
API_KEY_SERVICE_BLOCKED
Requests to this API firebaseinstallations.googleapis.com are blocked.
Requests to this API firebaseappcheck.googleapis.com are blocked.
```

**API Key detectada nos logs:** `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw`

---

## 🔧 SOLUÇÃO PASSO A PASSO

### Passo 1: Verificar e Habilitar APIs no Google Cloud Console

#### 1.1. Verificar Firebase Installations API

1. Acesse: https://console.developers.google.com/apis/api/firebaseinstallations.googleapis.com/overview?project=605187481719
2. Clique em **"ENABLE"** se não estiver habilitada
3. Aguarde alguns segundos para confirmar

#### 1.2. Verificar Firebase App Check API

1. Acesse: https://console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=605187481719
2. Clique em **"ENABLE"** se não estiver habilitada
3. Aguarde alguns segundos para confirmar

#### 1.3. Verificar Firebase Authentication API

1. Acesse: https://console.developers.google.com/apis/api/identitytoolkit.googleapis.com/overview?project=605187481719
2. Clique em **"ENABLE"** se não estiver habilitada

---

### Passo 2: Verificar e Remover Restrições da API Key

**⚠️ CRÍTICO:** As restrições da API Key podem estar bloqueando as APIs necessárias!

1. **Acesse Google Cloud Console:**
   - https://console.cloud.google.com/apis/credentials?project=605187481719

2. **Encontre a API Key:**
   - Procure por: `AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw`
   - Clique na chave para editar

3. **Verifique "API restrictions":**
   - Se estiver com **"Restrict key"** ativado:
     - Verifique se as seguintes APIs estão na lista:
       - ✅ Firebase Installations API
       - ✅ Firebase App Check API
       - ✅ Firebase Authentication API (Identity Toolkit API)
       - ✅ Firebase Cloud Messaging API (se usar notificações)
       - ✅ Cloud Firestore API
       - ✅ Cloud Storage API (se usar Storage)
     - Se alguma estiver faltando, **adicione** clicando em "Add an API"
     - Ou, **temporariamente**, altere para **"Don't restrict key"** para teste

4. **Verifique "Application restrictions":**
   - Se estiver com **"Android apps"** ou **"iOS apps"**:
     - Verifique se o package name está correto: `com.taskgoapp.taskgo`
     - Verifique se o SHA-1 certificate está correto (se configurado)
   - Ou, **temporariamente**, altere para **"None"** para teste

5. **Salve as alterações:**
   - Clique em **"Save"**
   - Aguarde 1-2 minutos para as mudanças propagarem

---

### Passo 3: Adicionar Token de Debug no Firebase Console

**Token de debug gerado:** `8c4aab63-0f88-4a42-a909-28f25d93a956`

1. **Acesse Firebase Console:**
   - https://console.firebase.google.com/project/task-go-ee85f/appcheck

2. **Vá em "Manage debug tokens":**
   - Clique em **"Apps"** no menu lateral
   - Selecione seu app Android (`com.taskgoapp.taskgo`)
   - Clique em **"Manage debug tokens"**

3. **Adicione o token:**
   - Clique em **"Add debug token"**
   - Cole: `8c4aab63-0f88-4a42-a909-28f25d93a956`
   - Clique em **"Add"**

---

### Passo 4: Verificar Quotas e Billing

1. **Verifique se há billing habilitado:**
   - https://console.cloud.google.com/billing?project=605187481719
   - Algumas APIs do Firebase podem exigir billing habilitado (mesmo que seja free tier)

2. **Verifique quotas:**
   - https://console.cloud.google.com/apis/api/firebaseinstallations.googleapis.com/quotas?project=605187481719
   - Verifique se há limites ou bloqueios

---

### Passo 5: Verificar Permissões do Projeto

1. **Verifique se você tem permissões de administrador:**
   - https://console.cloud.google.com/iam-admin/iam?project=605187481719
   - Você precisa ter permissão de **"Owner"** ou **"Editor"** para habilitar APIs

---

## 🔄 TESTE APÓS CORREÇÕES

Após fazer todas as correções acima:

1. **Aguarde 5-10 minutos** para as mudanças propagarem
2. **Desinstale o app** do dispositivo
3. **Feche completamente o Android Studio**
4. **Reabra o Android Studio**
5. **Faça Clean Build:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```
6. **Instale o app novamente**
7. **Teste o login**

---

## 📋 CHECKLIST RÁPIDO

- [ ] Firebase Installations API habilitada
- [ ] Firebase App Check API habilitada
- [ ] Firebase Authentication API habilitada
- [ ] API Key sem restrições bloqueantes OU com APIs corretas na lista
- [ ] Token de debug adicionado no Firebase Console: `8c4aab63-0f88-4a42-a909-28f25d93a956`
- [ ] Billing habilitado (se necessário)
- [ ] Permissões de administrador verificadas
- [ ] Aguardado 5-10 minutos após mudanças
- [ ] App reinstalado após correções

---

## 🆘 SE AINDA NÃO FUNCIONAR

### Opção 1: Criar Nova API Key (Temporária para Teste)

1. Acesse: https://console.cloud.google.com/apis/credentials?project=605187481719
2. Clique em **"Create Credentials"** > **"API Key"**
3. **NÃO adicione restrições** (temporariamente para teste)
4. Copie a nova chave
5. Substitua no `google-services.json` (localmente, para teste)
6. Teste o app
7. **Se funcionar:** Configure as restrições corretamente na nova chave
8. **Se não funcionar:** O problema não é a API Key, mas sim as APIs não habilitadas

### Opção 2: Verificar Logs Detalhados

Adicione mais logs no app para diagnosticar:

```kotlin
// No TaskGoApp.kt, adicione após inicializar Firebase
val apiKey = FirebaseApp.getInstance().options.apiKey
Log.d(TAG, "API Key sendo usada: $apiKey")
Log.d(TAG, "Project ID: ${FirebaseApp.getInstance().options.projectId}")
Log.d(TAG, "Application ID: ${FirebaseApp.getInstance().options.applicationId}")
```

---

## 📝 NOTAS IMPORTANTES

1. **Propagação:** Mudanças no Google Cloud Console podem levar até 10 minutos para propagar
2. **Cache:** O Firebase pode cachear respostas - desinstale o app completamente antes de testar
3. **Billing:** Algumas APIs do Firebase podem exigir billing habilitado (mesmo free tier)
4. **SHA-1:** Se usar restrições de Android apps, verifique se o SHA-1 está correto

---

## 🔗 LINKS ÚTEIS

- Google Cloud Console: https://console.cloud.google.com/?project=605187481719
- Firebase Console: https://console.firebase.google.com/project/task-go-ee85f
- API Credentials: https://console.cloud.google.com/apis/credentials?project=605187481719
- Firebase Installations API: https://console.developers.google.com/apis/api/firebaseinstallations.googleapis.com/overview?project=605187481719
- Firebase App Check API: https://console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=605187481719


