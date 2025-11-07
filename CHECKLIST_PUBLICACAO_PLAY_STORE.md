# 📱 Checklist para Publicação na Google Play Store

Este documento lista todas as tarefas necessárias para publicar o aplicativo TaskGo na Google Play Store.

---

## 🔴 OBRIGATÓRIO - Antes de Publicar

### 1. 🔐 Configurar Assinatura de Release (Keystore)

**Status:** ❌ **NÃO CONFIGURADO**

**O que fazer:**

1. **Criar o keystore:**
   ```bash
   keytool -genkeypair -v -storetype PKCS12 -keystore taskgo-release.jks -alias taskgo -keyalg RSA -keysize 2048 -validity 10000
   ```
   - Anote as senhas fornecidas!
   - Guarde o arquivo `taskgo-release.jks` em local seguro (não commitar no Git!)

2. **Adicionar ao `.gitignore`:**
   ```
   *.jks
   *.keystore
   keystore.properties
   ```

3. **Criar arquivo `keystore.properties` na raiz do projeto (NÃO commitar):**
   ```properties
   TASKGO_RELEASE_STORE_FILE=taskgo-release.jks
   TASKGO_RELEASE_STORE_PASSWORD=sua_senha_aqui
   TASKGO_RELEASE_KEY_ALIAS=taskgo
   TASKGO_RELEASE_KEY_PASSWORD=sua_senha_aqui
   ```

4. **Atualizar `app/build.gradle.kts`:**
   ```kotlin
   // Carregar keystore.properties
   val keystorePropertiesFile = rootProject.file("keystore.properties")
   val keystoreProperties = Properties()
   if (keystorePropertiesFile.exists()) {
       keystoreProperties.load(FileInputStream(keystorePropertiesFile))
   }

   android {
       // ... outras configs
       
       signingConfigs {
           create("release") {
               keyAlias = keystoreProperties["TASKGO_RELEASE_KEY_ALIAS"] as String
               keyPassword = keystoreProperties["TASKGO_RELEASE_KEY_PASSWORD"] as String
               storeFile = file(keystoreProperties["TASKGO_RELEASE_STORE_FILE"] as String)
               storePassword = keystoreProperties["TASKGO_RELEASE_STORE_PASSWORD"] as String
           }
       }

       buildTypes {
           release {
               isMinifyEnabled = true
               isShrinkResources = true
               signingConfig = signingConfigs.getByName("release")
               // ... resto da config
           }
       }
   }
   ```

**⚠️ IMPORTANTE:**
- **NUNCA** perca o keystore ou as senhas! Você precisará delas para todas as atualizações futuras.
- Faça backup do keystore em local seguro (ex: cloud com criptografia).
- Se perder o keystore, você não poderá atualizar o app na Play Store.

---

### 2. 📄 Política de Privacidade e Termos de Uso

**Status:** ❌ **NÃO IMPLEMENTADO**

**O que fazer:**

1. **Criar Política de Privacidade:**
   - Deve estar hospedada em URL pública (ex: `https://taskgo.com.br/privacy-policy`)
   - Deve incluir:
     - Quais dados são coletados
     - Como os dados são usados
     - Como os dados são compartilhados
     - Direitos do usuário (LGPD/GDPR)
     - Informações de contato

2. **Criar Termos de Uso:**
   - Deve estar hospedada em URL pública (ex: `https://taskgo.com.br/terms-of-service`)
   - Deve incluir:
     - Regras de uso do serviço
     - Responsabilidades do usuário
     - Limitações de responsabilidade
     - Propriedade intelectual

3. **Atualizar `AboutScreen.kt` e `PrivacyScreen.kt`:**
   ```kotlin
   // Abrir URLs nos botões de Política de Privacidade e Termos de Uso
   val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://taskgo.com.br/privacy-policy"))
   context.startActivity(intent)
   ```

**📝 Nota:** A Play Store exige que você forneça essas URLs ao criar o app.

---

### 3. 🎨 Ícone do Aplicativo

**Status:** ⚠️ **VERIFICAR SE É O OFICIAL**

**O que fazer:**

1. Verificar se os ícones em `app/src/main/res/mipmap-*/` são os oficiais do TaskGo
2. Garantir que há ícones para todas as densidades:
   - `mipmap-mdpi` (48x48)
   - `mipmap-hdpi` (72x72)
   - `mipmap-xhdpi` (96x96)
   - `mipmap-xxhdpi` (144x144)
   - `mipmap-xxxhdpi` (192x192)

3. **Criar ícone adaptativo (Android 8.0+):**
   - O arquivo `mipmap-anydpi-v26/ic_launcher.xml` já existe
   - Verificar se o foreground e background estão corretos

