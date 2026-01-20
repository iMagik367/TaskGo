# 🔍 RELATÓRIO COMPLETO DE RASTREAMENTO DE LOCALIZAÇÃO
## Backend ↔ Firestore ↔ App

**Data**: ${new Date().toISOString()}
**Versão**: 1.0.95
**Objetivo**: Identificar com prova concreta onde o fluxo quebra entre localização do usuário, local onde o backend grava, e local onde o frontend lê.

---

## 🧩 ETAPA 1 — MAPEAMENTO COMPLETO DA LOCALIZAÇÃO DO USUÁRIO

### 📍 FONTE DA LOCALIZAÇÃO

#### **Backend (Cloud Functions)**

**Arquivo**: `functions/src/utils/location.ts`
**Função**: `getUserLocation(db, userId)`

**Formato bruto**: Obtido do documento Firestore `users/{userId}`
- `userData.city` (string)
- `userData.state` (string)
- Fallback: `userData.address.city` ou `userData.address.cityName`
- Fallback: `userData.address.state` ou `userData.address.stateName`

**Formato normalizado**: 
- `city`: string (vazio se não disponível)
- `state`: string (vazio se não disponível)

**Momento em que fica disponível**: Após `await getUserLocation(db, userId)`
**Persistência**: Firestore document `users/{userId}`

**Logs instrumentados**:
```
📍 LOCATION TRACE
function: getUserLocation
userId: {userId}
rawCity: {city}
rawState: {state}
hasAddress: {boolean}
addressCity: {string}
addressState: {string}
timestamp: {ISO string}
```

---

#### **Frontend (Android App)**

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/firebase/LocationHelper.kt`
**Função**: `getUserLocation(userRepository)`

**Formato bruto**: Obtido do `UserRepository.observeCurrentUser()`
- `user.city` (String?)
- `user.state` (String?) — Adicionado na versão 88

**Formato normalizado**: 
- `city`: String (vazio se null ou blank)
- `state`: String (vazio se null ou blank)

**Momento em que fica disponível**: Após `userRepository.observeCurrentUser().first()`
**Persistência**: Room Database (cache local) + Firestore (fonte de verdade)

**Logs instrumentados**:
```
📍 FRONTEND LOCATION TRACE
Function: getUserLocation
RawCity: {city ou "null"}
RawState: {state ou "null"}
City: {normalized city}
State: {normalized state}
LocationId: {locationId}
Timestamp: {Date}
```

---

### 📍 NORMALIZAÇÃO DE LOCALIZAÇÃO

#### **Backend (Cloud Functions)**

**Arquivo**: `functions/src/utils/location.ts`
**Função**: `normalizeLocationId(city, state)`

**Regra de normalização**:
1. `toLowerCase()`
2. `trim()`
3. `normalize('NFD')` - Unicode normalization
4. Remove acentos: `.replace(/[\u0300-\u036f]/g, '')`
5. Substitui caracteres especiais por underscore: `.replace(/[^a-z0-9]/g, '_')`
6. Remove underscores duplicados: `.replace(/_+/g, '_')`
7. Remove underscores no início/fim: `.replace(/^_|_$/g, '')`
8. Se ambos vazios → `"unknown"`
9. Se apenas city vazio → `normalizedState`
10. Se apenas state vazio → `normalizedCity`
11. Senão → `"${normalizedCity}_${normalizedState}"`

**Exemplo**:
- Input: `city="Cascavel"`, `state="PR"`
- Output: `"cascavel_pr"`

**Logs instrumentados**:
```
📍 LOCATION TRACE
function: normalizeLocationId
rawCity: {city}
rawState: {state}
normalizedCity: {normalized}
normalizedState: {normalized}
locationId: {final locationId}
timestamp: {ISO string}
```

---

#### **Frontend (Android App)**

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/firebase/LocationHelper.kt`
**Função**: `normalizeLocationId(city, state)`

**Regra de normalização** (IDÊNTICA ao backend):
1. `lowercase()`
2. `trim()`
3. `Normalizer.normalize(NFD)`
4. Remove acentos: `.replace(Regex("[\\u0300-\\u036F]"), "")`
5. Substitui caracteres especiais por underscore: `.replace(Regex("[^a-z0-9]"), "_")`
6. Remove underscores duplicados: `.replace(Regex("_+"), "_")`
7. Remove underscores no início/fim: `.replace(Regex("^_|_\$"), "")`
8. Se ambos vazios → `"unknown"`
9. Se apenas city vazio → `normalizedState`
10. Se apenas state vazio → `normalizedCity`
11. Senão → `"${normalizedCity}_${normalizedState}"`

