# RESUMO DAS CORRECOES - AUTENTICACAO GOOGLE

## PROBLEMAS IDENTIFICADOS

1. **Tipo de conta ignorado**: Login com Google criava todas as contas como "cliente" mesmo selecionando "parceiro"
2. **Firestore API desabilitada**: Erro `FAILED_PRECONDITION: Firestore API data access is disabled`

## CORRECOES IMPLEMENTADAS

### 1. Fluxo de Autenticacao Corrigido

**Arquivos Modificados**:
- `LoginViewModel.kt` - Agora chama `setInitialUserRole` antes de salvar no Firestore
- `SignupViewModel.kt` - Tambem chama `setInitialUserRole` durante cadastro
- `FirebaseFunctionsService.kt` - Adicionado metodo `setInitialUserRole()`

**Fluxo Corrigido**:
1. Usuario seleciona tipo de conta (PARCEIRO/CLIENTE)
2. App chama Cloud Function `setInitialUserRole`
3. Cloud Function define Custom Claim no Firebase Auth
4. App recarrega token com `getIdToken(true)`
5. App salva role no Firestore
6. Navega para home

### 2. Scripts Criados

- `habilitar-firestore-api-completo.ps1` - Habilita Cloud Firestore API
- `verificar-firestore-config.ps1` - Verifica configuracao completa
- `habilitar-firestore-via-firebase.ps1` - Habilita via Firebase CLI

## ACAO NECESSARIA

**Habilitar Cloud Firestore API**:

Execute o script:
```powershell
.\habilitar-firestore-api-completo.ps1
```

Ou acesse diretamente:
https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f

Clique em **ENABLE** e aguarde 2-5 minutos.

## VERIFICACAO

Apos habilitar a API, execute:
```powershell
.\verificar-firestore-config.ps1
```

## TESTE

1. Faca logout do app
2. Faca login com Google
3. Selecione PARCEIRO no dialog
4. Verifique nos logs que o role foi salvo como "partner"
