# 🌍 MODELO CANÔNICO DO SISTEMA TASKGO

**Versão**: 1.0  
**Data**: 2024-01-15  
**Status**: LEI MÁXIMA DO SISTEMA

---

## ⚠️ DECLARAÇÃO DE AUTORIDADE

Este documento é a **LEI MÁXIMA** do TaskGo.  
Qualquer implementação que viole estas regras é um **BUG DE ARQUITETURA**.  
Não existem exceções. Não existem "casos especiais".  
Se uma regra não puder ser cumprida, o sistema **DEVE FALHAR EXPLICITAMENTE**.

---

## 1. VISÃO GERAL DO MUNDO TASKGO

### 1.1. Princípio Fundamental

**O TaskGo é um sistema 100% baseado em região geográfica.**

- Todo dado público pertence a uma região (cidade + estado).
- Não existe dado sem região válida.
- Não existe feed global.
- Não existe "default" ou "unknown".
- O usuário NUNCA escolhe sua localização.
- A localização é SEMPRE automática via GPS.

### 1.2. Premissas Imutáveis

1. **Firestore é a base de dados principal.**
2. **Cloud Functions são a autoridade de escrita** para dados públicos.
3. **Frontend é consumidor** de dados regionais.
4. **Localização é obtida via GPS** (FusedLocationProviderClient).
5. **Geocoding é obrigatório** para converter GPS em cidade/estado.
6. **Validação é obrigatória** antes de qualquer persistência.

---

## 2. LEIS FUNDAMENTAIS DO SISTEMA

### Lei 1: Localização Obtida no Momento da Operação
**A localização do usuário é obtida EXCLUSIVAMENTE via GPS no momento de cada operação (criar post, produto, serviço, story, ordem). O sistema NÃO depende de `city` e `state` estarem salvos no perfil do usuário. Os campos `city` e `state` no documento `users/{userId}` são OPCIONAIS e usados apenas para cache/otimização, não são obrigatórios para operações.**

### Lei 2: Proibição de "unknown"
**É PROIBIDO salvar, ler ou processar qualquer dado com `locationId` igual a "unknown", "unknown_unknown" ou string vazia. O sistema DEVE falhar explicitamente se isso ocorrer.**

### Lei 3: Validação Obrigatória
**Toda localização DEVE ser validada ANTES de ser persistida ou utilizada. Se a validação falhar, a operação DEVE ser abortada com erro explícito.**

### Lei 4: Organização por Região
**Todos os dados públicos (produtos, serviços, ordens, posts, stories) DEVEM ser salvos exclusivamente em `locations/{locationId}/{collection}/{documentId}`. Não existe outra estrutura permitida.**

### Lei 5: Autoridade de Escrita
**Apenas Cloud Functions podem escrever dados públicos em `locations/{locationId}/...`. O frontend NUNCA escreve diretamente nessas coleções.**

### Lei 6: Sincronização Determinística
**Frontend e Backend DEVEM usar EXATAMENTE as mesmas regras de validação, normalização e geração de `locationId`. Não existe divergência permitida.**

---

## 3. OBTENÇÃO DE LOCALIZAÇÃO NO MOMENTO DA OPERAÇÃO

### 3.1. Definição

**A localização do usuário é obtida EXCLUSIVAMENTE via GPS no momento de cada operação. O sistema NÃO depende de campos salvos no perfil.**

### 3.2. Fluxo de Obtenção de Localização

**Para OPERAÇÕES DE ESCRITA (criar post, produto, serviço, story, ordem)**:
1. Frontend obtém GPS via `LocationManager.getCurrentLocation()`
2. Frontend faz geocoding via `LocationManager.getAddressFromLocation(lat, lng)` → obtém `city` e `state`
3. Frontend valida `city` e `state` usando `LocationValidator`
4. Frontend normaliza `locationId` usando `LocationHelper.normalizeLocationId(city, state)`
5. Frontend envia GPS (latitude, longitude) + city/state para Cloud Function
6. Backend valida GPS e city/state recebidos
7. Backend salva em `locations/{locationId}/{collection}/{documentId}`

