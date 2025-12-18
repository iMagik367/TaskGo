# Configuração de Signing para Release Build

## Instruções para criar keystore e configurar signing

### 1. Criar o keystore

Execute o seguinte comando no terminal (substitua as informações conforme necessário):

```bash
keytool -genkey -v -keystore taskgo-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias taskgo-release
```

**Informações solicitadas:**
- Nome e sobrenome: TaskGo App
- Nome da unidade organizacional: [Sua empresa]
- Nome da organização: [Sua empresa]
- Nome da cidade: [Sua cidade]
- Nome do estado: [Seu estado]
- Código do país: BR
- Senha do keystore: [Escolha uma senha forte]
- Confirmação da senha: [Repita a senha]
- Alias: taskgo-release
- Senha do alias: [Pode ser a mesma do keystore ou diferente]

**IMPORTANTE:**
- Guarde o arquivo `taskgo-release-key.jks` em local seguro
- Anote as senhas em local seguro
- **NUNCA** faça commit do keystore no Git
- Adicione `*.jks` e `keystore.properties` ao `.gitignore`

### 2. Criar arquivo keystore.properties

Crie um arquivo `keystore.properties` na raiz do projeto com o seguinte conteúdo:

```properties
TASKGO_RELEASE_STORE_FILE=../taskgo-release-key.jks
TASKGO_RELEASE_KEY_ALIAS=taskgo-release
TASKGO_RELEASE_STORE_PASSWORD=sua_senha_do_keystore_aqui
TASKGO_RELEASE_KEY_PASSWORD=sua_senha_do_alias_aqui
```

**IMPORTANTE:**
- Substitua `sua_senha_do_keystore_aqui` e `sua_senha_do_alias_aqui` pelas senhas reais
- Adicione `keystore.properties` ao `.gitignore`
- Não faça commit deste arquivo no Git

### 3. Descomentar signing config no build.gradle.kts

Após criar o keystore e o arquivo `keystore.properties`, descomente as seguintes linhas no `app/build.gradle.kts`:

**Linha ~43-50:** Descomente o bloco que carrega keystore.properties
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

**Linha ~138-147:** Descomente o bloco signingConfigs
```kotlin
signingConfigs {
    create("release") {
        keyAlias = keystoreProperties["TASKGO_RELEASE_KEY_ALIAS"] as String
        keyPassword = keystoreProperties["TASKGO_RELEASE_KEY_PASSWORD"] as String
        storeFile = file(keystoreProperties["TASKGO_RELEASE_STORE_FILE"] as String)
        storePassword = keystoreProperties["TASKGO_RELEASE_STORE_PASSWORD"] as String
    }
}
```

**Linha ~132:** Descomente a linha que aplica o signing config
```kotlin
signingConfig = signingConfigs.getByName("release")
```

### 4. Verificar .gitignore

Certifique-se de que o `.gitignore` contém:

```
*.jks
*.keystore
keystore.properties
```

### 5. Gerar AAB assinado

Após configurar tudo, você pode gerar o AAB assinado com:

```bash
./gradlew bundleRelease
```

O arquivo será gerado em: `app/build/outputs/bundle/release/app-release.aab`

### 6. Upload para Google Play Console

1. Acesse https://play.google.com/console
2. Selecione seu app
3. Vá em "Produção" > "Criar nova versão"
4. Faça upload do arquivo `app-release.aab`
5. Preencha as informações da versão
6. Envie para revisão

---

## Segurança

- **NUNCA** compartilhe o keystore ou as senhas
- Se perder o keystore, você não poderá atualizar o app na Play Store
- Mantenha backups seguros do keystore
- Considere usar um serviço de gerenciamento de segredos para produção

