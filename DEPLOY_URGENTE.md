# ⚠️ DEPLOY URGENTE - Versão 1.2.4

## Status Atual

**NÃO FOI FEITO O DEPLOY** devido a problema no Firebase CLI instalado globalmente.

## O Que Precisa Ser Deployado

### ✅ Firestore Rules
- **Status**: Prontas para deploy
- **Mudanças**: Nenhuma (já permitem city/state)
- **Arquivo**: `firestore.rules`

### ✅ Cloud Functions  
- **Status**: Compiladas com sucesso
- **Mudanças**: Nenhuma (backend não foi alterado)
- **Arquivo**: `functions/lib/index.js` (após build)

## Como Fazer o Deploy AGORA

### Opção 1: Via Firebase Console (MAIS RÁPIDO)

1. **Firestore Rules**:
   - Acesse: https://console.firebase.google.com/project/task-go-ee85f/firestore/rules
   - Abra o arquivo `firestore.rules` deste projeto
   - Copie TODO o conteúdo
   - Cole no editor do Firebase Console
   - Clique em **Publicar**

2. **Cloud Functions**:
   - As functions já estão deployadas (não foram alteradas)
   - Se quiser garantir, pode fazer deploy via CLI quando corrigir

### Opção 2: Corrigir Firebase CLI e Deployar

```bash
# 1. Desinstalar Firebase Tools corrompido
npm uninstall -g firebase-tools

# 2. Limpar cache do npm
npm cache clean --force

# 3. Reinstalar Firebase Tools
npm install -g firebase-tools@latest

# 4. Fazer login
firebase login

# 5. Selecionar projeto
firebase use task-go-ee85f

# 6. Deploy Rules
firebase deploy --only firestore:rules

# 7. Deploy Functions (se necessário)
cd functions
npm run build
cd ..
firebase deploy --only functions
```

### Opção 3: Usar npx (sem instalar globalmente)

```bash
# Deploy Rules
npx firebase-tools@latest deploy --only firestore:rules --project task-go-ee85f

# Deploy Functions
cd functions
npm run build
cd ..
npx firebase-tools@latest deploy --only functions --project task-go-ee85f
```

## ⚠️ IMPORTANTE

**Nenhuma mudança foi feita no backend**. As functions e rules já estavam corretas. O deploy é apenas para garantir sincronização.

As mudanças foram **100% no frontend** (Android app).

## Verificação Pós-Deploy

Após fazer o deploy, verifique:

1. **Firestore Rules**:
   - Console → Firestore → Rules
   - Verifique se foram atualizadas

2. **Cloud Functions**:
   - Console → Functions
   - Verifique se estão ativas

3. **Logs**:
   - Console → Functions → Logs
   - Verifique se não há erros