---

### 4. 📸 Screenshots e Arte Promocional

**Status:** ❌ **NÃO CRIADO**

**O que fazer:**

1. **Criar screenshots para a Play Store:**
   - **Mínimo:** 2 screenshots
   - **Recomendado:** 4-8 screenshots
   - Tamanhos necessários:
     - Telefone: 16:9 ou 9:16 (mínimo 320px, máximo 3840px)
     - Tablet: 16:9 ou 9:16 (mínimo 320px, máximo 3840px)

2. **Criar ícone de destaque (Feature Graphic):**
   - Tamanho: 1024 x 500 pixels
   - Formato: PNG ou JPG (24 bits)
   - Sem texto (apenas logo/imagem)

3. **Criar ícone de canal do YouTube (opcional):**
   - Tamanho: 1440 x 1080 pixels

---

### 5. 📝 Descrição do Aplicativo

**Status:** ❌ **NÃO CRIADO**

**O que fazer:**

1. **Criar descrição curta (até 80 caracteres):**
   - Ex: "Plataforma para conectar clientes e prestadores de serviços"

2. **Criar descrição completa (até 4000 caracteres):**
   - Incluir principais funcionalidades
   - Benefícios para usuários
   - Destaques do app
   - Call-to-action

3. **Criar descrição em português (Brasil) e inglês (recomendado)**

---

### 6. 🔒 Configurar Firebase App Check (Produção)

**Status:** ⚠️ **VERIFICAR SE ESTÁ CONFIGURADO**

**O que fazer:**

