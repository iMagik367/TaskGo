# 🧹 Limpar OAuth Client IDs Duplicados

## ❌ Problema Identificado

Você tem **MÚLTIPLOS OAuth Client IDs Android** para o mesmo package name `com.taskgoapp.taskgo`, o que pode estar causando conflito e bloqueando o login.

## 🔍 Client IDs Encontrados

### Client ID em Uso (no google-services.json):
- **ID:** `1093466748007-k4vsgmdn43v5qd8q1tkhj8hg4q8j4ari`
- **Data:** 18 de dez. de 2025
- **Package:** `com.taskgoapp.taskgo`
- **SHA-1 no arquivo:** `fbaef1168afe519dcfba5f670e37f7fcbb9b407a`

### Client IDs Duplicados/Antigos (DELETAR):
1. `1093466748007-he1...` (18 dez 2025) - **DELETAR** (duplicado)
2. `1093466748007-usi...` (5 nov 2025) - **DELETAR** (antigo)
3. `1093466748007-4q0...` (5 nov 2025) - **DELETAR** (package errado: com.example.taskgoapp)
4. `1093466748007-c50...` (3 nov 2025) - **DELETAR** (package errado: com.example.taskgoapp)

## ✅ Solução Passo a Passo

### Passo 1: Verificar o Client ID Correto

1. Acesse: https://console.cloud.google.com/apis/credentials
2. Procure por **"OAuth 2.0 Client IDs"**
3. Encontre o client ID: `1093466748007-k4vsgmdn43v5qd8q1tkhj8hg4q8j4ari`
4. Clique em **"Editar"** (ícone de lápis)

### Passo 2: Configurar SHA-1 no Client ID Correto

No client ID `1093466748007-k4vsgmdn43v5qd8q1tkhj8hg4q8j4ari`:

1. Verifique se o **Package name** está: `com.taskgoapp.taskgo`
2. Adicione/verifique os **SHA-1**:
   - **RELEASE:** `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
   - **DEBUG:** `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
3. Clique em **"Salvar"**

### Passo 3: DELETAR Client IDs Duplicados

**⚠️ IMPORTANTE:** Deletar apenas os duplicados/antigos, NÃO o que está em uso!

1. Encontre o client ID: `1093466748007-he1...` (18 dez 2025)
   - Clique no ícone de **lixeira** (delete)
   - Confirme a exclusão

2. Encontre o client ID: `1093466748007-usi...` (5 nov 2025)
   - Clique no ícone de **lixeira** (delete)
   - Confirme a exclusão

3. Encontre o client ID: `1093466748007-4q0...` (5 nov 2025) - com.example.taskgoapp
   - Clique no ícone de **lixeira** (delete)
   - Confirme a exclusão

4. Encontre o client ID: `1093466748007-c50...` (3 nov 2025) - com.example.taskgoapp
   - Clique no ícone de **lixeira** (delete)
   - Confirme a exclusão

### Passo 4: Verificar no Firebase Console

1. Acesse: https://console.firebase.google.com/
2. Selecione o projeto: `task-go-ee85f`
3. Vá em **Configurações do projeto** (ícone de engrenagem)
4. Role até **"Seus apps"**
5. Clique no app Android
6. Verifique se os **SHA-1** estão configurados:
   - RELEASE: `FB:AE:F1:16:8A:FE:51:9D:CF:BA:5F:67:0E:37:F7:FC:BB:9B:40:7A`
   - DEBUG: `50:D8:12:CB:1E:41:20:CA:3A:C7:DF:9C:E7:2A:25:88:D8:80:4D:CD`
7. Se não estiverem, adicione ambos
8. **Baixe o novo `google-services.json`**
9. **Substitua o arquivo** `app/google-services.json` no projeto

### Passo 5: Recompilar o App

1. Limpe o build: `gradlew.bat clean`
2. Recompile o app
3. Teste o login

## 📋 Checklist

- [ ] Verificou qual client ID está no google-services.json: `1093466748007-k4v...`
- [ ] Editou o client ID correto e adicionou os SHA-1
- [ ] Deletou o client ID duplicado: `1093466748007-he1...`
- [ ] Deletou o client ID antigo: `1093466748007-usi...`
- [ ] Deletou o client ID com package errado: `1093466748007-4q0...`
- [ ] Deletou o client ID com package errado: `1093466748007-c50...`
- [ ] Verificou SHA-1 no Firebase Console
- [ ] Baixou novo google-services.json
- [ ] Substituiu o arquivo no projeto
- [ ] Limpou e recompilou o app
- [ ] Testou o login

## ⚠️ Importante

- **NÃO delete** o client ID `1093466748007-k4v...` (está em uso)
- **NÃO delete** o Web client `1093466748007-bk9...` (está em uso)
- Delete apenas os **duplicados** e **antigos**
- Após deletar, pode levar alguns minutos para propagar
- Sempre baixe o novo `google-services.json` após mudanças no Firebase

## 🔍 Por Que Isso Causa Problema?

Múltiplos OAuth Client IDs para o mesmo package podem causar:
- Conflito na autenticação
- Google não saber qual client ID usar
- Bloqueio de requisições
- Erro "Requests from this Android client application are blocked"

Mantendo apenas **UM** client ID Android correto, o Google saberá exatamente qual usar.
