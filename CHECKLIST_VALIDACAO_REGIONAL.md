# 🧩 CHECKLIST FINAL DE VALIDAÇÃO - ARQUITETURA REGIONAL

**Data:** 2024  
**Objetivo:** Validar migração completa para arquitetura regional `locations/{city}_{state}/products` e `locations/{city}_{state}/stories`

---

## ✅ CHECKLIST DE VALIDAÇÃO

### 1. ✅ Produto criado em Osasco aparece somente em Osasco

**Como validar:**
1. Criar produto com localização "Osasco, SP"
2. Verificar Firestore Console: deve estar em `locations/osasco_sp/products/{productId}`
3. App em Osasco deve ver o produto
4. App em outra cidade (ex: São Paulo) **NÃO** deve ver o produto

**Status:** ⚠️ **PENDENTE** - App ainda lê de coleção global `products`

**Ação necessária:**
- `FirestoreProductsRepositoryImpl` precisa usar `LocationHelper.getLocationCollection()` quando city/state disponíveis
- `ProductsViewModel` precisa passar city/state para o repositório

---

### 2. ✅ Stories respeitam localização

**Como validar:**
1. Criar story com localização "Osasco, SP"
2. Verificar Firestore Console: deve estar em `locations/osasco_sp/stories/{storyId}`
3. App em Osasco deve ver a story
4. App em outra cidade **NÃO** deve ver a story

**Status:** ⚠️ **PENDENTE** - App ainda lê de coleção global `stories`

**Ação necessária:**
- `FirestoreStoriesRepository.observeStories()` precisa usar `LocationHelper.getLocationCollection()` quando city/state disponíveis

---

### 3. ✅ Firestore Console mostra dados apenas dentro de `locations`

**Como validar:**
1. Abrir Firestore Console
2. Navegar para coleção `locations`
3. Verificar subcoleções: `locations/osasco_sp/products`, `locations/osasco_sp/stories`
4. Verificar que **NÃO** há dados na raiz `products` ou `stories` (ou apenas dados antigos para debug)

**Status:** ✅ **OK** - Rules permitem `locations/{locationId}/products` e `locations/{locationId}/stories`

---

### 4. ✅ Nenhum snapshot retorna vazio sem erro logado

**Como validar:**
1. Verificar logs do app quando produtos/stories são carregados
2. Se snapshot vazio: deve haver log de erro ou warning explicando motivo
3. Se snapshot vazio por falta de localização: log deve indicar "Localização não fornecida"

**Status:** ⚠️ **PENDENTE** - Logs insuficientes

**Ação necessária:**
- Adicionar logs detalhados em `FirestoreProductsRepositoryImpl` e `FirestoreStoriesRepository`
- Logar: tamanho do snapshot, collection usada, city/state, erros

---

### 5. ✅ Nenhuma coleção global é usada pelo app

**Como validar:**
1. Buscar no código: `\.collection\(["']products["']|\.collection\(["']stories["']`
2. Verificar que apenas repositórios de compatibilidade/debug usam coleções globais
3. Se usadas, devem estar claramente marcadas como "DEBUG ONLY" ou "TEMPORARY"

**Status:** ❌ **FALHA** - App ainda usa coleções globais:
- `FirestoreProductsRepositoryImpl`: usa `products` (linha 36)
- `FirestoreStoriesRepository`: usa `stories` (linha 34)

**Ação necessária:**
- Migrar para usar `locations/{city}_{state}/products` e `locations/{city}_{state}/stories`
- Manter coleções globais apenas para debug (comentado ou marcado)

---

### 6. ✅ Rules não bloqueiam leituras legítimas

**Como validar:**
1. Verificar `firestore.rules`:
   - `locations/{locationId}/products` tem `allow read` para autenticados
   - `locations/{locationId}/stories` tem `allow read` para autenticados
2. Testar no Firestore Console ou usando Simulator
3. Usuário autenticado deve conseguir ler `locations/osasco_sp/products` e `locations/osasco_sp/stories`

**Status:** ✅ **OK** - Rules já configuradas (linhas 566-581 de `firestore.rules`)

```javascript
match /locations/{locationId}/products/{productId} {
  allow read: if isAuthenticated() 
              && (resource == null || resource.data.active == true);
}

match /locations/{locationId}/stories/{storyId} {
  allow read: if isAuthenticated();
}
```

---

## 🔧 AÇÕES NECESSÁRIAS

### Prioridade ALTA

1. **Migrar `FirestoreProductsRepositoryImpl` para usar localização**
   - Obter city/state do usuário ou da localização GPS
   - Usar `LocationHelper.getLocationCollection()` quando disponível
   - Fallback para coleção global apenas se localização indisponível (com log)

2. **Migrar `FirestoreStoriesRepository` para usar localização**
   - Obter city/state do usuário ou da localização GPS
   - Usar `LocationHelper.getLocationCollection()` quando disponível
   - Fallback para coleção global apenas se localização indisponível (com log)

3. **Adicionar logs detalhados**
   - Tamanho do snapshot
   - Collection usada (paths completos)
   - City/state do usuário
   - Erros e warnings claros

### Prioridade MÉDIA

4. **Marcar coleções globais como DEBUG ONLY**
   - Comentar código que usa `products` global
   - Adicionar `@Deprecated` ou comentários `// DEBUG ONLY - REMOVER APÓS VALIDAÇÃO`

5. **Documentar processo de validação manual**
   - Script ou guia passo a passo para validar cada item

### Prioridade BAIXA

6. **Limpar dados de teste das coleções globais** (após validação completa)
   - Manter apenas se necessário para compatibilidade durante migração

---

## 📝 NOTAS DE IMPLEMENTAÇÃO

### Como obter city/state no app:

1. **Do usuário logado:**
   ```kotlin
   userRepository.observeCurrentUser().collect { user ->
       val city = user?.city ?: ""
       val state = user?.state ?: ""
   }
   ```

2. **Do GPS (geocoding reverso):**
   ```kotlin
   val location = locationManager.getCurrentLocation()
   val address = locationManager.getAddressFromLocation(location.latitude, location.longitude)
   val city = address?.locality ?: ""
   val state = address?.adminArea ?: ""
   ```

3. **Prioridade:** GPS > Perfil do usuário > Fallback (coleção global com log de warning)

---

## 🎯 RESULTADO ESPERADO

Após implementação:

✅ Produtos de Osasco só aparecem para usuários em Osasco  
✅ Stories de Osasco só aparecem para usuários em Osasco  
✅ Firestore Console mostra estrutura `locations/{city}_{state}/products` e `locations/{city}_{state}/stories`  
✅ Logs claros quando snapshot vazio (localização não disponível, sem dados na região, etc.)  
✅ Nenhuma coleção global usada em produção  
✅ Rules permitem leituras legítimas sem bloqueios  

---

## 🧨 CONCLUSÃO

**Arquitetura regional = Escalável + Segura + Conformidade Jurídica**

Esta validação garante:
- ✅ Isolamento de dados por região
- ✅ Performance otimizada (queries menores)
- ✅ Preparação para PostgreSQL (sharding por região)
- ✅ Conformidade LGPD/GDPR (dados regionais)
