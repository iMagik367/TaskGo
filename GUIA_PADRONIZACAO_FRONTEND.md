# 🎨 Guia de Padronização do Frontend - TaskGo App

## 📋 Resumo das Correções Necessárias

### **1. Arquivos Criados**

✅ **`DesignConstants.kt`** - Constantes centralizadas de design
✅ **`TextFieldHelper.kt`** - Helpers padronizados para TextFields
✅ **`PADRONIZACAO_DESIGN.md`** - Documentação completa dos padrões

---

## 🔧 Correções por Categoria

### **A. Espaçamentos**

**Problema**: Valores hardcoded como `padding(24.dp)`, `Spacer(Modifier.height(40.dp))`

**Solução**: Substituir por `DesignConstants.Spacing.*`

**Exemplos de substituição**:
- `padding(24.dp)` → `padding(DesignConstants.Spacing.lg)`
- `Spacer(Modifier.height(16.dp))` → `Spacer(Modifier.height(DesignConstants.Spacing.md))`
- `Spacer(Modifier.height(40.dp))` → `Spacer(Modifier.height(DesignConstants.Spacing.xl))`
- `padding(horizontal = 16.dp)` → `padding(horizontal = DesignConstants.Spacing.md)`

---

### **B. Botões**

**Problema**: Botões criados diretamente com `Button()`, `OutlinedButton()` com estilos inconsistentes

**Solução**: Usar componentes padronizados `PrimaryButton`, `SecondaryButton`, `TextButton`

**Exemplos de substituição**:
```kotlin
// ❌ ANTES
Button(
    onClick = { },
    modifier = Modifier.height(56.dp),
    colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
) {
    Text("Entrar")
}

// ✅ DEPOIS
PrimaryButton(
    text = "Entrar",
    onClick = { },
    modifier = Modifier.fillMaxWidth()
)
```

---

### **C. TextFields**

**Problema**: TextFields criados diretamente com estilos inconsistentes

**Solução**: Usar `TextFieldHelper.*`

**Exemplos de substituição**:
```kotlin
// ❌ ANTES
OutlinedTextField(
    value = email,
    onValueChange = { email = it },
    label = { Text("Email") },
    modifier = Modifier.height(56.dp),
    shape = RoundedCornerShape(8.dp),
    colors = OutlinedTextFieldDefaults.colors(...)
)

// ✅ DEPOIS
TextFieldHelper.EmailTextField(
    value = email,
    onValueChange = { email = it },
    modifier = Modifier.fillMaxWidth()
)
```

---

### **D. Cores**

**Problema**: Cores hardcoded como `Color(0xFF00BD48)`, `Color(0xFFD9D9D9)`

**Solução**: Usar constantes do tema

**Exemplos de substituição**:
- `Color(0xFF00BD48)` → `TaskGoGreen`
- `Color(0xFFD9D9D9)` → `TaskGoBorder`
- `Color(0xFF6C6C6C)` → `TaskGoTextGray`
- `Color.White` → `TaskGoBackgroundWhite` (quando apropriado)

---

### **E. Tipografia**

**Problema**: Estilos de texto hardcoded como `TextStyle(fontSize = 16.sp)`

**Solução**: Usar estilos do MaterialTheme ou Figma

**Exemplos de substituição**:
- `TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)` → `FigmaTitleLarge`
- `TextStyle(fontSize = 14.sp)` → `MaterialTheme.typography.bodyMedium`
- `TextStyle(fontSize = 12.sp)` → `MaterialTheme.typography.bodySmall`

---

### **F. Tamanhos de Elementos**

**Problema**: Tamanhos hardcoded como `Modifier.size(24.dp)`, `Modifier.height(56.dp)`

**Solução**: Usar `DesignConstants.Sizes.*`

**Exemplos de substituição**:
- `Modifier.size(24.dp)` → `Modifier.size(DesignConstants.Sizes.iconMedium)`
- `Modifier.height(56.dp)` → `Modifier.height(DesignConstants.Sizes.inputHeight)`
- `Modifier.size(48.dp)` → `Modifier.size(DesignConstants.Sizes.avatarMedium)`

---

## 📝 Arquivos Prioritários para Correção

### **1. Autenticação** (Alta Prioridade)
- `LoginPersonScreen.kt`
- `SignUpScreen.kt`
- `ForgotPasswordScreen.kt`
- `LoginStoreScreen.kt`

### **2. Serviços** (Alta Prioridade)
- `CreateWorkOrderScreen.kt`
- `ServicesScreen.kt`
- `MyServiceOrdersScreen.kt`
- `MeusServicosScreen.kt`

### **3. Produtos** (Média Prioridade)
- `CriarProdutoScreen.kt`
- `ProductDetailScreen.kt`
- `MeusProdutosScreen.kt`

### **4. Configurações** (Média Prioridade)
- `SettingsScreen.kt`
- `AccountScreen.kt`
- `PrivacyScreen.kt`
- `NotificationsSettingsScreen.kt`

### **5. Perfil** (Média Prioridade)
- `PublicUserProfileScreen.kt`
- `ProfileScreen.kt`
- `MeusDadosScreen.kt`

---

## 🔍 Verificações em ViewModels

### **Formatação de Texto**

Verificar se há formatação de:
- CPF/CNPJ (máscara)
- Telefone (máscara)
- CEP (máscara)
- Moeda (R$)
- Data (formato brasileiro)

**Padrão esperado**:
- CPF: `000.000.000-00`
- CNPJ: `00.000.000/0000-00`
- Telefone: `(00) 00000-0000`
- CEP: `00000-000`
- Moeda: `R$ 0,00`
- Data: `dd/MM/yyyy`

### **Validações**

Verificar se há validações para:
- Email (formato válido)
- CPF/CNPJ (dígitos verificadores)
- Telefone (formato válido)
- CEP (formato válido)
- Senha (força mínima)

---

## ✅ Checklist de Verificação

Para cada arquivo corrigido, verificar:

- [ ] Todos os espaçamentos usam `DesignConstants.Spacing`
- [ ] Todos os botões usam componentes padronizados
- [ ] Todos os TextFields usam `TextFieldHelper`
- [ ] Todas as cores usam constantes do tema
- [ ] Toda tipografia usa estilos do MaterialTheme ou Figma
- [ ] Todos os tamanhos usam `DesignConstants.Sizes`
- [ ] Não há valores hardcoded
- [ ] ViewModels têm formatação e validação corretas

---

## 🚀 Próximos Passos

1. Corrigir arquivos de autenticação
2. Corrigir arquivos de serviços
3. Corrigir arquivos de produtos
4. Corrigir arquivos de configurações
5. Verificar e corrigir ViewModels
6. Fazer varredura final em todos os arquivos

---

**Fim do Documento**
