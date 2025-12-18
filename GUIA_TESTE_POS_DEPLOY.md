# Guia de Teste Pós-Deploy das Regras

## ✅ Regras Publicadas - Pronto para Teste!

Agora que as regras do Firestore foram publicadas, você pode usar o app normalmente. Aqui está o que esperar:

---

## 🎯 O Que Foi Corrigido

### 1. **Crashes por PERMISSION_DENIED** ✅
- O app não deve mais crashar ao tentar buscar prestadores/lojas
- Erros de permissão agora são tratados graciosamente (logados, mas não causam crash)

### 2. **SyncWorker** ✅
- A sincronização em background deve funcionar corretamente
- Não deve mais aparecer erro de instanciação do Hilt

### 3. **Tratamento de Erros** ✅
- Todos os erros do Firestore são capturados e logados
- O app continua funcionando mesmo se houver problemas de conexão/permissão

---

## 🧪 Testes Recomendados

### Teste 1: Abrir o App
- ✅ O app deve abrir normalmente
- ✅ Não deve crashar na tela inicial
- ✅ Deve carregar dados do cache local (rápido)

### Teste 2: Navegar para Serviços
- ✅ Deve exibir a lista de prestadores
- ✅ Não deve aparecer erro de permissão nos logs
- ✅ Se não houver prestadores, deve mostrar lista vazia (não erro)

### Teste 3: Abrir o Mapa
- ✅ Deve carregar o mapa
- ✅ Deve exibir marcadores de prestadores/lojas (se houver)
- ✅ Não deve crashar ao tentar buscar localizações

### Teste 4: Criar/Editar Produtos
- ✅ Deve permitir criar produtos
- ✅ Deve salvar localmente primeiro (rápido)
- ✅ Deve sincronizar com Firebase em background

### Teste 5: Verificar Sincronização
- ✅ Aguardar alguns minutos
- ✅ Verificar se os dados foram sincronizados com Firebase
- ✅ Verificar logs do SyncWorker (não deve ter erros)

---

## 📊 O Que Monitorar nos Logs

### ✅ Logs Normais (Esperados):
```
SyncManager: Sincronização iniciada
FirestoreMapLocationsRepository: Observando providers...
FirestoreMapLocationsRepository: Observando stores...
```

### ⚠️ Logs de Aviso (Não Críticos):
```
FirestoreExceptionHandler: Permissão negada no Firestore: ...
```
- Se aparecer, pode indicar que algumas queries ainda precisam de ajuste
- O app não deve crashar, apenas logar o aviso

### ❌ Logs de Erro (Investigar):
```
FATAL EXCEPTION: main
FirebaseFirestoreException: PERMISSION_DENIED
```
- Se aparecer, as regras podem não ter sido deployadas corretamente
- Verificar se o deploy foi concluído no Firebase Console

---

## 🔍 Como Verificar se as Regras Foram Aplicadas

1. **No Firebase Console:**
   - Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/rules
   - Verifique se a regra `allow list: if isAuthenticated();` está presente
   - Deve estar dentro do bloco `match /users/{userId}`

2. **No App:**
   - Abra o Logcat no Android Studio
   - Filtre por: `FirestoreMapLocationsRepository`
   - Não deve aparecer erros `PERMISSION_DENIED` ao buscar prestadores/lojas

---

## 🚨 Se Ainda Houver Problemas

### Problema: Ainda aparece PERMISSION_DENIED
**Solução:**
1. Verificar se o usuário está autenticado
2. Verificar se as regras foram realmente publicadas (aguardar 1-2 minutos após publicação)
3. Limpar cache do app e tentar novamente

### Problema: App ainda crasha
**Solução:**
1. Verificar logs completos no Logcat
2. Verificar se todas as correções foram aplicadas
3. Fazer rebuild do app: `./gradlew clean :app:assembleDebug`

### Problema: Dados não aparecem
**Solução:**
1. Verificar se há dados no Firestore
2. Verificar se os índices compostos necessários foram criados
3. Verificar logs para erros específicos

---

## ✅ Checklist Final

- [ ] Regras do Firestore publicadas
- [ ] App compilado com sucesso
- [ ] App abre sem crashar
- [ ] Lista de prestadores/lojas carrega
- [ ] Mapa funciona corretamente
- [ ] Sincronização em background funciona
- [ ] Não há erros críticos nos logs

---

## 📝 Notas Importantes

1. **Cache Local**: O app agora usa cache local primeiro, então os dados aparecem instantaneamente mesmo sem internet.

2. **Sincronização**: A sincronização com Firebase acontece em background após 1 minuto. Não bloqueia a UI.

3. **Tratamento de Erros**: Todos os erros são tratados graciosamente. O app não deve crashar mesmo com problemas de conexão.

4. **Performance**: O app deve estar mais rápido agora, pois carrega dados do cache local primeiro.

---

**Status:** ✅ Pronto para uso!  
**Data:** 2025-11-16

