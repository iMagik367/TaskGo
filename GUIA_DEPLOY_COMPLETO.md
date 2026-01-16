# 🚀 Guia Completo de Deploy - Backend TaskGo

Este guia detalha todos os passos necessários para fazer deploy do backend transformado.

---

## ⚠️ PRÉ-REQUISITOS

1. **Firebase CLI instalado e configurado:**
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase projects:list
   ```

2. **Node.js e npm instalados:**
   - Node.js 20+ (conforme `functions/package.json`)
   - npm ou yarn

3. **Acesso ao projeto Firebase:**
   - Credenciais configuradas
   - Permissões de deploy

---

## 📋 CHECKLIST PRÉ-DEPLOY

### ✅ Verificações

- [ ] **Sem erros de compilação:**
  ```bash
  cd functions
  npm install
  npm run build
  ```

- [ ] **Sem erros de lint:**
  ```bash
  cd functions
  npm run lint
  ```

- [ ] **Firestore Rules válidas:**
  ```bash
  firebase deploy --only firestore:rules --dry-run
  ```

- [ ] **Variáveis de ambiente configuradas:**
  - Verificar `.env` ou Firebase Functions config
  - APIs necessárias habilitadas no Google Cloud

---

## 🔧 PASSO 1: PREPARAR AMBIENTE

### 1.1 Instalar Dependências

```bash
cd functions
npm install
```

### 1.2 Compilar TypeScript

```bash
npm run build
```

Verificar se `lib/` foi criado sem erros.

### 1.3 Verificar Exports

Certificar-se de que `functions/src/index.ts` exporta todas as funções:

```typescript
// Novas funções devem estar exportadas:
export * from './admin/roles';
export * from './users/role';
export * from './services/index';
export * from './products/index';
export * from './scripts/migrateExistingUsers';
```

---

## 🚀 PASSO 2: DEPLOY DAS FIRESTORE RULES

### 2.1 Validar Rules

```bash
# Validar sintaxe sem fazer deploy
firebase deploy --only firestore:rules --dry-run
```

### 2.2 Deploy das Rules

```bash
# Deploy apenas das Rules
firebase deploy --only firestore:rules
```

**⚠️ IMPORTANTE:** As novas rules bloqueiam escrita direta. Certifique-se de que:
- O app Android será atualizado ANTES de usar produção
- Ou que o deploy está sendo feito em ambiente de teste

---

## 🚀 PASSO 3: DEPLOY DAS CLOUD FUNCTIONS

### 3.1 Deploy Completo (Recomendado)

```bash
# Deploy de todas as funções
cd functions
npm run build
firebase deploy --only functions
```

### 3.2 Deploy Seletivo (Recomendado para Produção)

Deploy apenas das novas/atualizadas:

```bash
# Deploy apenas funções específicas
firebase deploy --only functions:setUserRole
firebase deploy --only functions:setInitialUserRole
firebase deploy --only functions:createService
firebase deploy --only functions:updateService
firebase deploy --only functions:deleteService
firebase deploy --only functions:createProduct
firebase deploy --only functions:updateProduct
firebase deploy --only functions:deleteProduct
firebase deploy --only functions:createOrder
firebase deploy --only functions:updateOrderStatus
firebase deploy --only functions:getMyOrders
firebase deploy --only functions:verifyIdentity
firebase deploy --only functions:approveIdentityVerification
firebase deploy --only functions:sendTwoFactorCode
firebase deploy --only functions:verifyTwoFactorCode
firebase deploy --only functions:startIdentityVerification
firebase deploy --only functions:aiChatProxy
firebase deploy --only functions:migrateExistingUsersToCustomClaims
```

### 3.3 Verificar Deploy

```bash
# Listar funções deployadas
firebase functions:list
```

---

## 🔐 PASSO 4: MIGRAR USUÁRIOS EXISTENTES

### 4.1 Opção 1: Via Cloud Function (Recomendado)

Após deploy, chamar a função como admin:

```bash
# Via Firebase CLI (requer autenticação como admin)
firebase functions:call migrateExistingUsersToCustomClaims --data '{"dryRun":true}'

