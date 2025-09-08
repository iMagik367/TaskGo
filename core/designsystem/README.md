# TaskGo Design System

Sistema de design completo para o aplicativo TaskGo, construído com Compose Material3.

## 🎨 Tema

### TaskGoTheme
O tema principal do aplicativo que inclui:
- Esquemas de cores (claro e escuro)
- Tipografia personalizada
- Espaçamento consistente
- Raios de borda padronizados

```kotlin
@Composable
fun App() {
    TaskGoTheme {
        // Seu conteúdo aqui
    }
}
```

### Cores
O sistema de cores segue as diretrizes do Material3 com paletas personalizadas:

- **Primary**: Cor principal (#6200EE)
- **Secondary**: Cor secundária (#03DAC5)
- **Tertiary**: Cor terciária (#6750A4)
- **Error**: Cor de erro (#BA1A1A)
- **Surface**: Cor de superfície (#FFFFFF)
- **Background**: Cor de fundo (#FFFBFE)

### Tipografia
Tipografia baseada na fonte OpenSans com hierarquia clara:

- **Display**: Para títulos principais (57sp, 45sp, 36sp)
- **Headline**: Para títulos de seção (32sp, 28sp, 24sp)
- **Title**: Para títulos de componentes (22sp, 16sp, 14sp)
- **Body**: Para texto principal (16sp, 14sp, 12sp)
- **Label**: Para rótulos e botões (14sp, 12sp, 11sp)

## 📏 Espaçamento e Raios

### LocalTaskGoSpacing
Acesso ao sistema de espaçamento consistente:

```kotlin
val spacing = MaterialTheme.taskGoSpacing

// Uso
.padding(spacing.medium)
.height(spacing.huge)
```

**Valores disponíveis:**
- `extraSmall`: 4dp
- `small`: 8dp
- `medium`: 16dp
- `large`: 24dp
- `extraLarge`: 32dp
- `huge`: 48dp

### LocalTaskGoRadii
Acesso aos raios de borda padronizados:

```kotlin
val radii = MaterialTheme.taskGoRadii

// Uso
.shape(RoundedCornerShape(radii.medium))
```

**Valores disponíveis:**
- `extraSmall`: 4dp
- `small`: 8dp
- `medium`: 12dp
- `large`: 16dp
- `extraLarge`: 24dp
- `round`: 50dp

## 🔘 Componentes

### TgButton
Botão personalizado com três variantes:

#### Variantes
- **FILLED**: Botão preenchido (primário)
- **TONAL**: Botão tonal (secundário)
- **TEXT**: Botão de texto (terciário)

#### Estados
- **ENABLED**: Botão habilitado
- **DISABLED**: Botão desabilitado
- **LOADING**: Botão carregando

#### Uso Básico
```kotlin
TgButton(
    onClick = { /* ação */ },
    text = "Clique aqui",
    config = TgButtonConfig(
        variant = TgButtonVariant.FILLED,
        fullWidth = true
    )
)
```

#### Botões Pré-configurados
```kotlin
// Botão primário
TgPrimaryButton(
    onClick = { /* ação */ },
    text = "Salvar"
)

// Botão secundário
TgSecondaryButton(
    onClick = { /* ação */ },
    text = "Cancelar"
)

// Botão de texto
TgTextButton(
    onClick = { /* ação */ },
    text = "Mais opções"
)
```

### TgTextField
Campo de texto personalizado com estados e validação:

#### Estados
- **DEFAULT**: Estado padrão
- **FOCUSED**: Campo focado
- **ERROR**: Campo com erro
- **DISABLED**: Campo desabilitado

#### Uso Básico
```kotlin
TgTextField(
    value = text,
    onValueChange = { text = it },
    label = "Nome",
    placeholder = "Digite seu nome"
)
```

#### Configurações Avançadas
```kotlin
TgTextField(
    value = text,
    onValueChange = { text = it },
    label = "Email",
    config = TgTextFieldConfig(
        helperText = "Este campo é obrigatório",
        cornerRadius = 16,
        maxLines = 3
    )
)
```

#### Campos Pré-configurados
```kotlin
// Campo com erro
TgErrorTextField(
    value = text,
    onValueChange = { text = it },
    label = "Senha",
    errorText = "A senha deve ter pelo menos 6 caracteres"
)

// Campo desabilitado
TgDisabledTextField(
    value = "Valor fixo",
    label = "Campo Desabilitado"
)
```

## 🚀 Como Usar

### 1. Importar o Design System
```kotlin
import com.example.taskgoapp.core.designsystem.*
```

### 2. Aplicar o Tema
```kotlin
@Composable
fun App() {
    TaskGoTheme {
        // Seu conteúdo aqui
    }
}
```

### 3. Usar os Componentes
```kotlin
@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.taskGoSpacing.medium)
    ) {
        TgTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            placeholder = "Digite seu email"
        )
        
        Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
        
        TgTextField(
            value = password,
            onValueChange = { password = it },
            label = "Senha",
            placeholder = "Digite sua senha"
        )
        
        Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.medium))
        
        TgPrimaryButton(
            onClick = { /* login */ },
            text = "Entrar",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

## 📱 Exemplos

Veja exemplos completos de uso no arquivo `DesignSystemExamples.kt`.

## 🎯 Boas Práticas

1. **Sempre use o TaskGoTheme** como tema base
2. **Acesse espaçamento e raios** através de `MaterialTheme.taskGoSpacing` e `MaterialTheme.taskGoRadii`
3. **Use os componentes pré-configurados** quando possível
4. **Mantenha consistência** usando as cores e tipografias do sistema
5. **Teste em modo claro e escuro** para garantir acessibilidade

## 🔧 Personalização

Para personalizar o design system:

1. **Cores**: Modifique `TaskGoColors.kt`
2. **Tipografia**: Ajuste `TaskGoTypography.kt`
3. **Espaçamento**: Configure `TaskGoSpacing` em `TaskGoTheme.kt`
4. **Raios**: Configure `TaskGoRadii` em `TaskGoTheme.kt`
5. **Componentes**: Estenda os componentes existentes ou crie novos

## 📚 Recursos Adicionais

- [Material Design 3](https://m3.material.io/)
- [Compose Material3](https://developer.android.com/jetpack/compose/material3)
- [Compose Guidelines](https://developer.android.com/jetpack/compose/layouts)
