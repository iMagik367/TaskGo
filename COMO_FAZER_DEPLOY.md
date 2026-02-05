# Como Fazer Deploy - Versão 1.2.4

## Opção 1: Via Firebase Console (Mais Simples)

### Firestore Rules:
1. Acesse: https://console.firebase.google.com
2. Selecione o projeto TaskGo
3. Vá em **Firestore Database** → **Rules**
4. Abra o arquivo `firestore.rules` deste projeto
5. Copie todo o conteúdo
6. Cole no editor do Firebase Console
7. Clique em **Publicar**

### Cloud Functions:
1. No Firebase Console, vá em **Functions**
2. Se já existirem functions, elas serão atualizadas automaticamente quando você fizer deploy via CLI
3. Ou use a interface web se disponível

## Opção 2: Reinstalar Firebase CLI (Se Quiser Usar CLI)

Se você quiser usar o CLI novamente:

```bash
npm uninstall -g firebase-tools
npm install -g firebase-tools@latest
firebase login
```

Depois:
```bash
firebase deploy --only firestore:rules,functions
```

## Importante

**Nenhuma mudança foi feita no backend**. As functions e rules já estavam corretas. O deploy é apenas para garantir sincronização.

As mudanças foram **apenas no frontend** (Android app).
