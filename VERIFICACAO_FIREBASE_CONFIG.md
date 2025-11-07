# ✅ Verificação Completa das Configurações do Firebase

## 📋 Informações do App

- **Package Name:** `com.taskgoapp.taskgo`
- **App Name no Firebase:** `TaskGo`
- **Token de Debug App Check:** `A1512298-3EBF-4FF9-B1F3-D777060E3BC3`
- **SHA-1:** `87:d7:77:5d:c6:21:9c:3a:6d:f7:b6:2e:02:49:05:1b:05:8a:f2:18`
- **SHA-256:** `465aTqmr9mjfSWYUMssSppD5y6ecDCBY3cQE5YngJXZhKvViWVK7446RPyBZRCE6pQKuT1bdwjRx5LAsfknBxL8YTrr97Hf`
- **Project ID:** `task-go-ee85f`
- **Project Number:** `1093466748007`
- **API Key Atual:** `AIzaSyANaNKqRi8IZa9QvT9oCkTuSOzWMjrOov8`

---

## 🔍 CHECKLIST DE VERIFICAÇÃO

### 1. ✅ Verificar App Android Registrado no Firebase

#### Passo 1: Acessar Firebase Console
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/settings/general
2. Vá em **Project settings** (ícone de engrenagem ⚙️)
3. Clique na aba **Your apps**
4. Procure pelo app Android com package name: `com.taskgoapp.taskgo`

#### Passo 2: Verificar Package Name
- ✅ O package name deve ser exatamente: `com.taskgoapp.taskgo`
- ✅ Se não encontrar, você precisa adicionar o app:
  1. Clique em **Add app** > **Android**
  2. Cole o package name: `com.taskgoapp.taskgo`
  3. Clique em **Register app**
  4. Baixe o novo `google-services.json`
  5. Substitua o arquivo `app/google-services.json` no projeto

#### Passo 3: Verificar SHA-1 e SHA-256
1. Na página do app Android, role até a seção **SHA certificate fingerprints**
2. Verifique se os seguintes certificados estão adicionados:

**SHA-1:**
```
87:d7:77:5d:c6:21:9c:3a:6d:f7:b6:2e:02:49:05:1b:05:8a:f2:18
```

**SHA-256:**
```
465aTqmr9mjfSWYUMssSppD5y6ecDCBY3cQE5YngJXZhKvViWVK7446RPyBZRCE6pQKuT1bdwjRx5LAsfknBxL8YTrr97Hf
```

3. Se não estiverem, adicione:
   - Clique em **Add fingerprint**
   - Cole o SHA-1 ou SHA-256
   - Clique em **Save**

**⚠️ IMPORTANTE:** Após adicionar SHA-1/SHA-256, você precisa baixar um novo `google-services.json` e substituir no projeto!

---

### 2. ✅ Verificar Firebase App Check

#### Passo 1: Verificar App Check Configurado
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/appcheck
2. Verifique se o app Android `com.taskgoapp.taskgo` está listado
3. Verifique se o provider está configurado:
   - **Debug builds:** Debug token
   - **Release builds:** Play Integrity API

#### Passo 2: Verificar Token de Debug
1. Clique no app Android
2. Clique em **Manage debug tokens**
3. Verifique se o token está na lista:
   ```
   A1512298-3EBF-4FF9-B1F3-D777060E3BC3
   ```
4. Se não estiver, adicione:
   - Clique em **Add debug token**
   - Cole: `A1512298-3EBF-4FF9-B1F3-D777060E3BC3`
   - Clique em **Add**

#### Passo 3: Verificar Play Integrity (para Release)
1. Se você vai fazer build de release, verifique se Play Integrity está configurado
2. No Firebase Console, vá em **App Check** > seu app
3. Verifique se **Play Integrity API** está ativado
4. Se não estiver, ative seguindo o guia do Firebase

---

### 3. ✅ Verificar API Key no Google Cloud Console

#### Passo 1: Acessar Google Cloud Console
1. Acesse: https://console.cloud.google.com/apis/credentials?project=605187481719
2. Procure pela API Key: `AIzaSyANaNKqRi8IZa9QvT9oCkTuSOzWMjrOov8`

