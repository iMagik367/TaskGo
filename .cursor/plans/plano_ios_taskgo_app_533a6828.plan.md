# Plano de Ação: Versão iOS do TaskGo App

## Visão Geral

Este plano detalha a criação da versão iOS do TaskGo App, replicando todas as funcionalidades já implementadas na versão Android. O projeto Android utiliza Kotlin + Jetpack Compose, enquanto a versão iOS será desenvolvida em Swift + SwiftUI.

## Requisitos e Ferramentas Necessárias

### 1. Hardware e Software

#### Hardware Mínimo:

- Mac com macOS 12.0 (Monterey) ou superior
- Processador Intel ou Apple Silicon (M1/M2/M3)
- 8GB RAM mínimo (16GB recomendado)
- Espaço em disco: 50GB+ livre

#### Software Obrigatório:

- **Xcode 15.0+** (última versão estável recomendada)
- Instalar via Mac App Store
- Inclui Swift compiler, iOS SDK, Simuladores
- **CocoaPods** (gerenciador de dependências)
  ```bash
          sudo gem install cocoapods
  ```




- **Homebrew** (opcional, mas recomendado)
  ```bash
          /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  ```




### 2. Contas e Configurações Necessárias

#### Apple Developer Account:

- **Apple Developer Program** ($99/ano)
- Necessário para:
    - TestFlight (testes beta)
    - App Store Connect
    - Certificados de assinatura
    - Push Notifications
    - In-App Purchases
- **Apple ID** (gratuito) - mínimo para desenvolvimento local

#### Firebase:

- Projeto Firebase existente: `task-go-ee85f`
- Arquivo `GoogleService-Info.plist` (será baixado do Firebase Console)
- Configuração de iOS App no Firebase Console

#### Stripe:

- Conta Stripe ativa
- Chaves de API (mesmas do Android)
- Webhook configurado

### 3. Dependências e Frameworks iOS

#### Firebase iOS SDK (via CocoaPods):

```ruby
pod 'Firebase/Auth'
pod 'Firebase/Firestore'
pod 'Firebase/Functions'
pod 'Firebase/Storage'
pod 'Firebase/Messaging'
pod 'Firebase/Crashlytics'
pod 'Firebase/AppCheck'
pod 'Firebase/Database'
```



#### Outras Dependências:

- **Stripe iOS SDK**: `pod 'StripePaymentSheet'`
- **Google Sign-In**: `pod 'GoogleSignIn'`
- **Alamofire** (networking): `pod 'Alamofire'`
- **Kingfisher** (image loading): `pod 'Kingfisher'`
- **Realm** ou **Core Data** (cache local)
- **Combine** (nativo iOS, para reactive programming)

#### Frameworks Nativos iOS:

- **SwiftUI** - UI framework (equivalente ao Compose)
- **Combine** - Reactive programming (equivalente ao Flow)
- **Core Data** - Database local (equivalente ao Room)
- **BackgroundTasks** - Tarefas em background (equivalente ao WorkManager)
- **MapKit** - Mapas (equivalente ao Google Maps)
- **AVFoundation** - Câmera e vídeo (equivalente ao CameraX)
- **LocalAuthentication** - Biometria (equivalente ao BiometricPrompt)
- **Vision Framework** - Face detection (equivalente ao ML Kit)
- **CoreImage** - Processamento de imagem e QR Code
- **PDFKit** - Geração de PDFs

## Estrutura do Projeto iOS

### Organização de Diretórios

