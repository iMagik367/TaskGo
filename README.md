# TaskGo App

Aplicativo Android para marketplace de serviços e produtos.

## 🚀 Status

- ✅ Build: SUCCESSFUL
- ✅ Firebase: Configurado
- ✅ Autenticação: Login, Cadastro e Google Sign-In funcionando
- ✅ Firestore: Integrado
- ✅ Navegação: Funcional

## 📱 Tecnologias

- **Android** com Kotlin
- **Jetpack Compose** para UI
- **Firebase** (Auth, Firestore, Functions, Storage)
- **Hilt** para Injeção de Dependências
- **Navigation Compose** para navegação
- **Room** para cache local
- **WorkManager** para tarefas em background

## 🔧 Configuração

1. Configure o `google-services.json` no diretório `app/`
2. Configure as variáveis de ambiente no Firebase Console
3. Execute `./gradlew assembleDebug` para build

## ⚠️ Firebase App Hosting

Este é um projeto Android, não um projeto web. O Firebase App Hosting está desabilitado via `apphosting.yaml`.

Se você ainda estiver recebendo erros de build do App Hosting, desabilite-o no Console do Firebase:
1. Acesse o [Firebase Console](https://console.firebase.google.com)
2. Selecione o projeto `task-go-ee85f`
3. Vá em **App Hosting**
4. Desabilite o build automático para este repositório

## 📦 Estrutura do Projeto

```
app/
├── src/main/java/com/example/taskgoapp/
│   ├── feature/          # Features do app
│   ├── data/             # Repositórios e mappers
│   ├── di/               # Módulos Hilt
│   ├── core/             # Componentes core
│   └── navigation/       # Navegação
└── build.gradle.kts      # Configuração do módulo
```

## 🔐 Autenticação

O app suporta:
- Login com email/senha
- Cadastro com email/senha
- Login com Google

## 📝 Licença

Este projeto é privado.
