# 🔧 Correções para Problema de Login no Firebase

## Problema Identificado

O aplicativo estava apresentando erro de "sem conexão com internet" ao tentar fazer login no dispositivo físico.

## Causas Identificadas

1. **Falta de permissão INTERNET** - Embora geralmente implícita, é melhor declarar explicitamente
2. **Firebase App Check bloqueando requisições** - App Check estava configurado apenas com Play Integrity, que não funciona em builds de debug
3. **Logs insuficientes** - Difícil diagnosticar o problema sem logs detalhados
4. **Tratamento de erros genérico** - Não estava identificando corretamente os tipos de erro

## Correções Aplicadas

### 1. ✅ Permissões de Internet Adicionadas

**Arquivo:** `app/src/main/AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 2. ✅ Firebase App Check Corrigido

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/TaskGoApp.kt`

**Mudanças:**
- Adicionado suporte para **DebugAppCheckProviderFactory** em builds de debug
- Mantido **PlayIntegrityAppCheckProviderFactory** apenas para builds de release
- Adicionado logs detalhados da inicialização
- Adicionado código para obter e logar o token de debug

**Como funciona:**
- **Debug builds:** Usa `DebugAppCheckProviderFactory` (não requer configuração no Firebase)
- **Release builds:** Usa `PlayIntegrityAppCheckProviderFactory` (requer Play Integrity configurado)

### 3. ✅ Logs Detalhados Adicionados

**Arquivos modificados:**
- `TaskGoApp.kt` - Logs de inicialização do Firebase
- `FirebaseAuthRepository.kt` - Logs detalhados de tentativas de login
- `LoginViewModel.kt` - Logs melhorados de tratamento de erros

### 4. ✅ Tratamento de Erros Melhorado

**Arquivo:** `app/src/main/java/com/taskgoapp/taskgo/feature/auth/presentation/LoginViewModel.kt`

**Melhorias:**
- Identificação de diferentes tipos de erro de rede
- Mensagens de erro mais específicas
- Logs detalhados para diagnóstico

### 5. ✅ Dependência Adicionada

**Arquivo:** `app/build.gradle.kts`

```kotlin
implementation("com.google.firebase:firebase-appcheck-debug")
```

---

## ⚠️ IMPORTANTE: Token de Debug do App Check

Quando você executar o app em modo debug pela primeira vez, verifique os logs do Android Studio. Você verá algo como:

```
App Check Debug Token: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
Adicione este token no Firebase Console > App Check > Manage debug tokens
```

**Se você quiser que o App Check funcione em modo debug, você precisa:**

1. Copiar o token de debug dos logs
2. Ir para [Firebase Console](https://console.firebase.google.com)
3. Selecionar seu projeto: `task-go-ee85f`
4. Ir em **App Check** > selecionar seu app Android
5. Clicar em **Manage debug tokens**
6. Adicionar o token copiado
7. Clicar em **Add**

**NOTA:** Se você **NÃO** adicionar o token de debug, o App Check pode bloquear requisições em modo debug. No entanto, para desenvolvimento básico, o App Check pode não ser estritamente necessário.

---

## 🧪 Como Testar

### 1. Limpar e Rebuildar o Projeto

```bash
./gradlew.bat clean
./gradlew.bat assembleDebug
```

### 2. Instalar no Dispositivo

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Verificar Logs

Execute o app e verifique os logs no Android Studio ou via adb:

```bash
adb logcat | grep -E "TaskGoApp|FirebaseAuthRepository|LoginViewModel"
```

### 4. Tentar Fazer Login

Tente fazer login e observe os logs. Você deve ver:
- Logs de inicialização do Firebase
- Logs detalhados da tentativa de login
- Logs de qualquer erro que ocorrer

---

## 🔍 Diagnóstico de Problemas

### Se ainda não funcionar:

1. **Verifique os logs:**
   - Procure por mensagens de erro específicas
   - Verifique se o Firebase está inicializando corretamente
   - Verifique se há erros de App Check

2. **Verifique a conexão:**
   - Certifique-se de que o dispositivo tem internet
   - Tente acessar outros apps que usam internet
   - Verifique se há firewall bloqueando conexões

3. **Verifique o Firebase Console:**
   - Certifique-se de que o projeto Firebase está ativo
   - Verifique se há limites de quota excedidos
   - Verifique se o `google-services.json` está correto

4. **Teste sem App Check (temporariamente):**
   - Comente a inicialização do App Check no `TaskGoApp.kt`
   - Rebuild e teste novamente
   - Se funcionar, o problema é com App Check

---

## 📝 Próximos Passos

1. **Testar o login** no dispositivo físico
2. **Verificar os logs** para ver o token de debug (se necessário)
3. **Adicionar o token de debug** no Firebase Console (se quiser App Check em debug)
4. **Verificar se o login funciona** agora

---

## 📚 Referências

- [Firebase App Check - Debug Tokens](https://firebase.google.com/docs/app-check/android/debug-provider)
- [Firebase Auth - Error Codes](https://firebase.google.com/docs/auth/android/errors)
- [Android Network Security Config](https://developer.android.com/training/articles/security-config)