```javascript
TaskGoApp-iOS/
├── TaskGoApp/
│   ├── App/
│   │   ├── TaskGoApp.swift (App entry point)
│   │   ├── AppDelegate.swift
│   │   └── SceneDelegate.swift
│   ├── Core/
│   │   ├── Navigation/
│   │   ├── Theme/
│   │   ├── Extensions/
│   │   ├── Utilities/
│   │   └── Models/
│   ├── Features/
│   │   ├── Auth/
│   │   │   ├── Views/
│   │   │   ├── ViewModels/
│   │   │   └── Services/
│   │   ├── Home/
│   │   ├── Products/
│   │   ├── Services/
│   │   ├── Orders/
│   │   ├── Messages/
│   │   ├── Profile/
│   │   └── Settings/
│   ├── Data/
│   │   ├── Repositories/
│   │   ├── Local/
│   │   │   ├── CoreData/
│   │   │   └── UserDefaults/
│   │   └── Remote/
│   │       ├── Firebase/
│   │       └── API/
│   ├── Domain/
│   │   ├── Entities/
│   │   ├── UseCases/
│   │   └── Repositories/
│   └── Resources/
│       ├── Assets.xcassets/
│       ├── GoogleService-Info.plist
│       └── Info.plist
├── TaskGoAppTests/
├── TaskGoAppUITests/
├── Podfile
├── Podfile.lock
└── README.md
```



## Mapeamento Android → iOS

### Arquitetura e Padrões

| Android | iOS | Observações ||---------|-----|-------------|| Kotlin | Swift | Linguagem principal || Jetpack Compose | SwiftUI | UI declarativa || ViewModel | ObservableObject/@Published | State management || Flow | Combine Publishers | Reactive streams || Hilt (DI) | Dependency Injection manual ou Swinject | Injeção de dependências || Room Database | Core Data ou Realm | Cache local || WorkManager | BackgroundTasks | Tarefas em background || Navigation Compose | NavigationStack (iOS 16+) | Navegação || Coroutines | async/await | Concorrência |

### Features e Telas

#### 1. Autenticação (Auth)

- LoginScreen → `LoginView.swift`
- SignUpScreen → `SignUpView.swift`
- ForgotPasswordScreen → `ForgotPasswordView.swift`
- Google Sign-In → `GoogleSignInService.swift`
- ViewModels → `AuthViewModel.swift` (ObservableObject)

#### 2. Produtos (Products)

- ProductsScreen → `ProductsView.swift`
- ProductDetailScreen → `ProductDetailView.swift`
- ProductFormScreen → `ProductFormView.swift`
- CartScreen → `CartView.swift`
- CheckoutScreen → `CheckoutView.swift`

#### 3. Serviços (Services)

- ServicesScreen → `ServicesView.swift`
- ServiceDetailScreen → `ServiceDetailView.swift`
- CreateWorkOrderScreen → `CreateWorkOrderView.swift`
- ProposalsScreen → `ProposalsView.swift`

#### 4. Pedidos (Orders)

- MyOrdersScreen → `MyOrdersView.swift`
- OrderDetailScreen → `OrderDetailView.swift`
- OrderTrackingScreen → `OrderTrackingView.swift`

#### 5. Mensagens (Messages)

- MessagesScreen → `MessagesView.swift`
- ChatScreen → `ChatView.swift`

#### 6. Perfil (Profile)

- ProfileScreen → `ProfileView.swift`
- SettingsScreen → `SettingsView.swift`
- AccountScreen → `AccountView.swift`

#### 7. Navegação

- BottomNavigationBar → `TabBarView.swift` (TabView no SwiftUI)

## Etapas de Implementação

### Fase 1: Setup Inicial (Semana 1)

#### 1.1 Configuração do Projeto

- [ ] Criar novo projeto Xcode (iOS App)
- [ ] Configurar Bundle ID: `com.taskgoapp.taskgo`
- [ ] Configurar Team e Signing no Xcode
- [ ] Configurar versão: 1.0.0 (version code: 1)

#### 1.2 Firebase Setup

- [ ] Adicionar app iOS no Firebase Console
- [ ] Baixar `GoogleService-Info.plist`
- [ ] Adicionar `GoogleService-Info.plist` ao projeto
- [ ] Configurar CocoaPods e instalar Firebase SDKs
- [ ] Configurar Firebase no `AppDelegate.swift`
- [ ] Testar conexão Firebase

#### 1.3 Estrutura Base

- [ ] Criar estrutura de diretórios
- [ ] Configurar Core Data (se necessário)
- [ ] Criar Theme/Design System básico
- [ ] Configurar Navigation base

### Fase 2: Core e Infraestrutura (Semana 2)

#### 2.1 Models e Domain

- [ ] Migrar todas as entidades do Android para Swift
- [ ] Criar mappers Firebase → Domain
- [ ] Configurar Codable para serialização

