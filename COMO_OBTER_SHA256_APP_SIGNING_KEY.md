# 🔑 Como Obter SHA-256 do App Signing Key - Play Console

## 📋 Caminhos Alternativos no Play Console

O menu do Play Console pode variar. Tente estes caminhos:

### OPÇÃO 1: Via "Testar e lançar" (Test and release)

1. Acesse: https://play.google.com/console
2. Selecione: **TaskGo**
3. No menu lateral, clique em: **"Testar e lançar"** (Test and release)
4. Procure por: **"Configuração"** ou **"Setup"**
5. Dentro de Setup, procure por: **"App signing"** ou **"Assinatura do app"**

### OPÇÃO 2: Via "Visão geral da publicação" (Publication overview)

1. Acesse: https://play.google.com/console
2. Selecione: **TaskGo**
3. No menu lateral, clique em: **"Visão geral da publicação"** (Publication overview)
4. Procure por: **"App signing"** ou **"Assinatura do app"**

### OPÇÃO 3: Via URL Direta (se disponível)

Tente acessar diretamente:
https://play.google.com/console/developers/1093466748007/app/4973841882000000000/setup/app-signing

### OPÇÃO 4: Buscar no Console

1. Acesse: https://play.google.com/console
2. Selecione: **TaskGo**
3. Use a barra de busca no topo e procure por: **"App signing"** ou **"SHA-256"**

---

## 🔍 Onde Encontrar o SHA-256

Quando encontrar a página de **App signing**, você verá:

### Seção: "App signing certificate"
- Mostra o certificado usado pela Play Store para reassinar seu app
- **SHA-256 certificate fingerprint** estará listado aqui
- Formato: `95:AF:63:3A:8F:CD:20:49:...` (hexadecimal com dois pontos)

### Seção: "Upload certificate" (se houver)
- Esta é sua Upload Key (não é a que precisamos)
- Ignore esta seção

---

## ⚠️ Se Não Encontrar App Signing

Se você não encontrar a seção "App signing", pode ser porque:

1. **App ainda não foi publicado:**
   - A Play Store só gera o App Signing Key após o primeiro upload
   - Se você ainda não fez upload de nenhuma versão, o App Signing Key pode não existir ainda

2. **App está em modo de teste:**
   - Algumas configurações só aparecem após publicação em produção
   - Tente fazer upload de uma versão de teste fechado primeiro

3. **Interface diferente:**
   - O Play Console pode ter interface diferente dependendo da região/versão
   - Tente usar a busca do console

---

## 🔄 Alternativa: Obter SHA-256 do AAB Assinado

Se não conseguir encontrar no Play Console, você pode:

### Método 1: Via Google Play Console API

1. Acesse: https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com?project=task-go-ee85f
2. Habilite a **Google Play Android Developer API**
3. Use a API para obter informações do app signing

### Método 2: Verificar após Upload

1. Faça upload de um AAB para a Play Store (teste fechado)
2. Após o upload, o Play Console mostrará o App Signing Key
3. Acesse a seção de App Signing após o upload

---

## 📝 Informações Importantes

### Diferença entre Upload Key e App Signing Key

**Upload Key (sua chave local):**
- SHA-256: `95:af:63:3a:8f:cd:20:49:a2:59:89:fb:86:71:d8:de:0f:11:89:cf:d7:82:7f:50:45:1c:fb:e7:98:cf:37:18`
- ✅ Já está cadastrado no App Check
- ❌ Não é suficiente para produção

**App Signing Key (chave da Play Store):**
- SHA-256: **DIFERENTE** - obtido do Play Console
- ❌ Ainda não está cadastrado
- ✅ **OBRIGATÓRIO** para produção

---

## 🎯 Próximos Passos

1. **Tente encontrar App Signing no Play Console** usando os caminhos acima
2. **Se não encontrar**, pode ser que o app ainda não tenha App Signing Key (primeiro upload)
3. **Se já fez upload**, o App Signing Key deve estar disponível em algum lugar do console

---

## 💡 Dica

Se você já fez upload de um AAB para a Play Store, o App Signing Key já existe. Procure por:
- "App signing"
- "Assinatura do app"
- "Certificate"
- "Certificado"
- "SHA-256"
- "Fingerprint"

No menu lateral ou usando a busca do console.





















