# 📐 Padronização de Design - TaskGo App

## 🎯 Objetivo

Padronizar todos os elementos visuais do aplicativo para garantir consistência, melhorar a experiência do usuário e facilitar a manutenção do código.

## 📋 Padrões Estabelecidos

### **1. Espaçamentos**

**SEMPRE usar `DesignConstants.Spacing`**:
- `xs` = 4.dp (extra pequeno)
- `sm` = 8.dp (pequeno)
- `md` = 16.dp (médio - padrão)
- `lg` = 24.dp (grande)
- `xl` = 32.dp (extra grande)
- `xxl` = 48.dp (extra extra grande)

**Espaçamentos específicos**:
- `screenPadding` = 16.dp (padding padrão das telas)
- `cardPadding` = 16.dp (padding interno dos cards)
- `cardSpacing` = 16.dp (espaçamento entre cards)
- `sectionSpacing` = 24.dp (espaçamento entre seções)
- `formFieldSpacing` = 16.dp (espaçamento entre campos de formulário)

**❌ NUNCA usar valores hardcoded como `padding(24.dp)`, `Spacer(Modifier.height(40.dp))`**
**✅ SEMPRE usar `padding(DesignConstants.Spacing.lg)`, `Spacer(Modifier.height(DesignConstants.Spacing.sectionSpacing))`**

---

### **2. Botões**

**SEMPRE usar componentes padronizados**:
- `PrimaryButton` - Botão principal (verde, preenchido)
- `SecondaryButton` - Botão secundário (verde, outline)
- `TextButton` - Botão de texto (apenas texto)

**Tamanhos padronizados**:
- Altura padrão: `DesignConstants.Sizes.buttonHeight` (52.dp)
- Altura grande: `DesignConstants.Sizes.buttonHeightLarge` (56.dp)
- Altura pequena: `DesignConstants.Sizes.buttonHeightSmall` (40.dp)

**❌ NUNCA criar botões customizados diretamente com `Button()` ou `OutlinedButton()`**
**✅ SEMPRE usar `PrimaryButton`, `SecondaryButton` ou `TextButton`**

---

### **3. Campos de Texto (TextFields)**

**SEMPRE usar `TextFieldHelper`**:
- `TextFieldHelper.StandardTextField` - Campo padrão
- `TextFieldHelper.EmailTextField` - Campo de email
- `TextFieldHelper.PasswordTextField` - Campo de senha
- `TextFieldHelper.NumberTextField` - Campo numérico (CPF, CNPJ, telefone)
- `TextFieldHelper.MultilineTextField` - Campo multilinha

**Tamanhos padronizados**:
- Altura padrão: `DesignConstants.Sizes.inputHeight` (56.dp)
- Raio de borda: `DesignConstants.Shapes.cornerRadiusMedium` (8.dp)

**❌ NUNCA criar TextFields customizados diretamente**
**✅ SEMPRE usar `TextFieldHelper`**

---

### **4. Cores**

**SEMPRE usar cores do tema**:
- `TaskGoGreen` - Cor principal
- `TaskGoGreenLight` - Verde claro
- `TaskGoGreenDark` - Verde escuro
- `TaskGoTextBlack` - Texto preto
- `TaskGoTextGray` - Texto cinza
- `TaskGoBackgroundWhite` - Fundo branco
- `TaskGoBackgroundGray` - Fundo cinza
- `TaskGoError` - Erro
- `TaskGoBorder` - Borda

**❌ NUNCA usar `Color(0xFF...)` diretamente**
**✅ SEMPRE usar cores do tema (`TaskGoGreen`, `TaskGoTextBlack`, etc.)**

---

### **5. Tipografia**

**SEMPRE usar estilos do MaterialTheme ou Figma**:
- `MaterialTheme.typography.titleLarge` - Títulos grandes
- `MaterialTheme.typography.titleMedium` - Títulos médios
- `MaterialTheme.typography.bodyLarge` - Corpo grande
- `MaterialTheme.typography.bodyMedium` - Corpo médio
- `FigmaTitleLarge` - Título grande (Figma)
- `FigmaSectionTitle` - Título de seção (Figma)
- `FigmaProductName` - Nome de produto (Figma)
- `FigmaButtonText` - Texto de botão (Figma)

