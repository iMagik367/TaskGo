# Configuração do Database Taskgo (Firestore MongoDB Mode)

## 🔴 SITUAÇÃO ATUAL

- Database 'default' foi **DELETADO**
- Database 'taskgo' está em modo **MongoDB compatibility**
- Connection string MongoDB disponível
- **TODOS** os dados devem ir para 'taskgo' agora

## ✅ CONFIGURAÇÃO NECESSÁRIA

### 1. Firebase Admin SDK (Cloud Functions)

O Firebase Admin SDK **NÃO usa connection strings MongoDB diretamente**. Ele usa:
- **Application Default Credentials (ADC)** do Google Cloud
- Credenciais configuradas automaticamente no ambiente do Cloud Functions

**O código já está configurado corretamente:**
```typescript
// functions/src/utils/firestore.ts
const db = app.firestore('taskgo');
```

**Verificar:**
1. ✅ Cloud Functions têm permissão para acessar o database 'taskgo'
2. ✅ Database 'taskgo' está criado no Firebase Console
3. ✅ Projeto tem acesso Enterprise (para múltiplos databases)

### 2. Android SDK

O Android SDK também **NÃO usa connection strings MongoDB**. Ele usa:
- Credenciais do `google-services.json`
- Configuração automática do Firebase

**O código já está configurado:**
```kotlin
// app/src/main/java/com/taskgoapp/taskgo/core/firebase/FirestoreHelper.kt
FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "taskgo")
```

**Verificar:**
1. ✅ `google-services.json` está atualizado
2. ✅ Database 'taskgo' está acessível via SDK do Android
3. ✅ App tem permissões corretas

## 🔧 CREDENCIAIS MONGODB (Para referência)

As credenciais MongoDB são para:
- **Ferramentas externas** (MongoDB Compass, etc.)
- **Não são usadas** pelo Firebase Admin SDK ou Android SDK

**Connection String (para ferramentas MongoDB):**
```
mongodb://taskgo:gXmmPs8FU9-dv2dNcGZdk3iHFthkcWBOnrlNEvD5xkN3cwcu@df7f20f8-abda-484c-bb47-3b309f569d09.nam5.firestore.goog:443/taskgo?loadBalanced=true&tls=true&authMechanism=SCRAM-SHA-256&retryWrites=false
```

## ✅ VERIFICAÇÕES NECESSÁRIAS

### 1. Firebase Console
- [ ] Database 'taskgo' existe e está ativo
- [ ] Database 'default' foi deletado (confirmado)
- [ ] Permissões do projeto estão corretas

### 2. Cloud Functions
- [ ] Todas as funções usam `getFirestore()` (✅ Já feito)
- [ ] Nenhuma função usa `admin.firestore()` sem parâmetro (✅ Já feito)
- [ ] Deploy completo realizado (✅ Já feito)

### 3. Android App
- [ ] `FirestoreHelper` está sendo usado (✅ Já feito)
- [ ] Nenhum lugar usa `FirebaseFirestore.getInstance()` sem parâmetro (✅ Já feito)
- [ ] `google-services.json` está atualizado

## 🚀 TESTES

### Teste 1: Cloud Function
```bash
# Chamar uma função que grava dados
# Verificar no Firebase Console que dados foram para 'taskgo'
```

### Teste 2: Android App
```bash
# Criar dados no app
# Verificar no Firebase Console que dados foram para 'taskgo'
```

### Teste 3: Verificar Logs
```bash
# Verificar logs das Cloud Functions
firebase functions:log

# Procurar por erros relacionados a database
```

## ⚠️ SE HOUVER ERROS

### Erro: "Database not found"
- Verificar se 'taskgo' existe no Firebase Console
- Verificar se projeto tem acesso Enterprise

### Erro: "Permission denied"
- Verificar IAM roles do service account
- Verificar Firestore Rules

### Erro: "Cannot access database"
- Verificar se database está em modo ativo
- Verificar configuração do projeto

## 📝 NOTAS IMPORTANTES

1. **Firebase Admin SDK e Android SDK NÃO usam connection strings MongoDB**
   - Eles usam credenciais do Google Cloud automaticamente
   - Connection strings são apenas para ferramentas externas

2. **Database 'taskgo' deve estar acessível via SDKs**
   - Se não estiver, verificar configuração no Firebase Console
   - Verificar se projeto tem suporte a múltiplos databases

3. **Não há mais database 'default'**
   - Qualquer tentativa de acessar 'default' deve falhar
   - Código já está configurado para falhar explicitamente