**Exemplo**:
- Input: `city="Cascavel"`, `state="PR"`
- Output: `"cascavel_pr"`

**Logs instrumentados**:
```
📍 FRONTEND LOCATION TRACE
Function: normalizeLocationId
RawCity: {city}
RawState: {state}
NormalizedCity: {normalized}
NormalizedState: {normalized}
LocationId: {final locationId}
Timestamp: {Date}
```

---

### 📍 GERAÇÃO DO FIRESTORE PATH

#### **Backend (Cloud Functions)**

**Arquivo**: `functions/src/utils/location.ts`
**Função**: `getLocationCollection(db, collection, city, state)`

**Estrutura gerada**:
```
locations/{locationId}/{collection}/{documentId}
```

**Exemplo**:
- Input: `city="Cascavel"`, `state="PR"`, `collection="products"`
- Output Path: `locations/cascavel_pr/products`

**Logs instrumentados**: 
- Via `normalizeLocationId()` (já logado acima)

---

#### **Frontend (Android App)**

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/core/firebase/LocationHelper.kt`
**Função**: `getLocationCollection(firestore, collection, city, state)`

**Estrutura gerada**:
```
locations/{locationId}/{collection}/{documentId}
```

**Exemplo**:
- Input: `city="Cascavel"`, `state="PR"`, `collection="products"`
- Output Path: `locations/cascavel_pr/products`

**Logs instrumentados**:
```
📍 FRONTEND LOCATION TRACE
Function: getLocationCollection
City: {city}
State: {state}
LocationId: {locationId}
Firestore Path: locations/{locationId}/{collection}
Collection: {collection}
Timestamp: {Date}
```

---

## 🧩 ETAPA 2 — INSTRUMENTAÇÃO DO BACKEND (PROVA)

### 📍 Cloud Functions que Escrevem Dados

#### **1. createProduct**
**Arquivo**: `functions/src/products/index.ts`
**Função**: `createProduct`

**Momento da escrita**:
```typescript
const locationProductsCollection = getLocationCollection(db, 'products', city || 'unknown', state || 'unknown');
const productRef = await locationProductsCollection.add(productData);
```

**Logs instrumentados**:
```
📍 LOCATION TRACE
function: createProduct
userId: {userId}
city: {city ou "unknown"}
state: {state ou "unknown"}
locationId: {locationId}
firestorePath: locations/{locationId}/products
rawCity: {city}
rawState: {state}
timestamp: {ISO string}

📍 BACKEND WRITE PROOF
function: createProduct
productId: {productId}
actualFirestorePath: locations/{locationId}/products/{productId}
collectionId: {collection.id}
documentId: {productId}
timestamp: {ISO string}
```

---

#### **2. createStory**
**Arquivo**: `functions/src/stories.ts`
**Função**: `createStory`

**Momento da escrita**:
```typescript
const locationStoriesCollection = getLocationCollection(db, 'stories', storyCity || 'unknown', storyState || 'unknown');
const storyRef = await locationStoriesCollection.add(storyData);
```

**Logs instrumentados**:
```
📍 LOCATION TRACE
function: createStory
userId: {userId}
city: {storyCity ou "unknown"}
state: {storyState ou "unknown"}
locationId: {locationId}
firestorePath: locations/{locationId}/stories
rawCity: {storyCity}
rawState: {storyState}
timestamp: {ISO string}

📍 BACKEND WRITE PROOF
function: createStory
storyId: {storyId}
actualFirestorePath: locations/{locationId}/stories/{storyId}
collectionId: {collection.id}
documentId: {storyId}
timestamp: {ISO string}
```

---

#### **3. onServiceOrderCreated**
**Arquivo**: `functions/src/orders.ts`
**Função**: `onServiceOrderCreated` (trigger)

**Momento da escrita**:
```typescript
const locationOrdersCollection = getLocationCollection(db, COLLECTIONS.ORDERS, finalCity, finalState);
const orderRef = await locationOrdersCollection.add(orderData);
```

**Logs instrumentados**:
```
📍 LOCATION TRACE
function: onServiceOrderCreated
userId: {userId}
city: {finalCity ou "unknown"}
state: {finalState ou "unknown"}
locationId: {locationId}
firestorePath: locations/{locationId}/orders
rawCity: {finalCity}
rawState: {finalState}
originalLocation: {location string}
parsedCity: {city}
parsedState: {state}
timestamp: {ISO string}

