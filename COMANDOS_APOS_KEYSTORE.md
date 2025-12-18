# 📋 Comandos para Executar Após Criar o Keystore

## ✅ Passo 1: Criar o Keystore (JÁ FEITO)

Você já criou o keystore e o arquivo `keystore.properties`.

---

## 🔧 Passo 2: Descomentar Linhas no build.gradle.kts

**EU VOU FAZER ISSO PARA VOCÊ** - Apenas me avise quando o keystore estiver criado!

Mas se quiser fazer manualmente, descomente estas 3 seções no arquivo `app/build.gradle.kts`:

1. **Linhas 45-51** - Carregamento do keystore.properties
2. **Linhas 140-147** - Signing configs  
3. **Linha 133** - Aplicar signing config

---

## 🔄 Passo 3: Sincronizar o Projeto

No Android Studio:
- Clique em **"Sync Now"** ou
- Vá em **File > Sync Project with Gradle Files**

**OU execute no terminal:**
```powershell
cd C:\Users\user\AndroidStudioProjects\TaskGoApp
.\gradlew.bat --refresh-dependencies
```

---

## 🧪 Passo 4: Testar Build de Release

Execute este comando para gerar o AAB assinado:

```powershell
.\gradlew.bat bundleRelease
```

**O que vai acontecer:**
- O Gradle vai compilar o projeto
- Vai assinar o AAB com o keystore
- Vai gerar o arquivo em: `app\build\outputs\bundle\release\app-release.aab`

**Tempo estimado:** 5-10 minutos (primeira vez pode demorar mais)

---

## ✅ Passo 5: Verificar se o AAB foi Gerado

Verifique se o arquivo existe:

```powershell
Test-Path app\build\outputs\bundle\release\app-release.aab
```

Se retornar `True`, o arquivo foi criado com sucesso!

**Verificar tamanho do arquivo:**
```powershell
(Get-Item app\build\outputs\bundle\release\app-release.aab).Length
```

Deve ser alguns MBs (não pode ser 0 bytes).

---

## 📤 Passo 6: Upload para Google Play Console

1. Acesse: https://play.google.com/console
2. Selecione seu app (ou crie um novo)
3. Vá em **"Produção"** > **"Criar nova versão"**
4. Faça upload do arquivo: `app\build\outputs\bundle\release\app-release.aab`
5. Preencha as informações da versão
6. Envie para revisão

---

## 🚨 Se Der Erro no Build

### Erro: "keystore.properties not found"
```powershell
# Verificar se o arquivo existe
Test-Path keystore.properties
```

### Erro: "Keystore file not found"
```powershell
# Verificar se o keystore existe
Test-Path "$env:USERPROFILE\AndroidKeystores\taskgo-release-key.jks"
```

### Erro: "Wrong password"
- Verifique as senhas no arquivo `keystore.properties`
- Não pode ter espaços antes/depois das senhas

---

## 📝 Resumo dos Comandos

```powershell
# 1. Navegar até o projeto
cd C:\Users\user\AndroidStudioProjects\TaskGoApp

# 2. Sincronizar dependências (opcional, mas recomendado)
.\gradlew.bat --refresh-dependencies

# 3. Gerar AAB assinado
.\gradlew.bat bundleRelease

# 4. Verificar se foi gerado
Test-Path app\build\outputs\bundle\release\app-release.aab
```

---

## ⚡ Comando Rápido (Tudo de Uma Vez)

Se você já criou o keystore e eu já descomentei as linhas, execute apenas:

```powershell
cd C:\Users\user\AndroidStudioProjects\TaskGoApp
.\gradlew.bat bundleRelease
```

**Pronto!** O AAB será gerado em alguns minutos.

