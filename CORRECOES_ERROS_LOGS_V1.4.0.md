# 🔴 CORREÇÕES DE ERROS - LOGS V1.4.0

## ERROS IDENTIFICADOS E CORREÇÕES

### ❌ ERRO 1: "User document not found" na Cloud Function
**Log**: `Erro na função setInitialUserRole: code=NOT_FOUND, message=User document not found`

**CAUSA**: App chama `setInitialUserRole` ANTES de criar o documento do usuário no Firestore.

**CORREÇÃO APLICADA**: ✅
- Criar documento inicial com role temporário "client"
- Aguardar propagação (500ms)
- Chamar `setInitialUserRole` para atualizar o role correto
- Cloud Function agora encontra o documento e atualiza o role

---

### ❌ ERRO 2: Query inválida `role==store`
**Log**: `Query(users where role==store`

**CAUSA**: `FirestoreMapLocationsRepository` busca role "store" que não existe.

**CORREÇÃO APLICADA**: ✅
- Substituído `whereEqualTo("role", "store")` por `whereEqualTo("role", "partner")`
- Arquivo: `FirestoreMapLocationsRepository.kt` (2 ocorrências)

---

### ❌ ERRO 3: PERMISSION_DENIED em múltiplas queries
**Log**: `PERMISSION_DENIED: Missing or insufficient permissions`

**CAUSA RAIZ**: Usuário recém-criado não tem role definido ainda, e as Firestore Rules bloqueiam acesso.

**QUERIES AFETADAS**:
1. `users/{userId}` - Leitura do próprio perfil
2. `cards where userId==...` - Cartões de pagamento
3. `addresses where userId==...` - Endereços
4. `service_categories` - Categorias de serviço
5. `product_categories` - Categorias de produto
6. `homeBanners where active==true` - Banners da home

**CORREÇÃO NECESSÁRIA**: ⚠️ PENDENTE
- Firestore Rules devem permitir:
  1. Usuário criar seu próprio documento inicial (com role temporário)
  2. Usuário ler categorias (públicas para todos)
  3. Usuário ler banners (públicos para todos)
  4. Usuário ler/criar seus próprios cartões e endereços

---

### ❌ ERRO 4: GPS sendo buscado durante login
**Log**: `getCurrentLocationGuaranteed: Obtendo GPS com garantia...` durante o login

**CAUSA**: `HomeViewModel` observa `userRepository.observeCurrentUser()` que aciona o GPS.

**PROBLEMA**: GPS não é necessário durante o login e causa delay.

**CORREÇÃO NECESSÁRIA**: ⚠️ PENDENTE
- GPS deve ser buscado apenas quando necessário (mapa, filtros por distância)
- NÃO deve ser acionado automaticamente ao observar o usuário

---

### ❌ ERRO 5: WorkManager não inicializa
**Log**: `Could not instantiate SyncWorker` e `Could not create Worker`

**CAUSA**: Hilt não está pronto quando WorkManager tenta criar os Workers.

**CORREÇÃO APLICADA**: ✅
- Aumentado delay de 2s para 5s antes de agendar Workers
- Retry automático se falhar

---

## 🔧 CORREÇÕES APLICADAS

1. ✅ LoginViewModel cria documento inicial ANTES de setInitialUserRole
2. ✅ Query role==store substituída por role==partner
3. ✅ WorkManager delay aumentado para 5s

## ⚠️ CORREÇÕES PENDENTES

1. ⏳ Firestore Rules - permitir leitura de categorias e banners sem autenticação
2. ⏳ Desabilitar GPS automático no observeCurrentUser
3. ⏳ Garantir que usuário com role temporário "client" possa acessar dados básicos

---

## 📝 PRÓXIMOS PASSOS

1. Ajustar Firestore Rules para permitir:
   - Leitura de `service_categories` (público)
   - Leitura de `product_categories` (público)
   - Leitura de `homeBanners where active==true` (público)
   - Criação de `users/{userId}` pelo próprio usuário
   - Leitura/escrita de `cards` e `addresses` pelo próprio usuário

2. Remover chamada de GPS do `observeCurrentUser`

3. Rebuild e redeploy