📍 BACKEND WRITE PROOF
function: onServiceOrderCreated (specific service / open order)
orderId: {orderId}
actualFirestorePath: locations/{locationId}/orders/{orderId}
collectionId: {collection.id}
documentId: {orderId}
timestamp: {ISO string}
```

---

## 🧩 ETAPA 3 — INSTRUMENTAÇÃO DO FRONTEND (CRÍTICO)

### 📍 Queries Firestore que Leem Dados

#### **1. observeProducts**
**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreProductsRepositoryImpl.kt`
**Função**: `observeProducts()`

**Momento da leitura**: Antes de configurar `addSnapshotListener`

**Logs instrumentados ANTES da query**:
```
📍 FRONTEND LOCATION TRACE
Function: observeProducts
City: {currentCity}
State: {currentState}
LocationId: {locationId}
Firestore Path: locations/{locationId}/products
Timestamp: {Date}
```

**OU se fallback para global**:
```
📍 FRONTEND LOCATION TRACE
Function: observeProducts
City: {currentCity ou "null"}
State: {currentState}
LocationId: unknown (fallback)
Firestore Path: products (global)
Timestamp: {Date}
```

---

#### **2. observeStories**
**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreStoriesRepository.kt`
**Função**: `observeStories(currentUserId, radiusKm, userLocation)`

**Momento da leitura**: Antes de configurar `addSnapshotListener`

**Logs instrumentados ANTES da query**:
```
📍 FRONTEND LOCATION TRACE
Function: observeStories
City: {currentCity}
State: {currentState}
LocationId: {locationId}
Firestore Path: locations/{locationId}/stories
Timestamp: {Date}
```

**OU se fallback para global**:
```
📍 FRONTEND LOCATION TRACE
Function: observeStories
City: {currentCity ou "null"}
State: {currentState}
LocationId: unknown (fallback)
Firestore Path: stories (global)
Timestamp: {Date}
```

---

## 🧩 ETAPA 4 — SNAPSHOT PROOF (SEM ACHISMO)

### 📍 Logs de Snapshot

#### **observeProducts - Snapshot**
**Logs instrumentados NO snapshot**:
```
📍 FRONTEND SNAPSHOT PROOF
Collection path: {collectionToUse.path}
Snapshot empty: {boolean}
Snapshot size: {int}
Documents count: {int}

📍 FRONTEND SNAPSHOT PROOF - Document {index}
Doc ID: {doc.id}
Doc data keys: {keys.joinToString(", ")}
Doc has createdAt: {boolean}
Doc has active: {value}
Doc has status: {value}
```

---

#### **observeStories - Snapshot**
**Logs instrumentados NO snapshot**:
```
📍 FRONTEND SNAPSHOT PROOF
Collection path: {collectionToUse.path}
Snapshot empty: {boolean}
Snapshot size: {int}
Documents count: {int}