**Para OPERAÇÕES DE LEITURA (visualizar feed, produtos, serviços)**:
1. Frontend obtém GPS via `LocationManager.getCurrentLocation()`
2. Frontend faz geocoding → obtém `city` e `state`
3. Frontend normaliza `locationId`
4. Frontend lê de `locations/{locationId}/{collection}`

### 3.3. Campos Opcionais no Perfil

```
users/{userId} {
  city: string,      // OPCIONAL - usado apenas para cache/otimização
  state: string,     // OPCIONAL - usado apenas para cache/otimização
  // ... outros campos
}
```

**IMPORTANTE**: Se `city` e `state` não estiverem no perfil, o sistema DEVE funcionar normalmente obtendo GPS no momento da operação.

### 3.3. Quando é Considerada Válida

Uma localização é válida SE E SOMENTE SE:

1. `city` existe, não é null, não é string vazia, não é "unknown" ou qualquer valor genérico.
2. `state` existe, não é null, não é string vazia, não é "unknown" ou qualquer valor genérico.
3. `city` tem no mínimo 2 caracteres após trim.
4. `state` tem exatamente 2 caracteres após trim e uppercase.
5. `state` é uma sigla válida do Brasil (AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO).
6. `city` não contém caracteres inválidos (apenas letras, espaços, hífens, apóstrofos e acentos).
7. `city` não é um valor genérico (lista definida na seção 5.3).

### 3.4. Quando é Considerada Inválida

Uma localização é inválida se QUALQUER uma das condições acima falhar.

### 3.5. Quando o Sistema Deve Bloquear Operações

O sistema DEVE bloquear uma operação APENAS se:

1. GPS não está disponível (permissão negada, GPS desligado, etc.)
2. Geocoding falhou após 3 tentativas
3. `city` ou `state` obtidos do geocoding são inválidos (não passam na validação)
4. `locationId` normalizado é "unknown" ou string vazia

**O sistema NÃO deve bloquear se**:
- `users/{userId}` não existe (não é necessário para operações)
- Campos `city` ou `state` não existem no perfil (GPS é obtido no momento)
- Campos `city` ou `state` estão vazios no perfil (GPS é obtido no momento)

**Bloqueio significa**: retornar erro explícito, NÃO criar dados, NÃO fazer fallback, NÃO usar valores padrão.

---

## 4. CICLO DE VIDA DA LOCALIZAÇÃO

### 4.1. Fase 1: Inicialização do App

**Quando**: App inicia (`TaskGoApp.onCreate()`)

**Ações**:
- Inicializar Firebase.
- NÃO iniciar monitoramento de localização ainda.

**Estado**: Localização desconhecida.

### 4.2. Fase 2: Autenticação do Usuário

**Quando**: Usuário faz login ou já está autenticado (SplashViewModel ou LoginViewModel)

**Ações**:
1. Iniciar `LocationUpdateService.startLocationMonitoring()` (opcional - para atualizar perfil em background).
2. NÃO bloquear navegação aguardando localização no perfil.
3. Localização será obtida no momento de cada operação.

**Estado**: Pronto para operações (localização será obtida quando necessário).

### 4.3. Fase 3: Obtenção de GPS

**Quando**: `LocationUpdateService` está ativo

**Ações**:
1. `LocationManager.getCurrentLocation()` obtém coordenadas GPS via FusedLocationProviderClient.
2. Validar qualidade GPS (não é (0,0), está no Brasil, etc.).
3. Se inválido, tentar novamente (até 3 tentativas com delay).

**Estado**: GPS obtido (ou falhou após 3 tentativas).

### 4.4. Fase 4: Geocoding

**Quando**: GPS válido obtido

