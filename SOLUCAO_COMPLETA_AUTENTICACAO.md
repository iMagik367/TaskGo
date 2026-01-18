# SOLUCAO COMPLETA - AUTENTICACAO GOOGLE E FIRESTORE API

## PROBLEMAS CORRIGIDOS

### 1. Tipo de Conta Ignorado no Login Google
- **Problema**: Todas as contas criadas como "cliente" mesmo selecionando "parceiro"
- **Solucao**: App agora chama `setInitialUserRole` Cloud Function e recarrega token

### 2. Firestore API Desabilitada
- **Problema**: Erro `FAILED_PRECONDITION: Firestore API data access is disabled`
- **Solucao**: Scripts criados para habilitar a API

## CORRECOES IMPLEMENTADAS

### Codigo Modificado

1. **LoginViewModel.kt**
   - Adicionado `FirebaseFunctionsService` como dependencia
   - Modificado `createUserWithAccountType()` para:
     - Chamar `setInitialUserRole` Cloud Function
     - Recarregar token com `getIdToken(true)`
     - Salvar role no Firestore

2. **SignupViewModel.kt**
   - Adicionado `FirebaseFunctionsService` como dependencia
   - Modificado fluxo de cadastro para chamar `setInitialUserRole`

3. **FirebaseFunctionsService.kt**
   - Adicionado metodo `setInitialUserRole(role, accountType)`

### Scripts Criados

1. `habilitar-api-automatico.ps1` - Habilita API automaticamente
2. `habilitar-firestore-api-completo.ps1` - Habilita via gcloud ou web
3. `verificar-firestore-config.ps1` - Verifica configuracao completa

## ACAO NECESSARIA

### Habilitar Cloud Firestore API

O Firebase CLI ja tentou habilitar automaticamente ao fazer deploy das regras.

**Verificar se foi habilitada**:

1. Acesse: https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f
2. Se aparecer botao "ENABLE", clique nele
3. Aguarde 2-5 minutos

**Ou execute**:
```powershell
.\habilitar-api-automatico.ps1
```

## VERIFICACAO

Apos habilitar a API, execute:
```powershell
.\verificar-firestore-config.ps1
```

## TESTE COMPLETO

1. Faca logout do app
2. Faca login com Google
3. Selecione **PARCEIRO** no dialog
4. Verifique nos logs:
   - `setInitialUserRole` foi chamado
   - Token foi recarregado
   - Role salvo como "partner" no Firestore

## STATUS ATUAL

- Database 'taskgo' existe e esta acessivel
- Firestore Rules deployadas com sucesso
- Isolamento por usuario implementado
- Fluxo de autenticacao corrigido
- Scripts de habilitacao criados

**Aguardando apenas habilitacao da Cloud Firestore API no Google Cloud Console**
