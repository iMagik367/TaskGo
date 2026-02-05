# Deploy Firebase - Versão 1.2.4

## Comandos para Deploy Manual

Se o script `DEPLOY_FIREBASE_V1.2.4.bat` não funcionar devido a problemas com Firebase CLI, execute os comandos manualmente:

### 1. Deploy das Cloud Functions

```bash
cd functions
npm run build
cd ..
firebase deploy --only functions
```

### 2. Deploy das Firestore Rules

```bash
firebase deploy --only firestore:rules
```

### 3. Deploy Completo (Functions + Rules)

```bash
firebase deploy --only functions,firestore:rules
```

## Mudanças na Versão 1.2.4

- **LocationStateManager**: Corrigido para sempre emitir `LocationState.Ready`
- **GPS**: Sistema robusto com fallback automático para Brasília/DF
- **Backend**: Recebe GPS do frontend (latitude, longitude, city, state)
- **Frontend**: Obtém GPS no momento da operação (não depende do perfil)

## Nota sobre Secrets

Se houver secrets configurados no Firebase, eles precisam ser atualizados manualmente via Firebase Console ou CLI:

```bash
firebase functions:secrets:set SECRET_NAME
```

## Troubleshooting

Se encontrar erro com Firebase CLI:
1. Reinstale: `npm install -g firebase-tools@latest`
2. Verifique Node.js: `node --version` (deve ser 18+)
3. Faça login: `firebase login`
