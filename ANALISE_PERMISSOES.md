# 🔍 Análise Completa de Permissões do App

## 📋 Status Atual das Permissões

### ✅ Permissões Declaradas no AndroidManifest.xml

1. ✅ `INTERNET` - Declarada
2. ✅ `ACCESS_NETWORK_STATE` - Declarada
3. ✅ `POST_NOTIFICATIONS` - Declarada (Android 13+)
4. ✅ `READ_MEDIA_IMAGES` - Declarada (Android 13+)
5. ✅ `READ_EXTERNAL_STORAGE` - Declarada (Android ≤32)
6. ✅ `WRITE_EXTERNAL_STORAGE` - Declarada (Android ≤28)
7. ✅ `CAMERA` - Declarada
8. ✅ `ACCESS_COARSE_LOCATION` - Declarada
9. ✅ `ACCESS_FINE_LOCATION` - Declarada
10. ✅ `USE_BIOMETRIC` - Declarada
11. ✅ `USE_FINGERPRINT` - Declarada

---

## ❌ Problemas Identificados

### 1. **PERMISSÕES EM RUNTIME NÃO ESTÃO SENDO SOLICITADAS**

#### Problema:
O app usa `ActivityResultContracts.GetContent()` e `TakePicture()`, mas:
- **Android 13+ (API 33+)**: `PickVisualMedia` requer `READ_MEDIA_IMAGES` em runtime
- **Câmera**: `TakePicture()` requer `CAMERA` em runtime (mesmo com contract)
- **Notificações**: `POST_NOTIFICATIONS` precisa ser solicitada em runtime para Android 13+

#### Impacto:
- App pode falhar ao tentar acessar galeria/câmera sem solicitar permissões
- Notificações podem não funcionar
- Usuário pode ver erros ou o app pode crashar

---

### 2. **CÂMERA NÃO ESTÁ SENDO USADA CORRETAMENTE**

**Arquivos afetados:**
- `app/src/main/java/com/taskgoapp/taskgo/core/design/ImagePicker.kt`
- `app/src/main/java/com/taskgoapp/taskgo/core/design/ImageEditor.kt`

**Problema:**
- `cameraLauncher` é criado mas não é usado
- Botão "Câmera" abre galeria em vez da câmera
- Não há criação de arquivo temporário para foto da câmera

---

### 3. **FALTA VERIFICAÇÃO DE PERMISSÕES ANTES DE USAR RECURSOS**

**Arquivos afetados:**
- `ImagePicker.kt`
- `SimpleImageCropper.kt`
- `IdentityVerificationScreen.kt`
- `AccountScreen.kt`
- `ProductFormScreen.kt`

**Problema:**
- Nenhum código verifica se a permissão foi concedida antes de usar
- Pode causar crashes ou comportamento inesperado

---

### 4. **LOCALIZAÇÃO DECLARADA MAS NÃO USADA**

**Problema:**
- Permissões de localização estão declaradas
- Não encontrei código que use GPS/localização
- Pode ser rejeitado na Play Store se não for usado

**Recomendação:**
- Remover se não for usar
- OU implementar funcionalidade de localização
- OU adicionar justificativa para a Play Store

---

### 5. **READ_MEDIA_VIDEO FALTANDO (SE FOR USAR VÍDEOS)**

**Status:** Não declarada
**Recomendação:** Adicionar se o app permitir upload de vídeos no futuro

---

### 6. **PERMISSÕES DO ANDROID 14+ (API 34)**

**Android 14+ introduziu:**
- `READ_MEDIA_VISUAL_USER_SELECTED` - Para acesso parcial a mídia
- Verificar se precisa ser adicionada

---

## 🔧 Correções Necessárias

### Prioridade ALTA 🔴

1. **Solicitar permissões em runtime antes de usar recursos**
2. **Corrigir uso da câmera**
3. **Adicionar verificação de permissões**

### Prioridade MÉDIA 🟡

4. **Decidir sobre permissões de localização**
5. **Adicionar tratamento de erro quando permissão é negada**

### Prioridade BAIXA 🟢

6. **Adicionar justificativas de permissões para Play Store**
7. **Adicionar READ_MEDIA_VISUAL_USER_SELECTED se necessário**

---

## 📝 Requisitos da Google Play Store

### Permissões Sensíveis

A Google Play Store exige que você:
1. **Justifique o uso** de permissões sensíveis
2. **Solicite apenas quando necessário** (não na inicialização)
3. **Forneça explicação clara** ao usuário sobre por que precisa da permissão

### Permissões que precisam de justificativa:

- ✅ `CAMERA` - Usado para: fotos de perfil, documentos de verificação
- ✅ `READ_MEDIA_IMAGES` - Usado para: selecionar imagens da galeria
- ✅ `ACCESS_FINE_LOCATION` - **NÃO USADO** - Remover ou justificar
- ✅ `ACCESS_COARSE_LOCATION` - **NÃO USADO** - Remover ou justificar
- ✅ `POST_NOTIFICATIONS` - Usado para: notificações push

---

## 🎯 Plano de Ação

1. ✅ Adicionar permissões faltando no manifest
2. ✅ Criar sistema de solicitação de permissões em runtime
3. ✅ Corrigir uso da câmera
4. ✅ Adicionar verificação de permissões antes de usar recursos
5. ⚠️ Decidir sobre localização
6. ⚠️ Adicionar justificativas para Play Store

