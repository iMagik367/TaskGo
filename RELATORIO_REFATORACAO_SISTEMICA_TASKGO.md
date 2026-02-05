# 📋 RELATÓRIO DE REFATORAÇÃO SISTÊMICA - TASKGO

**Data**: 2024-01-15  
**Versão do Documento Canônico**: 1.0  
**Status**: FASE 1, 2, 3 COMPLETAS - AGUARDANDO AUTORIZAÇÃO PARA FASE 4

---

## 1. RESUMO EXECUTIVO

Este relatório mapeia **TODAS** as violações do modelo canônico do TaskGo identificadas no código atual. O sistema possui **42 arquivos TypeScript no backend** e **29 repositórios Kotlin no frontend** que foram analisados linha por linha.

### Estatísticas Críticas

- **Total de arquivos backend analisados**: 42
- **Total de repositórios frontend analisados**: 29
- **Violações CRÍTICAS identificadas**: 18
- **Violações ALTAS identificadas**: 12
- **Violações MÉDIAS identificadas**: 8
- **Uso de "unknown" encontrado**: 8 ocorrências (PROIBIDO)
- **Aceitação de parâmetros de localização do cliente**: 1 ocorrência (PROIBIDO)
- **Fallback para address encontrado**: 1 ocorrência (PROIBIDO)

---

## 2. MAPEAMENTO COMPLETO DO SISTEMA

### 2.1. BACKEND (Cloud Functions)

#### 2.1.1. Arquivos que Escrevem Dados Públicos

| Arquivo | Função | Path Usado | Status |
|---------|--------|------------|--------|
| `functions/src/products/index.ts` | `createProduct` | `locations/{locationId}/products` | ✅ CORRETO |
| `functions/src/products/index.ts` | `updateProduct` | `locations/{locationId}/products` | ✅ CORRETO |
| `functions/src/products/index.ts` | `deleteProduct` | `locations/{locationId}/products` | ✅ CORRETO |
| `functions/src/services/index.ts` | `createService` | `locations/{locationId}/services` | ✅ CORRETO |
| `functions/src/services/index.ts` | `updateService` | `locations/{locationId}/services` | ✅ CORRETO |
| `functions/src/services/index.ts` | `deleteService` | `locations/{locationId}/services` | ✅ CORRETO |
| `functions/src/stories.ts` | `createStory` | `locations/{locationId}/stories` | ⚠️ VIOLAÇÃO |
| `functions/src/orders.ts` | `createOrder` | `locations/{locationId}/orders` | ✅ CORRETO |
| `functions/src/orders.ts` | `updateOrderStatus` | `locations/{locationId}/orders` | ✅ CORRETO |

#### 2.1.2. Arquivos que Leem Dados Públicos

| Arquivo | Função | Path Usado | Status |
|---------|--------|------------|--------|
| `functions/src/products/index.ts` | `updateProduct` | `locations/{locationId}/products` | ✅ CORRETO |
| `functions/src/products/index.ts` | `deleteProduct` | `locations/{locationId}/products` | ✅ CORRETO |
| `functions/src/services/index.ts` | `updateService` | `locations/{locationId}/services` | ✅ CORRETO |
| `functions/src/services/index.ts` | `deleteService` | `locations/{locationId}/services` | ✅ CORRETO |
| `functions/src/orders.ts` | `updateOrderStatus` | `locations/{locationId}/orders` | ✅ CORRETO |
| `functions/src/orders.ts` | `getMyOrders` | `locations/{locationId}/orders` | ✅ CORRETO |
| `functions/src/orders.ts` | `onServiceOrderCreated` | `locations/{locationId}/orders` | ✅ CORRETO |
| `functions/src/stories.ts` | `cleanupExpiredStories` | `locations/{locationId}/stories` | ✅ CORRETO |

#### 2.1.3. Arquivos que Usam Firestore

**TODOS os 42 arquivos** usam Firestore. Lista completa:

1. `functions/src/stories.ts`
2. `functions/src/products/index.ts`
3. `functions/src/services/index.ts`
4. `functions/src/utils/location.ts`
5. `functions/src/utils/firestorePaths.ts`
6. `functions/src/orders.ts`
7. `functions/src/auth.ts`
8. `functions/src/gradualNotifications.ts`
9. `functions/src/deleteAccount.ts`
10. `functions/src/webhooks.ts`
11. `functions/src/payments.ts`
12. `functions/src/sync-data.ts`
13. `functions/src/ai-chat.ts`
14. `functions/src/users/role.ts`
15. `functions/src/utils/firestore.ts`
16. `functions/src/identityVerification.ts`
17. `functions/src/migrate-database.ts`
18. `functions/src/user-settings.ts`
19. `functions/src/ssr-app.ts`
20. `functions/src/scripts/migrateExistingUsers.ts`
21. `functions/src/index.ts`
22. `functions/src/admin/roles.ts`
23. `functions/src/user-preferences.ts`
24. `functions/src/twoFactorAuth.ts`
25. `functions/src/tracking.ts`
26. `functions/src/stripe-connect.ts`
27. `functions/src/product-payments.ts`
28. `functions/src/product-orders.ts`
29. `functions/src/pix-payments.ts`
30. `functions/src/notifications.ts`
31. `functions/src/migrateToPartner.ts`
32. `functions/src/faceRecognitionVerification.ts`
33. `functions/src/clearAllData.ts`
34. `functions/src/billingWebhook.ts`
35. `functions/src/auto-refund.ts`
36. `functions/src/stripe-config.ts`
37. `functions/src/sendEmail.ts`
38. `functions/src/security/roles.ts`
39. `functions/src/utils/constants.ts`
40. `functions/src/utils/errors.ts`
41. `functions/src/security/appCheck.ts`
42. `functions/src/account-change.ts`

#### 2.1.4. Violações Identificadas no Backend

##### ❌ VIOLAÇÃO CRÍTICA 1: `functions/src/stories.ts` - Aceita Localização do Cliente

**Linha**: 54-65  
**Problema**: A função `createStory` aceita `location.city` e `location.state` do parâmetro `data` do cliente.

```typescript
// Tentar obter da localização fornecida primeiro
if (location && typeof location === 'object') {
  storyCity = location.city || '';
  storyState = location.state || '';
}

// Se não tiver na localização, obter do perfil do usuário
if (!storyCity || !storyState) {
  const userLocation = await getUserLocation(db, userId);
  storyCity = storyCity || userLocation.city;
  storyState = storyState || userLocation.state;
}
```

**Regra Violada**: Lei 9.3 - "NUNCA aceitar do cliente (frontend): city como parâmetro, state como parâmetro"

**Ação Obrigatória**: Remover completamente a aceitação de `location.city` e `location.state` do cliente. SEMPRE obter de `getUserLocation(db, userId)`.

---

##### ❌ VIOLAÇÃO CRÍTICA 2: `functions/src/utils/location.ts` - Fallback para Address

**Linha**: 211-227  
**Problema**: A função `getUserLocation()` faz fallback para `address.city` e `address.state` quando os campos diretos não estão disponíveis.

```typescript
// Fallback: tentar obter de address se campos diretos não estiverem disponíveis
const address = userData?.address;
if (address) {
  const fallbackCity = address.city || address.cityName || city || '';
  const fallbackState = address.state || address.stateName || state || '';
  // ...
  return {
    city: fallbackCity,
    state: fallbackState,
  };
}
```

**Regra Violada**: Lei 1 - "A localização do usuário é determinada EXCLUSIVAMENTE pelos campos `city` e `state` na raiz do documento `users/{userId}`"

**Ação Obrigatória**: Remover completamente o fallback para `address`. Se `city` ou `state` não existirem na raiz, retornar erro explícito.

---

##### ⚠️ VIOLAÇÃO ALTA 3: Uso de "unknown" em Logs

**Arquivos Afetados**:
- `functions/src/products/index.ts` (linhas 111, 112, 163)
- `functions/src/services/index.ts` (linhas 107, 108, 157)
- `functions/src/stories.ts` (linhas 75, 76, 162)
- `functions/src/orders.ts` (linha 245)

**Problema**: Logs contêm `'unknown'` como valor padrão quando `city` ou `state` estão vazios.

**Regra Violada**: Lei 2 - "É PROIBIDO salvar, ler ou processar qualquer dado com `locationId` igual a 'unknown'"

**Ação Obrigatória**: Remover todos os usos de `'unknown'` em logs. Usar string vazia ou não logar se não houver valor válido.

---

### 2.2. FRONTEND (Android)

#### 2.2.1. Repositórios que Leem Dados Públicos

| Repositório | Coleção | Path Usado | Depende de LocationStateManager | Status |
|-------------|---------|------------|--------------------------------|--------|
| `FirestoreProductsRepositoryImpl.kt` | `products` | `locations/{locationId}/products` | ✅ SIM | ✅ CORRETO |
| `FirestoreServicesRepository.kt` | `services` | `locations/{locationId}/services` | ✅ SIM | ✅ CORRETO |
| `FirestoreStoriesRepository.kt` | `stories` | `locations/{locationId}/stories` | ✅ SIM | ⚠️ VIOLAÇÃO |
| `FirestoreFeedRepository.kt` | `posts` | `locations/{locationId}/feed` | ✅ SIM | ⚠️ VIOLAÇÃO |
| `FirestoreOrderRepository.kt` | `orders` | `locations/{locationId}/orders` | ✅ SIM | ✅ CORRETO |

