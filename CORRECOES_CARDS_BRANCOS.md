# ✅ Correções de Cards - Branco com Traçado Cinza

## 📋 Padrão Estabelecido

**TODOS os cards devem ser**:
- ✅ Fundo: **BRANCO** (`TaskGoBackgroundWhite`)
- ✅ Borda: **TRAÇADO CINZA** (`TaskGoBorder`)
- ❌ **NUNCA** usar fundo cinza claro (`TaskGoBackgroundGray`, `TaskGoSurfaceGray`, etc.)

---

## ✅ Componente Criado

### **TaskGoCard**
**Localização**: `app/src/main/java/com/taskgoapp/taskgo/core/design/Components.kt`

**Uso**:
```kotlin
TaskGoCard(
    onClick = { /* opcional */ },
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(DesignConstants.Spacing.cardPadding)
) {
    // Conteúdo do card
}
```

**Características**:
- Sempre branco (`TaskGoBackgroundWhite`)
- Sempre com traçado cinza (`TaskGoBorder`)
- Elevação padrão: `DesignConstants.Sizes.cardElevation` (2.dp)
- Raio de borda: `DesignConstants.Shapes.cornerRadiusLarge` (12.dp)
- Padding padrão: `DesignConstants.Spacing.cardPadding` (16.dp)

---

## ✅ Correções Realizadas

### **1. Components.kt**
- ✅ Criado `TaskGoCard` padronizado
- ✅ Atualizado `ServiceCard` para usar `TaskGoCard`
- ✅ Atualizado `ProductCard` para usar `TaskGoCard`
- ✅ Atualizado `ProposalCard` para usar `TaskGoCard`

### **2. SettingsScreen.kt**
- ✅ `SettingsOptionCard` agora usa `TaskGoCard`

### **3. PreferencesScreen.kt**
- ✅ Cards de preferências agora usam `TaskGoCard`
- ✅ `PreferenceCategoryCard` agora usa `TaskGoCard`

---

## 🔧 Correções Pendentes

### **Arquivos com Cards que Precisam ser Corrigidos**:

1. **NotificationsSettingsScreen.kt** - 3 cards
2. **PublicUserProfileScreen.kt** - Múltiplos cards
3. **MeusServicosScreen.kt** - Múltiplos cards
4. **MyServiceOrdersScreen.kt** - Múltiplos cards
5. **ServicesScreen.kt** - Múltiplos cards
6. **ProviderProfileScreen.kt** - Múltiplos cards
7. **HomeScreen.kt** - Cards
8. **AccountScreen.kt** - Cards
9. **PrivacyScreen.kt** - Cards
10. E outros 87 arquivos com Cards

---

## 📝 Padrão de Correção

### **Antes (❌ ERRADO)**:
```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = TaskGoBackgroundGray // ❌ CINZA CLARO
    )
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Conteúdo
    }
}
```

### **Depois (✅ CORRETO)**:
```kotlin
TaskGoCard(
    modifier = Modifier.fillMaxWidth()
) {
    // Conteúdo (padding já incluído)
}
```

---

## 🔍 Busca por Cards com Fundo Cinza

**Padrões a buscar e corrigir**:
- `containerColor = TaskGoBackgroundGray`
- `containerColor = TaskGoSurfaceGray`
- `containerColor = TaskGoBackgroundGrayLight`
- `containerColor = MaterialTheme.colorScheme.surfaceVariant`
- `containerColor = Color(0xFFF7F7F7)` (qualquer cor cinza hardcoded)

**Todos devem ser substituídos por `TaskGoCard`**

---

## ✅ Status

- ✅ Componente `TaskGoCard` criado
- ✅ Cards em `Components.kt` corrigidos
- ✅ Cards em `SettingsScreen.kt` corrigidos
- ✅ Cards em `PreferencesScreen.kt` corrigidos
- ⏳ Correção sistemática dos demais arquivos (em andamento)

---

**Fim do Documento**
