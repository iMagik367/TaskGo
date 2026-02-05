# ✅ Correções Realizadas - Stories

## 📋 Problema Identificado

O método `observeUserStories` no `FirestoreStoriesRepository` estava usando o `city`/`state` do usuário atual (quem está visualizando) em vez do `city`/`state` do usuário que está sendo visualizado (`userId`).

### **Antes**:
```kotlin
// ❌ ERRADO: Usava locationState do usuário atual
val locationState = locationStateManager.locationState.first()
val location = locationState.location
val collectionToUse = LocationHelper.getLocationCollection(firestore, "stories", location.city, location.state)
```

### **Depois**:
```kotlin
// ✅ CORRETO: Usa city/state do usuário que está sendo visualizado
val targetUser = userRepository.getUser(userId)
val targetCity = targetUser?.city?.takeIf { it.isNotBlank() }
val targetState = targetUser?.state?.takeIf { it.isNotBlank() }
val collectionToUse = LocationHelper.getLocationCollection(firestore, "stories", targetCity, targetState)
```

---

## 🔧 Correções Aplicadas

### **1. FirestoreStoriesRepository.observeUserStories**

**Arquivo**: `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreStoriesRepository.kt`

**Mudança**:
- ✅ Agora busca `city`/`state` do usuário que está sendo visualizado (`userId`)
- ✅ Valida que o usuário tem `city`/`state` definidos no cadastro
- ✅ Usa `locations/{locationId}/stories` baseado no `city`/`state` do usuário visualizado
- ✅ Adiciona logs detalhados para rastreamento

**Impacto**:
- ✅ Stories de outros usuários agora aparecem corretamente na página pública
- ✅ Stories aparecem corretamente em "Meus Dados" quando visualizando o próprio perfil
- ✅ Stories aparecem corretamente no feed geral (já estava correto, usa locationState do usuário atual)

---

## 📊 Estrutura de Dados

### **Stories no Firestore**

```
locations/{locationId}/stories/{storyId}
├── id: String
├── userId: String              ← ID do usuário que criou a story
├── userName: String
├── userAvatarUrl: String?
├── mediaUrl: String
├── mediaType: String           ← "image" ou "video"
├── thumbnailUrl: String?
├── caption: String?
├── location: {
│   ├── city: String           ← SEMPRE do users/{userId}
│   ├── state: String          ← SEMPRE do users/{userId}
│   ├── latitude: Double       ← GPS (apenas para coordenadas)
│   └── longitude: Double      ← GPS (apenas para coordenadas)
│ }
├── city: String               ← SEMPRE do users/{userId}
├── state: String              ← SEMPRE do users/{userId}
├── locationId: String         ← Normalizado de city_state
├── createdAt: Timestamp
├── expiresAt: Timestamp       ← 24 horas após criação
└── viewsCount: Int
```

**Regra de Localização**:
- ✅ **Criação**: Usa `city`/`state` do usuário que está criando (do seu perfil)
- ✅ **Leitura (Feed Geral)**: Usa `city`/`state` do usuário atual (quem está visualizando)
- ✅ **Leitura (Página Pública)**: Usa `city`/`state` do usuário que está sendo visualizado
- ✅ **Leitura (Meus Dados)**: Usa `city`/`state` do próprio usuário

---

## ✅ Garantias Implementadas

1. ✅ **TODAS** as stories são salvas em `locations/{locationId}/stories`
2. ✅ **SEMPRE** usar `city`/`state` do perfil do usuário (cadastro)
3. ✅ **NUNCA** usar GPS para determinar `city`/`state`
4. ✅ **SEMPRE** buscar `city`/`state` do usuário correto:
   - Feed geral: usuário atual
   - Página pública: usuário visualizado
   - Meus Dados: próprio usuário

---

## 🔍 Arquivos Modificados

### **Frontend**:
1. `app/src/main/java/com/taskgoapp/taskgo/data/repository/FirestoreStoriesRepository.kt`
   - ✅ Corrigido `observeUserStories` para usar `city`/`state` do usuário visualizado

### **Backend**:
1. `functions/src/stories.ts`
   - ✅ Já estava correto - usa `city`/`state` do perfil do usuário

---

## 🎯 Resultado Final

### **Feed Geral**:
- ✅ Stories aparecem corretamente usando `city`/`state` do usuário atual
- ✅ Filtro por distância GPS funciona corretamente

### **Página Pública**:
- ✅ Stories do usuário visualizado aparecem corretamente
- ✅ Usa `city`/`state` do usuário que está sendo visualizado

### **Meus Dados**:
- ✅ Stories próprias aparecem corretamente
- ✅ Usa `city`/`state` do próprio usuário

### **Padronização**:
- ✅ **TODOS** os stories seguem o padrão `locations/{locationId}/stories`
- ✅ **TODAS** as queries usam `city`/`state` do cadastro
- ✅ **NENHUM** story fica sem aparecer por falta de localização

---

**Fim do Documento**
