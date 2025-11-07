# 🔥 Guia de Configuração do Firebase

Este guia contém todas as configurações que você precisa fazer manualmente no Firebase Console.

**📌 IMPORTANTE:** Você também tem uma nova API Key do Google Cloud (`AIzaSyA7podhNipqILvMV7mwZJc7ZYgd-f16TAw`) que precisa ser atualizada. Veja `GUIA_ATUALIZAR_API_KEY.md` para instruções detalhadas. As chaves adicionais disponibilizadas são:
- Browser API Key: `AIzaSyBYiaQk5X35XJgz-4BsM4Zd7RIE7YyxxtM`
- Gemini Developer API Key: `AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4`

---

## 📋 CHECKLIST FIREBASE

- [ ] Configurar Firebase App Check (Play Integrity)
- [ ] Configurar variáveis de ambiente nas Cloud Functions
- [ ] Verificar Firestore Rules
- [ ] Verificar Storage Rules
- [ ] Verificar índices do Firestore
- [ ] Configurar Facebook App ID (se necessário)
- [ ] Configurar Application ID (se mudou)

---

## 1. 🔐 CONFIGURAR FIREBASE APP CHECK

**Por quê:** Protege seu app contra tráfego abusivo e bot attacks.

### Passo 1: Ativar Play Integrity no Firebase Console

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. Selecione seu projeto: `task-go-ee85f`
3. No menu lateral, vá em **Build** > **App Check**
4. Clique em **Get Started** (se for a primeira vez)
5. Clique em **Add app** e selecione **Android**
6. Selecione seu app Android
7. Em **App Check providers**, escolha **Play Integrity**
8. Clique em **Save**

### Passo 2: Configurar Debug Tokens (para desenvolvimento)

**Importante:** Para builds de debug funcionarem, você precisa adicionar debug tokens.

1. No Firebase Console, vá em **App Check**
2. Selecione seu app Android
3. Clique em **Manage debug tokens**
4. Execute o app em modo debug uma vez
5. Verifique os logs do Android Studio - procure por uma mensagem como:
   ```
   App Check debug token: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
   ```
6. Copie o token e cole no Firebase Console
7. Clique em **Add**

**Alternativa:** Você pode obter o token programaticamente:
```kotlin
// Adicione temporariamente no TaskGoApp.kt para obter o token
FirebaseAppCheck.getInstance().getAppCheckToken(false).addOnSuccessListener { token ->
    Log.d("AppCheck", "Debug token: ${token.token}")
}
```

### Passo 3: Verificar Configuração

1. Acesse **App Check** no Firebase Console
2. Verifique se o status está **Active**
3. Para builds de produção, o Play Integrity funcionará automaticamente

---

## 2. 🔧 CONFIGURAR VARIÁVEIS DE AMBIENTE NAS CLOUD FUNCTIONS

### Passo 1: Obter Chaves de API

