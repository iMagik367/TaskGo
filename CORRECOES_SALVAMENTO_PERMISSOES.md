# Correções de Salvamento e Permissões

## Resumo das Correções Implementadas

### 1. Sistema de Solicitação de Permissões no Primeiro Acesso ✅

**Arquivos Modificados:**
- `app/src/main/java/com/taskgoapp/taskgo/data/local/datastore/PreferencesManager.kt`
- `app/src/main/java/com/taskgoapp/taskgo/feature/splash/presentation/SplashScreen.kt`
- `app/src/main/java/com/taskgoapp/taskgo/feature/splash/presentation/SplashViewModel.kt`

**Implementação:**
- Adicionada chave `PERMISSIONS_REQUESTED` no `PreferencesManager` para rastrear se as permissões já foram solicitadas
- Modificado `SplashScreen` para solicitar automaticamente as seguintes permissões no primeiro acesso:
  - `CAMERA`
  - `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION`
  - `POST_NOTIFICATIONS` (Android 13+)
  - `READ_MEDIA_IMAGES` (Android 13+) ou `READ_EXTERNAL_STORAGE` (versões anteriores)
- O sistema verifica quais permissões ainda não foram concedidas e solicita apenas essas
- Após solicitar permissões (ou se já foram concedidas), o app continua com a navegação normal

### 2. Melhorias no Salvamento com Retry Logic ✅

**Arquivos Modificados:**
- `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/AccountScreen.kt`
- `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/PreferencesScreen.kt`
- `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/NotificationsSettingsScreen.kt`
- `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/PrivacyScreen.kt`

**Implementação:**
- Criada função `retrySaveWithBackoff` em cada tela de configurações que implementa:
  - **Retry Logic**: Até 3 tentativas de salvamento
  - **Backoff Exponencial**: Delay de 1s, 2s, 4s entre tentativas
  - **Tratamento Específico**: Detecta erros do Secure Token API e não tenta novamente (requer configuração manual no Google Cloud)
  - **Logging Detalhado**: Registra cada tentativa e erro para facilitar debugging

### 3. Aumento do Debounce e Melhor Gerenciamento de Jobs ✅

**Mudanças:**
- Debounce aumentado de **1 segundo para 2 segundos** em todas as telas de configurações
- Debounce aumentado de **500ms para 2 segundos** na tela de notificações
- Uso de `persistentScope` (CoroutineScope com SupervisorJob) para garantir que operações continuem mesmo após o Composable sair da composição
- Cancelamento adequado de jobs anteriores antes de criar novos

### 4. Tratamento de Erros Específicos ✅

**Erros Tratados:**
- **Secure Token API Bloqueado**: Detectado e logado como warning, não tenta novamente
- **Firestore FAILED_PRECONDITION**: Tratado no retry logic (pode indicar índice faltando)
- **Job Cancellation**: Prevenido com debounce aumentado e gerenciamento adequado de jobs

## Problemas Conhecidos que Requerem Ação Manual

### 1. Secure Token API Bloqueado ⚠️

**Erro:**
```
Requests to this API securetoken.googleapis.com method google.identity.securetoken.v1.SecureToken.GrantToken are blocked.
```

**Causa:**
- API não habilitada no Google Cloud Console
- Restrições na API key
- Problemas de configuração do projeto Firebase

**Solução:**
1. Acesse o [Google Cloud Console](https://console.cloud.google.com/)
2. Selecione o projeto `task-go-ee85f`
3. Vá para **APIs & Services > Enabled APIs**
4. Procure por **Identity Toolkit API** e **Secure Token Service API**
5. Certifique-se de que ambas estão habilitadas
6. Verifique as restrições da API key em **APIs & Services > Credentials**

### 2. Firestore Index Faltando ⚠️

**Erro:**
```
FAILED_PRECONDITION: The query requires an index.
```

**Solução:**
- O erro fornece um link direto para criar o índice no Firebase Console
- Acesse o link fornecido no erro para criar o índice automaticamente
- Ou crie manualmente em **Firestore > Indexes**

## Melhorias Implementadas

### Performance
- ✅ Debounce aumentado reduz operações desnecessárias
- ✅ Retry logic garante que falhas temporárias sejam recuperadas
- ✅ Backoff exponencial evita sobrecarga do servidor

### Confiabilidade
- ✅ Salvamento automático com debounce em todas as telas
- ✅ Salvamento final garantido ao sair da tela
- ✅ Tratamento robusto de erros

### UX
- ✅ Permissões solicitadas automaticamente no primeiro acesso
- ✅ App continua funcionando mesmo se algumas permissões forem negadas
- ✅ Feedback adequado através de logs (pode ser melhorado com UI)

## Próximos Passos Recomendados

1. **Habilitar Secure Token API** no Google Cloud Console (ação manual necessária)
2. **Criar índices do Firestore** conforme solicitado pelos erros
3. **Testar salvamento** em todas as telas de configurações após as correções
4. **Monitorar logs** para verificar se os erros foram resolvidos
5. **Considerar adicionar feedback visual** para o usuário quando salvamento falhar após múltiplas tentativas

