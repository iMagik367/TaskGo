# Deploy e Build - Versão 1.0.97
## Correção Definitiva do Fluxo de Localização

### 📋 Resumo das Mudanças

#### 1. **Firestore Rules Atualizadas**
- ✅ Adicionadas regras para `locations/{locationId}/services`
- ✅ Adicionadas regras para `locations/{locationId}/orders`
- ✅ Adicionadas regras para `locations/{locationId}/feed` (com subcoleções de comments e ratings)

#### 2. **Versão do App Atualizada**
- ✅ `versionCode`: 96 → **97**
- ✅ `versionName`: 1.0.96 → **1.0.97**

#### 3. **Scripts Criados/Atualizados**
- ✅ `deploy-firebase-completo-localizacao.ps1` - Script completo de deploy
- ✅ `build-aab-release.ps1` - Atualizado com opção de deploy antes do build

---

## 🚀 Passos para Deploy e Build

### Passo 1: Deploy do Firebase

Execute o script de deploy completo:

```powershell
.\deploy-firebase-completo-localizacao.ps1
```

Este script irá:
1. ✅ Verificar Firebase CLI e autenticação
2. ✅ Fazer deploy das Firestore Rules
3. ✅ Fazer deploy dos Firestore Indexes
4. ✅ Compilar as Cloud Functions
5. ✅ Fazer deploy das Cloud Functions

**OU** execute manualmente:

```powershell
# Deploy das Rules
firebase deploy --only firestore:rules

# Deploy dos Indexes
firebase deploy --only firestore:indexes

# Build e Deploy das Functions
cd functions
npm run build
cd ..
firebase deploy --only functions
```

### Passo 2: Build do AAB

Execute o script de build:

```powershell
.\build-aab-release.ps1
```

O script irá:
1. ✅ Verificar versão atual (1.0.97)
2. ✅ Verificar keystore.properties
3. ✅ Limpar builds anteriores
4. ✅ Compilar AAB Release
5. ✅ Abrir pasta de outputs

**OU** execute manualmente:

```powershell
# Limpar build anterior
.\gradlew clean

# Compilar AAB
.\gradlew bundleRelease

# O AAB estará em: app/build/outputs/bundle/release/app-release.aab
```

---

## 📝 Checklist de Validação

Antes de fazer upload no Google Play Console, verifique:

- [ ] Deploy das Firestore Rules executado com sucesso
- [ ] Deploy das Cloud Functions executado com sucesso
- [ ] Versão do app atualizada para 1.0.97 (Code: 97)
- [ ] AAB gerado com sucesso
- [ ] Tamanho do AAB verificado (deve estar em MB razoável)
- [ ] Testado localmente (se possível)

---

## 🔍 Verificações Pós-Deploy

### Verificar Firestore Rules
```powershell
firebase firestore:rules:get
```

### Verificar Functions Deployadas
```powershell
firebase functions:list
```

### Verificar Versão do App
```powershell
# Verificar no arquivo app/build.gradle.kts
# Deve mostrar: versionCode = 97, versionName = "1.0.97"
```

---

## 📦 Estrutura das Novas Coleções por Localização

Após o deploy, as seguintes coleções estarão disponíveis:

```
locations/{locationId}/
  ├── products/     ✅ Já existia
  ├── stories/      ✅ Já existia
  ├── services/     ✅ NOVO
  ├── orders/       ✅ NOVO
  └── feed/         ✅ NOVO
      ├── comments/
      └── ratings/
```

Onde `locationId` = `normalize(city, state)` (ex: `cascavel_pr`, `osasco_sp`)

---

## ⚠️ Importante

1. **Nunca use "unknown" como locationId** - As regras e o código bloqueiam isso
2. **Todas as queries dependem de LocationStateManager** - Nenhuma query executa sem localização válida
3. **Feed agora é regional** - Usuários veem apenas posts da sua região
4. **Backend continua funcionando** - As Cloud Functions já salvam nos paths corretos

---

## 🐛 Troubleshooting

### Erro: "Firebase CLI não encontrado"
```powershell
npm install -g firebase-tools
```

### Erro: "Não autenticado"
```powershell
firebase login
```

### Erro: "Projeto não configurado"
```powershell
firebase use --add
```

### Erro no build das Functions
```powershell
cd functions
npm install
npm run build
```

### Erro no build do AAB
- Verifique se `keystore.properties` existe
- Verifique se a senha do keystore está correta
- Execute `.\gradlew clean` antes de tentar novamente

---

## 📞 Suporte

Em caso de problemas:
1. Verifique os logs do Firebase: `firebase functions:log`
2. Verifique os logs do build: `.\gradlew bundleRelease --stacktrace`
3. Consulte a documentação do Firebase: https://firebase.google.com/docs

---

**Data de Criação**: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
**Versão**: 1.0.97
**Status**: ✅ Pronto para deploy e build