# Se tudo estiver OK, executar de verdade
firebase functions:call migrateExistingUsersToCustomClaims --data '{"dryRun":false}'
```

### 4.2 Opção 2: Via Script Local

```bash
cd functions
npm run build
node -e "require('./lib/scripts/migrateExistingUsers').migrateLocal()"
```

### 4.3 Verificar Migração

```bash
# Verificar se Custom Claims foram definidas
# (requer acesso ao Firebase Console ou Admin SDK)
```

---

## 📱 PASSO 5: ATUALIZAR APP ANDROID

### 5.1 Seguir Guia de Migração

Consulte `GUIA_MIGRACAO_APP_ANDROID.md` para:
- Atualizar repositories para usar Cloud Functions
- Implementar `setInitialUserRole` após cadastro
- Atualizar tratamento de erros

### 5.2 Testar em Desenvolvimento

- [ ] Testar criação de services via Cloud Function
- [ ] Testar criação de products via Cloud Function
- [ ] Testar criação de orders via Cloud Function
- [ ] Testar atualização de status via Cloud Function
- [ ] Testar `setInitialUserRole` após cadastro

---

## 🔒 PASSO 6: CONFIGURAR APP CHECK

### 6.1 Verificar Configuração

- Firebase Console → App Check
- Verificar se Play Integrity está configurado
- Verificar se Debug Provider está configurado para desenvolvimento

### 6.2 Testar App Check

No app Android, verificar que:
- App Check token está sendo gerado
- Cloud Functions aceitam o token (em desenvolvimento)

### 6.3 Ativar Enforcement (PRODUÇÃO)

**⚠️ IMPORTANTE:** Apenas após testes completos!

- Firebase Console → App Check
- Ativar enforcement para:
  - Cloud Functions
  - Firestore
  - Storage (se aplicável)

---

## ✅ PASSO 7: VERIFICAÇÕES PÓS-DEPLOY

### 7.1 Verificar Logs

```bash
# Ver logs das Cloud Functions
firebase functions:log

# Ver logs específicos
firebase functions:log --only setUserRole
firebase functions:log --only createService
```

### 7.2 Verificar Métricas

- Firebase Console → Functions
- Verificar invocações, erros, latência
- Firebase Console → Firestore
- Verificar regras aplicadas

### 7.3 Testes Funcionais

- [ ] Criar serviço via Cloud Function
- [ ] Atualizar serviço via Cloud Function
- [ ] Criar produto via Cloud Function
- [ ] Criar ordem via Cloud Function
- [ ] Atualizar status de ordem
- [ ] Verificar Custom Claims sendo aplicadas

---

## 🚨 ROLLBACK (SE NECESSÁRIO)

### Rollback das Rules

```bash
# Restaurar rules anteriores
git checkout HEAD~1 firestore.rules
firebase deploy --only firestore:rules
```

### Rollback das Functions

```bash
# Listar versões anteriores
firebase functions:versions:list

# Restaurar versão anterior
firebase functions:versions:restore <VERSION_ID>
```

---

## 📊 MONITORAMENTO CONTÍNUO

### Métricas a Monitorar

1. **Cloud Functions:**
   - Taxa de erro
   - Latência
   - Invocações por função
   - App Check failures

2. **Firestore:**
   - Regras negadas (deve ser esperado para escrita direta)
   - Reads/Writes

3. **Custom Claims:**
   - Usuários sem Custom Claims (deve ser zero após migração)
   - Roles distribuídos

### Alertas Recomendados

- Taxa de erro > 5% em Cloud Functions
- App Check failures > 10%
- Regras negadas muito altas (pode indicar app não migrado)

---

## 🎯 SEQUÊNCIA RECOMENDADA DE DEPLOY

### Fase 1: Preparação (Desenvolvimento)
1. Deploy das Cloud Functions em desenvolvimento
2. Deploy das Firestore Rules em desenvolvimento
3. Testar tudo localmente

### Fase 2: Migração de Dados
1. Executar migração de Custom Claims (dry-run primeiro)
2. Verificar resultados
3. Executar migração real

### Fase 3: Atualização do App
1. Atualizar app Android para usar Cloud Functions
2. Testar em ambiente de desenvolvimento/staging
3. Atualizar app para produção

### Fase 4: Deploy Produção
1. Deploy das Cloud Functions em produção
2. Deploy das Firestore Rules em produção
3. Ativar App Check enforcement
4. Monitorar por 24-48 horas

---

## 📚 DOCUMENTAÇÃO ADICIONAL

- `BACKEND_TRANSFORMACAO_COMPLETA.md` - Documentação técnica completa
- `GUIA_MIGRACAO_APP_ANDROID.md` - Guia de migração do app
- `RESUMO_IMPLEMENTACAO_FINAL.md` - Resumo executivo

---

## 🆘 TROUBLESHOOTING

### Erro: "Permission denied" ao fazer deploy

```bash
# Verificar autenticação
firebase login
firebase projects:list

# Verificar permissões no projeto
```

### Erro: "Functions failed to deploy"

```bash
# Verificar logs de build
cd functions
npm run build

# Verificar erros específicos
firebase deploy --only functions:<FUNCTION_NAME> --debug
```

### Erro: "App Check validation failed"

- Verificar se App Check está configurado no app
- Verificar se token está sendo enviado
- Em desenvolvimento, desabilitar enforcement temporariamente

### Custom Claims não aparecem

- Verificar se migração foi executada
- Verificar se token foi atualizado (`getIdToken(true)`)
- Verificar logs da função de migração

---

**Data de Criação:** 2024
**Versão:** 1.0.0
**Última Atualização:** 2024