1. **Ativar Play Integrity API no Google Cloud Console:**
   - Ir para [Google Cloud Console](https://console.cloud.google.com/)
   - Ativar "Play Integrity API" para o projeto Firebase

2. **Configurar App Check no Firebase Console:**
   - Ir para Firebase Console > App Check
   - Ativar Play Integrity para Android
   - Adicionar SHA-256 do certificado de release (não debug!)

3. **Obter SHA-256 do certificado de release:**
   ```bash
   keytool -list -v -keystore taskgo-release.jks -alias taskgo
   ```
   - Copiar o SHA-256 e adicionar no Firebase App Check

---

### 7. 🌐 Configurar Domínio de API de Produção

**Status:** ⚠️ **VERIFICAR**

**O que fazer:**

1. Verificar se `api.taskgo.com` está configurado e funcionando
2. Verificar se o certificado SSL está válido
3. Atualizar `buildConfigField` em `app/build.gradle.kts`:
   ```kotlin
   release {
       buildConfigField("String", "API_BASE_URL", "\"https://api.taskgo.com/v1/\"")
   }
   ```

---

### 8. 🚫 Remover ou Configurar Facebook SDK

**Status:** ⚠️ **PRECISA DECISÃO**

**O que fazer:**

**Opção A - Remover Facebook (se não for usar):**
1. Remover dependências do Facebook do `build.gradle.kts`
2. Remover meta-data do Facebook do `AndroidManifest.xml`
3. Remover strings `facebook_app_id` e `facebook_client_token` do `strings.xml`

**Opção B - Configurar Facebook (se for usar):**
1. Criar app no [Facebook Developers](https://developers.facebook.com/)
2. Obter App ID e Client Token
3. Adicionar ao `strings.xml`:
   ```xml
   <string name="facebook_app_id">SEU_APP_ID</string>
   <string name="facebook_client_token">SEU_CLIENT_TOKEN</string>
   ```

---

### 9. 📊 Configurar Analytics e Crashlytics (Opcional mas Recomendado)

**Status:** ⚠️ **VERIFICAR**

**O que fazer:**

1. **Firebase Analytics:**
   - Verificar se está configurado no `google-services.json`
   - Adicionar eventos customizados para rastreamento

2. **Firebase Crashlytics:**
   - Adicionar dependência (se ainda não tiver)
   - Configurar para capturar crashes em produção

---

### 10. 🧪 Testar Build de Release

**Status:** ❌ **NÃO TESTADO**

**O que fazer:**

1. **Gerar APK de release:**
   ```bash
   ./gradlew.bat assembleRelease
   ```

2. **Gerar AAB (Android App Bundle) - RECOMENDADO:**
   ```bash
   ./gradlew.bat bundleRelease
   ```
   - A Play Store prefere AAB sobre APK

3. **Testar o AAB/APK:**
   - Instalar em dispositivo físico
   - Testar todas as funcionalidades principais
   - Verificar se não há crashes
   - Verificar se as APIs estão funcionando

4. **Verificar tamanho do app:**
   - Play Store tem limite de 150MB para AAB
   - Verificar se o app está dentro do limite

---

## 🟡 RECOMENDADO - Melhorias Antes de Publicar

### 11. 📱 Configurar Categorias e Conteúdo na Play Store

**O que fazer:**

1. Selecionar categorias:
   - Categoria principal: "Produtividade" ou "Negócios"
   - Categoria secundária: "Serviços"

2. Classificação de conteúdo:
   - Preencher questionário de classificação de conteúdo
   - Indicar se há conteúdo para menores de idade

---

### 12. 🌍 Localização (Idiomas)

**Status:** ⚠️ **VERIFICAR**

**O que fazer:**

1. Verificar se todas as strings estão em `strings.xml`
2. Criar traduções para outros idiomas (opcional):
   - `values-en/strings.xml` (Inglês)
   - `values-es/strings.xml` (Espanhol)
   - etc.

---

### 13. 🎯 Configurar Permissões Declaradas

**Status:** ✅ **JÁ CONFIGURADO**

**Verificar:**
- Todas as permissões no `AndroidManifest.xml` estão justificadas
- Permissões sensíveis (câmera, localização) têm justificativa para a Play Store

---

### 14. 🔄 Configurar Atualizações Automáticas

**Status:** ⚠️ **VERIFICAR**

**O que fazer:**

1. Configurar versão no `build.gradle.kts`:
   ```kotlin
   defaultConfig {
       versionCode = 2  // Incrementar a cada release
       versionName = "1.0.1"  // Versão exibida ao usuário
   }
   ```

2. Implementar verificação de atualização (opcional):
   - Usar In-App Updates API do Google Play

---

### 15. 📧 Configurar Email de Suporte

**Status:** ⚠️ **VERIFICAR**

**O que fazer:**

1. Verificar se o email `contato@taskgo.com.br` está configurado e funcionando
2. Adicionar email de suporte nas configurações da Play Store
3. Responder reviews e emails de usuários

---

## 🟢 OPCIONAL - Após Publicação

### 16. 📈 Configurar Google Play Console

**O que fazer:**

1. Criar conta de desenvolvedor na Play Store (taxa única de $25)
2. Criar novo app no Google Play Console
3. Preencher todas as informações:
   - Nome do app
   - Descrição curta e completa
   - Screenshots
   - Ícone de destaque
   - Política de privacidade
   - Termos de serviço
   - Categorias
   - Classificação de conteúdo
   - Preço (gratuito ou pago)

4. Fazer upload do AAB de release

5. Configurar teste interno (opcional):
   - Criar lista de testadores
   - Enviar para teste antes de publicar

6. Publicar em produção

---

### 17. 🔔 Configurar Notificações Push (Firebase Cloud Messaging)

**Status:** ⚠️ **VERIFICAR**

**O que fazer:**

1. Verificar se FCM está configurado
2. Testar notificações em produção
3. Configurar tópicos e segmentação

---

### 18. 💳 Configurar Pagamentos (Google Play Billing)

**Status:** ✅ **JÁ IMPLEMENTADO**

**Verificar:**
- Produtos configurados no Google Play Console
- Testes de compras in-app funcionando

---

## 📋 Resumo de Prioridades

### 🔴 CRÍTICO (Não pode publicar sem):
1. ✅ Configurar Keystore e Assinatura de Release
2. ✅ Criar Política de Privacidade e Termos de Uso
3. ✅ Testar Build de Release
4. ✅ Configurar Firebase App Check (produção)

### 🟡 IMPORTANTE (Recomendado antes de publicar):
5. ⚠️ Criar Screenshots e Arte Promocional
6. ⚠️ Criar Descrição do Aplicativo
7. ⚠️ Remover/Configurar Facebook SDK
8. ⚠️ Configurar Domínio de API de Produção

### 🟢 OPCIONAL (Pode fazer depois):
9. ⚠️ Configurar Analytics e Crashlytics
10. ⚠️ Localização para outros idiomas
11. ⚠️ Configurar Google Play Console

---

## 🚀 Próximos Passos

1. **Começar pelo Keystore** (mais crítico)
2. **Criar Política de Privacidade e Termos de Uso**
3. **Testar build de release**
4. **Preparar screenshots e descrições**
5. **Configurar Google Play Console**

---

## 📞 Suporte

Se tiver dúvidas sobre algum item, consulte:
- [Documentação do Google Play Console](https://support.google.com/googleplay/android-developer/)
- [Guia de Publicação na Play Store](https://developer.android.com/distribute/best-practices/launch)

