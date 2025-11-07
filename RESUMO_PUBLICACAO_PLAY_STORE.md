# 📋 Resumo: O que Fazer para Publicar na Play Store

## ✅ Status Atual

- ✅ Build funcionando (debug e release)
- ✅ ProGuard configurado
- ✅ Minify e shrink resources habilitados
- ✅ Versão configurada (versionCode: 2, versionName: 1.0.1)
- ✅ API de produção configurada (`https://api.taskgo.com/v1/`)
- ❌ **Keystore NÃO configurado** (CRÍTICO)
- ❌ **Política de Privacidade e Termos de Uso NÃO criados** (CRÍTICO)
- ❌ **Screenshots e descrições NÃO criados**
- ⚠️ **Facebook SDK precisa ser removido ou configurado**

---

## 🚀 Ações Imediatas (Ordem de Prioridade)

### 1. 🔐 Configurar Keystore (URGENTE - ~10 minutos)

**Execute o script:**
```powershell
powershell -ExecutionPolicy Bypass -File "configurar_keystore.ps1"
```

**Ou manualmente:**
```bash
keytool -genkeypair -v -storetype PKCS12 -keystore taskgo-release.jks -alias taskgo -keyalg RSA -keysize 2048 -validity 10000
```

**Depois de criar o keystore:**
1. Descomente as linhas no `app/build.gradle.kts`:
   - Carregamento do `keystore.properties` (linhas 26-32)
   - Configuração do `signingConfigs` (linhas 77-83)
   - `signingConfig` no buildType release (linha 69)

2. Teste o build:
   ```bash
   ./gradlew.bat bundleRelease
   ```

**⚠️ IMPORTANTE:** Guarde o keystore e as senhas em local seguro! Você precisará deles para todas as atualizações futuras.

---

### 2. 📄 Criar Política de Privacidade e Termos de Uso (CRÍTICO - ~2 horas)

**O que fazer:**

1. **Criar Política de Privacidade:**
   - Hospedar em URL pública (ex: `https://taskgo.com.br/privacy-policy`)
   - Incluir:
     - Quais dados são coletados
     - Como os dados são usados
     - Como os dados são compartilhados
     - Direitos do usuário (LGPD/GDPR)
     - Informações de contato

2. **Criar Termos de Uso:**
   - Hospedar em URL pública (ex: `https://taskgo.com.br/terms-of-service`)
   - Incluir:
     - Regras de uso do serviço
     - Responsabilidades do usuário
     - Limitações de responsabilidade
     - Propriedade intelectual

3. **Atualizar o app:**
   - Descomentar os TODOs em `AboutScreen.kt` e `PrivacyScreen.kt`
   - Adicionar código para abrir as URLs nos botões

**Recursos:**
- Use geradores online de política de privacidade (ex: [Privacy Policy Generator](https://www.privacypolicygenerator.info/))
- Consulte um advogado se necessário (especialmente para LGPD)

---

### 3. 🎨 Preparar Materiais para Play Store (~3 horas)

**Criar:**

1. **Screenshots (mínimo 2, recomendado 4-8):**
   - Tamanho: 320px - 3840px (16:9 ou 9:16)
   - Capturar telas principais:
     - Tela inicial
     - Lista de produtos/serviços
     - Detalhes de produto/serviço
     - Checkout
     - Perfil

2. **Feature Graphic (obrigatório):**
   - Tamanho: 1024 x 500 pixels
   - Formato: PNG ou JPG (24 bits)
   - Sem texto, apenas logo/imagem

3. **Descrição do App:**
   - Curta (até 80 caracteres)
   - Completa (até 4000 caracteres)
   - Em português (BR) e inglês (recomendado)

---

### 4. 🧪 Testar Build de Release (~1 hora)

```bash
# Gerar AAB (Android App Bundle)
./gradlew.bat bundleRelease

# O AAB estará em:
# app/build/outputs/bundle/release/app-release.aab
```

**Testar:**
1. Instalar em dispositivo físico
2. Testar todas as funcionalidades principais
3. Verificar se não há crashes
4. Verificar se APIs estão funcionando
5. Verificar tamanho do app (< 150MB)

---

### 5. 🔒 Configurar Firebase App Check (Produção) (~30 minutos)

1. **Ativar Play Integrity API:**
   - Ir para [Google Cloud Console](https://console.cloud.google.com/)
   - Ativar "Play Integrity API" para o projeto Firebase

2. **Configurar App Check no Firebase:**
   - Ir para Firebase Console > App Check
   - Ativar Play Integrity para Android
   - Obter SHA-256 do certificado de release:
     ```bash
     keytool -list -v -keystore taskgo-release.jks -alias taskgo
     ```
   - Adicionar SHA-256 no Firebase App Check

---

### 6. 🚫 Remover/Configurar Facebook SDK (~15 minutos)

**Opção A - Remover (recomendado se não for usar):**
1. Remover dependências do Facebook do `build.gradle.kts`
2. Remover meta-data do Facebook do `AndroidManifest.xml`
3. Remover strings `facebook_app_id` e `facebook_client_token`

**Opção B - Configurar:**
1. Criar app no [Facebook Developers](https://developers.facebook.com/)
2. Obter App ID e Client Token
3. Adicionar ao `strings.xml`

---

## 📱 Depois de Completar os Passos Acima

### 7. Criar Conta na Google Play Console

1. Criar conta de desenvolvedor ($25 taxa única)
2. Criar novo app
3. Preencher informações:
   - Nome do app
   - Descrição curta e completa
   - Screenshots
   - Feature Graphic
   - Política de privacidade (URL)
   - Termos de serviço (URL)
   - Categorias
   - Classificação de conteúdo

### 8. Fazer Upload do AAB

1. Ir para "Produção" > "Criar nova versão"
2. Fazer upload do AAB gerado
3. Preencher notas de versão
4. Enviar para revisão

---

## 📚 Documentação Criada

Foram criados os seguintes arquivos para ajudar:

1. **`CHECKLIST_PUBLICACAO_PLAY_STORE.md`** - Checklist completo e detalhado
2. **`configurar_keystore.ps1`** - Script para criar keystore facilmente
3. **`RESUMO_PUBLICACAO_PLAY_STORE.md`** - Este arquivo (resumo executivo)

---

## ⏱️ Tempo Estimado Total

- Configurar Keystore: ~10 minutos
- Criar Política de Privacidade e Termos: ~2 horas
- Preparar Screenshots e Descrições: ~3 horas
- Testar Build de Release: ~1 hora
- Configurar Firebase App Check: ~30 minutos
- Remover/Configurar Facebook: ~15 minutos
- Configurar Play Console e Upload: ~1 hora

**Total: ~8 horas**

---

## 🎯 Próximo Passo Imediato

**Comece agora:**
1. Execute `configurar_keystore.ps1` para criar o keystore
2. Descomente as linhas no `app/build.gradle.kts`
3. Teste o build de release

Depois disso, você pode trabalhar nos outros itens em paralelo (política de privacidade, screenshots, etc.)

---

## ❓ Dúvidas?

Consulte:
- `CHECKLIST_PUBLICACAO_PLAY_STORE.md` para detalhes completos
- [Documentação do Google Play Console](https://support.google.com/googleplay/android-developer/)
- [Guia de Publicação na Play Store](https://developer.android.com/distribute/best-practices/launch)

