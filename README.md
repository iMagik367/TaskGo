# TaskGo - Aplicativo de Marketplace de Serviços

## 📱 Sobre o Projeto

TaskGo é uma plataforma inovadora que conecta clientes a prestadores de serviços qualificados e confiáveis. O aplicativo oferece uma experiência segura, transparente e eficiente para contratação de serviços.

## 🏗️ Arquitetura

- **Padrão**: MVVM por feature
- **UI**: Jetpack Compose + Material 3
- **Navegação**: Navigation Compose
- **Injeção de Dependência**: Hilt
- **Estado**: Kotlin Flow
- **Banco de Dados**: Room (opcional)
- **Backend**: Mock repositories (dados simulados)

## 📁 Estrutura do Projeto

```
TaskGoApp/
├── app/                          # Módulo principal
│   └── src/main/
│       ├── java/com/example/taskgoapp/
│       │   ├── MainActivity.kt   # Activity principal
│       │   └── TaskGoApp.kt      # Application class (Hilt)
│       └── res/                  # Recursos (drawables, strings, etc.)
├── core/                         # Módulo core
│   ├── data/                     # Modelos e repositórios
│   ├── design/                   # Componentes compartilhados
│   ├── navigation/               # Navegação principal
│   └── theme/                    # Tema e estilos
└── feature/                      # Módulos por feature
    ├── home/                     # Tela inicial
    ├── services/                 # Serviços e prestadores
    ├── products/                 # Produtos e e-commerce
    ├── messages/                 # Chat e mensagens
    ├── profile/                  # Perfil do usuário
    ├── settings/                 # Configurações
    ├── orders/                   # Pedidos e rastreamento
    ├── proposals/                # Propostas de serviços
    └── chatai/                   # Suporte via IA
```

## 🚀 Funcionalidades Implementadas

### 🏠 Tela Inicial (Home)
- Busca inteligente
- Categorias de serviços
- Banners promocionais
- Produtos em destaque
- Ações rápidas

### 🔧 Serviços
- Lista de prestadores
- Categorias de serviços
- Detalhes do prestador
- Avaliações e comentários
- Criação de ordem de serviço
- Sistema de propostas

### 🛍️ Produtos
- Catálogo de produtos
- Detalhes do produto
- Carrinho de compras
- Checkout completo
- Rastreamento de pedidos
- Histórico de compras

### 💬 Mensagens
- Lista de conversas
- Chat em tempo real
- Histórico de mensagens
- Suporte ao usuário

### 👤 Perfil
- Dados pessoais
- Meus serviços
- Meus produtos
- Gerenciar propostas
- Minhas avaliações
- Meus pedidos

### ⚙️ Configurações
- Conta e segurança
- Preferências
- Notificações
- Idioma (pt-BR, en-US, es-ES)
- Privacidade
- Suporte
- Sobre o app

### 🤖 Suporte IA
- Chat inteligente
- Respostas automáticas
- Suporte 24/7

## 🎨 Design System

### Componentes Compartilhados
- `AppTopBar` - Barra superior padrão
- `AppBottomBar` - Navegação inferior
- `SearchBar` - Campo de busca
- `ServiceCard` - Card de serviço
- `ProductCard` - Card de produto
- `ProposalCard` - Card de proposta
- `RatingBar` - Sistema de avaliação
- `PriceTag` - Exibição de preços
- `EmptyState` - Estados vazios
- `Timeline` - Linha do tempo
- `ChatBubble` - Bolha de chat
- `InputMessage` - Input de mensagem

### Botões
- `PrimaryButton` - Botão principal
- `SecondaryButton` - Botão secundário
- `OutlinedButton` - Botão outline
- `TextButton` - Botão de texto

## 🧭 Navegação

### Rotas Principais
- `home` - Tela inicial
- `services` - Lista de serviços
- `products` - Catálogo de produtos
- `messages` - Conversas
- `profile` - Perfil do usuário

### Sub-rotas
- `serviceDetail/{id}` - Detalhes do serviço
- `productDetail/{id}` - Detalhes do produto
- `chat/{threadId}` - Chat específico
- `orderTracking/{orderId}` - Rastreamento
- `proposalDetail/{id}` - Detalhes da proposta
- `reviews/{providerId}` - Avaliações
- `account` - Configurações de conta
- `preferences` - Preferências
- `notificationsSettings` - Configurações de notificações
- `language` - Configurações de idioma
- `privacy` - Configurações de privacidade
- `support` - Central de suporte
- `about` - Sobre o aplicativo

## 📱 Como Executar

### Pré-requisitos
- Android Studio Hedgehog ou superior
- Android SDK 34
- Kotlin 1.9.0+
- JDK 17+

### Passos
1. Clone o repositório
2. Abra o projeto no Android Studio
3. Sincronize o Gradle
4. Execute o aplicativo em um emulador ou dispositivo

### Comandos Gradle
```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Limpar build
./gradlew clean
```

## 🔧 Dependências Principais

```kotlin
// Compose
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.compose.ui:ui:1.5.4")
implementation("androidx.compose.material3:material3:1.1.2")
implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")

// Navegação
implementation("androidx.navigation:navigation-compose:2.7.5")

// Hilt
implementation("com.google.dagger:hilt-android:2.48")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Room (opcional)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// Outras
implementation("com.google.code.gson:gson:2.10.1")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
```

## 🌐 Internacionalização

O aplicativo está preparado para múltiplos idiomas:
- **Português (Brasil)** - Padrão
- **English (US)** - Disponível
- **Español (España)** - Disponível
- Outros idiomas podem ser adicionados facilmente

## 📊 Dados Mock

O aplicativo utiliza repositórios simulados com dados de exemplo:
- Usuários fictícios
- Prestadores de serviços
- Produtos simulados
- Pedidos de exemplo
- Conversas simuladas

## 🚧 Funcionalidades Pendentes

- [ ] Integração com backend real
- [ ] Sistema de pagamentos real
- [ ] Notificações push
- [ ] Upload de imagens
- [ ] Geolocalização
- [ ] Sistema de avaliações real
- [ ] Chat em tempo real
- [ ] Analytics e métricas

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Push para a branch
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 📞 Contato

- **Website**: www.taskgo.com.br
- **E-mail**: contato@taskgo.com.br
- **Suporte**: suporte@taskgo.com.br
- **Telefone**: (11) 3000-0000

## 🙏 Agradecimentos

- Equipe de desenvolvimento TaskGo
- Comunidade Android
- Material Design 3
- Jetpack Compose

---

**TaskGo** - Conectando você aos melhores prestadores de serviços! 🚀

