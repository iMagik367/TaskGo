# APIs do Google Cloud que Precisam ser Ativadas

Para que o chat com IA funcione completamente, você precisa ativar as seguintes APIs no Google Cloud Console:

## APIs Obrigatórias:

1. **Generative Language API (Gemini API)**
   - URL: https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com
   - Uso: Chat com IA usando o modelo Gemini Pro
   - Status: ✅ Já configurada no código

2. **Cloud Translation API**
   - URL: https://console.cloud.google.com/apis/library/translate.googleapis.com
   - Uso: Tradução de mensagens no chat (entre usuários e com a IA)
   - Status: ✅ Já configurada no código

3. **Cloud Speech-to-Text API**
   - URL: https://console.cloud.google.com/apis/library/speech.googleapis.com
   - Uso: Converter fala em texto através do microfone
   - Status: ✅ Já configurada no código

## Como Ativar:

1. Acesse o Google Cloud Console: https://console.cloud.google.com/
2. Selecione seu projeto (ou crie um novo)
3. Vá em "APIs & Services" > "Library"
4. Busque e ative cada uma das APIs listadas acima
5. Certifique-se de que a API Key fornecida tem permissões para essas APIs

## API Key Configurada:

A API Key `AIzaSyAx7exddPWH1WyrfofgmQUC-7f3tKNa0Ww` está configurada no código em:
- `app/src/main/java/com/taskgoapp/taskgo/di/AIModule.kt`

## Permissões Necessárias:

A API Key precisa ter as seguintes permissões:
- ✅ Generative Language API (para chat com IA)
- ✅ Cloud Translation API (para tradução)
- ✅ Cloud Speech-to-Text API (para reconhecimento de voz)

## Restrições de API Key (Recomendado):

Para segurança, configure restrições na API Key:
1. Vá em "APIs & Services" > "Credentials"
2. Selecione sua API Key
3. Em "API restrictions", selecione "Restrict key"
4. Selecione apenas as 3 APIs mencionadas acima
5. Em "Application restrictions", configure restrições por plataforma (Android)

## Notas Importantes:

- A API Key está hardcoded no código. Para produção, considere usar:
  - `local.properties` para desenvolvimento
  - Firebase Remote Config para produção
  - Ou variáveis de ambiente seguras

- O Speech-to-Text requer permissão de microfone no AndroidManifest.xml (já adicionada)

- A tradução funciona tanto para mensagens da IA quanto entre usuários no chat


