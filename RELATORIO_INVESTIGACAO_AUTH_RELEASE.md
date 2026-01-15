# RELATÓRIO DE INVESTIGAÇÃO - AUTENTICAÇÃO FALHANDO EM RELEASE

## ETAPA 1 - IDENTIFICAÇÃO DO AMBIENTE REAL

### ✅ CONFIGURAÇÃO DO BUILD

**applicationId:** `com.taskgoapp.taskgo`
**namespace:** `com.taskgoapp.taskgo`
**versionCode:** 17
**versionName:** 1.0.16
**targetSdk:** 35
**compileSdk:** 35

### ✅ BUILD TYPES

**DEBUG:**
- Minify: false
- ShrinkResources: false
- API Base URL: `http://10.0.2.2:8091/v1/`
- App Check: Habilitado (DebugAppCheckProviderFactory)

**RELEASE:**
- Minify: **true** ⚠️
- ShrinkResources: **true** ⚠️
- API Base URL: `https://api.taskgo.com/v1/`
- App Check: Habilitado (PlayIntegrityAppCheckProviderFactory)
- ProGuard: **ATIVO** ⚠️

### ⚠️ PROBLEMA IDENTIFICADO #1: PROGUARD ATIVO SEM REGRAS ADEQUADAS

O release está com minify e shrinkResources ativos, mas as regras do ProGuard podem estar removendo classes críticas do Firebase.

---

## ETAPA 2 - GOOGLE-SERVICES.JSON

### ✅ VERIFICAÇÃO

**package_name:** `com.taskgoapp.taskgo` ✅ CORRETO
**mobilesdk_app_id:** `1:1093466748007:android:55d3d395716e81c4e8d0c2` ✅
**project_id:** `task-go-ee85f` ✅
**Localização:** `app/google-services.json` ✅

**Status:** ✅ ARQUIVO CORRETO E NO LUGAR CERTO

---

## ETAPA 3 - SHA-1 E SHA-256 (CRÍTICO)

### ✅ FINGERPRINTS DO KEYSTORE DE RELEASE

**SHA-1:** `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
**SHA-256:** `95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18`

### ⚠️ AÇÃO NECESSÁRIA

**VERIFICAR NO FIREBASE CONSOLE:**
1. Acesse: Firebase Console → Configurações do Projeto → Android App
2. Confirme se AMBOS os fingerprints estão cadastrados:
   - SHA-1: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
   - SHA-256: `95:AF:63:3A:8F:CD:20:49:A2:59:89:FB:86:71:D8:DE:0F:11:89:CF:D7:82:7F:50:45:1C:FB:E7:98:CF:37:18`

**IMPORTANTE:** Se o app está na Play Store, também verifique o SHA-256 do App Signing Key (diferente do upload key).

---

## ETAPA 4 - FIREBASE APP CHECK

### ✅ CONFIGURAÇÃO NO CÓDIGO

**DEBUG:**
- Provider: `DebugAppCheckProviderFactory`
- Token: Configurado via SharedPreferences
- Status: ✅ Funcionando

**RELEASE:**
- Provider: `PlayIntegrityAppCheckProviderFactory`
- Status: ⚠️ **PROBLEMA CRÍTICO IDENTIFICADO**

### 🔴 PROBLEMA IDENTIFICADO #2: APP CHECK COM ENFORCEMENT

O código está configurando Play Integrity, mas:
1. Se o Play Integrity não estiver configurado no Firebase Console → Token inválido
2. Se o enforcement estiver ativo → Firebase Auth REJEITA requisições sem token válido
3. Se o SHA-256 não estiver cadastrado → Play Integrity falha silenciosamente

### ✅ SOLUÇÃO TEMPORÁRIA PARA TESTE

Desativar App Check temporariamente para confirmar se é a causa:
- Adicionar `enableAppCheck=false` no `local.properties`
- Recompilar release
- Testar login

---

## ETAPA 5 - PLAY INTEGRITY

### ⚠️ VERIFICAÇÕES NECESSÁRIAS

1. **App instalado via Play Store?**
   - Play Integrity SÓ funciona se o app vier da Play Store
   - Instalação via APK local → Play Integrity falha

2. **Play Integrity API habilitada?**
   - Google Cloud Console → APIs → Play Integrity API
   - Deve estar habilitada para o projeto `task-go-ee85f`

3. **SHA-256 cadastrado no Firebase?**
   - Firebase Console → App Check → Play Integrity
   - SHA-256 deve estar registrado

---

## ETAPA 6 - LOGS DE AUTH

### ✅ LOGS ATUAIS

O código já possui logs em:
- `FirebaseAuthRepository.kt` - Logs detalhados de erro
- `LoginViewModel.kt` - Logs de tentativa de login
- `TaskGoApp.kt` - Logs de inicialização do App Check

### ⚠️ MELHORIAS NECESSÁRIAS

Adicionar logs mais explícitos antes/depois de cada operação crítica.

---

## ETAPA 7 - PROGUARD / R8

### ⚠️ PROBLEMA CRÍTICO IDENTIFICADO #3

O arquivo `proguard-rules.pro` precisa ser verificado para garantir que:
- Classes do Firebase Auth não sejam removidas
- Classes do App Check não sejam removidas
- Classes do Play Integrity não sejam removidas

---

## ETAPA 8 - DIAGNÓSTICO FINAL

### 🔴 CAUSA PROVÁVEL DO PROBLEMA

Com base na investigação, a causa mais provável é:

**APP CHECK COM PLAY INTEGRITY NÃO CONFIGURADO CORRETAMENTE**

O erro "Firebase App Check token is invalid" indica que:
1. O Play Integrity está tentando gerar um token
2. O token está sendo rejeitado pelo Firebase
3. O Firebase Auth está bloqueando a requisição porque o App Check falhou

### ✅ CORREÇÕES NECESSÁRIAS

1. **Verificar SHA-256 no Firebase Console**
2. **Verificar Play Integrity API habilitada**
3. **Adicionar regras ProGuard para Firebase**
4. **Desativar App Check temporariamente para teste**
5. **Adicionar logs mais detalhados**

---

## CHECKLIST DE CORREÇÃO

- [ ] Verificar SHA-1 e SHA-256 no Firebase Console
- [ ] Verificar Play Integrity API habilitada no Google Cloud
- [ ] Verificar SHA-256 cadastrado no App Check (Firebase Console)
- [ ] Adicionar regras ProGuard para Firebase
- [ ] Testar release SEM App Check
- [ ] Testar release COM App Check SEM enforcement
- [ ] Testar release COM App Check + Play Integrity configurado
- [ ] Verificar logs detalhados em cada teste


