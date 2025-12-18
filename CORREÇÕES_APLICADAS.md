# Correções Aplicadas - Análise de Logs

## Resumo das Correções

Este documento detalha todas as correções aplicadas com base na análise dos logs fornecidos.

## 1. Erro: Coroutine Scope Left Composition

### Problema
Múltiplos erros de `LeftCompositionCancellationException` ocorriam quando tentávamos salvar configurações no Firebase dentro de `DisposableEffect.onDispose`, usando `rememberCoroutineScope()`. Quando o composable saía da composição, o escopo era cancelado, causando falha nas operações assíncronas.

### Solução
Substituímos `rememberCoroutineScope()` por um `CoroutineScope` persistente usando `SupervisorJob()` e `Dispatchers.IO`, que não é cancelado quando o composable sai da composição.

### Arquivos Corrigidos:
- `NotificationsSettingsScreen.kt`
- `PreferencesScreen.kt`
- `PrivacyScreen.kt`
- `SecuritySettingsScreen.kt`

### Código Aplicado:
```kotlin
val persistentScope = remember {
    CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
}
```

## 2. Erro: Firebase Secure Token API Bloqueado

### Problema
O erro `Requests to this API securetoken.googleapis.com method google.identity.securetoken.v1.SecureToken.GrantToken are blocked` aparecia repetidamente nos logs, impedindo operações de autenticação e Firestore.

### Solução
Adicionamos tratamento de erro específico para detectar e logar este erro de forma mais clara, facilitando a identificação do problema.

### Arquivos Corrigidos:
- `NotificationsSettingsScreen.kt`
- `PreferencesScreen.kt`
- `PrivacyScreen.kt`
- `SecuritySettingsScreen.kt`
- `SettingsUseCase.kt`

### Ação Necessária (Configuração Manual):
Este erro requer configuração no Google Cloud Console:
1. Acesse o Google Cloud Console
2. Navegue até "APIs & Services" > "Enabled APIs"
3. Certifique-se de que a "Identity and Access Management (IAM) API" está habilitada
4. Verifique as restrições de API Key no projeto Firebase
5. Certifique-se de que a API Key não tem restrições que bloqueiem o Secure Token API

## 3. Bug: Solicitação Contínua de Permissão de Localização

### Problema
O switch de compartilhamento de localização na tela de privacidade ficava ligando e desligando continuamente devido a solicitações repetidas de permissão.

### Solução
Adicionamos uma flag `hasRequestedLocationPermission` para garantir que a permissão seja solicitada apenas uma vez quando o switch é habilitado.

### Arquivo Corrigido:
- `PrivacyScreen.kt`

## 4. Melhorias de Tratamento de Erros

### Adicionado:
- Tratamento específico para erros do Secure Token API em todas as operações Firebase
- Logs mais informativos para facilitar debugging
- Mensagens de erro mais claras para o desenvolvedor

## 5. Warnings Não Críticos Identificados

### Navigation Warning
- `Ignoring popBackStack to destination -1229886412 as it was not found on the current back stack`
- **Status**: Não crítico - warning de navegação que não afeta funcionalidade
- **Ação**: Monitorar se ocorre frequentemente, pode indicar lógica de navegação a ser revisada

### Google Maps Warnings
- `AdvancedMarkers: false: Capabilities unavailable without a Map ID`
- **Status**: Não crítico - funcionalidade limitada mas não quebra o app
- **Ação**: Para habilitar Advanced Markers, criar um Map ID no Google Cloud Console

## Próximos Passos Recomendados

1. **Configurar Secure Token API** (CRÍTICO):
   - Habilitar a API no Google Cloud Console
   - Verificar restrições de API Key
   - Testar autenticação após configuração

2. **Monitorar Logs**:
   - Verificar se os erros de `LeftCompositionCancellationException` foram resolvidos
   - Monitorar frequência do warning de navegação

3. **Otimizações Futuras**:
   - Considerar criar um Map ID para habilitar Advanced Markers
   - Revisar lógica de navegação se o warning persistir

## Status das Correções

✅ **Corrigido**: Erro de coroutine scope left composition
✅ **Corrigido**: Bug de solicitação contínua de permissão
✅ **Melhorado**: Tratamento de erros do Secure Token API
⚠️ **Requer Ação Manual**: Configuração do Secure Token API no Google Cloud
ℹ️ **Monitorar**: Warnings de navegação e Google Maps

## Notas Técnicas

- O uso de `SupervisorJob()` garante que falhas em uma coroutine não cancelem outras
- `Dispatchers.IO` é apropriado para operações de rede/IO como chamadas Firebase
- O escopo persistente é criado uma vez com `remember` e reutilizado durante o ciclo de vida do composable