#### Passo 2: Verificar Restrições da API Key
1. Clique na API Key para editar
2. Verifique **API restrictions:**
   - Se estiver com **"Don't restrict key"**: ✅ OK (mas não recomendado para produção)
   - Se estiver com **"Restrict key"**: Verifique se as seguintes APIs estão na lista:
     - ✅ Firebase Installations API
     - ✅ Firebase App Check API
     - ✅ Identity Toolkit API (Firebase Authentication)
     - ✅ Cloud Firestore API
     - ✅ Cloud Storage API (se usar Storage)
     - ✅ Cloud Functions API (se usar Functions)
     - ✅ Cloud Messaging API (se usar Notificações)

3. Verifique **Application restrictions:**
   - Se estiver com **"None"**: ✅ OK para teste
   - Se estiver com **"Android apps"**: Verifique se o package name e SHA-1 estão corretos:
     - Package name: `com.taskgoapp.taskgo`
     - SHA-1: `87:d7:77:5d:c6:21:9c:3a:6d:f7:b6:2e:02:49:05:1b:05:8a:f2:18`

#### Passo 3: Verificar se APIs Estão Habilitadas
1. Acesse: https://console.cloud.google.com/apis/dashboard?project=605187481719
2. Verifique se as seguintes APIs estão **habilitadas**:
   - ✅ Firebase Installations API
   - ✅ Firebase App Check API
   - ✅ Identity Toolkit API
   - ✅ Cloud Firestore API
   - ✅ Cloud Storage API
   - ✅ Cloud Functions API
   - ✅ Cloud Messaging API

---

### 4. ✅ Verificar google-services.json

#### Passo 1: Verificar Arquivo Local
1. Abra: `app/google-services.json`
2. Verifique se contém:
   - Package name: `com.taskgoapp.taskgo`
   - API Key: `AIzaSyANaNKqRi8IZa9QvT9oCkTuSOzWMjrOov8`
   - Project ID: `task-go-ee85f`
   - Project Number: `1093466748007`

#### Passo 2: Baixar Novo google-services.json
**⚠️ IMPORTANTE:** Após adicionar SHA-1/SHA-256 no Firebase Console, você DEVE baixar um novo `google-services.json`!

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/settings/general
2. Vá em **Your apps**
3. Clique no app Android
4. Clique em **Download google-services.json**
5. Substitua o arquivo `app/google-services.json` no projeto
6. Faça rebuild do app

---

### 5. ✅ Verificar Firebase Authentication

#### Passo 1: Verificar Métodos de Login Habilitados
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/authentication/providers
2. Verifique se os seguintes métodos estão **habilitados**:
   - ✅ Email/Password
   - ✅ Google (se usar Google Sign-In)

#### Passo 2: Verificar Domínios Autorizados
1. Vá em **Authentication** > **Settings** > **Authorized domains**
2. Verifique se os domínios necessários estão listados

---

### 6. ✅ Verificar Firestore Database

#### Passo 1: Verificar Firestore Criado
1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore
2. Verifique se o Firestore está criado
3. Se não estiver, clique em **Create database**

#### Passo 2: Verificar Regras do Firestore
1. Vá em **Rules**
2. Verifique se as regras permitem leitura/escrita (pelo menos para desenvolvimento)
3. Para desenvolvimento, você pode usar temporariamente:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 🔑 COMO OBTER UMA NOVA API KEY (SE NECESSÁRIO)

### Opção 1: Criar Nova API Key no Google Cloud Console

1. **Acesse Google Cloud Console:**
   - https://console.cloud.google.com/apis/credentials?project=605187481719

2. **Clique em "Create Credentials" > "API Key"**

3. **Configure a Nova API Key:**
   - **Name:** `TaskGo Firebase API Key` (ou outro nome)
   - **API restrictions:** Selecione "Restrict key" e adicione:
     - Firebase Installations API
     - Firebase App Check API
     - Identity Toolkit API
     - Cloud Firestore API
     - Cloud Storage API
     - Cloud Functions API
     - Cloud Messaging API
   - **Application restrictions:** Para desenvolvimento, use "None". Para produção, use "Android apps" e adicione:
     - Package name: `com.taskgoapp.taskgo`
     - SHA-1: `87:d7:77:5d:c6:21:9c:3a:6d:f7:b6:2e:02:49:05:1b:05:8a:f2:18`