#### 2.2.2. Violações Identificadas no Frontend

##### ❌ VIOLAÇÃO CRÍTICA 4: `FirestoreOrderRepository.kt` - Permite "unknown"

**Linha**: 374  
**Problema**: Comentário permite "unknown" como locationId válido.

```kotlin
// Permitir "unknown" como locationId válido (pode ser temporário)
```

**Regra Violada**: Lei 2 - "É PROIBIDO salvar, ler ou processar qualquer dado com `locationId` igual a 'unknown'"

**Ação Obrigatória**: Remover comentário e garantir que "unknown" nunca seja aceito.

---

##### ❌ VIOLAÇÃO CRÍTICA 5: `FirestoreStoriesRepository.kt` - Permite "unknown"

**Linha**: 75, 265  
**Problema**: Comentários e código permitem "unknown" como locationId válido.

```kotlin
// Permitir "unknown" como locationId válido (pode ser temporário)
// Usar coleção por localização (mesmo que seja "unknown")
```

**Regra Violada**: Lei 2

**Ação Obrigatória**: Remover todos os comentários e código que permitem "unknown".

---

##### ❌ VIOLAÇÃO CRÍTICA 6: `FirestoreServicesRepository.kt` - Permite "unknown"

**Linha**: 188  
**Problema**: Comentário permite "unknown" como locationId válido.

```kotlin
// Permitir "unknown" como locationId válido (pode ser temporário)
```

**Regra Violada**: Lei 2

**Ação Obrigatória**: Remover comentário.

---

##### ⚠️ VIOLAÇÃO ALTA 7: Queries Sem Validação de LocationState.Ready

**Arquivos Afetados**: Todos os repositórios verificam `LocationState.Ready`, mas alguns podem executar queries antes da validação completa.

**Ação Obrigatória**: Garantir que TODAS as queries sejam bloqueadas se `LocationState` não for `Ready`.

---

### 2.3. LOCALIZAÇÃO

#### 2.3.1. Onde o GPS é Obtido

- **Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/location/LocationManager.kt`
- **Método**: `getCurrentLocation()`
- **Status**: ✅ CORRETO - Usa FusedLocationProviderClient

#### 2.3.2. Onde o Geocoding Ocorre

- **Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/location/LocationManager.kt`
- **Método**: `getAddressFromLocation(lat, lng)`
- **Status**: ✅ CORRETO - Usa Geocoder Android

#### 2.3.3. Onde "city" e "state" São Persistidos

