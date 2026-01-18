# Executar Migração Completa: Default → Taskgo

## 🎯 OBJETIVO

Migrar **TODOS** os dados do database Firestore 'default' para 'taskgo' de forma completa, segura e validada.

## 🚀 OPÇÕES DE EXECUÇÃO

### Opção 1: Via Cloud Function HTTP (Recomendado)

A função `migrateDatabaseToTaskgo` está configurada como HTTP function e pode ser chamada diretamente.

**Passos:**

1. **Fazer deploy da função:**
   ```bash
   cd functions
   firebase deploy --only functions:migrateDatabaseToTaskgo
   ```

2. **Executar a migração:**
   ```bash
   # Obter o URL da função após o deploy
   curl -X POST https://us-central1-task-go-ee85f.cloudfunctions.net/migrateDatabaseToTaskgo
   ```

   Ou acesse o URL no navegador após o deploy.

3. **Acompanhar logs:**
   ```bash
   firebase functions:log --only migrateDatabaseToTaskgo
   ```

### Opção 2: Via Script Standalone (Local)

Execute o script diretamente no seu ambiente local.

**Passos:**

1. **Instalar dependências (se necessário):**
   ```bash
   cd functions
   npm install
   ```

2. **Executar script:**
   ```bash
   npx ts-node scripts/executar-migracao.ts
   ```

   Ou compile e execute:
   ```bash
   npm run build
   node lib/scripts/executar-migracao.js
   ```

### Opção 3: Via Firebase Functions Shell

Execute interativamente no shell do Firebase.

**Passos:**

1. **Iniciar shell:**
   ```bash
   cd functions
   firebase functions:shell
   ```

2. **Executar função:**
   ```javascript
   migrateDatabaseToTaskgo()
   ```

## ✅ VALIDAÇÃO PÓS-MIGRAÇÃO

Após executar a migração, valide:

1. **Contagem de documentos:**
   - Compare o número de documentos em cada coleção entre 'default' e 'taskgo'
   - Todas as coleções devem ter pelo menos o mesmo número de documentos

2. **Integridade dos dados:**
   - Verifique alguns documentos aleatórios em 'taskgo'
   - Confirme que os dados foram copiados corretamente

3. **Subcoleções:**
   - Verifique se subcoleções (ex: `conversations/{id}/messages`) foram migradas

4. **Logs:**
   - Revise os logs para garantir que não houve erros críticos

## 📊 COLEÇÕES MIGRADAS

O script migra as seguintes coleções:

- `users` - Usuários do sistema
- `products` - Produtos
- `services` - Serviços
- `orders` - Pedidos de serviços
- `conversations` - Conversas do chat IA
- `stories` - Stories do feed
- `posts` - Posts do feed
- `notifications` - Notificações
- `categories` - Categorias
- `reviews` - Avaliações
- `ai_usage` - Uso de IA
- `moderation_logs` - Logs de moderação
- `shipments` - Envios
- `purchase_orders` - Pedidos de compra
- `account_change_requests` - Solicitações de mudança de conta
- `identity_verifications` - Verificações de identidade
- `two_factor_codes` - Códigos 2FA

## ⚠️ IMPORTANTE

- **NÃO DELETE** o database 'default' antes de validar completamente a migração
- A migração usa **merge** para não sobrescrever dados existentes em 'taskgo'
- Processa em **batches de 500 documentos** para evitar timeouts
- **Valida** cada coleção após a migração
- **Loga** progresso detalhado para auditoria

## 🔒 SEGURANÇA

- Processa dados em batches seguros
- Valida integridade antes de commitar
- Trata erros sem interromper a migração completa
- Não sobrescreve dados existentes (usa merge)
- Loga todas as operações para auditoria

## 📝 RESULTADO ESPERADO

Após a migração bem-sucedida, você verá:

```
========================================
🎉 MIGRAÇÃO CONCLUÍDA
========================================
⏱️  Duração total: XXXs
📊 Coleções processadas: 17
   ✅ Sucesso: 17
   ⚠️  Parcial: 0
   ❌ Falhou: 0
📄 Documentos migrados: XXXX
📁 Subcoleções migradas: XXXX
❌ Erros: 0
========================================
```