**Ações**:
1. `LocationManager.getAddressFromLocation(lat, lng)` chama Geocoder.
2. Extrair `address.locality` → `city`.
3. Extrair `address.adminArea` → `state`.
4. Se `locality` vazio, tentar `address.subLocality` ou `address.featureName`.
5. Se `adminArea` vazio, tentar `address.subAdminArea`.
6. Tentar até 3 vezes com delay crescente se falhar.

**Estado**: Endereço obtido (ou falhou após 3 tentativas).

### 4.5. Fase 5: Validação

**Quando**: Endereço obtido do Geocoder

**Ações**:
1. `LocationValidator.validateAndNormalizeCity(city)` valida e normaliza city.
2. `LocationValidator.validateAndNormalizeState(state)` valida e normaliza state.
3. Se QUALQUER validação falhar, abortar e tentar novamente (até 3 tentativas).

**Estado**: Localização validada (ou falhou após 3 tentativas).

### 4.6. Fase 6: Uso na Operação

**Quando**: Localização validada

**Ações**:
1. **Para ESCRITA**: Frontend envia GPS (latitude, longitude) + city/state para Cloud Function. Backend valida e salva em `locations/{locationId}/{collection}`.
2. **Para LEITURA**: Frontend usa GPS obtido para determinar `locationId` e ler de `locations/{locationId}/{collection}`.
3. **OPCIONAL**: Se `users/{userId}.city` e `users/{userId}.state` estiverem vazios ou diferentes, atualizar em background (não bloqueia operação).

**Estado**: Operação executada com localização obtida no momento.

### 4.7. Fase 7: Cache Opcional no Perfil

**Quando**: Localização validada e operação executada

**Ações**:
1. **OPCIONAL**: `LocationUpdateService` pode atualizar `users/{userId}` com `city` e `state` em background.
2. **OPCIONAL**: `LocationStateManager` pode emitir `LocationState.Ready` quando detectar `city` e `state` no perfil (para otimização de queries).
3. **IMPORTANTE**: Operações NÃO dependem desta fase - GPS é sempre obtido no momento da operação.

**Estado**: Cache atualizado (opcional, não obrigatório).

### 4.8. Fase 8: Monitoramento Contínuo

**Quando**: Localização já está pronta

**Ações**:
1. `LocationUpdateService` continua observando mudanças de GPS.
2. Se GPS mudar significativamente (> 5km), repetir fases 3-7.
3. Se `city` ou `state` mudarem, atualizar `users/{userId}` automaticamente.

**Estado**: Monitoramento ativo.

---

## 5. CONTRATO CANÔNICO DE LOCALIZAÇÃO

### 5.1. Definição de `city`

**Tipo**: `string`  
**Localização**: Raiz do documento `users/{userId}`  
**Formato**: Texto livre, mas validado  
**Exemplos válidos**: "São Paulo", "Rio de Janeiro", "Goiânia", "Foz do Iguaçu"  
**Exemplos inválidos**: "unknown", "", null, "cidade", "city"

**Regras de Validação**:
- DEVE existir.
- DEVE ter no mínimo 2 caracteres após trim.
- DEVE conter apenas letras (incluindo acentos), espaços, hífens e apóstrofos.
- NÃO PODE ser um valor genérico (lista na seção 5.3).
- NÃO PODE ser null ou string vazia.

### 5.2. Definição de `state`

**Tipo**: `string`  
**Localização**: Raiz do documento `users/{userId}`  
**Formato**: Exatamente 2 caracteres, maiúsculas  
**Exemplos válidos**: "SP", "RJ", "GO", "PR"  
**Exemplos inválidos**: "unknown", "", null, "São Paulo", "SPA"

**Regras de Validação**:
- DEVE existir.
- DEVE ter exatamente 2 caracteres após trim e uppercase.
- DEVE ser uma sigla válida do Brasil (27 estados + DF).
- NÃO PODE ser null ou string vazia.
- NÃO PODE ser "unknown" ou qualquer valor genérico.