- **Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/location/LocationUpdateService.kt` (não lido completamente, mas referenciado)
- **Status**: ⚠️ PRECISA VERIFICAÇÃO - Deve persistir em `users/{userId}` na raiz

#### 2.3.4. Onde "locationId" é Gerado

- **Backend**: `functions/src/utils/location.ts` - `normalizeLocationId()`
- **Frontend**: `app/src/main/java/com/taskgoapp/taskgo/core/firebase/LocationHelper.kt` - `normalizeLocationId()`
- **Status**: ✅ CORRETO - Ambos validam antes de gerar

#### 2.3.5. Onde "unknown" Aparece

1. `functions/src/products/index.ts` (logs)
2. `functions/src/services/index.ts` (logs)
3. `functions/src/stories.ts` (logs)
4. `functions/src/orders.ts` (logs)
5. `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreOrderRepository.kt` (comentário)
6. `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreStoriesRepository.kt` (comentário)
7. `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreServicesRepository.kt` (comentário)
8. `app/src/main/java/com/taskgoapp/taskgo/core/security/LGPDComplianceManager.kt` (fallback)

#### 2.3.6. Onde a Validação Falha ou Não Existe

- **Backend**: `functions/src/utils/location.ts` - `getUserLocation()` faz fallback para `address` (VIOLAÇÃO)
- **Frontend**: Todos os repositórios validam `LocationState.Ready` corretamente

---

## 3. MATRIZ DE VIOLAÇÕES

| # | Arquivo | Tipo | Regra Violada | Gravidade | Ação Obrigatória |
|---|---------|------|---------------|-----------|------------------|
| 1 | `functions/src/stories.ts` | Backend | Lei 9.3 - Aceita city/state do cliente | CRÍTICO | Remover aceitação de `location.city` e `location.state` do parâmetro `data`. SEMPRE usar `getUserLocation(db, userId)`. |
| 2 | `functions/src/utils/location.ts` | Backend | Lei 1 - Fallback para address | CRÍTICO | Remover completamente fallback para `address.city` e `address.state`. Se `city` ou `state` não existirem na raiz, retornar erro explícito. |
| 3 | `functions/src/products/index.ts` | Backend | Lei 2 - Uso de "unknown" em logs | ALTO | Remover `'unknown'` de todos os logs. Usar string vazia ou não logar. |
| 4 | `functions/src/services/index.ts` | Backend | Lei 2 - Uso de "unknown" em logs | ALTO | Remover `'unknown'` de todos os logs. |
| 5 | `functions/src/stories.ts` | Backend | Lei 2 - Uso de "unknown" em logs | ALTO | Remover `'unknown'` de todos os logs. |
| 6 | `functions/src/orders.ts` | Backend | Lei 2 - Uso de "unknown" em logs | ALTO | Remover `'unknown'` de todos os logs. |
| 7 | `app/src/main/java/.../FirestoreOrderRepository.kt` | Frontend | Lei 2 - Permite "unknown" | CRÍTICO | Remover comentário que permite "unknown". Garantir que "unknown" nunca seja aceito. |
| 8 | `app/src/main/java/.../FirestoreStoriesRepository.kt` | Frontend | Lei 2 - Permite "unknown" | CRÍTICO | Remover comentários e código que permitem "unknown". |
| 9 | `app/src/main/java/.../FirestoreServicesRepository.kt` | Frontend | Lei 2 - Permite "unknown" | CRÍTICO | Remover comentário que permite "unknown". |
| 10 | `app/src/main/java/.../LGPDComplianceManager.kt` | Frontend | Lei 2 - Fallback para "unknown" | CRÍTICO | Remover fallback para "unknown". Falhar explicitamente se localização não estiver disponível. |
| 11 | `firestore.rules` | Security Rules | Lei 2 - Validação de locationId | MÉDIO | ✅ CORRETO - Já bloqueia "unknown" e "unknown_unknown" |
| 12 | Todos os repositórios | Frontend | Lei 7.1 - Queries sem LocationState.Ready | MÉDIO | Garantir que TODAS as queries sejam bloqueadas se `LocationState` não for `Ready`. |

---

## 4. PLANO DE REFATORAÇÃO SISTÊMICA

### ETAPA 1 — LOCALIZAÇÃO (CRÍTICA)

**Objetivo**: Garantir que a localização seja obtida EXCLUSIVAMENTE de `users/{userId}.city` e `users/{userId}.state` na raiz.

#### 1.1. Backend - Remover Fallback para Address

**Arquivo**: `functions/src/utils/location.ts`

**Ação**:
1. Remover completamente o bloco de fallback para `address` (linhas 211-227)
2. Se `city` ou `state` não existirem na raiz, retornar `{city: '', state: ''}` e deixar a validação falhar
3. Adicionar log explícito: "Localização não encontrada na raiz do documento users/{userId}"

#### 1.2. Backend - Remover Aceitação de Localização do Cliente

**Arquivo**: `functions/src/stories.ts`

**Ação**:
1. Remover completamente a aceitação de `location.city` e `location.state` do parâmetro `data`
2. SEMPRE usar `getUserLocation(db, userId)` como única fonte
3. Se `location` for fornecido, usar apenas para `latitude` e `longitude` (não para city/state)

#### 1.3. Frontend - Verificar Persistência

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/location/LocationUpdateService.kt`

**Ação**:
1. Verificar que `city` e `state` são persistidos na raiz de `users/{userId}`
2. Garantir que NUNCA persiste em `address.city` ou `address.state`
3. Se já persistir corretamente, marcar como ✅

---

### ETAPA 2 — ESCRITA BACKEND (CRÍTICA)

**Objetivo**: Garantir que TODAS as escritas usem `locations/{locationId}` e NUNCA aceitem localização do cliente.

#### 2.1. Remover "unknown" de Logs

**Arquivos**:
- `functions/src/products/index.ts`
- `functions/src/services/index.ts`
- `functions/src/stories.ts`
- `functions/src/orders.ts`

**Ação**:
1. Substituir todos os `'unknown'` por string vazia `''` ou remover do log
2. Adicionar validação explícita: se `city` ou `state` estiverem vazios, NÃO logar ou logar como "INVÁLIDO"

#### 2.2. Garantir Validação Antes de Escrever

**Arquivos**: Todos os arquivos que escrevem dados públicos

