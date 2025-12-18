# 🔐 Guia Completo: Criar Keystore e Configurar Signing

## 📋 Pré-requisitos

- Java JDK instalado (necessário para o comando `keytool`)
- Android Studio instalado
- Terminal/PowerShell aberto

---

## 🚀 Passo 1: Verificar se Java está instalado

Abra o PowerShell ou CMD e execute:

```powershell
java -version
```

Se aparecer a versão do Java, está tudo certo. Se não, instale o JDK primeiro.

---

## 🔑 Passo 2: Criar o Keystore

### Opção A: Usando PowerShell/CMD (Recomendado)

1. Abra o PowerShell ou CMD
2. Navegue até a raiz do projeto:
```powershell
cd C:\Users\user\AndroidStudioProjects\TaskGoApp
```

3. Execute o comando abaixo (substitua as informações conforme necessário):

```powershell
keytool -genkey -v -keystore taskgo-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias taskgo-release
```

### Informações que serão solicitadas:

```
Digite a senha do keystore: [Escolha uma senha forte - exemplo: TaskGo2024!Secure]
Digite novamente a senha do keystore: [Repita a senha]

Nome e sobrenome: TaskGo App
Nome da unidade organizacional: TaskGo
Nome da organização: TaskGo
Nome da cidade: [Sua cidade - exemplo: São Paulo]
Nome do estado: [Seu estado - exemplo: SP]
Código do país: BR

Confirme? [sim]: sim

Digite a senha do alias <taskgo-release>: [Pode ser a mesma do keystore ou diferente]
Digite novamente a senha do alias: [Repita]
```

**⚠️ IMPORTANTE:**
- **ANOTE TODAS AS SENHAS** em local seguro
- A senha do keystore e do alias podem ser iguais ou diferentes
- O arquivo `taskgo-release-key.jks` será criado na pasta do projeto

---

## 📁 Passo 3: Mover o Keystore para Local Seguro

**NÃO deixe o keystore na pasta do projeto!** Mova para um local seguro:

```powershell
# Criar pasta para keystore (fora do projeto)
mkdir C:\Users\user\AndroidKeystores

# Mover o keystore
move taskgo-release-key.jks C:\Users\user\AndroidKeystores\
```

**Ou use um local ainda mais seguro:**
- Pendrive criptografado
- Serviço de backup na nuvem (criptografado)
- Gerenciador de senhas (como 1Password, LastPass)

---

## 📝 Passo 4: Criar arquivo keystore.properties

1. Na raiz do projeto (`C:\Users\user\AndroidStudioProjects\TaskGoApp`), crie um arquivo chamado `keystore.properties`

2. Abra o arquivo e adicione o seguinte conteúdo (substitua pelos seus valores):

```properties
TASKGO_RELEASE_STORE_FILE=C:/Users/user/AndroidKeystores/taskgo-release-key.jks
TASKGO_RELEASE_KEY_ALIAS=taskgo-release
TASKGO_RELEASE_STORE_PASSWORD=sua_senha_do_keystore_aqui
TASKGO_RELEASE_KEY_PASSWORD=sua_senha_do_alias_aqui
```

**Exemplo real:**
```properties
TASKGO_RELEASE_STORE_FILE=C:/Users/user/AndroidKeystores/taskgo-release-key.jks
TASKGO_RELEASE_KEY_ALIAS=taskgo-release
TASKGO_RELEASE_STORE_PASSWORD=TaskGo2024!Secure
TASKGO_RELEASE_KEY_PASSWORD=TaskGo2024!Secure
```

**⚠️ IMPORTANTE:**
- Use barras `/` ou `\\` no caminho do Windows
- Substitua `sua_senha_do_keystore_aqui` pela senha real que você escolheu
- Substitua `sua_senha_do_alias_aqui` pela senha do alias (pode ser a mesma)

---

## 🔒 Passo 5: Adicionar ao .gitignore

**CRÍTICO:** Nunca faça commit do keystore ou do arquivo de propriedades!

1. Abra o arquivo `.gitignore` na raiz do projeto
2. Adicione estas linhas (se ainda não estiverem):

```
# Keystore files
*.jks
*.keystore
keystore.properties
taskgo-release-key.jks
```

---

## ⚙️ Passo 6: Configurar build.gradle.kts

Agora vamos descomentar as configurações no `app/build.gradle.kts`:

### 6.1: Descomentar carregamento do keystore.properties

Encontre estas linhas (por volta da linha 43-50) e **descomente**:

```kotlin
// ANTES (comentado):
/*
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
*/

// DEPOIS (descomentado):
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

### 6.2: Descomentar signingConfigs

Encontre estas linhas (por volta da linha 138-147) e **descomente**:

```kotlin
// ANTES (comentado):
/*
signingConfigs {
    create("release") {
        keyAlias = keystoreProperties["TASKGO_RELEASE_KEY_ALIAS"] as String
        keyPassword = keystoreProperties["TASKGO_RELEASE_KEY_PASSWORD"] as String
        storeFile = file(keystoreProperties["TASKGO_RELEASE_STORE_FILE"] as String)
        storePassword = keystoreProperties["TASKGO_RELEASE_STORE_PASSWORD"] as String
    }
}
*/

// DEPOIS (descomentado):
signingConfigs {
    create("release") {
        keyAlias = keystoreProperties["TASKGO_RELEASE_KEY_ALIAS"] as String
        keyPassword = keystoreProperties["TASKGO_RELEASE_KEY_PASSWORD"] as String
        storeFile = file(keystoreProperties["TASKGO_RELEASE_STORE_FILE"] as String)
        storePassword = keystoreProperties["TASKGO_RELEASE_STORE_PASSWORD"] as String
    }
}
```

### 6.3: Descomentar aplicação do signing config

Encontre esta linha (por volta da linha 132) e **descomente**:

```kotlin
// ANTES (comentado):
// signingConfig = signingConfigs.getByName("release")

// DEPOIS (descomentado):
signingConfig = signingConfigs.getByName("release")
```

---

## ✅ Passo 7: Verificar Configuração

1. Sincronize o projeto no Android Studio (Sync Now)
2. Verifique se não há erros
3. Se houver erro de "keystore.properties not found", verifique se o arquivo está na raiz do projeto

---

## 🧪 Passo 8: Testar Build de Release

Execute no terminal:

```powershell
.\gradlew.bat bundleRelease
```

**Ou no Android Studio:**
1. Build > Generate Signed Bundle / APK
2. Selecione "Android App Bundle"
3. Selecione o keystore
4. Preencha as senhas
5. Clique em Next e depois Finish

O arquivo será gerado em:
```
app\build\outputs\bundle\release\app-release.aab
```

---

## 📦 Passo 9: Verificar AAB Gerado

1. Verifique se o arquivo `app-release.aab` foi criado
2. O tamanho deve ser de alguns MBs (não pode ser 0 bytes)
3. Você pode verificar a assinatura com:

```powershell
jarsigner -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab
```

---

## 🚨 Troubleshooting

### Erro: "keystore.properties not found"
- Verifique se o arquivo está na raiz do projeto (mesmo nível do `build.gradle.kts`)
- Verifique se o nome está correto: `keystore.properties` (sem espaços)

### Erro: "Keystore file not found"
- Verifique o caminho no `keystore.properties`
- Use barras `/` ou `\\` no Windows
- Verifique se o arquivo `.jks` existe no caminho especificado

### Erro: "Wrong password"
- Verifique se as senhas no `keystore.properties` estão corretas
- Verifique se não há espaços extras antes/depois das senhas

### Erro: "Alias not found"
- Verifique se o alias no `keystore.properties` corresponde ao usado na criação do keystore
- O alias padrão é `taskgo-release`

---

## 🔐 Segurança - Checklist

- [ ] Keystore movido para local seguro (fora do projeto)
- [ ] `keystore.properties` adicionado ao `.gitignore`
- [ ] `*.jks` e `*.keystore` adicionados ao `.gitignore`
- [ ] Senhas anotadas em local seguro
- [ ] Backup do keystore criado em local seguro
- [ ] Verificado que keystore NÃO está no Git

---

## 📚 Informações Importantes

### ⚠️ PERDA DO KEYSTORE
Se você perder o keystore ou esquecer as senhas:
- **NÃO será possível atualizar o app na Play Store**
- Você terá que criar um novo app com novo package name
- Todos os usuários terão que desinstalar e reinstalar

### 🔄 RENOVAÇÃO DO KEYSTORE
O keystore tem validade de 10000 dias (~27 anos). Se precisar renovar antes:
- Use o mesmo keystore e alias
- Execute o comando de criação novamente com o mesmo nome
- Isso atualizará a validade

### 📱 MÚLTIPLOS KEYSTORES
Se você tem múltiplos apps:
- Crie um keystore diferente para cada app
- Use nomes descritivos: `app1-release-key.jks`, `app2-release-key.jks`
- Mantenha todos em local seguro organizado

---

## ✅ Pronto!

Agora você está pronto para gerar builds assinados para a Google Play Store!

**Próximos passos:**
1. Gerar o AAB assinado
2. Fazer upload na Play Console
3. Preencher informações do listing
4. Enviar para revisão

**Boa sorte com o lançamento! 🚀**