#### 2.2 Repositories

- [ ] Implementar `FirebaseAuthRepository`
- [ ] Implementar `FirestoreUserRepository`
- [ ] Implementar `FirestoreProductsRepository`
- [ ] Implementar `FirestoreServicesRepository`
- [ ] Implementar `FirestoreOrdersRepository`
- [ ] Implementar `FirestoreMessagesRepository`
- [ ] Implementar cache local (Core Data)

#### 2.3 Services

- [ ] Implementar `FirebaseFunctionsService`
- [ ] Implementar `StripePaymentService`
- [ ] Implementar `ImageUploadService`
- [ ] Implementar `LocationService` (CoreLocation)
- [ ] Implementar `NotificationService` (Push Notifications)

### Fase 3: Features Principais (Semanas 3-6)

#### 3.1 Autenticação (Semana 3)

- [ ] Implementar LoginView
- [ ] Implementar SignUpView
- [ ] Implementar ForgotPasswordView
- [ ] Integrar Google Sign-In
- [ ] Implementar AuthViewModel
- [ ] Testar fluxo completo de autenticação

#### 3.2 Home e Navegação (Semana 3)

- [ ] Implementar TabBarView (bottom navigation)
- [ ] Implementar HomeView
- [ ] Implementar navegação entre telas
- [ ] Configurar deep linking (se necessário)

#### 3.3 Produtos (Semana 4)

- [ ] Implementar ProductsView
- [ ] Implementar ProductDetailView
- [ ] Implementar ProductFormView
- [ ] Implementar CartView
- [ ] Implementar CheckoutView
- [ ] Integrar Stripe Payment Sheet
- [ ] Implementar PIX payment

#### 3.4 Serviços (Semana 5)

- [ ] Implementar ServicesView
- [ ] Implementar ServiceDetailView
- [ ] Implementar CreateWorkOrderView
- [ ] Implementar ProposalsView
- [ ] Implementar ProposalDetailView

#### 3.5 Pedidos (Semana 5)

- [ ] Implementar MyOrdersView
- [ ] Implementar OrderDetailView
- [ ] Implementar OrderTrackingView
- [ ] Integrar rastreamento de pedidos

#### 3.6 Mensagens (Semana 6)

- [ ] Implementar MessagesView
- [ ] Implementar ChatView
- [ ] Integrar Firebase Realtime Database para chat
- [ ] Implementar notificações de mensagens

#### 3.7 Perfil e Configurações (Semana 6)

- [ ] Implementar ProfileView
- [ ] Implementar SettingsView
- [ ] Implementar AccountView
- [ ] Implementar telas de configurações

### Fase 4: Features Avançadas (Semanas 7-8)

#### 4.1 Câmera e Imagens

- [ ] Implementar captura de foto (AVFoundation)
- [ ] Implementar seleção de imagem da galeria
- [ ] Implementar crop de imagens
- [ ] Implementar upload para Firebase Storage

#### 4.2 Mapas e Localização

- [ ] Integrar MapKit
- [ ] Implementar seleção de localização
- [ ] Implementar busca de endereços (Geocoding)
- [ ] Implementar permissões de localização

#### 4.3 Biometria e Segurança

- [ ] Implementar autenticação biométrica (Face ID/Touch ID)
- [ ] Implementar verificação de identidade
- [ ] Integrar Vision Framework para face detection

#### 4.4 QR Code e PDF

- [ ] Implementar geração de QR Code (CoreImage)
- [ ] Implementar leitura de QR Code (AVFoundation)
- [ ] Implementar geração de PDFs (PDFKit)

#### 4.5 Background Tasks

- [ ] Implementar sincronização em background
- [ ] Configurar BackgroundTasks framework
- [ ] Implementar notificações push

### Fase 5: Polimento e Testes (Semana 9)

#### 5.1 Testes

- [ ] Testes unitários para ViewModels
- [ ] Testes unitários para Repositories
- [ ] Testes de integração Firebase
- [ ] Testes de UI (XCTest)

#### 5.2 Ajustes e Otimizações

