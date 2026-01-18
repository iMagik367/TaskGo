# COMPILAR NOVO AAB - CORRECOES APLICADAS

## IMPORTANTE

Você precisa compilar um novo AAB porque as correções foram feitas no código fonte, mas o app instalado ainda está com a versão antiga (1.0.84).

## CORRECOES APLICADAS NA VERSAO 1.0.85

### 1. ShipmentScreen.kt
- ✅ Agora usa `FirestoreHelper.getInstance()` ao invés de `FirebaseFirestore.getInstance()`
- ✅ Garante que está usando database 'taskgo' ao invés de 'default'

### 2. FirestoreHelper.kt
- ✅ Adicionados logs de diagnóstico detalhados
- ✅ Logs mostram quando o database 'taskgo' é acessado
- ✅ Logs de erro mais detalhados para debug

### 3. Fluxo de Autenticação
- ✅ LoginViewModel chama `setInitialUserRole` corretamente
- ✅ SignupViewModel chama `setInitialUserRole` corretamente
- ✅ Token é recarregado após definir role
- ✅ Role é salvo corretamente no Firestore

## COMO COMPILAR

### Opção 1: Script Automático (Recomendado)

Execute no PowerShell ou CMD:
```batch
BUILD_CLEAN.bat
```

Este script:
- Limpa o build anterior
- Compila o novo AAB
- Versão: **1.0.85 (Code: 85)**

### Opção 2: Build Manual

No Android Studio:
1. Build > Clean Project
2. Build > Generate Signed Bundle / APK
3. Selecione "Android App Bundle"
4. Selecione "release"
5. Assine com sua keystore
6. O AAB será gerado em: `app/build/outputs/bundle/release/app-release.aab`

## O QUE MUDOU NA VERSAO 1.0.85

### Versão Anterior (1.0.84):
- ❌ ShipmentScreen usava database 'default'
- ❌ Logs limitados no FirestoreHelper
- ❌ Possível problema de arquitetura frontend-backend

### Versão Nova (1.0.85):
- ✅ ShipmentScreen usa database 'taskgo' corretamente
- ✅ Logs detalhados para diagnóstico
- ✅ Arquitetura frontend-backend corrigida

## APOS COMPILAR

1. **Instale o novo AAB** no dispositivo
2. **Faça logout** (se estiver logado)
3. **Crie uma nova conta** ou faça login
4. **Selecione PARCEIRO** no dialog de tipo de conta
5. **Verifique os logs** do Android Studio:
   ```
   FirestoreHelper: 🔍 Acessando database 'taskgo'...
   FirestoreHelper: ✅ Database 'taskgo' acessado com sucesso
   LoginViewModel: Chamando setInitialUserRole Cloud Function...
   LoginViewModel: setInitialUserRole bem-sucedido
   LoginViewModel: Token recarregado com sucesso
   LoginViewModel: Perfil atualizado com sucesso no Firestore. role: partner
   ```
6. **Verifique no Firebase Console**:
   - Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/databases/taskgo/data
   - Verifique se a coleção `users` foi criada
   - Verifique se há um documento com o UID do usuário
   - Verifique se o campo `role` está como `"partner"` (não "client")

## SE AINDA NÃO FUNCIONAR

### Verificar Logs do Android Studio:
1. Abra o Logcat
2. Filtre por: `FirestoreHelper`, `LoginViewModel`, `SignupViewModel`
3. Procure por erros relacionados a:
   - `FAILED_PRECONDITION`
   - `PERMISSION_DENIED`
   - `Database 'taskgo' não está disponível`

### Verificar Cloud Functions:
1. Acesse: https://console.cloud.google.com/functions/list?project=task-go-ee85f
2. Verifique logs da função `setInitialUserRole`
3. Verifique logs da função `onUserCreate`

### Verificar Firestore Rules:
```powershell
firebase firestore:rules:get --project=task-go-ee85f
```

As rules devem permitir criação de documentos em `/users/{userId}` para usuários autenticados.

## RESUMO

- ✅ Versão atualizada: **1.0.85 (Code: 85)**
- ✅ Correções aplicadas no código
- ⏳ **AÇÃO NECESSÁRIA**: Compilar novo AAB
- ⏳ **AÇÃO NECESSÁRIA**: Instalar novo AAB no dispositivo
- ⏳ **AÇÃO NECESSÁRIA**: Testar login/cadastro com tipo de conta PARCEIRO
