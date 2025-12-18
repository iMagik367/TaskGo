# Plano de Implementação - Sistema de Rastreamento de Pedidos

## Fluxo Completo (Baseado em iFood)

### 1. Cliente Compra Produto
- **Tela:** Tela de Produto → Botão "Comprar"
- **Ação:** Criar pedido no Firestore com status `PENDING_PAYMENT`
- **Notificação:** Cliente recebe confirmação de pedido criado

### 2. Pagamento Confirmado
- **Trigger:** Webhook do Stripe ou confirmação manual
- **Ação:** Atualizar status para `PAID`
- **Notificação:** 
  - Cliente: "Pagamento confirmado! Aguardando preparo"
  - Loja: "Novo pedido recebido! #12345"

### 3. Loja Envia Produto
- **Tela:** Dashboard da Loja → Botão "Enviar Pedido"
- **Ação:** Atualizar status para `SHIPPED` e criar evento de rastreamento
- **Notificação:** Cliente recebe "Seu pedido foi enviado! Acompanhe o rastreamento"

### 4. Rastreamento em Tempo Real
- **Tela:** Tela de Rastreamento (similar ao iFood)
- **Funcionalidades:**
  - Mapa com rota estimada
  - Timeline de eventos
  - Localização em tempo real do entregador (se aplicável)
  - Tempo estimado de entrega
  - Código de rastreamento

### 5. Produto Chega
- **Trigger:** Geolocalização (quando entregador está próximo) ou confirmação manual
- **Ação:** Atualizar status para `DELIVERED`
- **Notificação:** Cliente recebe "Pedido entregue! Avalie sua experiência"

### 6. Atualização Automática em Pedidos
- **Tela:** Lista de Pedidos
- **Ação:** Atualizar status automaticamente via listener do Firestore

## Telas Necessárias

### 1. OrderTrackingScreen (Melhorar existente)
- Mapa com rota
- Timeline de eventos
- Informações do pedido
- Botão de contato com loja
- Código de rastreamento

### 2. OrderDetailScreen (Melhorar existente)
- Detalhes completos do pedido
- Botão "Rastrear Pedido"
- Histórico de status
- Informações de pagamento

### 3. StoreDashboardScreen (Nova - para lojas)
- Lista de pedidos pendentes
- Botão "Enviar Pedido"
- Botão "Marcar como Enviado"
- Estatísticas de vendas

## Modelos de Dados

### Order (Firestore)
```kotlin
data class Order(
    val id: String,
    val clientId: String,
    val storeId: String,
    val productId: String,
    val status: OrderStatus,
    val trackingCode: String?,
    val shippingAddress: Address,
    val paymentStatus: PaymentStatus,
    val createdAt: Date,
    val shippedAt: Date?,
    val deliveredAt: Date?,
    val estimatedDelivery: Date?
)
```

### TrackingEvent (Firestore)
```kotlin
data class TrackingEvent(
    val id: String,
    val orderId: String,
    val type: TrackingEventType,
    val description: String,
    val location: LatLng?,
    val timestamp: Date,
    val done: Boolean
)
```

### OrderStatus (Enum)
```kotlin
enum class OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PREPARING,
    SHIPPED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
```

## Notificações

### Cloud Functions
1. `onOrderCreated` - Notifica loja
2. `onPaymentConfirmed` - Notifica cliente e loja
3. `onOrderShipped` - Notifica cliente e inicia rastreamento
4. `onOrderDelivered` - Notifica cliente e solicita avaliação

## Integrações

### Google Maps
- Rota estimada entre loja e cliente
- Localização em tempo real (se entregador usar app)
- Geofencing para detecção de chegada

### Firebase
- Firestore para dados de pedidos
- Cloud Functions para notificações
- Realtime Database para localização em tempo real (opcional)