- [ ] Otimizar performance
- [ ] Ajustar layouts para diferentes tamanhos de tela
- [ ] Implementar dark mode
- [ ] Ajustar acessibilidade

#### 5.3 Preparação para Release

- [ ] Configurar App Store Connect
- [ ] Criar ícone do app
- [ ] Criar screenshots para App Store
- [ ] Configurar TestFlight
- [ ] Preparar release notes

## Configurações Específicas iOS

### Info.plist Permissions

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>Precisamos da sua localização para mostrar serviços e produtos próximos</string>

<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>Precisamos da sua localização para rastreamento de pedidos</string>

<key>NSCameraUsageDescription</key>
<string>Precisamos da câmera para tirar fotos de produtos e documentos</string>

<key>NSPhotoLibraryUsageDescription</key>
<string>Precisamos acessar suas fotos para selecionar imagens</string>

<key>NSFaceIDUsageDescription</key>
<string>Usamos Face ID para autenticação segura</string>

<key>NSUserTrackingUsageDescription</key>
<string>Usamos dados de uso para melhorar sua experiência</string>
```



### Capabilities Necessárias

- Push Notifications
- Background Modes:
- Background fetch
- Remote notifications
- Background processing
- Sign in with Apple (opcional)
- In-App Purchase (se necessário)

## Comandos Úteis

### CocoaPods

```bash
# Instalar dependências
pod install

# Atualizar dependências
pod update

# Limpar cache
pod cache clean --all
```



### Build e Test

```bash
# Build para simulador
xcodebuild -workspace TaskGoApp.xcworkspace -scheme TaskGoApp -sdk iphonesimulator

# Build para dispositivo físico
xcodebuild -workspace TaskGoApp.xcworkspace -scheme TaskGoApp -sdk iphoneos

# Executar testes
xcodebuild test -workspace TaskGoApp.xcworkspace -scheme TaskGoApp -destination 'platform=iOS Simulator,name=iPhone 15'
```



### Firebase

```bash
# Instalar Firebase CLI
npm install -g firebase-tools

# Login Firebase
firebase login

# Deploy functions (mesmo projeto)
firebase deploy --only functions
```



## Checklist de Compilação e Teste

### Pré-requisitos para Compilar

- [ ] Xcode instalado e atualizado
- [ ] CocoaPods instalado
- [ ] Apple Developer Account configurada
- [ ] GoogleService-Info.plist adicionado
- [ ] Dependências instaladas (`pod install`)
- [ ] Bundle ID configurado
- [ ] Signing configurado no Xcode

### Pré-requisitos para Testar

- [ ] Simulador iOS configurado
- [ ] Dispositivo físico conectado (opcional)
- [ ] Firebase configurado e funcionando
- [ ] Stripe test keys configuradas
- [ ] Permissões configuradas no Info.plist

### Testes em Dispositivo Físico

- [ ] Provisioning Profile criado
- [ ] Certificado de desenvolvimento instalado
- [ ] Dispositivo registrado no Apple Developer
- [ ] Trust no computador configurado no dispositivo

## Recursos e Documentação

### Documentação Oficial

- [SwiftUI Documentation](https://developer.apple.com/documentation/swiftui)
- [Firebase iOS Documentation](https://firebase.google.com/docs/ios/setup)
- [Stripe iOS SDK](https://stripe.dev/stripe-ios/)
- [Apple Developer Documentation](https://developer.apple.com/documentation/)

### Tutoriais Recomendados

- SwiftUI Navigation
- Firebase iOS Integration
- Core Data com SwiftUI
- Combine Framework
- MapKit Integration

## Notas Importantes

1. **Compatibilidade**: iOS 15.0+ (para suporte a recursos modernos do SwiftUI)
2. **Arquitetura**: MVVM com Combine (similar ao Android)
3. **Cache Local**: Core Data ou Realm (decidir baseado em performance)
4. **Networking**: URLSession nativo ou Alamofire
5. **Image Loading**: Kingfisher (equivalente ao Coil no Android)
6. **State Management**: Combine Publishers + @Published properties

## Próximos Passos Imediatos

1. Configurar ambiente de desenvolvimento (Xcode, CocoaPods)