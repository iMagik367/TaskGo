# TaskGo Navigation System

Sistema de navegação completo para o aplicativo TaskGo usando Navigation Compose.

## 🗺️ Estrutura de Rotas

### Rotas Principais
- **`splash`** - Tela de splash inicial
- **`onboarding`** - Tela de introdução para novos usuários
- **`auth/login`** - Tela de login
- **`auth/signup`** - Tela de cadastro
- **`home`** - Tela principal do aplicativo
- **`task/new`** - Criação de nova tarefa
- **`task/{id}`** - Detalhes de uma tarefa específica
- **`calendar`** - Visualização do calendário
- **`settings`** - Configurações do usuário

## 🚀 Como Usar

### 1. Configurar na MainActivity

```kotlin
@Composable
fun TaskGoApp() {
    TaskGoTheme {
        val navController = rememberNavController()
        
        TaskGoNavGraph(
            navController = navController
        )
    }
}
```

### 2. Navegação entre Telas

```kotlin
// Navegação simples
navController.navigate(TaskGoDestinations.HOME)

// Navegação com popUpTo (remove telas do back stack)
navController.navigate(TaskGoDestinations.HOME) {
    popUpTo(TaskGoDestinations.SPLASH) { inclusive = true }
}

// Navegação com parâmetros
navController.navigate(TaskGoDestinations.taskDetail("task-123"))

// Navegação de volta
navController.popBackStack()
```

### 3. Navegação com Argumentos

```kotlin
// Para rotas com parâmetros como task/{id}
composable(
    route = TaskGoDestinations.TASK_DETAIL,
    arguments = listOf(
        navArgument("id") {
            type = NavType.StringType
        }
    )
) { backStackEntry ->
    val taskId = backStackEntry.arguments?.getString("id") ?: ""
    TaskDetailScreen(taskId = taskId)
}
```

## 📱 Telas Implementadas

### SplashScreen
- Tela inicial com logo do TaskGo
- Navegação automática após 2 segundos
- Verifica se o usuário está logado

### OnboardingScreen
- Apresentação do aplicativo
- Botões para login ou cadastro
- Navegação com popUpTo para limpar back stack

### LoginScreen
- Formulário de login com email e senha
- Link para tela de cadastro
- Navegação para home após login bem-sucedido

### SignupScreen
- Formulário de cadastro completo
- Link para tela de login
- Navegação para home após cadastro bem-sucedido

### HomeScreen
- Lista de tarefas do dia
- FAB para criar nova tarefa
- Navegação para calendário e configurações

### NewTaskScreen
- Formulário para criar nova tarefa
- TopAppBar com botão de voltar
- Navegação para detalhes da tarefa criada

### TaskDetailScreen
- Visualização completa de uma tarefa
- Botões de ação (concluir, editar)
- Navegação com parâmetro taskId

### CalendarScreen
- Visualização do calendário
- Lista de tarefas do dia selecionado
- Navegação para detalhes das tarefas

### SettingsScreen
- Lista de configurações
- Switches para modo escuro e notificações
- Botão de logout

## 🔄 Fluxo de Navegação

```
Splash → Onboarding → Login/Signup → Home
                ↓
            Task Management
                ↓
        New Task → Task Detail
                ↓
            Calendar ← Settings
```

## 🎯 Características

### Navegação Segura
- Uso de `popUpTo` para limpar back stack quando necessário
- Navegação com `inclusive = true` para remover telas específicas

### Argumentos Tipados
- Parâmetros de rota com tipos específicos
- Validação de argumentos obrigatórios

### TopAppBar Consistente
- Botões de navegação padronizados
- Títulos apropriados para cada tela

### Previews
- Todas as telas incluem previews para desenvolvimento
- Uso do TaskGoTheme para consistência visual

## 📚 Dependências

```kotlin
implementation "androidx.navigation:navigation-compose:2.7.5"
implementation "androidx.compose.material:material-icons-extended:1.5.4"
```

## 🔧 Personalização

### Adicionar Novas Rotas
1. Adicionar constante em `TaskGoDestinations`
2. Criar nova tela em feature apropriada
3. Adicionar composable no `TaskGoNavGraph`
4. Implementar navegação na tela origem

### Modificar Navegação Existente
1. Editar callbacks de navegação nas telas
2. Ajustar parâmetros de `popUpTo` conforme necessário
3. Adicionar novos argumentos se necessário

## 📱 Exemplos de Uso

Veja exemplos completos no arquivo `NavigationExample.kt`.

## 🎨 Integração com Design System

Todas as telas utilizam:
- **TaskGoTheme** para consistência visual
- **Componentes TgButton** e **TgTextField** personalizados
- **Espaçamento e raios** padronizados
- **Tipografia** consistente

## 🚨 Boas Práticas

1. **Sempre use TaskGoDestinations** para navegação
2. **Implemente popUpTo** quando apropriado para limpar back stack
3. **Use argumentos tipados** para rotas com parâmetros
4. **Mantenha consistência** na TopAppBar e navegação
5. **Teste navegação** em diferentes cenários (login, logout, etc.)