📍 FRONTEND SNAPSHOT PROOF - Document {index}
Doc ID: {doc.id}
Doc data keys: {keys.joinToString(", ")}
Doc has expiresAt: {boolean}
Doc has createdAt: {boolean}
```

---

## 🧩 ETAPA 5 — VALIDAÇÕES AUTOMÁTICAS

### 📍 Função Utilitária Única

**Backend**: `functions/src/utils/location.ts` → `normalizeLocationId()`
**Frontend**: `app/src/main/java/com/taskgoapp/taskgo/core/firebase/LocationHelper.kt` → `normalizeLocationId()`

**Status**: ✅ **IMPLEMENTAÇÕES IDÊNTICAS** - Mesma lógica de normalização

**Exemplos de normalização**:

| Input City | Input State | Output LocationId |
|------------|-------------|-------------------|
| "São Paulo" | "SP" | `sao_paulo_sp` |
| "Foz do Iguaçu" | "PR" | `foz_do_iguacu_pr` |
| "Cascavel" | "PR" | `cascavel_pr` |
| "Osasco" | "SP" | `osasco_sp` |
| "" | "" | `unknown` |
| "Rio de Janeiro" | "" | `rio_de_janeiro` |
| "" | "SP" | `sp` |

---

## 🧩 ETAPA 6 — COMPARAÇÃO REAL (PROVA FINAL)

### 📍 Tabela Comparativa Backend vs Frontend

**Como usar esta tabela**: Após executar o app e criar/ler dados, comparar os logs:

| Fonte | city | state | locationId | path |
|-------|------|-------|------------|------|
| Backend (createProduct) | {ver log `📍 LOCATION TRACE`} | {ver log} | {ver log} | `locations/{locationId}/products` |
| Frontend (observeProducts) | {ver log `📍 FRONTEND LOCATION TRACE`} | {ver log} | {ver log} | `locations/{locationId}/products` |

**🚨 Se UMA letra divergir, o sistema quebra.**

---

## 🧩 ETAPA 7 — CONCLUSÃO (APÓS TESTES)

### 📍 Hipóteses a Validar

Após coletar os logs, o relatório DEVE concluir com UMA das opções abaixo:

#### ✅ **A** - Localização não disponível no momento da query
**Sintomas nos logs**:
- Frontend loga `City: null` ou `State: ""`
- Frontend usa fallback `products (global)` ou `stories (global)`
- Backend loga `city: "Cascavel"`, `state: "PR"` com `locationId: "cascavel_pr"`

**Causa**: O app está consultando antes da localização estar pronta.

---

#### ✅ **B** - Backend e frontend usam locationId DIFERENTES
**Sintomas nos logs**:
- Backend `📍 LOCATION TRACE` mostra `locationId: "cascavel_pr"`
- Frontend `📍 FRONTEND LOCATION TRACE` mostra `LocationId: "Cascavel_PR"` ou `"cascavel_parana"`

**Causa**: Normalização divergente ou campos city/state diferentes.

---

#### ✅ **C** - Query está correta, mas filtros excluem dados
**Sintomas nos logs**:
- Frontend `📍 FRONTEND LOCATION TRACE` mostra path correto: `locations/cascavel_pr/products`
- Frontend `📍 FRONTEND SNAPSHOT PROOF` mostra `Snapshot size: 0` ou `Documents count: 0`
- Backend `📍 BACKEND WRITE PROOF` mostra `actualFirestorePath: locations/cascavel_pr/products/{productId}`

**Causa**: Filtros `whereEqualTo("active", true)` ou `whereEqualTo("status", "active")` estão excluindo documentos.

---

#### ✅ **D** - Dado existe, mas parsing falha
**Sintomas nos logs**:
- Frontend `📍 FRONTEND SNAPSHOT PROOF` mostra `Documents count: 5`
- Frontend não exibe produtos na UI

**Causa**: Erro no parsing de documentos (ex: `imageUrls` vs `images`, `createdAt` formato incorreto).

---

## 📋 CHECKLIST DE VALIDAÇÃO

- [ ] Backend logs `📍 LOCATION TRACE` estão sendo gerados em `createProduct`
- [ ] Backend logs `📍 LOCATION TRACE` estão sendo gerados em `createStory`
- [ ] Backend logs `📍 LOCATION TRACE` estão sendo gerados em `onServiceOrderCreated`
- [ ] Backend logs `📍 BACKEND WRITE PROOF` mostram o path REAL onde dados foram gravados
- [ ] Frontend logs `📍 FRONTEND LOCATION TRACE` estão sendo gerados ANTES de queries Firestore
- [ ] Frontend logs `📍 FRONTEND SNAPSHOT PROOF` estão sendo gerados NO snapshot
- [ ] Logs mostram `locationId` idêntico entre backend e frontend
- [ ] Logs mostram `firestorePath` idêntico entre backend e frontend

---

## 🚀 PRÓXIMOS PASSOS

1. **Executar o app** e criar produtos/stories/orders
2. **Coletar logs** do Firebase Functions (backend) e Logcat (frontend)
3. **Comparar valores** usando a tabela comparativa acima
4. **Identificar divergências** usando as hipóteses A, B, C ou D
5. **Corrigir a causa raiz** baseado na conclusão

---

**NOTA**: Este relatório documenta apenas a instrumentação. As correções serão aplicadas após a identificação da causa raiz através dos logs.