### 5.3. Lista de Valores Genéricos Proibidos

Os seguintes valores são PROIBIDOS para `city` ou `state`:

- "unknown"
- "desconhecido"
- "null"
- "undefined"
- "n/a"
- "na"
- "cidade"
- "city"
- "local"
- "location"
- "endereço"
- "address"
- "default"
- "legacy"
- ""

**Qualquer tentativa de usar estes valores DEVE resultar em erro explícito.**

### 5.4. Definição de `locationId`

**Tipo**: `string`  
**Formato**: `{normalizedCity}_{normalizedState}`  
**Exemplos válidos**: "sao_paulo_sp", "rio_de_janeiro_rj", "goiania_go"  
**Exemplos inválidos**: "unknown", "unknown_unknown", "", "sao_paulo", "sp"

**Regras de Geração**:
1. `city` e `state` DEVEM ser validados ANTES de gerar `locationId`.
2. Se validação falhar, `locationId` NÃO PODE ser gerado (erro explícito).
3. Normalização:
   - Converter para lowercase.
   - Remover acentos (NFD normalization).
   - Substituir caracteres não alfanuméricos por underscore.
   - Remover underscores duplicados.
   - Remover underscores no início e fim.
4. Formato final: `{normalizedCity}_{normalizedState}`.

**Proibições**:
- NUNCA gerar `locationId` se `city` ou `state` forem inválidos.
- NUNCA retornar "unknown" como `locationId`.
- NUNCA usar fallback genérico.

---

## 6. MODELO CANÔNICO DE FIRESTORE

### 6.1. Estrutura Obrigatória

**TODOS os dados públicos DEVEM ser salvos em:**

```
locations/{locationId}/{collection}/{documentId}
```

**Onde**:
- `locationId` é gerado conforme seção 5.4.
- `collection` é uma das: `products`, `services`, `orders`, `posts`, `stories`.
- `documentId` é o ID único do documento.

### 6.2. Exemplos Reais

```
locations/sao_paulo_sp/products/abc123
locations/rio_de_janeiro_rj/services/def456
locations/goiania_go/orders/ghi789
locations/cascavel_pr/posts/jkl012
locations/foz_do_iguacu_pr/stories/mno345
```

### 6.3. Estrutura do Documento de Usuário

```
users/{userId} {
  city: "São Paulo",           // OPCIONAL na raiz - usado apenas para cache/otimização
  state: "SP",                 // OPCIONAL na raiz - usado apenas para cache/otimização
  displayName: "...",
  email: "...",
  // ... outros campos
  // NÃO incluir city/state dentro de address (evita duplicação)
}
```

**IMPORTANTE**: Se `city` e `state` não estiverem no perfil, o sistema DEVE funcionar normalmente obtendo GPS no momento de cada operação.

### 6.4. O Que é Proibido Gravar

**É PROIBIDO gravar**:
- Dados em coleções globais (ex: `products/...`, `services/...`).
- Dados com `locationId` igual a "unknown", "unknown_unknown" ou string vazia.
- Dados sem `city` e `state` explícitos no documento.
- Dados em paths diferentes de `locations/{locationId}/{collection}/...`.

### 6.5. O Que NUNCA Pode Existir no Banco

**NUNCA pode existir**:
- Documento em `locations/unknown/...`.
- Documento em `locations/unknown_unknown/...`.
- Documento em `locations//...` (locationId vazio).
- Documento em coleções globais (fora de `locations/...`).
- Usuário com `city` ou `state` igual a "unknown" ou vazio.

**Se qualquer um destes existir, é um BUG DE ARQUITETURA que DEVE ser corrigido.**

---

## 7. LEITURA DE DADOS

### 7.1. Quem Pode Ler

