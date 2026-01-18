# 🔧 Como Configurar o Chat IA

O erro "AI service unavailable" ocorre quando nenhuma chave de API de IA está configurada. Siga estes passos para resolver:

## 📋 Pré-requisitos

1. **Firebase CLI instalado e autenticado**
   ```bash
   firebase login
   ```

2. **Pelo menos uma chave de API:**
   - **Gemini API Key** (recomendado - gratuito): https://aistudio.google.com/app/apikey
   - **OpenAI API Key** (opcional - pago): https://platform.openai.com/api-keys

## 🚀 Configuração Rápida

### Opção 1: Usando Gemini (Recomendado - Gratuito)

```bash
# No diretório functions/
cd functions
firebase functions:secrets:set GEMINI_API_KEY
# Cole sua chave quando solicitado
```

### Opção 2: Usando OpenAI

```bash
# No diretório functions/
cd functions
firebase functions:secrets:set OPENAI_API_KEY
# Cole sua chave quando solicitado
```

### Opção 3: Configurar Ambos (Fallback Automático)

Configure ambas as chaves para ter fallback automático:
- Se OpenAI falhar, usa Gemini automaticamente
- Se Gemini falhar, usa OpenAI automaticamente

```bash
cd functions
firebase functions:secrets:set GEMINI_API_KEY
firebase functions:secrets:set OPENAI_API_KEY
```

## 🔄 Após Configurar

**IMPORTANTE:** Faça redeploy das functions para aplicar as mudanças:

```bash
firebase deploy --only functions
```

## ✅ Verificação

Após o deploy, teste o chat IA no app. Se ainda houver erro, verifique os logs:

```bash
firebase functions:log --only aiChatProxy
```

## 🐛 Troubleshooting

### Erro: "AI service unavailable"

**Causa:** Nenhuma chave de API configurada ou secrets não foram aplicadas.

**Solução:**
1. Verifique se as secrets foram configuradas:
   ```bash
   firebase functions:secrets:access GEMINI_API_KEY
   ```

2. Se não estiver configurada, configure novamente:
   ```bash
   firebase functions:secrets:set GEMINI_API_KEY
   ```

3. Faça redeploy:
   ```bash
   firebase deploy --only functions
   ```

### Erro: "AI service unavailable after multiple attempts"

**Causa:** Ambas as APIs falharam (problema de rede, chave inválida, ou quota excedida).

**Solução:**
1. Verifique se as chaves estão válidas
2. Verifique se há quota disponível
3. Verifique os logs para mais detalhes:
   ```bash
   firebase functions:log --only aiChatProxy
   ```

## 📝 Notas

- **Gemini** é gratuito e recomendado para começar
- **OpenAI** oferece melhor qualidade mas é pago
- O sistema usa **fallback automático**: tenta OpenAI primeiro, se falhar usa Gemini
- As secrets são **seguras** e não aparecem no código

## 🔗 Links Úteis

- [Firebase Secrets Documentation](https://firebase.google.com/docs/functions/config-env)
- [Gemini API Key](https://aistudio.google.com/app/apikey)
- [OpenAI API Key](https://platform.openai.com/api-keys)