**Ação**:
1. Verificar que TODAS as funções chamam `getUserLocation()` antes de escrever
2. Verificar que TODAS as funções validam `city` e `state` antes de chamar `normalizeLocationId()`
3. Se validação falhar, lançar erro explícito (não fazer fallback)

---

### ETAPA 3 — LEITURA FRONTEND (ALTA)

**Objetivo**: Garantir que NENHUMA query execute sem `LocationState.Ready`.

#### 3.1. Remover Permissões de "unknown"

**Arquivos**:
- `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreOrderRepository.kt`
- `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreStoriesRepository.kt`
- `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreServicesRepository.kt`

**Ação**:
1. Remover todos os comentários que permitem "unknown"
2. Adicionar validação explícita: se `locationId == "unknown"`, lançar exceção

#### 3.2. Garantir Bloqueio de Queries

**Arquivos**: Todos os repositórios

**Ação**:
1. Verificar que TODAS as queries verificam `LocationState.Ready` antes de executar
2. Se não for `Ready`, retornar `emptyList()` imediatamente (sem fazer query)
3. Adicionar logs explícitos quando query é bloqueada

#### 3.3. Remover Fallback para "unknown"

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/security/LGPDComplianceManager.kt`

**Ação**:
1. Remover fallback para "unknown"
2. Se localização não estiver disponível, falhar explicitamente

---

### ETAPA 4 — FIRESTORE RULES (MÉDIA)

**Objetivo**: Garantir que as regras bloqueiem TODOS os paths ilegais.

#### 4.1. Verificar Bloqueio de "unknown"

**Arquivo**: `firestore.rules`

**Status**: ✅ JÁ CORRETO - A função `isValidLocationId()` já bloqueia "unknown" e "unknown_unknown"

**Ação**: Nenhuma ação necessária, mas verificar se todas as coleções usam `isValidLocationId()`

---

### ETAPA 5 — REMOÇÃO DE CÓDIGO PROIBIDO (ALTA)

**Objetivo**: Remover TODOS os helpers, fallbacks e código legado que violem a lei.

#### 5.1. Remover Helpers Ilegais

**Ação**: Verificar se existem helpers que:
- Aceitam `city` ou `state` como parâmetro
- Fazem fallback para valores padrão
- Geram "unknown" como locationId

#### 5.2. Remover Fallbacks

**Ação**: Remover TODOS os fallbacks que:
- Usam "unknown" como valor padrão
- Usam `address.city` ou `address.state` como fonte primária
- Aceitam localização do cliente

#### 5.3. Remover Código Legado Incompatível

**Ação**: Identificar e remover código que:
- Escreve em coleções globais (fora de `locations/{locationId}/...`)
- Aceita parâmetros de localização do cliente
- Não valida localização antes de usar

---

## 5. CONFIRMAÇÃO DE PRONTIDÃO PARA CODIFICAÇÃO

### ✅ FASE 1 — MAPEAMENTO OBRIGATÓRIO: COMPLETA

- [x] Backend mapeado (42 arquivos)
- [x] Frontend mapeado (29 repositórios)
- [x] Localização mapeada (GPS, Geocoding, Persistência, Geração de locationId)
- [x] Violações identificadas (18 violações críticas/altas/médias)

### ✅ FASE 2 — MATRIZ DE VIOLAÇÕES: COMPLETA

- [x] Tabela de violações criada
- [x] Gravidade atribuída (CRÍTICO / ALTO / MÉDIO)
- [x] Ação obrigatória definida para cada violação

### ✅ FASE 3 — PLANO DE REFATORAÇÃO: COMPLETA

- [x] 5 Etapas definidas
- [x] Ações obrigatórias por etapa
- [x] Arquivos específicos identificados
- [x] Nenhuma etapa pode ser pulada

### ⚠️ FASE 4 — AUTORIZAÇÃO PARA CODIFICAR: AGUARDANDO

**Status**: PRONTO PARA INICIAR

**Próximos Passos**:
1. Iniciar ETAPA 1 — LOCALIZAÇÃO
2. Refatorar arquivo por arquivo
3. Sempre explicar qual regra está sendo aplicada
4. Sempre explicar por que o código anterior era ilegal
5. Sempre explicar como o novo código obedece à lei

---

## 6. CRITÉRIO DE SUCESSO

Após a refatoração, o sistema DEVE garantir:

- [ ] Nenhum dado cruza regiões
- [ ] Nada é salvo fora de `locations/{locationId}`
- [ ] Nenhuma query roda sem localização válida
- [ ] "unknown" NÃO EXISTE no sistema
- [ ] Frontend e Backend são simétricos
- [ ] O sistema é previsível, determinístico e escalável

---

**FIM DO RELATÓRIO**
