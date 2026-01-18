# ✅ CORREÇÃO DO FLUXO DE AUTENTICAÇÃO GOOGLE

## 🔧 PROBLEMAS CORRIGIDOS

### 1. **Tipo de Conta Ignorado no Login Google**

**Problema**: Ao fazer login com Google e selecionar "PARCEIRO", o app criava todas as contas como "cliente" mesmo quando o usuário selecionava "parceiro".

**Causa**: O app não estava chamando a Cloud Function `setInitialUserRole` para definir os Custom Claims no Firebase Auth.

**Solução Implementada**:
- ✅ Adicionado método `setInitialUserRole` no `FirebaseFunctionsService`
- ✅ Modificado `LoginViewModel.createUserWithAccountType()` para:
  1. Chamar `setInitialUserRole` Cloud Function primeiro
  2. Recarregar token com `getIdToken(true)` para obter novos Custom Claims
  3. Salvar role no Firestore
- ✅ Modificado `SignupViewModel` para também chamar `setInitialUserRole` durante cadastro

### 2. **Erro Firestore API Desabilitada**

**Problema**: `FAILED_PRECONDITION: Firestore API data access is disabled`

**Causa**: A Cloud Firestore API não estava habilitada no Google Cloud Console para o projeto.

**Solução**:
- ✅ Criado script `habilitar-firestore-api-completo.ps1` para habilitar a API
- ✅ Criado script `verificar-firestore-config.ps1` para verificar configuração
- ✅ Database 'taskgo' confirmado como existente e acessível

## 📋 FLUXO CORRIGIDO

### Login com Google (Novo Usuário)

1. Usuário faz login com Google
2. App detecta que é novo usuário (documento não existe no Firestore)
3. Mostra dialog de seleção de tipo de conta
4. Usuário seleciona **PARCEIRO** ou **CLIENTE**
5. **App chama `setInitialUserRole` Cloud Function** ← NOVO
6. Cloud Function define Custom Claim no Firebase Auth:
   - PARCEIRO → `role: "partner"`
   - CLIENTE → `role: "client"`
7. **App recarrega token** (`getIdToken(true)`) ← NOVO
8. App salva role no Firestore
9. Navega para home

### Cadastro com Email

1. Usuário preenche formulário e seleciona tipo de conta
2. App cria usuário no Firebase Auth
3. **App chama `setInitialUserRole` Cloud Function** ← NOVO
4. Cloud Function define Custom Claim
5. **App recarrega token** (`getIdToken(true)`) ← NOVO
6. App salva perfil no Firestore com role correto
7. Navega para home

## 🔑 CUSTOM CLAIMS

Os Custom Claims são a **autoridade única** para roles:
- Definidos via Cloud Function `setInitialUserRole`
- Acessíveis via `request.auth.token.role` nas Firestore Rules
- Recarregados com `getIdToken(true)` após definição

## ✅ ARQUIVOS MODIFICADOS

1. `app/src/main/java/com/taskgoapp/taskgo/data/firebase/FirebaseFunctionsService.kt`
   - Adicionado método `setInitialUserRole()`

2. `app/src/main/java/com/taskgoapp/taskgo/feature/auth/presentation/LoginViewModel.kt`
   - Modificado `createUserWithAccountType()` para chamar `setInitialUserRole`
   - Adicionado `getIdToken(true)` após definir role
   - Adicionado `FirebaseFunctionsService` como dependência

3. `app/src/main/java/com/taskgoapp/taskgo/feature/auth/presentation/SignupViewModel.kt`
   - Modificado fluxo de cadastro para chamar `setInitialUserRole`
   - Adicionado `getIdToken(true)` após definir role
   - Adicionado `FirebaseFunctionsService` como dependência

## 🚀 SCRIPTS CRIADOS

1. `habilitar-firestore-api-completo.ps1` - Habilita Cloud Firestore API
2. `verificar-firestore-config.ps1` - Verifica configuração completa
3. `habilitar-firestore-via-firebase.ps1` - Habilita via Firebase CLI
4. `HABILITAR_FIRESTORE_API.md` - Documentação completa

## ⚠️ AÇÃO NECESSÁRIA

**Habilitar Cloud Firestore API**:

Execute:
```powershell
.\habilitar-firestore-api-completo.ps1
```

Ou acesse diretamente:
https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f

Clique em **"ENABLE"** e aguarde 2-5 minutos.

## ✅ TESTE

Após habilitar a API:

1. Faça logout do app
2. Faça login com Google
3. Selecione **PARCEIRO** no dialog
4. Verifique nos logs que:
   - `setInitialUserRole` foi chamado
   - Token foi recarregado
   - Role foi salvo como "partner" no Firestore
   - Custom Claim está presente no token

## 📊 VERIFICAÇÃO

Execute para verificar tudo:
```powershell
.\verificar-firestore-config.ps1
```