1. **OpenAI API Key:**
   - Acesse [OpenAI Platform](https://platform.openai.com/api-keys)
   - Crie uma nova chave de API
   - Copie a chave

2. **Stripe Secret Key:**
   - Acesse [Stripe Dashboard](https://dashboard.stripe.com/apikeys)
   - Copie a **Secret key** (não a Publishable key)

3. **Stripe Webhook Secret:**
   - Acesse [Stripe Dashboard](https://dashboard.stripe.com/webhooks)
   - Crie um webhook apontando para sua função
   - Copie o **Signing secret**

### Passo 2: Configurar no Firebase

**Opção A: Via Firebase Console (Recomendado)**

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. Selecione seu projeto
3. Vá em **Functions**
4. Clique em **Config**
5. Vá na aba **Environment variables**
6. Adicione as seguintes variáveis:

   - **Nome:** `OPENAI_API_KEY`
   - **Valor:** `sk-...` (sua chave OpenAI)
   - **Função:** Selecione todas ou apenas as que usam

   - **Nome:** `STRIPE_SECRET_KEY`
   - **Valor:** `sk_live_...` ou `sk_test_...` (sua chave Stripe)
   - **Função:** Selecione todas ou apenas as que usam

   - **Nome:** `STRIPE_WEBHOOK_SECRET`
   - **Valor:** `whsec_...` (seu webhook secret)
   - **Função:** Selecione apenas `stripeWebhook`

7. Clique em **Save** para cada variável

**Opção B: Via Firebase CLI**

```bash
# Instalar Firebase CLI se ainda não tiver
npm install -g firebase-tools

# Fazer login
firebase login

# Configurar variáveis
firebase functions:config:set \
  openai.api_key="sk-..." \
  stripe.secret_key="sk_live_..." \
  stripe.webhook_secret="whsec_..."

# Deploy das functions
firebase deploy --only functions
```

### Passo 3: Verificar Configuração

1. Acesse **Functions** > **Config** no Firebase Console
2. Verifique se todas as variáveis estão configuradas
3. Teste as functions para garantir que funcionam

---

## 3. 📊 VERIFICAR FIRESTORE RULES

### Passo 1: Verificar Rules Atuais

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. Selecione seu projeto
3. Vá em **Firestore Database** > **Rules**
4. Verifique se as rules estão corretas (já estão no arquivo `firestore.rules`)

### Passo 2: Testar Rules

1. No Firebase Console, vá em **Firestore Database** > **Rules**
2. Clique em **Rules Playground**
3. Teste diferentes cenários:
   - Usuário autenticado lendo próprio perfil
   - Usuário tentando ler perfil de outro
   - Criar produto
   - Atualizar pedido

### Passo 3: Deploy das Rules

```bash
firebase deploy --only firestore:rules
```

---

## 4. 🗄️ VERIFICAR STORAGE RULES

### Passo 1: Verificar Rules Atuais

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. Selecione seu projeto
3. Vá em **Storage** > **Rules**
4. Verifique se as rules estão corretas (já estão no arquivo `storage.rules`)

### Passo 2: Deploy das Rules

```bash
firebase deploy --only storage
```

---

## 5. 📑 VERIFICAR ÍNDICES DO FIRESTORE

### Passo 1: Verificar Índices Necessários

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. Selecione seu projeto
3. Vá em **Firestore Database** > **Indexes**
4. Verifique se todos os índices do arquivo `firestore.indexes.json` estão criados

### Passo 2: Criar Índices Faltantes

Se algum índice estiver faltando:

1. Clique em **Create Index**
2. Selecione a coleção
3. Adicione os campos conforme `firestore.indexes.json`
4. Clique em **Create**

**Ou via CLI:**
```bash
firebase deploy --only firestore:indexes
```

---

## 6. 🔵 CONFIGURAR FACEBOOK APP ID (SE NECESSÁRIO)

### Se você NÃO usa Facebook Login:

1. **Remover do código:**
   - Remover meta-data do `AndroidManifest.xml` (linhas 31-36)
   - Remover ou comentar `auth_config.xml`

### Se você USA Facebook Login:

1. **Criar App no Facebook:**
   - Acesse [Facebook Developers](https://developers.facebook.com)
   - Crie um novo app
   - Adicione produto "Facebook Login"
   - Configure para Android

2. **Obter Credenciais:**
   - App ID
   - Client Token

3. **Atualizar no projeto:**
   - Edite `app/src/main/res/values/auth_config.xml`
   - Substitua `seu_facebook_app_id_aqui` pelo App ID real
   - Substitua `seu_facebook_client_token_aqui` pelo Client Token real

4. **Configurar no Firebase:**
   - Acesse Firebase Console > Authentication > Sign-in method
   - Habilite Facebook
   - Adicione App ID e App Secret

---

## 7. 📱 CONFIGURAR APPLICATION ID (SE MUDOU)

**⚠️ IMPORTANTE:** Se você mudou o `applicationId` no `build.gradle.kts`:

1. **Adicionar novo app no Firebase:**
   - Acesse Firebase Console > Project Settings
   - Vá na aba **Your apps**
   - Clique em **Add app** > **Android**
   - Digite o novo package name
   - Baixe o novo `google-services.json`
   - Substitua o arquivo antigo em `app/google-services.json`

2. **Ou manter o mesmo package:**
   - Se quiser manter `com.example.taskgoapp`, não precisa fazer nada
   - Mas **não poderá publicar no Google Play** com esse package name

---

## 8. 🚀 DEPLOY DAS CLOUD FUNCTIONS

### Passo 1: Verificar Código

```bash
cd functions
npm install
npm run build
```

### Passo 2: Deploy

```bash
# Deploy todas as functions
firebase deploy --only functions

# Ou deploy de uma function específica
firebase deploy --only functions:aiChatProxy
```

### Passo 3: Verificar Logs

1. Acesse Firebase Console > Functions
2. Clique em uma function
3. Vá em **Logs** para verificar se está funcionando

---

## 9. ✅ VERIFICAÇÕES FINAIS

### Checklist de Verificação:

- [ ] App Check configurado e ativo
- [ ] Debug tokens adicionados para desenvolvimento
- [ ] Todas as variáveis de ambiente configuradas
- [ ] Firestore Rules deployadas e testadas
- [ ] Storage Rules deployadas
- [ ] Todos os índices do Firestore criados
- [ ] Cloud Functions deployadas e funcionando
- [ ] Facebook configurado (se necessário)
- [ ] Application ID atualizado (se mudou)

---

## 🆘 TROUBLESHOOTING

### Problema: App Check não funciona em debug

**Solução:**
1. Verifique se o debug token foi adicionado no Firebase Console
2. Certifique-se de que o token está correto
3. Reinicie o app após adicionar o token

### Problema: Functions retornam erro de variável não encontrada

**Solução:**
1. Verifique se as variáveis foram configuradas corretamente
2. Faça redeploy das functions após configurar variáveis
3. Verifique os logs das functions no Firebase Console

### Problema: Firestore Rules bloqueiam requisições legítimas

**Solução:**
1. Use o Rules Playground para testar
2. Verifique se o usuário está autenticado
3. Verifique se o usuário tem as permissões necessárias

---

## 📞 PRÓXIMOS PASSOS

Após configurar tudo no Firebase:

1. ✅ Teste o app em modo debug
2. ✅ Teste o app em modo release
3. ✅ Verifique logs das Cloud Functions
4. ✅ Teste todas as funcionalidades principais
5. ✅ Faça um build de release
6. ✅ Prepare para publicação no Google Play

---

**Última atualização:** 2024


