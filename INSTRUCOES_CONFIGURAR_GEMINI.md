# 🔧 Como Configurar GEMINI_API_KEY via CLI

## Método 1: Script PowerShell (Recomendado)

Execute o script que criei:

```powershell
.\configurar-gemini.ps1
```

O script vai solicitar sua chave da API Gemini.

---

## Método 2: Comando Direto

Se você já tem a chave, execute diretamente:

```powershell
firebase functions:config:set gemini.api_key="SUA_CHAVE_AQUI"
```

**Substitua `SUA_CHAVE_AQUI` pela sua chave real da API Gemini.**

---

## Obter a Chave da API Gemini

1. Acesse: https://aistudio.google.com/app/apikey
2. Faça login com sua conta Google
3. Clique em "Create API Key"
4. Copie a chave gerada

---

## Após Configurar

**IMPORTANTE:** Depois de configurar, você precisa fazer redeploy das functions:

```powershell
firebase deploy --only functions
```

Ou apenas das functions de AI Chat:

```powershell
firebase deploy --only functions:aiChatProxy,functions:getConversationHistory,functions:createConversation,functions:listConversations
```

---

## Verificar se Está Configurado

```powershell
firebase functions:config:get
```

Você deve ver `gemini.api_key` na lista.

---

**Nota:** O método `functions:config:set` está deprecated mas ainda funciona. Para projetos novos, recomenda-se usar Secret Manager, mas esse método é mais simples e funciona perfeitamente.