4. **Copie a Nova API Key**

5. **Atualizar no Firebase:**
   - **⚠️ IMPORTANTE:** A API Key no `google-services.json` é gerenciada automaticamente pelo Firebase
   - Você NÃO precisa editar o `google-services.json` manualmente
   - A API Key será atualizada automaticamente quando você fizer alterações no Firebase Console

6. **Verificar se a Nova API Key Está Sendo Usada:**
   - Baixe um novo `google-services.json` do Firebase Console
   - Verifique se a API Key no arquivo foi atualizada
   - Se não foi, pode levar alguns minutos para propagar

### Opção 2: Usar a API Key do google-services.json

**A API Key no `google-services.json` é a correta para usar!** Não é necessário criar uma nova, a menos que você tenha problemas específicos com a atual.

---

## 🔧 PROBLEMAS COMUNS E SOLUÇÕES

### Problema 1: App Não Aparece no Firebase Console
**Solução:**
- Adicione o app Android no Firebase Console
- Use o package name exatamente: `com.taskgoapp.taskgo`
- Baixe o novo `google-services.json`

### Problema 2: SHA-1/SHA-256 Não Estão Configurados
**Solução:**
- Adicione SHA-1 e SHA-256 no Firebase Console
- **BAIXE um novo `google-services.json`** após adicionar
- Faça rebuild do app

### Problema 3: Token de Debug Não Funciona
**Solução:**
- Verifique se o token está adicionado no Firebase Console
- Verifique se o token está correto: `A1512298-3EBF-4FF9-B1F3-D777060E3BC3`
- Aguarde alguns minutos após adicionar o token
- Reinicie o app completamente

### Problema 4: API Key Bloqueada
**Solução:**
- Verifique as restrições da API Key no Google Cloud Console
- Adicione todas as APIs necessárias na lista de restrições
- Ou temporariamente remova as restrições para teste
- Verifique se o SHA-1 está correto nas restrições de aplicação

### Problema 5: Erro de Autenticação
**Solução:**
- Verifique se Firebase Authentication está habilitado
- Verifique se Email/Password está habilitado
- Verifique se o reCAPTCHA está configurado (pode estar bloqueando)

---

## 📋 CHECKLIST FINAL

Antes de testar o app, verifique:

- [ ] App Android registrado no Firebase com package name `com.taskgoapp.taskgo`
- [ ] SHA-1 adicionado: `87:d7:77:5d:c6:21:9c:3a:6d:f7:b6:2e:02:49:05:1b:05:8a:f2:18`
- [ ] SHA-256 adicionado: `465aTqmr9mjfSWYUMssSppD5y6ecDCBY3cQE5YngJXZhKvViWVK7446RPyBZRCE6pQKuT1bdwjRx5LAsfknBxL8YTrr97Hf`
- [ ] Novo `google-services.json` baixado após adicionar SHA-1/SHA-256
- [ ] Token de debug adicionado: `A1512298-3EBF-4FF9-B1F3-D777060E3BC3`
- [ ] API Key verificada e sem restrições bloqueantes
- [ ] Todas as APIs do Firebase habilitadas no Google Cloud Console
- [ ] Firebase Authentication habilitado com Email/Password
- [ ] Firestore criado e com regras configuradas
- [ ] App rebuild após atualizar `google-services.json`

---

## 🔗 LINKS ÚTEIS

- **Firebase Console:** https://console.firebase.google.com/project/task-go-ee85f
- **Google Cloud Console:** https://console.cloud.google.com/?project=605187481719
- **API Credentials:** https://console.cloud.google.com/apis/credentials?project=605187481719
- **App Check:** https://console.firebase.google.com/project/task-go-ee85f/appcheck
- **Authentication:** https://console.firebase.google.com/project/task-go-ee85f/authentication
- **Firestore:** https://console.firebase.google.com/project/task-go-ee85f/firestore

---

**Última atualização:** 2025-11-07

