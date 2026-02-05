# Instruções para Deploy Manual - Versão 1.2.4

## ⚠️ Problema Detectado

O Firebase CLI instalado globalmente está com um erro no módulo `chardet`. Isso é um problema de instalação corrompida.

## 🔧 Solução 1: Corrigir Firebase CLI

Execute o script:
```bash
CORRIGIR_FIREBASE_CLI.bat
```

Ou manualmente:
```bash
npm uninstall -g firebase-tools
npm install -g firebase-tools@latest
firebase login
```

## 🔧 Solução 2: Usar npx (sem instalação global)

Se a correção não funcionar, você pode usar npx diretamente:

### 1. Compilar Functions
```bash
cd functions
npm run build
cd ..
```

### 2. Deploy Rules
```bash
npx firebase-tools@latest deploy --only firestore:rules
```

### 3. Deploy Functions
```bash
npx firebase-tools@latest deploy --only functions
```

## 🔧 Solução 3: Usar Firebase Console

Se o CLI continuar com problemas, você pode fazer o deploy via Firebase Console:

### Firestore Rules:
1. Acesse: https://console.firebase.google.com
2. Selecione o projeto TaskGo
3. Vá em Firestore Database → Rules
4. Copie o conteúdo de `firestore.rules`
5. Cole no editor e publique

### Cloud Functions:
1. Acesse: https://console.firebase.google.com
2. Selecione o projeto TaskGo
3. Vá em Functions
4. Use o botão "Deploy" ou faça upload via CLI local

## 📋 Checklist de Deploy

- [ ] Functions compiladas (`npm run build` em `functions/`)
- [ ] Firestore Rules atualizadas
- [ ] Cloud Functions deployadas
- [ ] Verificar logs após deploy
- [ ] Testar funcionalidades críticas

## 🔍 Verificação Pós-Deploy

### 1. Verificar Rules
```bash
firebase firestore:rules:get
```

### 2. Verificar Functions
```bash
firebase functions:list
```

### 3. Ver Logs
```bash
firebase functions:log
```

## 📝 Mudanças que Precisam de Deploy

### Firestore Rules
- ✅ Já permitem city/state no perfil do usuário
- ✅ Não precisam de alterações

### Cloud Functions
- ✅ Já estão corretas (não foram alteradas)
- ✅ Já recebem GPS do frontend
- ✅ Já validam location corretamente

## 🚨 Importante

As mudanças feitas foram **apenas no frontend**:
- Refatoração da camada de localização
- Dialog de seleção de conta expandido
- LoginViewModel atualizado

**O backend não precisa de alterações**, mas é recomendado fazer o deploy para garantir que tudo está sincronizado.
