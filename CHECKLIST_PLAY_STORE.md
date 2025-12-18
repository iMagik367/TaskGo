# 📋 Checklist Completo - Preparação para Google Play Store

**Data:** 2024  
**Versão do App:** 1.0.1 (versionCode: 2)  
**Status:** ⚠️ **NÃO PRONTO** - Requer correções antes do lançamento

---

## 🔴 CRÍTICO - Deve ser corrigido ANTES do lançamento

### 1. **Política de Privacidade e Termos de Uso** ❌
- **Status:** Não implementado
- **Localização:** `AboutScreen.kt` e `PrivacyScreen.kt`
- **Problema:** Botões com TODOs, não abrem telas/documentos
- **Impacto:** Obrigatório pela Google Play Store e LGPD
- **Ação:** Implementar telas ou links para documentos web

### 2. **Configuração do Facebook** ⚠️
- **Status:** Valores placeholder
- **Localização:** `app/src/main/res/values/auth_config.xml`
- **Problema:** `facebook_app_id` e `facebook_client_token` com valores placeholder
- **Impacto:** Login com Facebook não funcionará, pode causar crashes
- **Ação:** Configurar valores reais ou remover se não usar Facebook

### 3. **Signing Config para Release** ⚠️
- **Status:** Comentado no `build.gradle.kts`
- **Problema:** Não há configuração de assinatura para builds de release
- **Impacto:** Não é possível gerar APK/AAB assinado para Play Store
- **Ação:** Criar keystore e configurar `keystore.properties`

### 4. **Firebase Crashlytics** ⚠️
- **Status:** Não encontrado
- **Problema:** Não há integração com Crashlytics para monitoramento de crashes
- **Impacto:** Não será possível monitorar crashes em produção
- **Ação:** Adicionar Firebase Crashlytics

---

## 🟡 IMPORTANTE - Recomendado antes do lançamento

### 5. **TODOs em Funcionalidades** ⚠️
- **Localização:** Vários arquivos
- **Problemas encontrados:**
  - Exclusão de produtos (TODO)
  - Aceitar/rejeitar propostas (TODO)
  - Alguns placeholders em formulários
- **Impacto:** Funcionalidades incompletas podem confundir usuários
- **Ação:** Implementar ou remover funcionalidades não implementadas

### 6. **Validação de Formulários** ✅
- **Status:** Implementado parcialmente
- **Observação:** Validações básicas presentes, mas podem ser melhoradas

### 7. **Tratamento de Erros** ✅
- **Status:** Implementado
- **Localização:** `ErrorHandler.kt`, `FirebaseErrorHandler.kt`
- **Observação:** Sistema de tratamento de erros presente

### 8. **ProGuard/R8** ✅
- **Status:** Configurado
- **Localização:** `proguard-rules.pro`
- **Observação:** Regras configuradas para Firebase, Hilt, Retrofit, etc.

---

## ✅ VERIFICADO - Está correto

### 9. **Configurações de Build** ✅
- **versionCode:** 2
- **versionName:** "1.0.1"
- **minSdk:** 24 (Android 7.0)
- **targetSdk:** 34 (Android 14)
- **compileSdk:** 34

### 10. **Permissões** ✅
- Todas as permissões necessárias estão no `AndroidManifest.xml`
- `POST_NOTIFICATIONS` presente
- Permissões de câmera, localização, etc. configuradas corretamente

### 11. **Segurança** ✅
- `usesCleartextTraffic="false"` ✅
- Firebase App Check configurado ✅
- Network Security Config presente ✅

### 12. **Funcionalidades Principais** ✅
- Autenticação: ✅ Implementada
- Produtos: ✅ Implementada (com bloqueio de documentos)
- Serviços: ✅ Implementada (com bloqueio de documentos)
- Mensagens: ✅ Implementada
- Notificações: ✅ Implementada (sem mocks)
- Pedidos: ✅ Implementada (sem mocks)
- Perfil: ✅ Implementada
- Checkout/Pagamento: ✅ Implementada

### 13. **Integração Firebase** ✅
- Firestore: ✅ Configurado
- Authentication: ✅ Configurado
- Storage: ✅ Configurado
- Functions: ✅ Configurado
- App Check: ✅ Configurado

### 14. **Remoção de Mocks** ✅
- Notificações: ✅ Sem mocks
- Pedidos: ✅ Sem mocks
- Dados reais do Firestore: ✅ Implementado

---

## 📝 AÇÕES NECESSÁRIAS ANTES DO LANÇAMENTO

### Prioridade ALTA (Crítico):
1. ✅ Implementar telas de Política de Privacidade e Termos de Uso
2. ✅ Configurar ou remover Facebook (se não usar)
3. ✅ Configurar signing config para release builds
4. ✅ Adicionar Firebase Crashlytics

### Prioridade MÉDIA (Recomendado):
5. Implementar ou remover TODOs em funcionalidades
6. Melhorar validações de formulários
7. Adicionar analytics (Firebase Analytics)

### Prioridade BAIXA (Opcional):
8. Otimizar imagens e recursos
9. Adicionar testes automatizados
10. Documentação adicional

---

## 🎯 PRÓXIMOS PASSOS

1. **Implementar Política de Privacidade e Termos de Uso**
   - Criar telas ou links para documentos web
   - Atualizar navegação

2. **Configurar Signing para Release**
   - Criar keystore
   - Configurar `keystore.properties`
   - Descomentar signing config no `build.gradle.kts`

3. **Configurar Facebook ou Remover**
   - Se usar: Configurar App ID e Client Token reais
   - Se não usar: Remover configurações do manifest

4. **Adicionar Firebase Crashlytics**
   - Adicionar dependência
   - Inicializar no `TaskGoApp.kt`
   - Configurar ProGuard rules

5. **Testar Build de Release**
   - Gerar AAB assinado
   - Testar em dispositivo físico
   - Verificar se todas as funcionalidades funcionam

---

## 📊 RESUMO

- **Total de itens críticos:** 4
- **Total de itens importantes:** 4
- **Total de itens verificados:** 6
- **Status geral:** ⚠️ **NÃO PRONTO** - Requer correções críticas

**Estimativa de tempo para correções:** 2-4 horas

