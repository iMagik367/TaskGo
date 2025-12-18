# 📝 Guia Rápido: Descomentar Linhas no build.gradle.kts

## 🔧 Linhas para Descomentar

### 1️⃣ Carregamento do keystore.properties (Linhas ~45-51)

**ENCONTRE ESTE BLOCO:**
```kotlin
// Load keystore.properties for release signing
// Descomente estas linhas após criar o keystore e o arquivo keystore.properties
/*
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
*/
```

**DESCOMENTE PARA FICAR ASSIM:**
```kotlin
// Load keystore.properties for release signing
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

---

### 2️⃣ Signing Configs (Linhas ~140-147)

**ENCONTRE ESTE BLOCO:**
```kotlin
// Signing configs - será configurado após criar keystore
// Descomente o bloco abaixo após criar o keystore e o arquivo keystore.properties
signingConfigs {
    /*
    create("release") {
        keyAlias = keystoreProperties["TASKGO_RELEASE_KEY_ALIAS"] as String
        keyPassword = keystoreProperties["TASKGO_RELEASE_KEY_PASSWORD"] as String
        storeFile = file(keystoreProperties["TASKGO_RELEASE_STORE_FILE"] as String)
        storePassword = keystoreProperties["TASKGO_RELEASE_STORE_PASSWORD"] as String
    }
    */
}
```

**DESCOMENTE PARA FICAR ASSIM:**
```kotlin
// Signing configs
signingConfigs {
    create("release") {
        keyAlias = keystoreProperties["TASKGO_RELEASE_KEY_ALIAS"] as String
        keyPassword = keystoreProperties["TASKGO_RELEASE_KEY_PASSWORD"] as String
        storeFile = file(keystoreProperties["TASKGO_RELEASE_STORE_FILE"] as String)
        storePassword = keystoreProperties["TASKGO_RELEASE_STORE_PASSWORD"] as String
    }
}
```

---

### 3️⃣ Aplicar Signing Config no Release (Linha ~133)

**ENCONTRE ESTA LINHA:**
```kotlin
// Descomente a linha abaixo após configurar o signingConfigs
// signingConfig = signingConfigs.getByName("release")
```

**DESCOMENTE PARA FICAR ASSIM:**
```kotlin
signingConfig = signingConfigs.getByName("release")
```

---

## ✅ Após Descomentar

1. **Sincronize o projeto** no Android Studio (Sync Now)
2. **Verifique se não há erros** na aba Build
3. **Teste o build** com: `.\gradlew.bat bundleRelease`

---

## 🚨 Se Der Erro

### Erro: "keystore.properties not found"
- Verifique se o arquivo está na **raiz do projeto** (mesmo nível do `build.gradle.kts`)
- Verifique se o nome está correto: `keystore.properties` (sem espaços)

### Erro: "Keystore file not found"
- Verifique o caminho no `keystore.properties`
- Use barras `/` ou `\\` no Windows
- Exemplo: `C:/Users/user/AndroidKeystores/taskgo-release-key.jks`

### Erro: "Wrong password"
- Verifique se as senhas no `keystore.properties` estão corretas
- Não pode ter espaços antes/depois das senhas