**Frontend**:
- Repositórios (ProductsRepository, ServicesRepository, OrdersRepository, StoriesRepository, FeedRepository).
- Obtêm GPS no momento da query via `LocationManager.getCurrentLocation()`.
- Fazem geocoding para obter `city` e `state`.
- Normalizam `locationId` e leem de `locations/{locationId}/{collection}`.
- **OPCIONAL**: Podem usar `LocationState.Ready` como cache/otimização quando disponível.

**Backend**:
- Cloud Functions que processam notificações, relatórios, etc.
- Recebem `locationId` do frontend ou obtêm via `getUserLocationId(db, userId)` (se disponível no perfil).

### 7.2. Quando Pode Ler

**Frontend**:
- SEMPRE pode ler - obtém GPS no momento da query.
- Se GPS não disponível (permissão negada, GPS desligado), retorna `emptyList()` e mostra erro.
- **OPCIONAL**: Se `LocationState.Ready` estiver disponível, pode usar como cache (não obrigatório).

**Backend**:
- Sempre pode ler (tem acesso direto ao Firestore).
- DEVE validar `locationId` antes de fazer query.

### 7.3. O Que Acontece Se GPS Não Estiver Disponível

**Frontend**:
- Retorna `emptyList()`.
- UI mostra erro: "Localização não disponível. Ative o GPS e tente novamente."
- NÃO tenta fazer query no Firestore.
- Usuário pode tentar novamente quando GPS estiver disponível.

**Backend**:
- Se receber `locationId` inválido do frontend, a operação DEVE falhar com erro explícito.
- NÃO faz fallback.
- NÃO usa valores padrão.

### 7.4. O Que Acontece Se Localização For Inválida

**Frontend**:
- `LocationStateManager` emite `LocationState.Error(message)`.
- Repositórios retornam `emptyList()`.
- UI mostra erro ao usuário.

**Backend**:
- `normalizeLocationId()` lança exceção.
- Operação é abortada.
- Erro é retornado ao cliente.

### 7.5. Path de Leitura Obrigatório

**TODAS as queries DEVEM usar:**

```
firestore.collection("locations").document(locationId).collection(collection)
```

**NÃO existe outro path permitido para dados públicos.**

---

## 8. ESCRITA DE DADOS

### 8.1. Quem Escreve

**Cloud Functions são a AUTORIDADE DE ESCRITA** para dados públicos:
- `createProduct` → escreve em `locations/{locationId}/products`
- `createService` → escreve em `locations/{locationId}/services`
- `createOrder` → escreve em `locations/{locationId}/orders`
- `createStory` → escreve em `locations/{locationId}/stories`
- `createPost` → escreve em `locations/{locationId}/posts` (via frontend, mas valida no backend)

**Frontend**:
- NUNCA escreve diretamente em `locations/{locationId}/...`.
- Pode escrever em subcoleções privadas (ex: `users/{userId}/posts`).
- Posts são escritos pelo frontend, mas a localização é validada e o path é construído corretamente.

### 8.2. De Onde Vem a Localização

**SEMPRE do GPS obtido no momento da operação:**

1. **Frontend**:
   - Obtém GPS via `LocationManager.getCurrentLocation()`.
   - Faz geocoding via `LocationManager.getAddressFromLocation(lat, lng)` → obtém `city` e `state`.
   - Valida `city` e `state` usando `LocationValidator`.
   - Normaliza `locationId` usando `LocationHelper.normalizeLocationId(city, state)`.
   - Envia GPS (latitude, longitude) + city/state para Cloud Function.

2. **Backend (Cloud Function)**:
   - Recebe GPS (latitude, longitude) + city/state do frontend.
   - Valida GPS e city/state recebidos.
   - Se válido, usa para determinar `locationId` e salvar em `locations/{locationId}/{collection}`.
   - Se inválido, retorna erro (não faz fallback).

**OPCIONAL - Fallback para perfil (apenas se GPS não disponível)**:
- Se GPS não disponível, backend pode tentar `getUserLocation(db, userId)` do perfil.
- Mas GPS do frontend tem PRIORIDADE sobre perfil.

