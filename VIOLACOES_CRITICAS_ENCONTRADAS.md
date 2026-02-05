# VIOLAÇÕES CRÍTICAS ENCONTRADAS - VERIFICAÇÃO PROFUNDA

## 🚨 VIOLAÇÕES CRÍTICAS ENCONTRADAS

### 1. **functions/src/pix-payments.ts** ❌ CRÍTICO
**Violação**: Usa coleção global `purchase_orders` em vez de `locations/{locationId}/orders`
**Linhas afetadas**: 28, 82, 236, 299
**Status**: ✅ CORRIGIDO PARCIALMENTE (precisa verificar todas as ocorrências)

### 2. **functions/src/product-payments.ts** ❌ CRÍTICO
**Violação**: Usa coleção global `purchase_orders` em vez de `locations/{locationId}/orders`
**Linhas afetadas**: 33, 119, 203, 259, 381, 440, 527
**Status**: ❌ PENDENTE

### 3. **functions/src/product-orders.ts** ❌ CRÍTICO
**Violação**: 
- Usa coleção global `purchase_orders` em vez de `locations/{locationId}/orders`
- Triggers do Firestore escutam `purchase_orders/{orderId}` em vez de `locations/{locationId}/orders/{orderId}`
**Linhas afetadas**: 13, 167, 234
**Status**: ❌ PENDENTE (CRÍTICO - triggers precisam ser reconfigurados)

### 4. **functions/src/auto-refund.ts** ❌ CRÍTICO
**Violação**: Usa coleção global `purchase_orders` em vez de `locations/{locationId}/orders`
**Linhas afetadas**: 37, 123, 196
**Status**: ❌ PENDENTE

### 5. **functions/src/webhooks.ts** ❌ CRÍTICO
**Violação**: Usa coleção global `purchase_orders` em vez de `locations/{locationId}/orders`
**Linhas afetadas**: 133, 225
**Status**: ❌ PENDENTE

## 📊 RESUMO

- **Total de arquivos com violações críticas**: 5
- **Arquivos corrigidos**: 1 (parcialmente)
- **Arquivos pendentes**: 4
- **Gravidade**: CRÍTICA - violação direta do modelo canônico

## ⚠️ OBSERVAÇÕES IMPORTANTES

1. **Triggers do Firestore**: `product-orders.ts` tem triggers que escutam `purchase_orders/{orderId}`. Esses triggers precisam ser reconfigurados para escutar `locations/{locationId}/orders/{orderId}`, o que requer uma mudança arquitetural mais complexa.

2. **Queries em auto-refund.ts**: A função `checkAndRefundUnshippedOrders` faz query em `purchase_orders` global. Isso precisa ser refatorado para buscar em todas as localizações ou usar uma abordagem diferente.

3. **Consistência**: Todos os arquivos que lidam com `purchase_orders` precisam ser atualizados para usar `locations/{locationId}/orders` (ou `purchaseOrdersPath` helper).

## 🔧 PRÓXIMOS PASSOS

1. Corrigir `product-payments.ts` - substituir todas as ocorrências de `db.collection('purchase_orders')` por `purchaseOrdersPath(db, locationId)`
2. Corrigir `webhooks.ts` - substituir todas as ocorrências de `db.collection('purchase_orders')` por `purchaseOrdersPath(db, locationId)`
3. Corrigir `auto-refund.ts` - refatorar para buscar orders em todas as localizações
4. Corrigir `product-orders.ts` - reconfigurar triggers (requer mudança arquitetural)
5. Verificar se há mais arquivos que usam `purchase_orders`
