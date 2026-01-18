# 🔧 HABILITAR FIRESTORE API NO FIREBASE CONSOLE

## ❌ PROBLEMA

O erro `FAILED_PRECONDITION: Firestore API data access is disabled` indica que a **API do Firestore não está habilitada** para o database 'taskgo' no Firebase Console.

## ✅ SOLUÇÃO

### Passo 1: Acessar Firebase Console

1. Acesse: https://console.firebase.google.com/
2. Selecione o projeto: **task-go-ee85f**

### Passo 2: Habilitar Firestore API

1. No menu lateral, vá em **Firestore Database**
2. Se você ver uma mensagem pedindo para criar um database, **NÃO crie um novo**
3. Verifique se o database **'taskgo'** está listado
4. Se não estiver visível, verifique se você está usando o projeto correto

### Passo 3: Habilitar API no Google Cloud Console

1. Acesse: https://console.cloud.google.com/
2. Selecione o projeto: **task-go-ee85f**
3. No menu lateral, vá em **APIs & Services** > **Library**
4. Procure por **"Cloud Firestore API"**
5. Clique em **Enable** (Habilitar)
6. Aguarde alguns minutos para a API ser habilitada

### Passo 4: Verificar Database 'taskgo'

1. Volte ao Firebase Console
2. Vá em **Firestore Database**
3. No topo, verifique se o database selecionado é **'taskgo'** (não 'default')
4. Se não aparecer 'taskgo', você pode precisar criar o database:
   - Clique em **"Create database"** ou **"Add database"**
   - Selecione **"Start in production mode"** ou **"Start in test mode"** (temporariamente)
   - Escolha a localização (ex: `us-central1`)
   - **IMPORTANTE**: No campo **Database ID**, digite: **taskgo**
   - Clique em **Enable**

### Passo 5: Verificar Regras do Firestore

1. No Firebase Console, vá em **Firestore Database** > **Rules**
2. Verifique se as regras estão deployadas corretamente
3. Se necessário, faça deploy novamente:
   ```bash
   firebase deploy --only firestore:rules
   ```

## 🔍 VERIFICAÇÃO

Após habilitar a API, teste novamente o cadastro/login. O erro `FAILED_PRECONDITION` deve desaparecer.

## ⚠️ NOTA IMPORTANTE

- O database 'taskgo' deve estar **criado e ativo** no Firebase Console
- A **Cloud Firestore API** deve estar **habilitada** no Google Cloud Console
- As **Firestore Rules** devem estar **deployadas** corretamente

## 📝 COMANDOS ÚTEIS

```bash
# Verificar databases disponíveis
firebase firestore:databases:list

# Verificar regras
firebase firestore:rules:get

# Deploy das regras
firebase deploy --only firestore:rules
```