**NUNCA**:
- Aceitar `city` ou `state` como parâmetro do cliente SEM GPS correspondente.
- Usar `address.city` ou `address.state` como fonte primária.
- Fazer fallback para valores padrão.

### 8.3. O Que Acontece Se GPS Não Estiver Disponível

**Se GPS não estiver disponível (permissão negada, GPS desligado, etc.):**

1. Frontend mostra erro: "Localização não disponível. Ative o GPS e tente novamente."
2. Operação é ABORTADA.
3. NÃO cria dados.
4. NÃO faz fallback.
5. Usuário deve ativar GPS e tentar novamente.

**Se geocoding falhar após GPS obtido:**

1. Frontend tenta até 3 vezes com delay crescente.
2. Se falhar após 3 tentativas, mostra erro: "Não foi possível determinar sua localização. Tente novamente."
3. Operação é ABORTADA.
4. NÃO cria dados.

### 8.4. Como Erros São Tratados

**Validação falha**:
- `normalizeLocationId()` lança exceção.
- Cloud Function retorna `HttpsError('failed-precondition', message)`.
- Frontend mostra erro ao usuário.

**Localização não encontrada**:
- `getUserLocation()` retorna `{city: '', state: ''}`.
- Cloud Function valida e retorna erro antes de criar dados.

**Geocoding falha**:
- `LocationUpdateService` tenta até 3 vezes.
- Se falhar, localização não é atualizada.
- Usuário não pode criar dados até localização ser obtida.

---

## 9. SINCRONIZAÇÃO FRONTEND ↔ BACKEND

### 9.1. Quem Confia em Quem

**Backend valida GPS e city/state recebidos do Frontend.**

- Backend recebe GPS (latitude, longitude) + city/state do frontend.
- Backend valida GPS (não é (0,0), está no Brasil, etc.).
- Backend valida city/state recebidos usando `validateCityAndState()`.
- Backend NUNCA aceita city/state SEM GPS correspondente.
- Backend SEMPRE valida antes de usar.

**Frontend obtém GPS e envia para Backend.**

- Frontend obtém GPS via `LocationManager.getCurrentLocation()`.
- Frontend faz geocoding para obter city/state.
- Frontend valida city/state antes de enviar.
- Frontend envia GPS + city/state para Cloud Function.
- Backend valida e rejeita se inválido.

### 9.2. O Que é Recalculado

**`locationId` é SEMPRE recalculado:**

- Frontend: `LocationHelper.normalizeLocationId(city, state)` recalcula sempre.
- Backend: `normalizeLocationId(city, state)` recalcula sempre.
- NUNCA é armazenado como campo no documento (exceto para logs/traces).

**Validação é SEMPRE refeita:**

- Frontend: `LocationValidator` valida sempre antes de usar.
- Backend: `validateCityAndState()` valida sempre antes de usar.
- NUNCA confia em validação anterior.

### 9.3. O Que Nunca é Aceito do Cliente

**NUNCA aceitar do cliente (frontend):**
- `city` como parâmetro.
- `state` como parâmetro.
- `locationId` como parâmetro.
- Qualquer tentativa de definir localização manualmente.

**Backend SEMPRE obtém do Firestore:**
- `getUserLocation(db, userId)` busca de `users/{userId}`.
- Valida antes de usar.
- Rejeita se inválido.

---

## 10. PROIBIÇÕES ABSOLUTAS

### 10.1. Proibições de Dados

**É PROIBIDO:**
1. Salvar dados com `locationId` igual a "unknown", "unknown_unknown" ou string vazia.
2. Salvar dados em coleções globais (fora de `locations/{locationId}/...`).
3. Salvar dados sem `city` e `state` explícitos no documento.
4. Aceitar `city` ou `state` como parâmetro do cliente.
5. Usar fallback genérico para localização.
6. Armazenar `locationId` como campo no documento (exceto para logs).
7. Fazer queries em coleções globais.
8. Criar dados sem localização válida.

