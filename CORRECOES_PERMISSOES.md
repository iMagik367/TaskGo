# 🔧 Correções de Permissões - Resumo Executivo

## 📊 Problemas Encontrados e Soluções

### ✅ 1. Criado Sistema de Permissões

**Arquivo criado:** `app/src/main/java/com/taskgoapp/taskgo/core/permissions/PermissionHandler.kt`

**Funcionalidades:**
- Funções helper para verificar permissões
- Composables para criar launchers de permissões
- Suporte a Android 13+ e versões anteriores

---

### 🔴 2. Problemas Críticos Identificados

#### A. Permissões não solicitadas em runtime
- **Câmera**: Não solicita permissão antes de usar
- **Galeria**: Não solicita `READ_MEDIA_IMAGES` no Android 13+
- **Notificações**: Não solicita `POST_NOTIFICATIONS` no Android 13+

#### B. Câmera não funciona
- `cameraLauncher` criado mas não usado
- Botão "Câmera" abre galeria
- Falta criação de arquivo temporário para foto

#### C. Localização declarada mas não usada
- Pode causar rejeição na Play Store
- Precisa decidir: remover ou implementar

---

## 🎯 Próximas Correções Necessárias

### Prioridade ALTA (Fazer Agora)

1. **Corrigir ImagePicker.kt**
   - Adicionar verificação de permissões
   - Implementar uso correto da câmera
   - Solicitar permissões antes de usar

2. **Corrigir SimpleImageCropper.kt**
   - Adicionar verificação de permissão de galeria
   - Solicitar permissão antes de abrir

3. **Corrigir IdentityVerificationScreen.kt**
   - Adicionar verificação de permissões
   - Solicitar antes de abrir seletores de imagem

4. **Adicionar solicitação de notificações**
   - Solicitar no primeiro uso ou na tela de configurações

### Prioridade MÉDIA

5. **Decidir sobre localização**
   - Opção A: Remover do manifest (se não usar)
   - Opção B: Implementar funcionalidade de localização
   - Opção C: Manter e justificar na Play Store

6. **Adicionar tratamento de permissão negada**
   - Mostrar diálogo explicando por que precisa
   - Oferecer ir para configurações

### Prioridade BAIXA

7. **Adicionar justificativas para Play Store**
   - Preparar texto explicando uso de cada permissão

---

## 📝 Como Usar o PermissionHandler

### Exemplo: Solicitar Permissão de Câmera

```kotlin
@Composable
fun MyScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    
    val cameraPermissionLauncher = rememberCameraPermissionLauncher(
        onPermissionGranted = { hasPermission = true },
        onPermissionDenied = { /* mostrar erro */ }
    )
    
    // Verificar se já tem permissão
    LaunchedEffect(Unit) {
        hasPermission = PermissionHandler.hasCameraPermission(context)
    }
    
    // Solicitar permissão
    Button(onClick = {
        if (!hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // Usar câmera
        }
    }) {
        Text("Tirar Foto")
    }
}
```

---

## ⚠️ Notas Importantes

1. **Android 13+ (API 33+)**
   - `READ_MEDIA_IMAGES` substitui `READ_EXTERNAL_STORAGE` para imagens
   - `POST_NOTIFICATIONS` precisa ser solicitada em runtime
   - `PickVisualMedia` pode funcionar sem permissão em alguns casos, mas é melhor solicitar

2. **Android 14+ (API 34+)**
   - `READ_MEDIA_VISUAL_USER_SELECTED` para acesso parcial
   - Verificar se precisa ser adicionada

3. **Play Store**
   - Todas as permissões sensíveis precisam de justificativa
   - Solicitar apenas quando necessário (não na inicialização)
   - Fornecer explicação clara ao usuário

---

## ✅ Checklist de Correções

- [x] Criar `PermissionHandler.kt`
- [ ] Corrigir `ImagePicker.kt`
- [ ] Corrigir `SimpleImageCropper.kt`
- [ ] Corrigir `IdentityVerificationScreen.kt`
- [ ] Adicionar solicitação de notificações
- [ ] Decidir sobre localização
- [ ] Adicionar tratamento de permissão negada
- [ ] Testar em Android 13+
- [ ] Testar em Android 12 e anteriores
- [ ] Preparar justificativas para Play Store

