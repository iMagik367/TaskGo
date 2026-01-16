# ✅ Checklist Rápido de Deploy

## 🚀 Deploy Rápido (Sequência Mínima)

### 1. Preparar Backend

```bash
cd functions
npm install
npm run build
```

### 2. Deploy das Firestore Rules

```bash
# Validar primeiro
firebase deploy --only firestore:rules --dry-run

# Deploy real
firebase deploy --only firestore:rules
```

### 3. Deploy das Cloud Functions

```bash
# Deploy completo
firebase deploy --only functions

# OU deploy seletivo (apenas novas funções)
firebase deploy --only functions:setUserRole,functions:setInitialUserRole,functions:createService,functions:updateService,functions:deleteService,functions:createProduct,functions:updateProduct,functions:deleteProduct
```

### 4. Migrar Usuários Existentes

```bash
# Via Cloud Function (após deploy)
firebase functions:call migrateExistingUsersToCustomClaims --data '{"dryRun":true}'
firebase functions:call migrateExistingUsersToCustomClaims --data '{"dryRun":false}'
```

### 5. Verificar Deploy

```bash
# Ver funções deployadas
firebase functions:list

# Ver logs
firebase functions:log
```

---

## ✅ Verificações Pós-Deploy

- [ ] Firestore Rules deployadas
- [ ] Cloud Functions deployadas
- [ ] Migração de Custom Claims executada
- [ ] Logs sem erros críticos
- [ ] App Check configurado (se aplicável)

---

## 📚 Documentação Completa

- `GUIA_DEPLOY_COMPLETO.md` - Guia detalhado completo
- `BACKEND_TRANSFORMACAO_COMPLETA.md` - Documentação técnica
- `GUIA_MIGRACAO_APP_ANDROID.md` - Migração do app

---

**Tempo estimado:** 15-30 minutos