**❌ NUNCA usar `TextStyle(fontSize = 16.sp)` diretamente**
**✅ SEMPRE usar estilos do tema**

---

### **6. Cards**

**Tamanhos padronizados**:
- Elevação padrão: `DesignConstants.Sizes.cardElevation` (2.dp)
- Raio de borda: `DesignConstants.Shapes.cornerRadiusLarge` (12.dp)
- Padding interno: `DesignConstants.Spacing.cardPadding` (16.dp)

**❌ NUNCA usar `Card(elevation = CardDefaults.cardElevation(4.dp))` diretamente**
**✅ SEMPRE usar `CardDefaults.cardElevation(DesignConstants.Sizes.cardElevation)`**

---

### **7. Ícones**

**Tamanhos padronizados**:
- Pequeno: `DesignConstants.Sizes.iconSmall` (16.dp)
- Médio: `DesignConstants.Sizes.iconMedium` (24.dp) - **PADRÃO**
- Grande: `DesignConstants.Sizes.iconLarge` (32.dp)
- Extra Grande: `DesignConstants.Sizes.iconXLarge` (48.dp)

**❌ NUNCA usar `Modifier.size(20.dp)` diretamente**
**✅ SEMPRE usar `Modifier.size(DesignConstants.Sizes.iconMedium)`**

---

### **8. Avatares**

**Tamanhos padronizados**:
- Pequeno: `DesignConstants.Sizes.avatarSmall` (32.dp)
- Médio: `DesignConstants.Sizes.avatarMedium` (48.dp)
- Grande: `DesignConstants.Sizes.avatarLarge` (64.dp)
- Extra Grande: `DesignConstants.Sizes.avatarXLarge` (96.dp)

---

## 🔧 Correções Necessárias

### **Arquivos Prioritários para Correção**:

1. **Telas de Autenticação**:
   - `LoginPersonScreen.kt`
   - `SignUpScreen.kt`
   - `ForgotPasswordScreen.kt`

2. **Telas de Serviços**:
   - `CreateWorkOrderScreen.kt`
   - `ServicesScreen.kt`
   - `MyServiceOrdersScreen.kt`

3. **Telas de Produtos**:
   - `CriarProdutoScreen.kt`
   - `ProductDetailScreen.kt`

4. **Telas de Configurações**:
   - `SettingsScreen.kt`
   - `AccountScreen.kt`
   - `PrivacyScreen.kt`

---

## ✅ Checklist de Padronização

Para cada arquivo, verificar:

- [ ] Espaçamentos usam `DesignConstants.Spacing`
- [ ] Botões usam `PrimaryButton`, `SecondaryButton` ou `TextButton`
- [ ] TextFields usam `TextFieldHelper`
- [ ] Cores usam constantes do tema
- [ ] Tipografia usa estilos do MaterialTheme ou Figma
- [ ] Cards usam constantes de tamanho
- [ ] Ícones usam constantes de tamanho
- [ ] Avatares usam constantes de tamanho
- [ ] Não há valores hardcoded de espaçamento, tamanho ou cor

---

## 📝 Exemplos de Correção

### **Antes (❌ ERRADO)**:
```kotlin
Column(
    modifier = Modifier.padding(24.dp)
) {
    Spacer(modifier = Modifier.height(40.dp))
    
    Button(
        onClick = { },
        modifier = Modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00BD48)
        )
    ) {
        Text("Entrar", fontSize = 16.sp)
    }
}
```

### **Depois (✅ CORRETO)**:
```kotlin
Column(
    modifier = Modifier.padding(DesignConstants.Spacing.lg)
) {
    Spacer(modifier = Modifier.height(DesignConstants.Spacing.xl))
    
    PrimaryButton(
        text = "Entrar",
        onClick = { },
        modifier = Modifier.fillMaxWidth()
    )
}
```

---

**Fim do Documento**
