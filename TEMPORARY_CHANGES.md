# Mudanças Temporárias - Contorno do Login

## 📝 **Descrição**

Este documento descreve as mudanças temporárias feitas no aplicativo TaskGo para contornar a tela de login enquanto não há conexão com API de autenticação.

## 🔧 **Mudanças Realizadas**

### 1. **SplashScreen** (`feature/splash/presentation/SplashScreen.kt`)

**Antes:**
```kotlin
// TODO: Verificar se o usuário já fez login
val isUserLoggedIn = false

if (isUserLoggedIn) {
    onNavigateToHome()
} else {
    onNavigateToOnboarding()
}
```

**Depois:**
```kotlin
// TEMPORÁRIO: Navegar diretamente para HOME para contornar login
// TODO: Restaurar verificação de login quando API estiver disponível
onNavigateToHome()
```

### 2. **SettingsScreen** (`feature/settings/presentation/SettingsScreen.kt`)

**Antes:**
```kotlin
// Botão de logout
TgPrimaryButton(
    onClick = onNavigateToLogin,
    text = "Sair da Conta",
    modifier = Modifier.fillMaxWidth()
)
```

**Depois:**
```kotlin
// TEMPORÁRIO: Botão de logout desabilitado
// TODO: Restaurar quando API de login estiver disponível
/*
TgPrimaryButton(
    onClick = onNavigateToLogin,
    text = "Sair da Conta",
    modifier = Modifier.fillMaxWidth()
)
*/
```

## 🎯 **Resultado**

Com essas mudanças, o fluxo do aplicativo agora é:

```
Splash (2s) → Home (Marketplace)
```

Em vez do fluxo original:
```
Splash → Onboarding → Login/Signup → Home
```

## 🔄 **Como Restaurar**

### **Para restaurar o fluxo de login:**

1. **SplashScreen**: Descomente a verificação de login e remova a navegação direta
2. **SettingsScreen**: Descomente o botão de logout

### **Exemplo de restauração:**

```kotlin
// Em SplashScreen.kt
LaunchedEffect(Unit) {
    delay(2000)
    
    // Verificar se o usuário já fez login
    val isUserLoggedIn = checkUserLoginStatus() // Implementar quando API estiver pronta
    
    if (isUserLoggedIn) {
        onNavigateToHome()
    } else {
        onNavigateToOnboarding()
    }
}
```

## ⚠️ **Importante**

- Estas mudanças são **temporárias** e devem ser revertidas quando a API de autenticação estiver disponível
- A estrutura de navegação permanece intacta
- Todas as outras funcionalidades do marketplace continuam funcionando normalmente
- Os comentários `TODO` indicam onde as mudanças devem ser revertidas

## 📱 **Funcionalidades Disponíveis**

Com o login contornado, você pode acessar:

- ✅ **Home** - Lista de serviços do marketplace
- ✅ **Novo Serviço** - Criação de serviços
- ✅ **Detalhes do Serviço** - Visualização completa
- ✅ **Calendário** - Visualização de tarefas
- ✅ **Configurações** - Preferências do usuário (sem logout)

## 🚀 **Próximos Passos**

1. Testar todas as funcionalidades do marketplace
2. Implementar a API de autenticação
3. Restaurar o fluxo de login original
4. Implementar persistência de sessão do usuário
