# DIAGNOSTICO DE ARQUITETURA FRONTEND-BACKEND

## PROBLEMA IDENTIFICADO

O banco de dados 'taskgo' está vazio, indicando possível falha na arquitetura entre frontend e backend.

## VERIFICACOES NECESSARIAS

### 1. Frontend (Android)

#### A. Inicializacao do Firestore

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/di/FirebaseModule.kt`
- ✅ Usa `FirestoreHelper.getInstance()` em producao
- ✅ Configurado para usar database 'taskgo'

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/firebase/FirestoreHelper.kt`
- ✅ Tenta acessar database 'taskgo' via `FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "taskgo")`
- ⚠️ **PROBLEMA POTENCIAL**: Se o database 'taskgo' nao estiver totalmente inicializado, pode falhar silenciosamente

#### B. Uso Direto do Firestore (SEM FirestoreHelper)

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/feature/orders/presentation/ShipmentScreen.kt`
- ❌ **PROBLEMA**: Usa `FirebaseFirestore.getInstance()` diretamente
- **CORRECAO NECESSARIA**: Trocar para `FirestoreHelper.getInstance()`

#### C. Repositorios

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreUserRepository.kt`
- ✅ Usa `firestore` injetado (que vem do `FirebaseModule`)
- ✅ Deve estar usando database 'taskgo' corretamente

### 2. Backend (Cloud Functions)

**Arquivo**: `functions/src/utils/firestore.ts`
- ✅ Usa `app.firestore('taskgo')`
- ✅ Nao faz fallback para 'default'

**Arquivo**: `functions/src/auth.ts`
- ✅ Usa `getFirestore()` (que retorna database 'taskgo')
- ✅ `onUserCreate` deve criar usuarios no database 'taskgo'

## POSSIVEIS CAUSAS

### 1. Database 'taskgo' nao esta totalmente inicializado
- O database pode existir mas nao estar pronto para receber dados
- Solucao: Verificar no Firebase Console se o database esta ativo

### 2. Problema de timing na inicializacao
- O app pode estar tentando escrever antes do database estar pronto
- Solucao: Adicionar verificacao de conectividade

### 3. Permissoes do database 'taskgo'
- As Firestore Rules podem estar bloqueando escritas
- Solucao: Verificar se as rules permitem criacao de documentos

### 4. Cache offline do Firestore
- O app pode estar escrevendo apenas no cache local
- Solucao: Verificar se `setPersistenceEnabled(true)` esta causando problemas

### 5. Erro silencioso na inicializacao
- O `FirestoreHelper.getInstance()` pode estar falhando mas o erro nao esta sendo logado
- Solucao: Adicionar logs detalhados

## CORRECOES NECESSARIAS

### 1. Corrigir ShipmentScreen.kt
Trocar `FirebaseFirestore.getInstance()` por `FirestoreHelper.getInstance()`

### 2. Adicionar logs de diagnostico
Adicionar logs no `FirestoreHelper` para verificar se o database esta sendo acessado corretamente

### 3. Verificar se o database esta ativo
Criar um teste para verificar se o database 'taskgo' esta acessivel

### 4. Verificar Firestore Rules
Garantir que as rules permitem criacao de documentos em `/users/{userId}`

## TESTE DE DIAGNOSTICO

Execute este teste para verificar se o database esta funcionando:

1. Fazer login no app
2. Verificar logs do Android Studio:
   - `FirestoreHelper: Acessando database 'taskgo'`
   - `FirestoreUserRepository: Buscando usuário no Firestore`
   - Se houver erro, verificar a mensagem

3. Verificar no Firebase Console:
   - Acessar: https://console.firebase.google.com/project/task-go-ee85f/firestore/databases/taskgo/data
   - Verificar se aparece a colecao `users`
   - Verificar se ha algum documento sendo criado

4. Verificar Cloud Functions logs:
   - Acessar: https://console.cloud.google.com/functions/list?project=task-go-ee85f
   - Verificar logs da funcao `onUserCreate`
   - Verificar se ha erros
