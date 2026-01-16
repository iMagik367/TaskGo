# 🚀 Deploy do Backend TaskGo - Guia Rápido

## ⚡ Início Rápido

Para fazer deploy completo do backend transformado:

```bash
# 1. Preparar
cd functions
npm install
npm run build

# 2. Deploy Rules
firebase deploy --only firestore:rules

# 3. Deploy Functions
firebase deploy --only functions

# 4. Migrar usuários (após deploy)
firebase functions:call migrateExistingUsersToCustomClaims --data '{"dryRun":false}'
```

---

## 📚 Documentação Completa

- **`CHECKLIST_DEPLOY_RAPIDO.md`** ⚡ - Checklist rápido
- **`GUIA_DEPLOY_COMPLETO.md`** 📖 - Guia detalhado completo
- **`BACKEND_TRANSFORMACAO_COMPLETA.md`** 🔒 - Documentação técnica
- **`GUIA_MIGRACAO_APP_ANDROID.md`** 📱 - Migração do app Android
- **`RESUMO_IMPLEMENTACAO_FINAL.md`** 📋 - Resumo executivo

---

## ⚠️ IMPORTANTE

1. **Backup:** Faça backup das Firestore Rules antes do deploy
2. **App Android:** O app precisa ser atualizado ANTES de usar em produção
3. **Migração:** Execute migração de Custom Claims após deploy
4. **Testes:** Teste em desenvolvimento antes de produção

---

## 🆘 Problemas?

Consulte `GUIA_DEPLOY_COMPLETO.md` seção "Troubleshooting"

---

**Última atualização:** 2024
