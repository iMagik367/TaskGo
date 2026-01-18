# TESTE FINAL - VERIFICACAO COMPLETA

## STATUS ATUAL

- ✅ Cloud Firestore API HABILITADA
- ✅ Database 'taskgo' existe e esta acessivel
- ✅ Firestore Rules deployadas com isolamento por usuario
- ✅ Fluxo de autenticacao corrigido (chama setInitialUserRole)
- ✅ Token recarregado apos definir role

## TESTE COMPLETO

### 1. Teste de Login com Google

1. Faca logout do app (se estiver logado)
2. Faca login com Google
3. Quando aparecer o dialog de selecao de tipo de conta:
   - Selecione **PARCEIRO**
4. Verifique nos logs do Android Studio:
   - `setInitialUserRole` foi chamado
   - Token foi recarregado
   - Role salvo como "partner" no Firestore
   - Nenhum erro `FAILED_PRECONDITION`

### 2. Teste de Cadastro com Email

1. Faca logout do app
2. Vá para tela de cadastro
3. Preencha os dados e selecione tipo de conta **PARCEIRO**
4. Complete o cadastro
5. Verifique nos logs:
   - `setInitialUserRole` foi chamado
   - Token foi recarregado
   - Role salvo como "partner" no Firestore
   - Nenhum erro `FAILED_PRECONDITION`

### 3. Verificacao no Firestore Console

1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/databases/taskgo/data
2. Vá em: `users/{userId}`
3. Verifique que o campo `role` esta como `"partner"` (nao "client")
4. Verifique que `pendingAccountType` esta como `false`

### 4. Verificacao de Custom Claims

1. No app, apos login, verifique o token:
   - O Custom Claim `role` deve estar presente
   - Para PARCEIRO: `role: "partner"`
   - Para CLIENTE: `role: "client"`

## LOGS ESPERADOS

### Login com Google (PARCEIRO):

```
LoginViewModel: Criando/atualizando perfil no Firestore com AccountType: PARCEIRO, role: partner
LoginViewModel: Chamando setInitialUserRole Cloud Function...
FirebaseFunctionsService: setInitialUserRole bem-sucedido
LoginViewModel: Token recarregado com sucesso
LoginViewModel: Perfil atualizado com sucesso no Firestore. AccountType: PARCEIRO, role: partner
```

### NENHUM destes erros deve aparecer:

- ❌ `FAILED_PRECONDITION: Firestore API data access is disabled`
- ❌ `Erro ao criar perfil: FAILED_PRECONDITION`
- ❌ `Erro ao buscar usuário: FAILED_PRECONDITION`

## SE AINDA HOUVER ERROS

1. Aguarde mais 2-3 minutos (API pode estar ainda sendo ativada)
2. Reinicie o app completamente
3. Limpe o cache do app
4. Verifique se o database 'taskgo' esta selecionado no Firebase Console
5. Verifique se as Firestore Rules estao deployadas

## VERIFICACAO RAPIDA

Execute no terminal:
```powershell
firebase firestore:databases:list --project=task-go-ee85f
```

Deve mostrar:
- `(default)`
- `taskgo` ✅
