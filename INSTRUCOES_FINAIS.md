# INSTRUCOES FINAIS - CORRECAO AUTENTICACAO

## STATUS DAS CORRECOES

### ✅ CORRIGIDO NO CODIGO

1. **Fluxo de Autenticacao Google**
   - App agora chama `setInitialUserRole` Cloud Function
   - Token e recarregado apos definir role
   - Role e salvo corretamente no Firestore

2. **Database e Rules**
   - Database 'taskgo' existe e esta acessivel
   - Firestore Rules deployadas com isolamento por usuario
   - Isolamento completo implementado

### ⚠️ ACAO NECESSARIA

**Habilitar Cloud Firestore API no Google Cloud Console**

## PASSO A PASSO

### 1. Habilitar Cloud Firestore API

**Opcao A - Via Web (Recomendado)**:
1. Acesse: https://console.cloud.google.com/apis/library/firestore.googleapis.com?project=task-go-ee85f
2. Clique no botao **"ENABLE"** (Habilitar)
3. Aguarde 2-5 minutos

**Opcao B - Via Script**:
```powershell
.\habilitar-api-automatico.ps1
```

O Firebase CLI ja tentou habilitar automaticamente, mas pode ser necessario confirmar no console.

### 2. Verificar Configuracao

Apos habilitar a API, aguarde 2-5 minutos e execute:
```powershell
.\verificar-firestore-config.ps1
```

### 3. Testar no App

1. Faca logout do app
2. Faca login com Google
3. Selecione **PARCEIRO** no dialog
4. Verifique que o role foi salvo como "partner"

## ARQUIVOS MODIFICADOS

- `LoginViewModel.kt` - Chama setInitialUserRole
- `SignupViewModel.kt` - Chama setInitialUserRole  
- `FirebaseFunctionsService.kt` - Adicionado metodo setInitialUserRole

## SCRIPTS CRIADOS

- `habilitar-api-automatico.ps1` - Habilita API automaticamente
- `habilitar-firestore-api-completo.ps1` - Habilita via gcloud ou web
- `verificar-firestore-config.ps1` - Verifica configuracao

## VERIFICACAO FINAL

Apos habilitar a API, o erro `FAILED_PRECONDITION: Firestore API data access is disabled` deve desaparecer e:

- Login com Google deve salvar o role correto
- Cadastro com email deve salvar o role correto
- Custom Claims devem estar presentes no token
- Firestore Rules devem funcionar corretamente