### 10.2. Proibições de Comportamento

**É PROIBIDO:**
1. Continuar operação se localização for inválida.
2. Usar valores padrão se localização não estiver disponível.
3. Fazer fallback para "unknown" em qualquer situação.
4. Aceitar localização manual do usuário.
5. Divergir regras de validação entre frontend e backend.
6. Aceitar `city` ou `state` vazios como válidos.
7. Processar dados sem validar localização primeiro.

### 10.3. Proibições de Implementação

**É PROIBIDO:**
1. Ter múltiplas formas de obter localização (deve ser UMA).
2. Ter múltiplas formas de validar localização (deve ser UMA).
3. Ter múltiplas formas de gerar `locationId` (deve ser UMA).
4. Ter múltiplas formas de salvar dados (deve ser UMA).
5. Ter código que não segue este modelo canônico.

---

## 11. CONSEQUÊNCIAS DE VIOLAÇÃO

### 11.1. Se Localização For Inválida

**Frontend:**
- `LocationStateManager` emite `LocationState.Error`.
- Repositórios retornam `emptyList()`.
- UI mostra erro: "Localização não disponível. Aguarde a localização ser detectada."

**Backend:**
- `normalizeLocationId()` lança exceção.
- Cloud Function retorna `HttpsError('failed-precondition', message)`.
- Nenhum dado é criado.

### 11.2. Se Tentar Salvar com "unknown"

**Firestore Security Rules:**
- Bloqueia escrita em `locations/unknown/...`.
- Bloqueia escrita em `locations/unknown_unknown/...`.
- Retorna `PERMISSION_DENIED`.

**Backend:**
- `normalizeLocationId()` lança exceção antes de chegar no Firestore.
- Operação é abortada.

**Frontend:**
- `LocationHelper.normalizeLocationId()` lança exceção.
- Operação é abortada.

### 11.3. Se Tentar Ler Sem Localização Pronta

**Frontend:**
- Repositórios retornam `emptyList()`.
- UI mostra estado de carregamento.
- NÃO faz query no Firestore.

**Backend:**
- `getUserLocation()` retorna `{city: '', state: ''}`.
- Validação falha.
- Operação é abortada com erro.

### 11.4. Se Tentar Aceitar Localização do Cliente

**Backend:**
- Cloud Functions NUNCA aceitam `city` ou `state` como parâmetro.
- Sempre obtêm de `users/{userId}`.
- Se cliente tentar enviar, é ignorado.

---

## 12. GARANTIAS DO SISTEMA

### 12.1. Garantias de Consistência

1. **Frontend e Backend usam EXATAMENTE as mesmas regras** de validação, normalização e geração de `locationId`.
2. **Toda localização é validada ANTES de ser usada.**
3. **Nunca existe "unknown" no banco de dados.**
4. **Todos os dados públicos estão em `locations/{locationId}/...`.**

### 12.2. Garantias de Segurança

1. **Firestore Security Rules bloqueiam `locationId` inválido.**
2. **Backend valida antes de escrever.**
3. **Frontend valida antes de usar.**
4. **Nenhum dado é criado sem localização válida.**

### 12.3. Garantias de Funcionamento

1. **Localização é obtida automaticamente via GPS.**
2. **Localização é atualizada automaticamente quando usuário muda de região.**
3. **Sistema falha explicitamente se localização não estiver disponível.**
4. **Usuário não pode criar dados sem localização válida.**

---

## 13. CONCLUSÃO

Este documento define o **MUNDO TASKGO**: um sistema determinístico, imutável e sem ambiguidades.

**Qualquer implementação que viole estas regras é um BUG DE ARQUITETURA.**

**Não existem exceções. Não existem "casos especiais".**

**Se uma regra não puder ser cumprida, o sistema DEVE FALHAR EXPLICITAMENTE.**

---

**FIM DO MODELO CANÔNICO**
