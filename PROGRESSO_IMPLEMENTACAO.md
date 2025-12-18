# Progresso da Implementação - Tarefas Solicitadas

## ✅ Tarefas Concluídas

### 1. Remover botão "Salvar" da AccountScreen
- ✅ Removido o botão "Salvar Alterações"
- ✅ Aproximados os botões "Solicitar Mudança de Modo de Conta" e "Sair da Conta" (espaçamento de 8.dp)

### 2. Abas "Ativas" e "Canceladas" na tela de ordens de serviço
- ✅ Implementado sistema de abas similar ao da HomeScreen (Produtos/Serviços)
- ✅ Aba "Ativas" selecionada por padrão ao abrir a tela
- ✅ Filtragem automática: ordens canceladas aparecem apenas na aba "Canceladas"
- ✅ Quando uma ordem é excluída, ela é marcada como cancelada e move para a aba "Canceladas"
- ✅ Botões de ação (Editar/Excluir) aparecem apenas na aba "Ativas"

### 4. Correção da verificação de identidade (parcial)
- ✅ TopBar corrigido para usar AppTopBar (mesmo tamanho das outras telas)
- ⚠️ Simplificação do teste facial ainda em progresso

### 5. Serviços na busca universal
- ✅ A busca universal já exibe serviços abaixo dos produtos
- ⚠️ Verificar se serviços em destaque estão sendo exibidos corretamente

## ⚠️ Tarefas em Progresso

### 3. Sistema Financeiro Completo
**Status**: Pendente - Requer investigação e implementação completa

**O que precisa ser feito:**
1. Sistema de cadastro de contas bancárias para vendedores
   - Modelo de dados para contas bancárias
   - Tela de cadastro/edição de contas bancárias
   - Validação de dados bancários (banco, agência, conta, CPF/CNPJ)
   - Integração com Stripe Connect para vendedores

2. Gateway de pagamento com split de pagamento
   - Modificar função `createPaymentIntent` para produtos (atualmente só para serviços)
   - Implementar split de 2% de comissão para o app
   - 98% do valor vai para o vendedor
   - Criar função específica para pagamentos de produtos: `createProductPaymentIntent`
   - Atualizar webhooks para processar pagamentos de produtos

3. Melhorar módulo de finalização de compra
   - Integrar split de pagamento no checkout
   - Adicionar validação de conta bancária do vendedor antes de finalizar
   - Notificações para vendedor quando pagamento é confirmado
   - Atualizar status do pedido após pagamento confirmado

**Arquivos que precisam ser criados/modificados:**
- `app/src/main/java/com/taskgoapp/taskgo/data/firestore/models/BankAccount.kt` (novo)
- `app/src/main/java/com/taskgoapp/taskgo/feature/settings/presentation/BankAccountScreen.kt` (novo)
- `functions/src/product-payments.ts` (novo)
- `functions/src/payments.ts` (modificar)
- `app/src/main/java/com/taskgoapp/taskgo/feature/checkout/presentation/CheckoutViewModel.kt` (modificar)

### 6. Sistema de Rastreamento Completo
**Status**: Pendente - Requer implementação completa

**O que precisa ser feito:**
1. Página de envio para vendedores
   - Tela para vendedor confirmar envio do produto
   - Opção para inserir código de rastreamento (Correios ou outra transportadora)
   - Campo para URL de rastreamento (quando não for Correios)
   - Lógica diferente para pedidos na mesma cidade vs entre cidades

2. Integração com rastreamento dos Correios
   - Algoritmo para buscar rastreamento no site oficial dos Correios
   - Atualização automática do status do pedido
   - Worker/Cloud Function para verificar rastreamento periodicamente

3. Suporte a outras transportadoras
   - Campo para URL de rastreamento personalizada
   - Algoritmo genérico para buscar informações de rastreamento

4. Lógica para pedidos na mesma cidade
   - Opção para vendedor confirmar recebimento do pedido
   - Horário de chegada do pedido
   - Tela de rastreamento estilo iFood para o cliente

**Arquivos que precisam ser criados/modificados:**
- `app/src/main/java/com/taskgoapp/taskgo/feature/orders/presentation/ShipmentScreen.kt` (novo)
- `app/src/main/java/com/taskgoapp/taskgo/core/tracking/CorreiosTracker.kt` (novo)
- `app/src/main/java/com/taskgoapp/taskgo/core/tracking/GenericTracker.kt` (novo)
- `functions/src/tracking.ts` (novo)
- `app/src/main/java/com/taskgoapp/taskgo/core/work/TrackingWorker.kt` (novo)

## 📋 Próximos Passos

1. Completar simplificação do teste facial (adicionar botão de confirmação manual)
2. Verificar se serviços em destaque aparecem na busca universal
3. Implementar sistema financeiro completo
4. Implementar sistema de rastreamento completo
5. Criar índices e functions necessárias no Firebase
6. Fazer deploy e build completa

## 🔧 Configurações Necessárias

### Firebase Functions
- Variáveis de ambiente para Stripe (já configuradas)
- Nova função para pagamentos de produtos com split
- Função para rastreamento automático

### Firestore
- Nova coleção: `bank_accounts`
- Índices para consultas de rastreamento
- Índices para consultas de pagamentos de produtos

### Android
- Permissões para acesso à câmera (já configuradas)
- WorkManager para rastreamento periódico
- Integração com APIs de rastreamento

